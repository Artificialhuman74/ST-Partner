package com.sanathanatattva.partner.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanathanatattva.partner.R
import com.sanathanatattva.partner.alerts.AlertReadiness
import com.sanathanatattva.partner.alerts.OnlineService
import com.sanathanatattva.partner.alerts.Readiness
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ActiveWork
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.PartnerHome
import com.sanathanatattva.partner.data.api.StartedFrom
import com.sanathanatattva.partner.data.partner.StartPoint
import com.sanathanatattva.partner.ui.signup.currentLocation
import com.sanathanatattva.partner.ui.signup.hasLocationPermission
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val container = app.appContainer

    var home by mutableStateOf(container.partner.lastHome)
        private set
    var readiness by mutableStateOf(AlertReadiness.check(app))
        private set
    var busy by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    /** What [failed] says: couldn't go online (also used when Home won't load) or couldn't go offline. */
    var failedMessage by mutableStateOf(R.string.home_offline_failed)
        private set
    /** The phone's last fix, for "You're here" / distances in the picker. */
    var here by mutableStateOf<Pair<Double, Double>?>(null)
        private set
    var autoOfflineDismissed by mutableStateOf(false)
    /** Orders this partner is on right now, and offers still waiting for an answer. */
    var active by mutableStateOf<ActiveWork?>(null)
        private set

    fun refresh(onSignedOut: () -> Unit) {
        readiness = AlertReadiness.check(getApplication())
        viewModelScope.launch {
            when (val r = container.partner.home()) {
                is ApiResult.Success -> {
                    // A slide is still being sent: the card already shows where it's going.
                    if (switching > 0) return@launch
                    home = r.value
                    // Keep the location service in step with the server (it may have been killed).
                    if (r.value.online && !OnlineService.running.value && hasLocationPermission(getApplication())) {
                        OnlineService.start(getApplication())
                    }
                    if (!r.value.online && OnlineService.running.value) OnlineService.stopOnline(getApplication())
                }
                is ApiResult.Failure -> {
                    val p = r.problem
                    if (p is ApiProblem.Server && p.code == 401) {
                        container.auth.logOut()
                        onSignedOut()
                    }
                }
            }
        }
        if (hasLocationPermission(getApplication())) viewModelScope.launch { here = currentLocation(getApplication()) }
        viewModelScope.launch { (container.orders.active() as? ApiResult.Success)?.let { active = it.value } }
    }

    /** A fresh fix for the "near you" map while online. */
    fun refreshHere() {
        if (hasLocationPermission(getApplication())) viewModelScope.launch { currentLocation(getApplication())?.let { here = it } }
    }

    /**
     * "Go online" before Home has loaded (slow network, just back from the ring): load it,
     * then [onLoaded] opens the start-place picker. A failure shows the retry line instead
     * of the slide silently doing nothing.
     */
    fun loadHomeThen(onLoaded: () -> Unit) {
        if (busy) return
        busy = true
        failed = false
        viewModelScope.launch {
            try {
                val r = withTimeoutOrNull(ACTION_TIMEOUT_MS) { container.partner.home() }
                if (r is ApiResult.Success) {
                    home = r.value
                    onLoaded()
                } else {
                    failedMessage = R.string.home_offline_failed
                    failed = true
                }
            } finally {
                busy = false
            }
        }
    }

    /*
     * The online switch is optimistic: the card flips the moment the partner slides or picks a
     * place, and the server is told in the background. Only a failure comes back to the screen
     * (the card flips back, with a line saying so). Server calls go out one at a time, in order,
     * and a reply that belongs to an older slide never overwrites a newer one.
     */
    private val switchLock = Mutex()
    private var switchGen = 0
    /** Switch calls still on their way; Home refreshes leave the optimistic card alone meanwhile. */
    private var switching = 0

    fun goOnline(from: StartPoint) {
        val before = home ?: return // the picker only opens once Home is loaded
        val gen = ++switchGen
        failed = false
        autoOfflineDismissed = true
        home = before.copy(
            online = true,
            startedFrom = when (from) {
                is StartPoint.Saved -> StartedFrom("place", from.place)
                is StartPoint.Here -> StartedFrom("live")
            },
            autoOfflineAt = null,
            missedOfferStreak = 0,
        )
        tellServer(gen, online = true, revertTo = before) { container.partner.goOnline(from, here?.first, here?.second) }
    }

    fun goOffline() {
        val before = home ?: return
        val gen = ++switchGen
        failed = false
        home = before.copy(online = false)
        OnlineService.stopOnline(getApplication())
        tellServer(gen, online = false, revertTo = before) { container.partner.goOffline() }
    }

    private fun tellServer(gen: Int, online: Boolean, revertTo: PartnerHome, call: suspend () -> ApiResult<*>) {
        switching++
        viewModelScope.launch {
            try {
                val r = switchLock.withLock { withTimeoutOrNull(ACTION_TIMEOUT_MS) { call() } }
                if (gen != switchGen) return@launch // a newer slide owns the card now
                if (r is ApiResult.Success) {
                    // Started only once the server agrees: its location pings get a 409 (and stop
                    // the service) while the server still thinks the partner is offline.
                    if (online) OnlineService.start(getApplication())
                    // Pick up what only the server knows (online since, today's numbers), quietly.
                    (container.partner.home() as? ApiResult.Success)?.value?.let { if (gen == switchGen) home = it }
                } else {
                    home = revertTo
                    if (revertTo.online && hasLocationPermission(getApplication())) OnlineService.start(getApplication())
                    if (!revertTo.online) OnlineService.stopOnline(getApplication())
                    failedMessage = if (online) R.string.home_offline_failed else R.string.home_go_offline_failed
                    failed = true
                }
            } finally {
                switching--
            }
        }
    }

    val ready: Readiness get() = readiness

    private companion object {
        const val ACTION_TIMEOUT_MS = 25_000L
    }
}
