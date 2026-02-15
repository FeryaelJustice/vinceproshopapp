package com.billiardsdraw.vinceproshop.core

import kotlin.math.absoluteValue

fun formatEuro(value: Double): String {
    val normalized = if (value.isFinite()) value else 0.0
    val cents = (normalized * 100.0).toInt()
    val whole = cents / 100
    val fraction = cents.absoluteValue % 100
    return "€$whole.${fraction.toString().padStart(2, '0')}"
}
