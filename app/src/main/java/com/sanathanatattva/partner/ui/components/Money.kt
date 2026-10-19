package com.sanathanatattva.partner.ui.components

import java.text.NumberFormat
import java.util.Locale

/** "₹900", "₹1,240", "₹87.50": Indian grouping, paise only when there are any. */
fun rupees(amount: Double): String {
    val f = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"))
    f.minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    f.maximumFractionDigits = 2
    return "₹" + f.format(amount)
}
