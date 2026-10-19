package com.sanathanatattva.partner.alerts

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * "Open this order" requests from outside the nav graph (an accepted ring, a tapped
 * notification). MainActivity posts them; the nav host opens the order screen and clears it.
 */
object OpenOrder {
    const val EXTRA = "open_order_id"

    private val pending = MutableStateFlow<Long?>(null)
    val requests = pending.asStateFlow()

    fun post(orderId: Long) { pending.value = orderId }
    fun consumed() { pending.value = null }

    /** Pushes about a specific order; tapping one opens it (FCM puts its data in the intent as strings). */
    private val ORDER_TYPES = setOf("pack_request", "rider_assigned", "order_packed", "picked_up", "delivered", "returned", "returning")

    fun from(intent: Intent?) {
        intent ?: return
        intent.getLongExtra(EXTRA, -1L).takeIf { it > 0 }?.let { post(it); return }
        if (intent.getStringExtra("type") in ORDER_TYPES) {
            intent.getStringExtra("order_id")?.toLongOrNull()?.takeIf { it > 0 }?.let(::post)
        }
    }
}

/**
 * Offers that stopped being yours while ringing (another rider took it, the shop accepted on
 * the website, admin took over, time ran out). The ring screen listens and closes itself.
 */
object OfferEvents {
    private val _closed = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 8)
    val closed = _closed

    fun close(offerId: String) { _closed.tryEmit(offerId) }
}
