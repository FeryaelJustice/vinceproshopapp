package com.billiardsdraw.vinceproshop.presentation.admin

import com.billiardsdraw.vinceproshop.data.remote.CategoryDto
import com.billiardsdraw.vinceproshop.data.remote.ProductDto

fun ProductDto.localizedName(languageCode: String): String =
    if (languageCode.startsWith("es", ignoreCase = true)) {
        nameEs.ifBlank { name }
    } else {
        name
    }

fun CategoryDto.localizedName(languageCode: String): String =
    if (languageCode.startsWith("es", ignoreCase = true)) {
        nameEs.ifBlank { name }
    } else {
        name
    }

data class CategorySelectOption(
    val id: String,
    val label: String,
)

fun buildCategorySelectOptions(
    categories: List<CategoryDto>,
    languageCode: String,
    onlyLeaf: Boolean,
): List<CategorySelectOption> {
    val childrenByParent = categories.groupBy { it.parentId }
    val byId = categories.associateBy { it.id }
    val rootIds = categories.filter { it.parentId.isNullOrBlank() }.map { it.id }.sorted()

    fun breadcrumb(categoryId: String): String {
        val visited = mutableSetOf<String>()
        val path = mutableListOf<String>()
        var cursor: String? = categoryId
        while (!cursor.isNullOrBlank() && visited.add(cursor)) {
            val category = byId[cursor] ?: break
            path.add(category.localizedName(languageCode))
            cursor = category.parentId
        }
        return path.reversed().joinToString(" / ")
    }

    val options = mutableListOf<CategorySelectOption>()

    fun walk(categoryId: String) {
        val children = childrenByParent[categoryId].orEmpty().sortedBy { it.localizedName(languageCode) }
        val isLeaf = children.isEmpty()
        if (!onlyLeaf || isLeaf) {
            options += CategorySelectOption(id = categoryId, label = breadcrumb(categoryId))
        }
        children.forEach { walk(it.id) }
    }

    rootIds.forEach { walk(it) }
    return options
}

fun categoryBreadcrumb(
    categoryId: String,
    categories: List<CategoryDto>,
    languageCode: String,
): String {
    if (categoryId.isBlank()) return "-"
    val byId = categories.associateBy { it.id }
    val visited = mutableSetOf<String>()
    val labels = mutableListOf<String>()
    var cursor: String? = categoryId
    while (!cursor.isNullOrBlank() && visited.add(cursor)) {
        val category = byId[cursor] ?: break
        labels.add(category.localizedName(languageCode))
        cursor = category.parentId
    }
    if (labels.isEmpty()) return categoryId
    return labels.reversed().joinToString(" / ")
}
