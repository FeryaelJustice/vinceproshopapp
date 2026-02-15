package com.billiardsdraw.vinceproshop.presentation.admin.manage.inventory

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.billiardsdraw.vinceproshop.core.formatEuro
import com.billiardsdraw.vinceproshop.data.remote.AdminInventoryMediaDto
import com.billiardsdraw.vinceproshop.data.remote.AdminInventorySizeInputDto
import com.billiardsdraw.vinceproshop.data.remote.AdminMediaPlanItemDto
import com.billiardsdraw.vinceproshop.data.remote.AdminProductUpsertRequestDto
import com.billiardsdraw.vinceproshop.data.remote.AdminUploadImage
import com.billiardsdraw.vinceproshop.presentation.admin.AdminImageValidationError
import com.billiardsdraw.vinceproshop.presentation.admin.PRODUCT_IMAGE_MAX_COUNT
import com.billiardsdraw.vinceproshop.presentation.admin.PRODUCT_IMAGE_MAX_FILE_SIZE_MB
import com.billiardsdraw.vinceproshop.presentation.admin.PRODUCT_IMAGE_MIN_COUNT
import com.billiardsdraw.vinceproshop.presentation.admin.availableProductImageSlots
import com.billiardsdraw.vinceproshop.presentation.admin.buildCategorySelectOptions
import com.billiardsdraw.vinceproshop.presentation.admin.categoryBreadcrumb
import com.billiardsdraw.vinceproshop.presentation.admin.detectImageMimeType
import com.billiardsdraw.vinceproshop.presentation.admin.fileExtensionForImageMimeType
import com.billiardsdraw.vinceproshop.presentation.admin.validatePickedImage
import com.billiardsdraw.vinceproshop.presentation.common.tr
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.extensions.loadPainter
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private data class ProductSizeDraft(
    var sizeId: String,
    var quantity: String,
    var price: String,
    var discount: String,
)

private sealed interface ProductMediaDraft {
    data class Existing(
        val url: String,
    ) : ProductMediaDraft

    data class New(
        val photo: GalleryPhotoResult,
        var bytesCache: ByteArray? = null,
        var mimeTypeCache: String? = null,
    ) : ProductMediaDraft {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as New

            if (photo != other.photo) return false
            if (!bytesCache.contentEquals(other.bytesCache)) return false
            if (mimeTypeCache != other.mimeTypeCache) return false

            return true
        }

        override fun hashCode(): Int {
            var result = photo.hashCode()
            result = 31 * result + (bytesCache?.contentHashCode() ?: 0)
            result = 31 * result + (mimeTypeCache?.hashCode() ?: 0)
            return result
        }
    }
}

private data class ProductDraft(
    val id: Int,
    var name: String,
    var nameEs: String,
    var description: String,
    var descriptionEs: String,
    var vendor: String,
    var categoryId: String,
    var basePrice: String,
    var sizeRows: List<ProductSizeDraft>,
    var mediaItems: List<ProductMediaDraft>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageInventoryScreen(
    languageCode: String,
    modifier: Modifier = Modifier,
    viewModel: AdminManageInventoryViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    var localError by remember { mutableStateOf<String?>(null) }

    var draft by remember { mutableStateOf<ProductDraft?>(null) }
    var openImagePicker by remember { mutableStateOf(false) }
    val isSaving = state.isSaving

    val leafCategoryOptions =
        remember(state.categories, languageCode) {
            buildCategorySelectOptions(state.categories, languageCode = languageCode, onlyLeaf = true)
        }

    fun imageValidationMessage(reason: AdminImageValidationError): String =
        when (reason) {
            AdminImageValidationError.Empty -> {
                tr("Image file is empty", "El archivo de imagen esta vacio")
            }

            AdminImageValidationError.UnsupportedType -> {
                tr(
                    "Image type is not supported. Use JPG/PNG/WEBP/AVIF",
                    "Tipo de imagen no soportado. Usa JPG/PNG/WEBP/AVIF",
                )
            }

            AdminImageValidationError.TooLarge -> {
                tr(
                    "Image exceeds $PRODUCT_IMAGE_MAX_FILE_SIZE_MB MB",
                    "La imagen excede $PRODUCT_IMAGE_MAX_FILE_SIZE_MB MB",
                )
            }
        }

    fun appendPickedPhotos(photos: List<GalleryPhotoResult>) {
        val current = draft ?: return
        if (photos.isEmpty()) return
        val availableSlots = availableProductImageSlots(current.mediaItems.size)
        if (availableSlots <= 0) {
            localError =
                tr(
                    "Max $PRODUCT_IMAGE_MAX_COUNT images allowed",
                    "Maximo $PRODUCT_IMAGE_MAX_COUNT imagenes permitidas",
                )
            return
        }
        val accepted = photos.take(availableSlots).map { ProductMediaDraft.New(photo = it) }
        current.mediaItems = current.mediaItems + accepted
        draft = current.copy()
        localError = null
        if (photos.size > accepted.size) {
            localError =
                tr(
                    "Only $availableSlots additional images were added",
                    "Solo se agregaron $availableSlots imagenes adicionales",
                )
        }
    }

    if (openImagePicker) {
        val pickerMaxSelection =
            draft
                ?.let { availableProductImageSlots(it.mediaItems.size) }
                ?.coerceAtLeast(1)
                ?: PRODUCT_IMAGE_MAX_COUNT
        GalleryPickerLauncher(
            mimeTypes = listOf(MimeType.IMAGE_WEBP, MimeType.IMAGE_JPEG, MimeType.IMAGE_PNG),
            onPhotosSelected = { photos ->
                appendPickedPhotos(photos)
                openImagePicker = false
            },
            onError = {
                localError = it.message ?: tr("Image picker failed", "Fallo selector de imagen")
                openImagePicker = false
            },
            onDismiss = { openImagePicker = false },
            allowMultiple = true,
            selectionLimit = pickerMaxSelection.toLong(),
        )
    }

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("Manage Inventory", "Gestionar inventario"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = viewModel::refresh,
                    enabled = !state.isLoading && !isSaving,
                ) { Text(tr("Refresh", "Actualizar")) }
                Button(
                    enabled = !state.isLoading && !isSaving,
                    onClick = {
                        draft =
                            ProductDraft(
                                id = 0,
                                name = "",
                                nameEs = "",
                                description = "",
                                descriptionEs = "",
                                vendor = "",
                                categoryId = "",
                                basePrice = "0",
                                sizeRows =
                                    if (state.sizes.isEmpty()) {
                                        emptyList()
                                    } else {
                                        listOf(
                                            ProductSizeDraft(
                                                sizeId =
                                                    state.sizes
                                                        .first()
                                                        .id
                                                        .toString(),
                                                quantity = "0",
                                                price = "0",
                                                discount = "0",
                                            ),
                                        )
                                    },
                                mediaItems = emptyList(),
                            )
                    },
                ) { Text(tr("Add", "Agregar")) }
            }
        }

        when {
            state.isLoading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            (localError ?: state.error) != null -> {
                Text(
                    (localError ?: state.error).orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            state.products.isEmpty() -> {
                Text(tr("No products", "No hay productos"))
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.products, key = { it.id }) { product ->
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("${product.name} (${product.slug})", fontWeight = FontWeight.SemiBold)
                            Text(categoryBreadcrumb(product.categoryId, state.categories, languageCode))
                            Text(
                                "${
                                    tr(
                                        "Status",
                                        "Estado",
                                    )
                                }: ${
                                    if (product.discontinued == 1) {
                                        tr(
                                            "Discontinued",
                                            "Descontinuado",
                                        )
                                    } else {
                                        tr("Active", "Activo")
                                    }
                                }",
                            )
                            product.sizes.forEach { size ->
                                Text("- ${size.sizeName}: qty ${size.quantity}, ${formatEuro(size.price)}")
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    draft =
                                        ProductDraft(
                                            id = product.id,
                                            name = product.name,
                                            nameEs = product.nameEs,
                                            description = product.description,
                                            descriptionEs = product.descriptionEs,
                                            vendor = product.vendor,
                                            categoryId = product.categoryId,
                                            basePrice = product.price.toString(),
                                            sizeRows =
                                                product.sizes.map { size ->
                                                    ProductSizeDraft(
                                                        sizeId = size.sizeId.toString(),
                                                        quantity = size.quantity.toString(),
                                                        price = size.price.toString(),
                                                        discount = size.discount.toString(),
                                                    )
                                                },
                                            mediaItems =
                                                product.media
                                                    .sortedBy(AdminInventoryMediaDto::position)
                                                    .map { ProductMediaDraft.Existing(it.url) },
                                        )
                                }, enabled = !isSaving) {
                                    Text(tr("Edit", "Editar"))
                                }
                                TextButton(onClick = {
                                    viewModel.discontinueProduct(product.id)
                                }, enabled = !isSaving) {
                                    Text(
                                        tr("Discontinue", "Descontinuar"),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val activeDraft = draft
    if (activeDraft != null) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    draft = null
                }
            },
            title = {
                Text(
                    if (activeDraft.id == 0) {
                        tr(
                            "Add Product",
                            "Agregar producto",
                        )
                    } else {
                        tr("Edit Product", "Editar producto")
                    },
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = activeDraft.name,
                        onValueChange = {
                            activeDraft.name = it
                            draft = activeDraft.copy()
                        },
                        label = { Text(tr("Name", "Nombre")) },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.nameEs,
                        onValueChange = {
                            activeDraft.nameEs = it
                            draft = activeDraft.copy()
                        },
                        label = { Text("Name ES") },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.vendor,
                        onValueChange = {
                            activeDraft.vendor = it
                            draft = activeDraft.copy()
                        },
                        label = { Text(tr("Vendor", "Marca")) },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    var categoryExpanded by remember(activeDraft.id) { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = {
                            if (!isSaving) categoryExpanded = !categoryExpanded
                        },
                    ) {
                        val selectedCategoryLabel =
                            leafCategoryOptions.firstOrNull { it.id == activeDraft.categoryId }?.label
                                ?: if (activeDraft.categoryId.isBlank()) {
                                    tr("Select category", "Selecciona categoria")
                                } else {
                                    tr(
                                        "Legacy category: ${activeDraft.categoryId}",
                                        "Categoria legacy: ${activeDraft.categoryId}",
                                    )
                                }
                        OutlinedTextField(
                            value = selectedCategoryLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(tr("Category", "Categoria")) },
                            enabled = !isSaving,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier =
                                Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                        ) {
                            leafCategoryOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        categoryExpanded = false
                                        activeDraft.categoryId = option.id
                                        draft = activeDraft.copy()
                                    },
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = activeDraft.basePrice,
                        onValueChange = {
                            activeDraft.basePrice = it
                            draft = activeDraft.copy()
                        },
                        label = { Text(tr("Display price", "Precio base")) },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.description,
                        onValueChange = {
                            activeDraft.description = it
                            draft = activeDraft.copy()
                        },
                        label = { Text(tr("Description", "Descripcion")) },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.descriptionEs,
                        onValueChange = {
                            activeDraft.descriptionEs = it
                            draft = activeDraft.copy()
                        },
                        label = { Text("Description ES") },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text(
                        tr("Sizes and pricing", "Tallas y precios"),
                        fontWeight = FontWeight.SemiBold,
                    )
                    activeDraft.sizeRows.forEachIndexed { index, row ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            var sizeExpanded by remember(activeDraft.id, index) {
                                mutableStateOf(
                                    false,
                                )
                            }
                            ExposedDropdownMenuBox(
                                expanded = sizeExpanded,
                                onExpandedChange = {
                                    if (!isSaving) sizeExpanded = !sizeExpanded
                                },
                            ) {
                                val selected =
                                    state.sizes.firstOrNull { it.id.toString() == row.sizeId }
                                OutlinedTextField(
                                    value = selected?.name ?: tr("Select size", "Selecciona talla"),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(tr("Size", "Talla")) },
                                    enabled = !isSaving,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = sizeExpanded,
                                        )
                                    },
                                    modifier =
                                        Modifier
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth(),
                                )
                                ExposedDropdownMenu(
                                    expanded = sizeExpanded,
                                    onDismissRequest = { sizeExpanded = false },
                                ) {
                                    state.sizes.forEach { size ->
                                        DropdownMenuItem(
                                            text = { Text(size.name) },
                                            onClick = {
                                                sizeExpanded = false
                                                activeDraft.sizeRows =
                                                    activeDraft.sizeRows.toMutableList().also {
                                                        it[index] =
                                                            it[index].copy(sizeId = size.id.toString())
                                                    }
                                                draft = activeDraft.copy()
                                            },
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = row.quantity,
                                    onValueChange = { quantity ->
                                        activeDraft.sizeRows =
                                            activeDraft.sizeRows.toMutableList().also {
                                                it[index] = it[index].copy(quantity = quantity)
                                            }
                                        draft = activeDraft.copy()
                                    },
                                    label = { Text(tr("Qty", "Cant")) },
                                    enabled = !isSaving,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = row.price,
                                    onValueChange = { price ->
                                        activeDraft.sizeRows =
                                            activeDraft.sizeRows.toMutableList().also {
                                                it[index] = it[index].copy(price = price)
                                            }
                                        draft = activeDraft.copy()
                                    },
                                    label = { Text(tr("Price", "Precio")) },
                                    enabled = !isSaving,
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedTextField(
                                    value = row.discount,
                                    onValueChange = { discount ->
                                        activeDraft.sizeRows =
                                            activeDraft.sizeRows.toMutableList().also {
                                                it[index] = it[index].copy(discount = discount)
                                            }
                                        draft = activeDraft.copy()
                                    },
                                    label = { Text(tr("Disc", "Desc")) },
                                    enabled = !isSaving,
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = {
                                    if (index > 0) {
                                        val next = activeDraft.sizeRows.toMutableList()
                                        val moved = next.removeAt(index)
                                        next.add(index - 1, moved)
                                        activeDraft.sizeRows = next
                                        draft = activeDraft.copy()
                                    }
                                }, enabled = !isSaving) { Text(tr("Up", "Subir")) }
                                TextButton(onClick = {
                                    if (index < activeDraft.sizeRows.lastIndex) {
                                        val next = activeDraft.sizeRows.toMutableList()
                                        val moved = next.removeAt(index)
                                        next.add(index + 1, moved)
                                        activeDraft.sizeRows = next
                                        draft = activeDraft.copy()
                                    }
                                }, enabled = !isSaving) { Text(tr("Down", "Bajar")) }
                                TextButton(onClick = {
                                    val next = activeDraft.sizeRows.toMutableList()
                                    next.removeAt(index)
                                    activeDraft.sizeRows = next
                                    draft = activeDraft.copy()
                                }, enabled = !isSaving) {
                                    Text(
                                        tr("Remove", "Quitar"),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }

                    TextButton(onClick = {
                        val defaultSizeId =
                            state.sizes
                                .firstOrNull()
                                ?.id
                                ?.toString()
                                .orEmpty()
                        activeDraft.sizeRows = activeDraft.sizeRows +
                            ProductSizeDraft(
                                defaultSizeId,
                                "0",
                                activeDraft.basePrice,
                                "0",
                            )
                        draft = activeDraft.copy()
                    }, enabled = !isSaving && state.sizes.isNotEmpty()) {
                        Text(tr("Add size row", "Agregar fila talla"))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            tr(
                                "Images (${activeDraft.mediaItems.size}/$PRODUCT_IMAGE_MAX_COUNT)",
                                "Imagenes (${activeDraft.mediaItems.size}/$PRODUCT_IMAGE_MAX_COUNT)",
                            ),
                            fontWeight = FontWeight.SemiBold,
                        )
                        Button(
                            enabled = !isSaving,
                            onClick = {
                                val slots = availableProductImageSlots(activeDraft.mediaItems.size)
                                if (slots <= 0) {
                                    localError =
                                        tr(
                                            "Max $PRODUCT_IMAGE_MAX_COUNT images allowed",
                                            "Maximo $PRODUCT_IMAGE_MAX_COUNT imagenes permitidas",
                                        )
                                } else {
                                    openImagePicker = true
                                }
                            },
                        ) {
                            Text(tr("Pick images", "Seleccionar imagenes"))
                        }
                    }

                    activeDraft.mediaItems.forEachIndexed { index, media ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            when (media) {
                                is ProductMediaDraft.Existing -> {
                                    AsyncImage(
                                        model = media.url,
                                        contentDescription = tr("Existing image", "Imagen existente"),
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                        contentScale = ContentScale.Crop,
                                    )
                                }

                                is ProductMediaDraft.New -> {
                                    media.photo.loadPainter()?.let {
                                        Image(
                                            painter = it,
                                            contentDescription = tr("New image", "Imagen nueva"),
                                            modifier = Modifier.fillMaxWidth().height(120.dp),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = {
                                    if (index > 0) {
                                        val next = activeDraft.mediaItems.toMutableList()
                                        val moved = next.removeAt(index)
                                        next.add(index - 1, moved)
                                        activeDraft.mediaItems = next
                                        draft = activeDraft.copy()
                                    }
                                }, enabled = !isSaving) { Text(tr("Up", "Subir")) }
                                TextButton(onClick = {
                                    if (index < activeDraft.mediaItems.lastIndex) {
                                        val next = activeDraft.mediaItems.toMutableList()
                                        val moved = next.removeAt(index)
                                        next.add(index + 1, moved)
                                        activeDraft.mediaItems = next
                                        draft = activeDraft.copy()
                                    }
                                }, enabled = !isSaving) { Text(tr("Down", "Bajar")) }
                                TextButton(onClick = {
                                    val next = activeDraft.mediaItems.toMutableList()
                                    next.removeAt(index)
                                    activeDraft.mediaItems = next
                                    draft = activeDraft.copy()
                                }, enabled = !isSaving) {
                                    Text(
                                        tr("Remove", "Quitar"),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = {
                        val cleanName = activeDraft.name.trim()
                        val cleanNameEs = activeDraft.nameEs.trim()
                        if (cleanName.isBlank() || cleanNameEs.isBlank()) {
                            localError =
                                tr(
                                    "Name and Name ES are required",
                                    "Nombre y Nombre ES son requeridos",
                                )
                            return@TextButton
                        }
                        if (activeDraft.categoryId.isBlank()) {
                            localError = tr("Category is required", "Categoria requerida")
                            return@TextButton
                        }

                        val parsedBasePrice = activeDraft.basePrice.toDoubleOrNull()
                        if (parsedBasePrice == null || parsedBasePrice < 0.0) {
                            localError =
                                tr(
                                    "Display price must be a valid positive number",
                                    "El precio base debe ser un numero valido positivo",
                                )
                            return@TextButton
                        }

                        if (activeDraft.sizeRows.isEmpty()) {
                            localError =
                                tr(
                                    "At least one size row is required",
                                    "Se requiere al menos una fila de talla",
                                )
                            return@TextButton
                        }

                        val sizeRows = mutableListOf<AdminInventorySizeInputDto>()
                        val seenSizeIds = mutableSetOf<Int>()
                        activeDraft.sizeRows.forEachIndexed { index, row ->
                            val rowNumber = index + 1
                            val sizeId = row.sizeId.toIntOrNull()
                            val quantity = row.quantity.toIntOrNull()
                            val price = row.price.toDoubleOrNull()
                            val discount = row.discount.toDoubleOrNull()
                            if (sizeId == null) {
                                localError =
                                    tr(
                                        "Size row $rowNumber has invalid size",
                                        "La fila de talla $rowNumber tiene una talla invalida",
                                    )
                                return@TextButton
                            }
                            if (!seenSizeIds.add(sizeId)) {
                                localError =
                                    tr(
                                        "Duplicate size in row $rowNumber",
                                        "Talla duplicada en la fila $rowNumber",
                                    )
                                return@TextButton
                            }
                            if (quantity == null || quantity < 0) {
                                localError =
                                    tr(
                                        "Size row $rowNumber has invalid quantity",
                                        "La fila de talla $rowNumber tiene cantidad invalida",
                                    )
                                return@TextButton
                            }
                            if (price == null || price < 0.0) {
                                localError =
                                    tr(
                                        "Size row $rowNumber has invalid price",
                                        "La fila de talla $rowNumber tiene precio invalido",
                                    )
                                return@TextButton
                            }
                            if (discount == null || discount < 0.0 || discount > 100.0) {
                                localError =
                                    tr(
                                        "Size row $rowNumber has invalid discount (0-100)",
                                        "La fila de talla $rowNumber tiene descuento invalido (0-100)",
                                    )
                                return@TextButton
                            }
                            sizeRows +=
                                AdminInventorySizeInputDto(
                                    sizeId = sizeId,
                                    quantity = quantity,
                                    price = price,
                                    discount = discount,
                                )
                        }

                        if (activeDraft.mediaItems.size < PRODUCT_IMAGE_MIN_COUNT) {
                            localError =
                                tr(
                                    "At least $PRODUCT_IMAGE_MIN_COUNT image is required",
                                    "Se requiere al menos $PRODUCT_IMAGE_MIN_COUNT imagen",
                                )
                            return@TextButton
                        }
                        if (activeDraft.mediaItems.size > PRODUCT_IMAGE_MAX_COUNT) {
                            localError =
                                tr(
                                    "Max $PRODUCT_IMAGE_MAX_COUNT images allowed",
                                    "Maximo $PRODUCT_IMAGE_MAX_COUNT imagenes permitidas",
                                )
                            return@TextButton
                        }

                        val uploads = mutableListOf<AdminUploadImage>()
                        val mediaPlan = mutableListOf<AdminMediaPlanItemDto>()
                        activeDraft.mediaItems.forEachIndexed { index, media ->
                            when (media) {
                                is ProductMediaDraft.Existing -> {
                                    mediaPlan +=
                                        AdminMediaPlanItemDto(
                                            type = "existing",
                                            url = media.url,
                                        )
                                }

                                is ProductMediaDraft.New -> {
                                    val fileIndex = uploads.size
                                    val bytes =
                                        media.bytesCache ?: media.photo
                                            .loadBytes()
                                            .also { media.bytesCache = it }
                                    val validationError =
                                        validatePickedImage(
                                            bytes = bytes,
                                            maxFileSizeMb = PRODUCT_IMAGE_MAX_FILE_SIZE_MB,
                                        )
                                    if (validationError != null) {
                                        localError = imageValidationMessage(validationError)
                                        return@TextButton
                                    }
                                    val mimeType =
                                        media.mimeTypeCache
                                            ?: detectImageMimeType(bytes)?.also {
                                                media.mimeTypeCache = it
                                            }
                                    if (mimeType.isNullOrBlank()) {
                                        localError =
                                            imageValidationMessage(AdminImageValidationError.UnsupportedType)
                                        return@TextButton
                                    }
                                    val extension = fileExtensionForImageMimeType(mimeType)
                                    uploads +=
                                        AdminUploadImage(
                                            fileName = "product_${Clock.System.now()}_$index.$extension",
                                            mimeType = mimeType,
                                            bytes = bytes,
                                        )
                                    mediaPlan +=
                                        AdminMediaPlanItemDto(
                                            type = "new",
                                            fileIndex = fileIndex,
                                        )
                                }
                            }
                        }

                        val payload =
                            AdminProductUpsertRequestDto(
                                name = cleanName,
                                nameEs = cleanNameEs,
                                description = activeDraft.description,
                                descriptionEs = activeDraft.descriptionEs,
                                vendor = activeDraft.vendor,
                                categoryId = activeDraft.categoryId,
                                price = parsedBasePrice,
                                sizes = sizeRows,
                                mediaPlan = mediaPlan,
                            )
                        localError = null
                        viewModel.saveProduct(
                            productId = activeDraft.id,
                            payload = payload,
                            uploads = uploads,
                        )
                        draft = null
                    },
                    content = {
                        Text(
                            if (isSaving) {
                                tr("Saving...", "Guardando...")
                            } else {
                                tr(
                                    "Save",
                                    "Guardar",
                                )
                            },
                        )
                    },
                )
            },
            dismissButton = {
                TextButton(
                    enabled = !isSaving,
                    onClick = { draft = null },
                ) {
                    Text(tr("Cancel", "Cancelar"))
                }
            },
        )
    }
}
