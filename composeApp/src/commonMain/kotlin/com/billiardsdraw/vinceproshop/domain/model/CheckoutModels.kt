package com.billiardsdraw.vinceproshop.domain.model

data class CheckoutCustomerInfo(
    val name: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val zipCode: String = "",
    val country: String = "",
    val phone: String = "",
)

data class CheckoutLineItem(
    val slug: String,
    val size: String,
    val quantity: Int,
)

data class CheckoutPaymentIntentPayload(
    val items: List<CheckoutLineItem>,
    val currency: String,
    val customerEmail: String,
    val customerName: String,
    val address: String,
    val country: String,
    val phone: String,
    val locale: String? = null,
)

data class CheckoutPaymentIntent(
    val clientSecret: String,
    val amount: Double,
)

fun CheckoutCustomerInfo.isValid(): Boolean {
    return name.isNotBlank() &&
        email.isNotBlank() &&
        address.isNotBlank() &&
        city.isNotBlank() &&
        zipCode.isNotBlank() &&
        country.isNotBlank() &&
        phone.isNotBlank()
}

fun CheckoutCustomerInfo.fullAddress(): String {
    return listOf(address, city, zipCode)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString(", ")
}

fun paymentIntentIdFromClientSecret(clientSecret: String): String {
    val trimmed = clientSecret.trim()
    val marker = "_secret_"
    val markerIndex = trimmed.indexOf(marker)
    return if (markerIndex > 0) trimmed.substring(0, markerIndex) else ""
}
