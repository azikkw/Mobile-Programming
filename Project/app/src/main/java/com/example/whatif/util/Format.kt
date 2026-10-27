package com.example.whatif.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

fun formatMoney(value: Double): String {
    val rounded = value.roundToLong()
    val digits = abs(rounded).toString().reversed().chunked(3).joinToString(",").reversed()
    val sign = if (rounded < 0) "\u2212" else ""
    return "$sign$digits \u20B8"
}

fun formatCompact(value: Double): String {
    val v = abs(value)
    return when {
        v >= 1_000_000_000 -> trimmed(value / 1_000_000_000) + "B"
        v >= 1_000_000 -> trimmed(value / 1_000_000) + "M"
        v >= 1_000 -> trimmed(value / 1_000) + "K"
        else -> trimmed(value)
    }
}

private fun trimmed(x: Double): String =
    String.format(Locale.US, "%.1f", x).removeSuffix(".0")

fun yearsText(n: Int): String = if (n == 1) "1 year" else "$n years"

fun formatPercent(value: Float): String =
    trimmed(value.toDouble()) + "%"
