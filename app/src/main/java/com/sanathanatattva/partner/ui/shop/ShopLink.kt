package com.sanathanatattva.partner.ui.shop

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where a shop alert (or its button) leads inside My Shop. */
sealed interface ShopTarget {
    data object AskStock : ShopTarget
    data object Money : ShopTarget
    data object Orders : ShopTarget
    data class SubDealer(val id: Long) : ShopTarget
}

/**
 * "Open this part of My Shop" requests from outside the nav graph (a tapped shop alert).
 * MainActivity posts them; the nav host opens the screen once signed in and clears it.
 */
object ShopLink {
    const val EXTRA = "shop_target"
    const val EXTRA_ID = "shop_target_id"

    private val pending = MutableStateFlow<ShopTarget?>(null)
    val requests = pending.asStateFlow()

    fun consumed() { pending.value = null }

    fun from(intent: Intent?) {
        intent ?: return
        val target = when (intent.getStringExtra(EXTRA)) {
            "ask" -> ShopTarget.AskStock
            "money" -> ShopTarget.Money
            "orders" -> ShopTarget.Orders
            "sub" -> intent.getLongExtra(EXTRA_ID, -1L).takeIf { it > 0 }?.let { ShopTarget.SubDealer(it) }
            else -> null
        } ?: return
        pending.value = target
    }

    /** The extras that open [target], for a notification's tap or button. */
    fun extras(intent: Intent, target: ShopTarget): Intent = when (target) {
        ShopTarget.AskStock -> intent.putExtra(EXTRA, "ask")
        ShopTarget.Money -> intent.putExtra(EXTRA, "money")
        ShopTarget.Orders -> intent.putExtra(EXTRA, "orders")
        is ShopTarget.SubDealer -> intent.putExtra(EXTRA, "sub").putExtra(EXTRA_ID, target.id)
    }
}
