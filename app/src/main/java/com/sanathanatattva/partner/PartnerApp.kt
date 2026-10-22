package com.sanathanatattva.partner

import android.app.Application
import android.content.Context
import com.sanathanatattva.partner.data.AuthRepository
import com.sanathanatattva.partner.data.GoogleSignIn
import com.sanathanatattva.partner.data.SessionStore
import com.sanathanatattva.partner.data.SettingsStore
import com.sanathanatattva.partner.alerts.OnlineService
import com.sanathanatattva.partner.alerts.OrderChannel
import com.sanathanatattva.partner.data.api.ApiClient
import com.sanathanatattva.partner.data.orders.OrderRepository
import com.sanathanatattva.partner.data.partner.PartnerRepository
import com.sanathanatattva.partner.data.shop.ShopRepository
import com.sanathanatattva.partner.data.signup.BankLookup
import com.sanathanatattva.partner.data.signup.RiderRepository
import com.sanathanatattva.partner.data.signup.SignupDraftStore
import com.sanathanatattva.partner.push.Channels
import com.sanathanatattva.partner.push.PushRegistrar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PartnerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // The map library starts with the first map (LiveMap), not with every launch.
        Channels.create(this)
        OrderChannel.ensure(this)
        OnlineService.ensureChannel(this)
        // Keep the server's copy of this phone's push token fresh for a saved login.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { container.push.register() }
    }
}

/** Hand-wired dependencies. Small enough that a DI framework would cost more than it saves. */
class AppContainer(context: Context) {
    val settings = SettingsStore(context)
    val sessions = SessionStore(context)
    val google = GoogleSignIn(context)
    val api by lazy { ApiClient.create(context.cacheDir) { sessions.token } }
    val push by lazy { PushRegistrar(context, api, sessions) }
    val partner by lazy { PartnerRepository(api) }
    val orders by lazy { OrderRepository(api) }
    val shop by lazy { ShopRepository(api) }
    val auth by lazy { AuthRepository(api, sessions, google, push, partner, shop) }
    val riders by lazy { RiderRepository(api, sessions, google, SignupDraftStore(context), push) }
    val banks by lazy { BankLookup() }
}

val Context.appContainer: AppContainer
    get() = (applicationContext as PartnerApp).container
