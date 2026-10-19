package com.sanathanatattva.partner.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanathanatattva.partner.alerts.AlertReadiness
import com.sanathanatattva.partner.alerts.OnlineService
import com.sanathanatattva.partner.alerts.Readiness
import com.sanathanatattva.partner.appContainer
import com.sanathanatattva.partner.data.api.ApiProblem
import com.sanathanatattva.partner.data.api.ActiveWork
import com.sanathanatattva.partner.data.api.ApiResult
import com.sanathanatattva.partner.data.api.PartnerHome
import com.sanathanatattva.partner.data.partner.StartPoint
import com.sanathanatattva.partner.ui.signup.currentLocation
import com.sanathanatattva.partner.ui.signup.hasLocationPermission
import kotlinx.coroutines.launch
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
                if (r is ApiResult.Success) { home = r.value; onLoaded() } else failed = true
            } finally {
                busy = false
            }
        }
    }

    fun goOnline(from: StartPoint) {
        if (busy) return
        busy = true
        failed = false
        viewModelScope.launch {
            // Whatever happens (timeout, error), the switch is never left busy.
            try {
                when (withTimeoutOrNull(ACTION_TIMEOUT_MS) { container.partner.goOnline(from, here?.first, here?.second) }) {
                    is ApiResult.Success -> {
                        OnlineService.start(getApplication())
                        autoOfflineDismissed = true
                        home = (withTimeoutOrNull(ACTION_TIMEOUT_MS) { container.partner.home() } as? ApiResult.Success)?.value
                            ?: home?.copy(online = true)
                    }
                    else -> failed = true
                }
            } finally {
                busy = false
            }
        }
    }

    fun goOffline() {
        if (busy) return
        busy = true
        failed = false
        viewModelScope.launch {
            try {
                withTimeoutOrNull(ACTION_TIMEOUT_MS) { container.partner.goOffline() }
                OnlineService.stopOnline(getApplication())
                home = (withTimeoutOrNull(ACTION_TIMEOUT_MS) { container.partner.home() } as? ApiResult.Success)?.value
                    ?: home?.copy(online = false)
            } finally {
                busy = false
            }
        }
    }

    val ready: Readiness get() = readiness

    private companion object {
        const val ACTION_TIMEOUT_MS = 25_000L
    }
}
