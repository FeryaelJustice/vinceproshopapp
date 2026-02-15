package com.billiardsdraw.vinceproshop.data.remote

import com.billiardsdraw.vinceproshop.data.security.AuthTokenStore
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class KtorAdminApi(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val tokenStore: AuthTokenStore,
    private val json: Json,
) : AdminApi {
    override suspend fun orders(): List<OrderDto> {
        val token = requireToken()
        return httpClient
            .get(url("admin/orders")) {
                applyAuthHeaders(token)
            }.body()
    }

    override suspend fun updateOrderStatus(
        orderId: Int,
        status: String,
    ) {
        val token = requireToken()
        httpClient
            .put(url("admin/orders/$orderId/status")) {
                applyAuthHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(AdminOrderStatusUpdateRequestDto(status = status))
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun inventory(): List<AdminInventoryProductDto> {
        val token = requireToken()
        return httpClient
            .get(url("admin/inventory")) {
                applyAuthHeaders(token)
            }.body()
    }

    override suspend fun createInventoryProduct(
        payload: AdminProductUpsertRequestDto,
        images: List<AdminUploadImage>,
    ) {
        val token = requireToken()
        val content =
            MultiPartFormDataContent(
                formData {
                    append("data", json.encodeToString(AdminProductUpsertRequestDto.serializer(), payload))
                    images.forEach { image ->
                        append(
                            key = "images",
                            value = image.bytes,
                            headers = imagePartHeaders(fieldName = "images", image = image),
                        )
                    }
                },
            )

        httpClient
            .post(url("admin/inventory")) {
                applyAuthHeaders(token)
                setBody(content)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun updateInventoryProduct(
        productId: Int,
        payload: AdminProductUpsertRequestDto,
        images: List<AdminUploadImage>,
    ) {
        val token = requireToken()
        val content =
            MultiPartFormDataContent(
                formData {
                    append("data", json.encodeToString(AdminProductUpsertRequestDto.serializer(), payload))
                    images.forEach { image ->
                        append(
                            key = "images",
                            value = image.bytes,
                            headers = imagePartHeaders(fieldName = "images", image = image),
                        )
                    }
                },
            )

        httpClient
            .put(url("admin/inventory/$productId")) {
                applyAuthHeaders(token)
                setBody(content)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun discontinueInventoryProduct(productId: Int) {
        val token = requireToken()
        httpClient
            .delete(url("admin/inventory/$productId")) {
                applyAuthHeaders(token)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun stockInterestRequests(): List<AdminStockInterestRequestDto> {
        val token = requireToken()
        return httpClient
            .get(url("admin/inventory/stock-interest-requests")) {
                applyAuthHeaders(token)
            }.body()
    }

    override suspend fun sizes(): List<AdminSizeDto> {
        val token = requireToken()
        return httpClient
            .get(url("admin/inventory/sizes")) {
                applyAuthHeaders(token)
            }.body()
    }

    override suspend fun createSize(name: String) {
        val token = requireToken()
        httpClient
            .post(url("admin/inventory/sizes")) {
                applyAuthHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(AdminSizeUpsertRequestDto(name = name))
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun updateSize(
        sizeId: Int,
        name: String,
    ) {
        val token = requireToken()
        httpClient
            .put(url("admin/inventory/sizes/$sizeId")) {
                applyAuthHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(AdminSizeUpsertRequestDto(name = name))
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun deleteSize(sizeId: Int) {
        val token = requireToken()
        httpClient
            .delete(url("admin/inventory/sizes/$sizeId")) {
                applyAuthHeaders(token)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun categories(): List<CategoryDto> {
        val token = requireToken()
        return httpClient
            .get(url("admin/inventory/categories")) {
                applyAuthHeaders(token)
            }.body()
    }

    override suspend fun createCategory(
        id: String,
        name: String,
        nameEs: String,
        isCue: Boolean,
        parentId: String?,
        image: AdminUploadImage,
    ) {
        val token = requireToken()
        val content =
            MultiPartFormDataContent(
                formData {
                    append("id", id)
                    append("name", name)
                    append("name_es", nameEs)
                    append("is_cue", if (isCue) "1" else "0")
                    if (!parentId.isNullOrBlank()) {
                        append("parent_id", parentId)
                    }
                    append(
                        key = "image",
                        value = image.bytes,
                        headers = imagePartHeaders(fieldName = "image", image = image),
                    )
                },
            )

        httpClient
            .post(url("admin/inventory/categories")) {
                applyAuthHeaders(token)
                setBody(content)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun updateCategory(
        id: String,
        name: String,
        nameEs: String,
        isCue: Boolean,
        parentId: String?,
        image: AdminUploadImage?,
    ) {
        val token = requireToken()
        val content =
            MultiPartFormDataContent(
                formData {
                    append("name", name)
                    append("name_es", nameEs)
                    append("is_cue", if (isCue) "1" else "0")
                    append("parent_id", parentId.orEmpty())
                    image?.let {
                        append(
                            key = "image",
                            value = it.bytes,
                            headers = imagePartHeaders(fieldName = "image", image = it),
                        )
                    }
                },
            )

        httpClient
            .put(url("admin/inventory/categories/$id")) {
                applyAuthHeaders(token)
                setBody(content)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun deleteCategory(id: String) {
        val token = requireToken()
        httpClient
            .delete(url("admin/inventory/categories/$id")) {
                applyAuthHeaders(token)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun navbarConfig(): AdminNavbarConfigDto {
        val token = requireToken()
        return httpClient
            .get(url("admin/inventory/navbar")) {
                applyAuthHeaders(token)
            }.body()
    }

    override suspend fun updateNavbarConfig(
        rootVisibleLimit: Int,
        orderedRootCategoryIds: List<String>,
    ): AdminNavbarConfigDto {
        val token = requireToken()
        return httpClient
            .put(url("admin/inventory/navbar")) {
                applyAuthHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(
                    AdminNavbarUpdateRequestDto(
                        rootVisibleLimit = rootVisibleLimit,
                        orderedRootCategoryIds = orderedRootCategoryIds,
                    ),
                )
            }.body()
    }

    override suspend fun featured(): List<FeaturedSlideDto> {
        val token = requireToken()
        return httpClient
            .get(url("admin/inventory/featured")) {
                applyAuthHeaders(token)
            }.body()
    }

    override suspend fun createFeatured(payload: AdminFeaturedUpsertRequestDto) {
        val token = requireToken()
        httpClient
            .post(url("admin/inventory/featured")) {
                applyAuthHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun updateFeatured(
        featuredId: Int,
        payload: AdminFeaturedUpsertRequestDto,
    ) {
        val token = requireToken()
        httpClient
            .put(url("admin/inventory/featured/$featuredId")) {
                applyAuthHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun deleteFeatured(featuredId: Int) {
        val token = requireToken()
        httpClient
            .delete(url("admin/inventory/featured/$featuredId")) {
                applyAuthHeaders(token)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun crossSellRules(sourceType: String): List<AdminCrossSellRuleDto> {
        val token = requireToken()
        return httpClient
            .get(url("admin/inventory/cross-sell/rules?source_type=$sourceType")) {
                applyAuthHeaders(token)
            }.body()
    }

    override suspend fun createCrossSellRule(payload: AdminCrossSellRuleUpsertRequestDto) {
        val token = requireToken()
        httpClient
            .post(url("admin/inventory/cross-sell/rules")) {
                applyAuthHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun updateCrossSellRule(
        ruleId: Int,
        payload: AdminCrossSellRuleUpsertRequestDto,
    ) {
        val token = requireToken()
        httpClient
            .put(url("admin/inventory/cross-sell/rules/$ruleId")) {
                applyAuthHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun deleteCrossSellRule(ruleId: Int) {
        val token = requireToken()
        httpClient
            .delete(url("admin/inventory/cross-sell/rules/$ruleId")) {
                applyAuthHeaders(token)
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun updateCrossSellAnalyticsControl(
        productId: Int,
        isLocked: Boolean,
    ) {
        val token = requireToken()
        httpClient
            .put(url("admin/inventory/cross-sell/analytics/$productId/control")) {
                applyAuthHeaders(token)
                contentType(ContentType.Application.Json)
                setBody(AdminCrossSellAnalyticsControlRequestDto(isLocked = isLocked))
            }.body<AdminSimpleResponseDto>()
    }

    override suspend fun recomputeCrossSellAnalytics(productId: Int) {
        val token = requireToken()
        httpClient
            .post(url("admin/inventory/cross-sell/analytics/$productId/recompute")) {
                applyAuthHeaders(token)
            }.body<AdminSimpleResponseDto>()
    }

    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/$path"

    private suspend fun requireToken(): String =
        tokenStore.readToken()?.trim().orEmpty().ifBlank {
            throw IllegalStateException("Authentication required")
        }

    private fun HttpRequestBuilder.applyAuthHeaders(token: String) {
        header(HttpHeaders.Authorization, "Bearer $token")
        header(HttpHeaders.Cookie, "token=$token")
    }

    private fun imagePartHeaders(
        fieldName: String,
        image: AdminUploadImage,
    ): Headers =
        Headers.build {
            append(HttpHeaders.ContentType, image.mimeType.ifBlank { ContentType.Image.JPEG.toString() })
            append(
                HttpHeaders.ContentDisposition,
                ContentDisposition.File
                    .withParameter(ContentDisposition.Parameters.Name, fieldName)
                    .withParameter(ContentDisposition.Parameters.FileName, image.fileName)
                    .toString(),
            )
        }
}
