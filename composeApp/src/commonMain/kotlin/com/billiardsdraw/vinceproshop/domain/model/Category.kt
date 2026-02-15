package com.billiardsdraw.vinceproshop.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val nameEs: String,
    val imageUrl: String,
    val parentId: String?,
    val isCue: Boolean,
)

fun Category.localizedName(languageCode: String): String =
    if (languageCode.startsWith("es", ignoreCase = true)) {
        nameEs.ifBlank {
            name
        }
    } else {
        name
    }
