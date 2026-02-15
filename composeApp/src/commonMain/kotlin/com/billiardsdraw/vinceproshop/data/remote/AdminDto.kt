package com.billiardsdraw.vinceproshop.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminInventorySizeDto(
    @SerialName("product_size_id")
    @Serializable(with = LenientIntSerializer::class)
    val productSizeId: Int = 0,
    @SerialName("size_id")
    @Serializable(with = LenientIntSerializer::class)
    val sizeId: Int = 0,
    @SerialName("size_name")
    val sizeName: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val quantity: Int = 0,
    @Serializable(with = LenientDoubleSerializer::class)
    val price: Double = 0.0,
    @Serializable(with = LenientDoubleSerializer::class)
    val discount: Double = 0.0,
)

@Serializable
data class AdminInventoryMediaDto(
    @Serializable(with = LenientIntSerializer::class)
    val position: Int = 0,
    val url: String = "",
)

@Serializable
data class AdminInventoryProductDto(
    @Serializable(with = LenientIntSerializer::class)
    val id: Int = 0,
    val slug: String = "",
    val name: String = "",
    @SerialName("name_es")
    val nameEs: String = "",
    val vendor: String = "",
    @Serializable(with = LenientDoubleSerializer::class)
    val price: Double = 0.0,
    @SerialName("category_id")
    val categoryId: String = "",
    val description: String = "",
    @SerialName("description_es")
    val descriptionEs: String = "",
    @Serializable(with = LenientIntSerializer::class)
    val discontinued: Int = 0,
    val sizes: List<AdminInventorySizeDto> = emptyList(),
    val media: List<AdminInventoryMediaDto> = emptyList(),
    @SerialName("stock_interest_count")
    @Serializable(with = LenientIntSerializer::class)
    val stockInterestCount: Int = 0,
)

@Serializable
data class AdminStockInterestRequestDto(
    @Serializable(with = LenientIntSerializer::class)
    val id: Int = 0,
    @SerialName("product_id")
    @Serializable(with = LenientIntSerializer::class)
    val productId: Int = 0,
    @SerialName("product_slug")
    val productSlug: String = "",
    @SerialName("product_name")
    val productName: String = "",
    @SerialName("product_name_es")
    val productNameEs: String = "",
    @SerialName("size_label")
    val sizeLabel: String = "",
    @SerialName("requester_name")
    val requesterName: String = "",
    @SerialName("requester_email")
    val requesterEmail: String = "",
    @SerialName("requester_phone")
    val requesterPhone: String? = null,
    @SerialName("created_at")
    val createdAt: String = "",
)

@Serializable
data class AdminSizeDto(
    @Serializable(with = LenientIntSerializer::class)
    val id: Int = 0,
    val name: String = "",
)

@Serializable
data class AdminNavbarConfigDto(
    @SerialName("root_visible_limit")
    @Serializable(with = LenientIntSerializer::class)
    val rootVisibleLimit: Int = 6,
    @SerialName("root_categories")
    val rootCategories: List<AdminNavbarCategoryDto> = emptyList(),
)

@Serializable
data class AdminNavbarCategoryDto(
    val id: String = "",
    val name: String = "",
    @SerialName("name_es")
    val nameEs: String = "",
    val image: String = "",
    @SerialName("parent_id")
    val parentId: String? = null,
    @SerialName("is_cue")
    @Serializable(with = LenientIntSerializer::class)
    val isCue: Int = 0,
    @SerialName("sort_order")
    @Serializable(with = LenientNullableIntSerializer::class)
    val sortOrder: Int? = null,
)

@Serializable
data class AdminNavbarUpdateRequestDto(
    @SerialName("root_visible_limit")
    val rootVisibleLimit: Int,
    @SerialName("ordered_root_category_ids")
    val orderedRootCategoryIds: List<String>,
)

@Serializable
data class AdminOrderStatusUpdateRequestDto(
    val status: String,
)

@Serializable
data class AdminSizeUpsertRequestDto(
    val name: String,
)

@Serializable
data class AdminFeaturedUpsertRequestDto(
    @SerialName("target_type")
    val targetType: String,
    @SerialName("product_id")
    val productId: Int?,
    @SerialName("category_id")
    val categoryId: String?,
    val title: String,
    @SerialName("title_es")
    val titleEs: String,
    val subtitle: String,
    @SerialName("subtitle_es")
    val subtitleEs: String,
    @SerialName("sort_order")
    val sortOrder: Int,
    @SerialName("is_active")
    val isActive: Int,
)

@Serializable
data class AdminCrossSellRuleItemDto(
    @Serializable(with = LenientIntSerializer::class)
    val id: Int = 0,
    @SerialName("product_id")
    @Serializable(with = LenientIntSerializer::class)
    val productId: Int = 0,
    @SerialName("product_name")
    val productName: String? = null,
    @SerialName("product_name_es")
    val productNameEs: String? = null,
    @SerialName("product_slug")
    val productSlug: String? = null,
    @SerialName("sort_order")
    @Serializable(with = LenientIntSerializer::class)
    val sortOrder: Int = 0,
    @Serializable(with = LenientNullableDoubleSerializer::class)
    val score: Double? = null,
    @SerialName("is_active")
    @Serializable(with = LenientIntSerializer::class)
    val isActive: Int = 1,
)

@Serializable
data class AdminCrossSellRuleDto(
    @Serializable(with = LenientIntSerializer::class)
    val id: Int = 0,
    @SerialName("source_type")
    val sourceType: String = "manual",
    @SerialName("trigger_type")
    val triggerType: String = "product",
    @SerialName("trigger_product_id")
    @Serializable(with = LenientNullableIntSerializer::class)
    val triggerProductId: Int? = null,
    @SerialName("trigger_category_id")
    val triggerCategoryId: String? = null,
    @SerialName("trigger_product_name")
    val triggerProductName: String? = null,
    @SerialName("trigger_product_slug")
    val triggerProductSlug: String? = null,
    @SerialName("trigger_category_name")
    val triggerCategoryName: String? = null,
    val name: String = "",
    val description: String? = null,
    @SerialName("max_suggestions")
    @Serializable(with = LenientIntSerializer::class)
    val maxSuggestions: Int = 10,
    @Serializable(with = LenientIntSerializer::class)
    val priority: Int = 100,
    @SerialName("is_active")
    @Serializable(with = LenientIntSerializer::class)
    val isActive: Int = 1,
    @SerialName("valid_from")
    val validFrom: String? = null,
    @SerialName("valid_to")
    val validTo: String? = null,
    @SerialName("is_locked")
    @Serializable(with = LenientNullableIntSerializer::class)
    val isLocked: Int? = null,
    val items: List<AdminCrossSellRuleItemDto> = emptyList(),
)

@Serializable
data class AdminCrossSellRuleUpsertItemDto(
    @SerialName("product_id")
    val productId: Int,
    @SerialName("sort_order")
    val sortOrder: Int,
    val score: Double?,
    @SerialName("is_active")
    val isActive: Int,
)

@Serializable
data class AdminCrossSellRuleUpsertRequestDto(
    @SerialName("source_type")
    val sourceType: String,
    @SerialName("trigger_type")
    val triggerType: String,
    @SerialName("trigger_product_id")
    val triggerProductId: Int?,
    @SerialName("trigger_category_id")
    val triggerCategoryId: String?,
    val name: String,
    val description: String?,
    @SerialName("max_suggestions")
    val maxSuggestions: Int,
    val priority: Int,
    @SerialName("is_active")
    val isActive: Int,
    @SerialName("valid_from")
    val validFrom: String?,
    @SerialName("valid_to")
    val validTo: String?,
    val items: List<AdminCrossSellRuleUpsertItemDto>,
)

@Serializable
data class AdminCrossSellAnalyticsControlRequestDto(
    @SerialName("is_locked")
    val isLocked: Boolean,
)

@Serializable
data class AdminInventorySizeInputDto(
    @SerialName("size_id")
    val sizeId: Int,
    val quantity: Int,
    val price: Double,
    val discount: Double,
)

@Serializable
data class AdminMediaPlanItemDto(
    val type: String,
    val url: String? = null,
    val fileIndex: Int? = null,
)

@Serializable
data class AdminProductUpsertRequestDto(
    val name: String,
    @SerialName("name_es")
    val nameEs: String,
    val description: String,
    @SerialName("description_es")
    val descriptionEs: String,
    val vendor: String,
    @SerialName("category_id")
    val categoryId: String,
    val price: Double,
    val sizes: List<AdminInventorySizeInputDto>,
    val mediaPlan: List<AdminMediaPlanItemDto>,
)

@Serializable
data class AdminSimpleResponseDto(
    val message: String = "",
    @Serializable(with = LenientNullableIntSerializer::class)
    val id: Int? = null,
)

data class AdminUploadImage(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as AdminUploadImage

        if (fileName != other.fileName) return false
        if (mimeType != other.mimeType) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}
