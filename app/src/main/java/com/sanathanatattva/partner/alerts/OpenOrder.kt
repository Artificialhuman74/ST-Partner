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

    fun from(intent: Intent?) {
        intent?.getLongExtra(EXTRA, -1L)?.takeIf { it > 0 }?.let(::post)
    }
}
