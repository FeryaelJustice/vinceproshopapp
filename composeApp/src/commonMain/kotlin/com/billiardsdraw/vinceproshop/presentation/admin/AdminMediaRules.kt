package com.billiardsdraw.vinceproshop.presentation.admin

const val PRODUCT_IMAGE_MIN_COUNT: Int = 1
const val PRODUCT_IMAGE_MAX_COUNT: Int = 20
const val PRODUCT_IMAGE_MAX_FILE_SIZE_MB: Int = 20
const val CATEGORY_IMAGE_MAX_FILE_SIZE_MB: Int = 10

val ALLOWED_IMAGE_MIME_TYPES: Set<String> = setOf(
    "image/jpeg",
    "image/jpg",
    "image/png",
    "image/webp",
    "image/avif",
)

enum class AdminImageValidationError {
    Empty,
    UnsupportedType,
    TooLarge,
}

fun availableProductImageSlots(currentCount: Int): Int {
    return (PRODUCT_IMAGE_MAX_COUNT - currentCount).coerceAtLeast(0)
}

fun validatePickedImage(
    bytes: ByteArray,
    maxFileSizeMb: Int,
): AdminImageValidationError? {
    if (bytes.isEmpty()) return AdminImageValidationError.Empty
    val maxBytes = maxFileSizeMb * 1024 * 1024
    if (bytes.size > maxBytes) return AdminImageValidationError.TooLarge
    val mimeType = detectImageMimeType(bytes) ?: return AdminImageValidationError.UnsupportedType
    if (!ALLOWED_IMAGE_MIME_TYPES.contains(mimeType.lowercase())) {
        return AdminImageValidationError.UnsupportedType
    }
    return null
}

fun detectImageMimeType(bytes: ByteArray): String? {
    if (bytes.size >= 3 &&
        bytes[0] == 0xFF.toByte() &&
        bytes[1] == 0xD8.toByte() &&
        bytes[2] == 0xFF.toByte()
    ) {
        return "image/jpeg"
    }
    if (bytes.size >= 8 &&
        bytes[0] == 0x89.toByte() &&
        bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x4E.toByte() &&
        bytes[3] == 0x47.toByte() &&
        bytes[4] == 0x0D.toByte() &&
        bytes[5] == 0x0A.toByte() &&
        bytes[6] == 0x1A.toByte() &&
        bytes[7] == 0x0A.toByte()
    ) {
        return "image/png"
    }
    if (bytes.size >= 12 &&
        bytes[0] == 0x52.toByte() &&
        bytes[1] == 0x49.toByte() &&
        bytes[2] == 0x46.toByte() &&
        bytes[3] == 0x46.toByte() &&
        bytes[8] == 0x57.toByte() &&
        bytes[9] == 0x45.toByte() &&
        bytes[10] == 0x42.toByte() &&
        bytes[11] == 0x50.toByte()
    ) {
        return "image/webp"
    }
    if (bytes.size >= 12 &&
        bytes[4] == 0x66.toByte() &&
        bytes[5] == 0x74.toByte() &&
        bytes[6] == 0x79.toByte() &&
        bytes[7] == 0x70.toByte()
    ) {
        val brand = byteArrayOf(bytes[8], bytes[9], bytes[10], bytes[11]).decodeToString().lowercase()
        if (brand == "avif" || brand == "avis") {
            return "image/avif"
        }
    }
    return null
}

fun fileExtensionForImageMimeType(mimeType: String): String {
    return when (mimeType.lowercase()) {
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/avif" -> "avif"
        else -> "jpg"
    }
}
