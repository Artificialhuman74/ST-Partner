package com.sanathanatattva.partner.ui.order

import android.app.Application
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.sanathanatattva.partner.alerts.OnlineService
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.OrderCard
import com.sanathanatattva.partner.data.api.apiCall
import com.sanathanatattva.partner.ui.login.UiText
import com.sanathanatattva.partner.ui.login.toUiText
import com.sanathanatattva.partner.ui.signup.currentLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** One order on screen: polls it so the other side's moves (rider accepts, shop types the code) show up by themselves. */
class OrderViewModel(app: Application, saved: SavedStateHandle) : AndroidViewModel(app) {
    private val container = app.appContainer
    val orderId: Long = checkNotNull(saved["orderId"])

    var order by mutableStateOf<OrderCard?>(null)
        private set
    /** We were on this order but aren't any more (e.g. a rider after handing a failed order back). */
    var gone by mutableStateOf(false)
        private set
    var me by mutableStateOf<Pair<Double, Double>?>(null)
        private set
    var riderPhoto by mutableStateOf<ImageBitmap?>(null)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<UiText?>(null)
    /** Set when delivery is confirmed: drives the Delivered celebration. */
    var earned by mutableStateOf<Double?>(null)
        private set
    var todayDeliveries by mutableStateOf<Int?>(null)
        private set
    var todayEarned by mutableStateOf<Double?>(null)
        private set
    /** A just-finished step whose button shows a tick before the screen moves on. */
    var doneFlash by mutableStateOf(false)
        private set

    private var poller: Job? = null
    private var lastStage: String? = null

    fun start() {
        if (poller?.isActive == true) return
        poller = viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(if (order?.stage in setOf("rider_assigned", "returning", "rider_search")) 3_000 else 6_000)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                currentLocation(getApplication())?.let { me = it }
                delay(10_000)
            }
        }
    }

    fun stop() {
        poller?.cancel()
    }

    suspend fun refresh() {
        when (val r = container.orders.order(orderId)) {
            is ApiResult.Success -> {
                order = r.value
                gone = false
                if (r.value.stage != lastStage) onStageChanged(r.value)
                lastStage = r.value.stage
            }
            is ApiResult.Failure -> {
                val p = r.problem
                if (p is ApiProblem.Server && (p.code == 403 || p.code == 404) && order != null) gone = true
            }
        }
    }

    private fun onStageChanged(o: OrderCard) {
        val carrying = (o.role == "rider" && o.stage in setOf("rider_assigned", "picked_up", "returning")) ||
            (o.role == "self" && o.deliveryStatus == "out_for_delivery")
        OnlineService.delivering(getApplication(), carrying)
        if (o.role == "packer" && o.rider?.hasPhoto == true && riderPhoto == null) loadRiderPhoto()
    }

    private fun loadRiderPhoto() = viewModelScope.launch {
        val r = apiCall { container.api.riderPhoto(orderId) }
        if (r is ApiResult.Success) {
            riderPhoto = withContext(Dispatchers.IO) {
                r.value.use { body -> BitmapFactory.decodeStream(body.byteStream())?.asImageBitmap() }
            }
        }
    }

    /** Runs a server step; on success flashes a tick + refreshes. Returns true on success. */
    private fun step(flash: Boolean = true, call: suspend () -> ApiResult<*>, then: suspend () -> Unit = {}) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            when (val r = call()) {
                is ApiResult.Success -> {
                    if (flash) {
                        doneFlash = true
                        delay(650)
                        doneFlash = false
                    }
                    then()
                    refresh()
                }
                is ApiResult.Failure -> error = r.problem.toUiText()
            }
            busy = false
        }
    }

    fun packed(startAfter: Boolean) = step(call = { container.orders.packed(orderId) }, then = {
        if (startAfter) container.orders.start(orderId)
    })

    fun startDelivery() = step(call = { container.orders.start(orderId) })
    fun handover(code: String, onWrong: () -> Unit) = codeStep({ container.orders.handover(orderId, code) }, onWrong)
    fun returned(code: String, onWrong: () -> Unit) = codeStep({ container.orders.returned(orderId, code) }, onWrong)

    private fun codeStep(call: suspend () -> ApiResult<*>, onWrong: () -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            when (val r = call()) {
                is ApiResult.Success -> {
                    doneFlash = true
                    delay(900)
                    doneFlash = false
                    refresh()
                }
                is ApiResult.Failure -> {
                    error = r.problem.toUiText()
                    onWrong()
                }
            }
            busy = false
        }
    }

    fun fail(reason: String, note: String?, onDone: () -> Unit) = step(flash = false, call = { container.orders.fail(orderId, reason, note) }, then = onDone)

    fun deliver(otp: String, photo: File, onWrongCode: () -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            when (val r = container.orders.deliver(orderId, otp, photo)) {
                is ApiResult.Success -> {
                    OnlineService.delivering(getApplication(), false)
                    (container.partner.home() as? ApiResult.Success)?.value?.today?.let {
                        todayDeliveries = it.deliveries
                        todayEarned = it.earned
                    }
                    earned = r.value.earned
                }
                is ApiResult.Failure -> {
                    error = r.problem.toUiText()
                    val p = r.problem
                    if (p is ApiProblem.Server && p.message?.contains("code", ignoreCase = true) == true) onWrongCode()
                }
            }
            busy = false
        }
    }

    /** The customer's code, checked before the door photo so a typo is caught right away. */
    fun checkCode(otp: String, onResult: (Boolean) -> Unit) {
        if (busy) return
        busy = true
        error = null
        viewModelScope.launch {
            when (val r = container.orders.checkCode(orderId, otp)) {
                is ApiResult.Success -> onResult(true)
                is ApiResult.Failure -> { error = r.problem.toUiText(); onResult(false) }
            }
            busy = false
        }
    }

    var resent by mutableStateOf(false)
        private set

    fun resendCode() = viewModelScope.launch {
        when (val r = container.orders.resendCode(orderId)) {
            is ApiResult.Success -> resent = true
            is ApiResult.Failure -> error = r.problem.toUiText()
        }
    }

    /** Fetches the customer's number only when the Call button is pressed (never shown on screen). */
    fun callCustomer(dial: (String) -> Unit) = viewModelScope.launch {
        when (val r = container.orders.customerPhone(orderId)) {
            is ApiResult.Success -> dial(r.value.phone)
            is ApiResult.Failure -> error = r.problem.toUiText()
        }
    }

    override fun onCleared() {
        poller?.cancel()
        super.onCleared()
    }
}
