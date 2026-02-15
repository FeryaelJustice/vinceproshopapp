package com.billiardsdraw.vinceproshop.data.remote

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

@Serializable
data class LoginRequestDto(
    val identifier: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    val role: String = "user",
    val token: String? = null,
    val jwt: String? = null,
    val accessToken: String? = null,
)

@Serializable
data class AuthUserDto(
    @Serializable(with = LenientIntSerializer::class) val id: Int = 0,
    val role: String = "user",
    val email: String = "",
    val username: String = "",
)

@Serializable
data class AuthSessionDto(
    val isAuthenticated: Boolean = false,
    val user: AuthUserDto? = null,
)

@Serializable
data class OrderItemDto(
    @Serializable(with = LenientIntSerializer::class) val quantity: Int = 0,
    @Serializable(with = LenientDoubleSerializer::class) val price: Double = 0.0,
    val size: String = "",
    val product_name: String = "",
)

@Serializable
data class OrderDto(
    @Serializable(with = LenientIntSerializer::class) val id: Int = 0,
    val transaction_id: String? = null,
    val status: String = "pending",
    @Serializable(with = LenientDoubleSerializer::class) val total: Double = 0.0,
    val address: String = "",
    val country: String = "",
    val customer_email: String = "",
    val customer_name: String = "",
    val created_at: String = "",
    val items: List<OrderItemDto> = emptyList(),
)

object LenientDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeDouble()
        return parseToDouble(jsonDecoder.decodeJsonElement())
    }

    override fun serialize(
        encoder: Encoder,
        value: Double,
    ) {
        encoder.encodeDouble(value)
    }
}

object LenientIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeInt()
        return parseToInt(jsonDecoder.decodeJsonElement())
    }

    override fun serialize(
        encoder: Encoder,
        value: Int,
    ) {
        encoder.encodeInt(value)
    }
}

private fun parseToDouble(element: JsonElement): Double {
    val primitive = element as? JsonPrimitive ?: return 0.0
    return primitive.doubleOrNull
        ?: primitive.content.toDoubleOrNull()
        ?: 0.0
}

private fun parseToInt(element: JsonElement): Int {
    val primitive = element as? JsonPrimitive ?: return 0
    return primitive.intOrNull
        ?: primitive.content.toIntOrNull()
        ?: 0
}
