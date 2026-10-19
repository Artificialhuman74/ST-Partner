package com.sanathanatattva.partner.alerts

import com.sanathanatattva.partner.data.api.OfferDetail
import com.sanathanatattva.partner.data.api.OrderCard
import com.sanathanatattva.partner.ui.components.rupees
import java.time.Instant

/** What the ring screen shows. Real offers come from the dispatch engine; test alerts use [sample]. */
enum class LineKind { Refill, New, Standard }

data class OfferLine(val qty: Int, val litres: String, val oil: String, val kind: LineKind)

data class Offer(
    val id: String,
    val forRider: Boolean,
    val test: Boolean,
    val lines: List<OfferLine>,
    val customer: String,
    val area: String,
    val distanceKm: String,
    /** Trader: their commission. Rider: the delivery fee. */
    val money: String,
    val pickupName: String? = null,
    val pickupKm: String? = null,
    val expiresAt: Long,
    /** What the server calls it (null for test alerts). */
    val offerId: Long? = null,
) {
    val cans: Int get() = lines.sumOf { it.qty }
    val totalLitres: String get() {
        val total = lines.sumOf { it.qty * (it.litres.toDoubleOrNull() ?: 0.0) }
        return if (total % 1.0 == 0.0) total.toInt().toString() else "%.1f".format(total)
    }

    companion object {
        fun from(detail: OfferDetail): Offer = from(detail.card, detail.offerId, detail.kind == "rider", detail.expiresAt)

        fun from(card: OrderCard, offerId: Long?, forRider: Boolean, expiresAt: String) = Offer(
            id = offerId?.toString() ?: card.orderId.toString(),
            forRider = forRider,
            test = false,
            lines = card.lines.map { l ->
                OfferLine(
                    qty = l.quantity,
                    litres = l.litres?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: l.size.orEmpty(),
                    oil = l.name,
                    kind = when (l.lineType) { "refill" -> LineKind.Refill; "new" -> LineKind.New; else -> LineKind.Standard },
                )
            },
            customer = card.customer.firstName,
            area = card.customer.area,
            distanceKm = km(if (forRider) card.shopToCustomerKm else card.distanceKm),
            money = rupees(card.earn),
            pickupName = card.shop?.let { listOf(it.name, it.area).filter(String::isNotBlank).joinToString(", ") },
            pickupKm = if (forRider) km(card.toShopKm) else null,
            expiresAt = runCatching { Instant.parse(expiresAt).toEpochMilli() }.getOrElse { System.currentTimeMillis() + 20 * 60_000L },
            offerId = offerId,
        )

        private fun km(v: Double?) = v?.let { "%.1f".format(it) } ?: "–"

        /** The canvas's sample order, used for "Send me a test alert". */
        fun sample(forRider: Boolean, id: String, windowMs: Long = 2 * 60_000L) = Offer(
            id = id,
            forRider = forRider,
            test = true,
            lines = listOf(
                OfferLine(2, "5", "Groundnut oil", LineKind.Refill),
                OfferLine(1, "2.8", "Coconut oil", LineKind.New),
            ),
            customer = "Lakshmi",
            area = "Jayanagar 4th Block",
            distanceKm = "3.2",
            money = if (forRider) "₹45" else "₹124",
            pickupName = if (forRider) "Ganesh Stores, 9th Block" else null,
            pickupKm = if (forRider) "1.1" else null,
            expiresAt = System.currentTimeMillis() + windowMs,
        )
    }
}
