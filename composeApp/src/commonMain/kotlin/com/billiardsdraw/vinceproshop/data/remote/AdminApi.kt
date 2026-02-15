package com.billiardsdraw.vinceproshop.data.remote

interface AdminApi {
    suspend fun orders(): List<OrderDto>

    suspend fun updateOrderStatus(
        orderId: Int,
        status: String,
    )

    suspend fun inventory(): List<AdminInventoryProductDto>

    suspend fun createInventoryProduct(
        payload: AdminProductUpsertRequestDto,
        images: List<AdminUploadImage>,
    )

    suspend fun updateInventoryProduct(
        productId: Int,
        payload: AdminProductUpsertRequestDto,
        images: List<AdminUploadImage>,
    )

    suspend fun discontinueInventoryProduct(productId: Int)

    suspend fun stockInterestRequests(): List<AdminStockInterestRequestDto>

    suspend fun sizes(): List<AdminSizeDto>

    suspend fun createSize(name: String)

    suspend fun updateSize(
        sizeId: Int,
        name: String,
    )

    suspend fun deleteSize(sizeId: Int)

    suspend fun categories(): List<CategoryDto>

    suspend fun createCategory(
        id: String,
        name: String,
        nameEs: String,
        isCue: Boolean,
        parentId: String?,
        image: AdminUploadImage,
    )

    suspend fun updateCategory(
        id: String,
        name: String,
        nameEs: String,
        isCue: Boolean,
        parentId: String?,
        image: AdminUploadImage?,
    )

    suspend fun deleteCategory(id: String)

    suspend fun navbarConfig(): AdminNavbarConfigDto

    suspend fun updateNavbarConfig(
        rootVisibleLimit: Int,
        orderedRootCategoryIds: List<String>,
    ): AdminNavbarConfigDto

    suspend fun featured(): List<FeaturedSlideDto>

    suspend fun createFeatured(payload: AdminFeaturedUpsertRequestDto)

    suspend fun updateFeatured(
        featuredId: Int,
        payload: AdminFeaturedUpsertRequestDto,
    )

    suspend fun deleteFeatured(featuredId: Int)

    suspend fun crossSellRules(sourceType: String = "all"): List<AdminCrossSellRuleDto>

    suspend fun createCrossSellRule(payload: AdminCrossSellRuleUpsertRequestDto)

    suspend fun updateCrossSellRule(
        ruleId: Int,
        payload: AdminCrossSellRuleUpsertRequestDto,
    )

    suspend fun deleteCrossSellRule(ruleId: Int)

    suspend fun updateCrossSellAnalyticsControl(
        productId: Int,
        isLocked: Boolean,
    )

    suspend fun recomputeCrossSellAnalytics(productId: Int)
}
