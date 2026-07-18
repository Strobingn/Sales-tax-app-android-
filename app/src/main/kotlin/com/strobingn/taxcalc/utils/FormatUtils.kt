package com.strobingn.taxcalc.utils

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatCurrency(amount: Double): String {
    val formatter = DecimalFormat("$###,###,##0.00")
    return formatter.format(amount)
}

fun formatTimestamp(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

fun getModeString(mode: String): String {
    return if (mode == "FORWARD") "Forward" else "Reverse"
}
