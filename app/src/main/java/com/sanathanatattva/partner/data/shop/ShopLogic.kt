package com.sanathanatattva.partner.data.shop

import com.sanathanatattva.partner.data.api.Commission
import com.sanathanatattva.partner.data.api.InventoryItem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/** How a product stands on the shelf (canvas "Stock · the oil shelf"). */
enum class Stock { Plenty, Low, Finished }

/** The container drawn on the shelf, picked from the pack size. */
enum class Pack { Bottle, Can, Tin }

/** Oil colour inside the container, from the oil's name (groundnut gold, coconut white, sesame amber…). */
enum class Oil { Groundnut, Sesame, Coconut, Sunflower, Mustard, Castor }

val InventoryItem.stock: Stock
    get() = when {
        quantity <= 0 -> Stock.Finished
        quantity <= alertAt -> Stock.Low
        else -> Stock.Plenty
    }

val InventoryItem.needsAttention: Boolean get() = stock != Stock.Plenty

/**
 * How full the container looks, 0..0.92. A full container is four times the alert level, so a
 * product sitting right at its alert level shows about a quarter full.
 */
val InventoryItem.level: Float
    get() {
        val full = max(alertAt * 4, 8)
        return (quantity.toFloat() / full).coerceIn(0f, 0.92f)
    }

/** Litres in one pack ("5L", "2.8L", "500 ml", or the size written in the product's name). */
fun litresOf(containerType: String?, name: String): Double? {
    val re = Regex("""(\d+(?:\.\d+)?)\s*(ml|l|lt|ltr|litre|liter|litres|liters)\b""", RegexOption.IGNORE_CASE)
    val m = re.find(containerType.orEmpty()) ?: re.find(name) ?: return null
    val n = m.groupValues[1].toDoubleOrNull() ?: return null
    return if (m.groupValues[2].lowercase() == "ml") n / 1000.0 else n
}

fun packOf(litres: Double?): Pack = when {
    litres == null || litres < 2.0 -> Pack.Bottle
    litres <= 10.0 -> Pack.Can
    else -> Pack.Tin
}

val InventoryItem.litres: Double? get() = litresOf(containerType, name)
val InventoryItem.pack: Pack get() = packOf(litres)

fun oilOf(name: String): Oil {
    val n = name.lowercase()
    return when {
        listOf("sesame", "gingelly", "til ", "ellu").any { it in "$n " } -> Oil.Sesame
        listOf("coconut", "kobbari", "thengai").any { it in n } -> Oil.Coconut
        "sunflower" in n -> Oil.Sunflower
        listOf("mustard", "sasive").any { it in n } -> Oil.Mustard
        listOf("castor", "haralu").any { it in n } -> Oil.Castor
        else -> Oil.Groundnut
    }
}

val InventoryItem.oil: Oil get() = oilOf(name)

/** The product's name without its size ("Groundnut Oil 5L" → "Groundnut Oil"), for the shelf label. */
fun shortName(name: String): String =
    name.replace(Regex("""\s*[-–(]?\s*\d+(?:\.\d+)?\s*(ml|l|lt|ltr|litre|liter|litres|liters|kg)\b\)?""", RegexOption.IGNORE_CASE), "")
        .trim().ifEmpty { name }

/** "5 L", "1 L", "500 ml". */
fun sizeLabel(litres: Double): String = when {
    litres < 1.0 -> "${(litres * 1000).roundToInt()} ml"
    litres % 1.0 == 0.0 -> "${litres.toInt()} L"
    else -> "${litres.toString().trimEnd('0')} L"
}

/** What "Ask for stock" fills in: enough to get back to three times the alert level. */
val InventoryItem.suggestedQuantity: Int get() = max(alertAt * 3 - quantity, 1)

/** "ORD-1792596838123-45" → "38123"; the website's own number is long. */
fun shortOrderNumber(orderNumber: String, id: Long): String =
    orderNumber.split('-').getOrNull(1)?.takeIf { it.isNotBlank() }?.takeLast(5) ?: id.toString()

/** An Indian mobile number with its country code, digits only, for WhatsApp ("9972922514" → "919972922514"). */
fun whatsappNumber(phone: String?): String? {
    val digits = phone.orEmpty().filter(Char::isDigit)
    return when {
        digits.length == 10 -> "91$digits"
        digits.length == 12 && digits.startsWith("91") -> digits
        digits.length == 11 && digits.startsWith("0") -> "91${digits.drop(1)}"
        digits.length >= 10 -> digits
        else -> null
    }
}

/** "+91 99xxx xxx14": enough to recognise the admin's number without showing all of it. */
fun maskedPhone(phone: String?): String {
    val digits = whatsappNumber(phone) ?: return ""
    val local = digits.takeLast(10)
    return "+91 ${local.take(2)}xxx xxx${local.takeLast(2)}"
}

/** "Jayanagar 4th Block" from a full address (same rule as the website's dispatch.areaOf). */
fun areaOf(address: String?): String {
    val parts = address.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
        .filter { !Regex("""^\d{6}$""").matches(it) && !Regex("bengaluru|bangalore|karnataka|india", RegexOption.IGNORE_CASE).containsMatchIn(it) }
    if (parts.size <= 1) return parts.firstOrNull().orEmpty()
    return parts.last().replace(Regex("""\s*\d{6}$"""), "")
}

/** The website stores times as "2026-10-19 06:32:43" in UTC (SQLite CURRENT_TIMESTAMP) or ISO. */
fun parseServerTime(text: String?): LocalDateTime? {
    text ?: return null
    return runCatching {
        if ('T' in text) java.time.OffsetDateTime.parse(text).atZoneSameInstant(Ist).toLocalDateTime()
        else LocalDateTime.parse(text.replace(' ', 'T')).atOffset(ZoneOffset.UTC).atZoneSameInstant(Ist).toLocalDateTime()
    }.getOrNull()
}

val Ist: ZoneId = ZoneId.of("Asia/Kolkata")

/** Monday of the week a day falls in (the website's commission weeks run Monday to Sunday). */
fun weekOf(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

data class WeekTotal(val start: LocalDate, val amount: Double)

/** The last [weeks] weeks of shop commission, oldest first, this week last. */
fun weeklyTotals(commissions: List<Commission>, weeks: Int = 6, today: LocalDate = LocalDate.now(Ist)): List<WeekTotal> {
    val thisWeek = weekOf(today)
    val starts = (weeks - 1 downTo 0).map { thisWeek.minusWeeks(it.toLong()) }
    val sums = commissions.groupBy { c -> parseServerTime(c.createdAt)?.toLocalDate()?.let(::weekOf) }
        .mapValues { (_, list) -> list.sumOf { it.amount } }
    return starts.map { WeekTotal(it, sums[it] ?: 0.0) }
}

/** Money that has left for the bank (the website's "Paid to bank"). */
val Commission.paidOut: Boolean get() = status == "paid" || status == "transferred"

fun dayMonth(date: LocalDate, locale: Locale): String = date.format(DateTimeFormatter.ofPattern("d MMM", locale))

/**
 * The WhatsApp message "Ask for stock" opens, always in English (whoever handles dispatch reads
 * it), canvas "Order saved → WhatsApp".
 */
fun restockMessage(
    traderName: String,
    shopArea: String,
    lines: List<Pair<String, Int>>,
    nowInShop: List<Pair<String, Int>>,
    orderRef: String,
    total: String,
): String = buildString {
    appendLine("Namaskara,")
    append("This is ").append(traderName)
    if (shopArea.isNotBlank()) append(", ").append(shopArea)
    appendLine(" (ST partner).")
    appendLine()
    appendLine("Please send:")
    lines.forEach { (name, qty) -> appendLine("• $name × $qty") }
    if (nowInShop.isNotEmpty()) {
        appendLine()
        appendLine("Now in my shop:")
        nowInShop.forEach { (name, left) -> appendLine("$name – ${if (left <= 0) "none left" else "$left left"}") }
    }
    appendLine()
    appendLine("Order #$orderRef · $total")
    append("— sent from ST Delivery app")
}
