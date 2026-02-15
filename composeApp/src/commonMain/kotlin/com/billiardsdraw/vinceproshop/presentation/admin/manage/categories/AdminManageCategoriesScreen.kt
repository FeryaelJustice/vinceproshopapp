package com.billiardsdraw.vinceproshop.presentation.admin.manage.categories

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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.billiardsdraw.vinceproshop.data.remote.AdminUploadImage
import com.billiardsdraw.vinceproshop.data.remote.CategoryDto
import com.billiardsdraw.vinceproshop.presentation.admin.AdminImageValidationError
import com.billiardsdraw.vinceproshop.presentation.admin.CATEGORY_IMAGE_MAX_FILE_SIZE_MB
import com.billiardsdraw.vinceproshop.presentation.admin.buildCategorySelectOptions
import com.billiardsdraw.vinceproshop.presentation.admin.categoryBreadcrumb
import com.billiardsdraw.vinceproshop.presentation.admin.detectImageMimeType
import com.billiardsdraw.vinceproshop.presentation.admin.fileExtensionForImageMimeType
import com.billiardsdraw.vinceproshop.presentation.admin.localizedName
import com.billiardsdraw.vinceproshop.presentation.admin.validatePickedImage
import com.billiardsdraw.vinceproshop.presentation.common.tr
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.domain.extensions.loadPainter
import io.github.ismoy.imagepickerkmp.domain.models.GalleryPhotoResult
import io.github.ismoy.imagepickerkmp.domain.models.MimeType
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Clock

private data class DraftCategoryImage(
    val photo: GalleryPhotoResult,
    var bytesCache: ByteArray,
    var mimeType: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DraftCategoryImage

        if (photo != other.photo) return false
        if (!bytesCache.contentEquals(other.bytesCache)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = photo.hashCode()
        result = 31 * result + bytesCache.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

private data class CategoryDraft(
    var id: String,
    var name: String,
    var nameEs: String,
    var isCue: Boolean,
    var parentId: String?,
    var currentImageUrl: String,
    var pickedImage: DraftCategoryImage?,
)

@Suppress("EffectKeys")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageCategoriesScreen(
    languageCode: String,
    modifier: Modifier = Modifier,
    viewModel: AdminManageCategoriesViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    var localError by remember { mutableStateOf<String?>(null) }
    var navbarLimit by remember { mutableStateOf(state.navbarLimit.toString()) }
    var navbarRootOrder by remember { mutableStateOf(state.navbarRootOrder) }

    var draft by remember { mutableStateOf<CategoryDraft?>(null) }
    var createMode by remember { mutableStateOf(true) }
    var openImagePicker by remember { mutableStateOf(false) }
    val isSaving = state.isSaving

    LaunchedEffect(state.navbarLimit, state.navbarRootOrder) {
        navbarLimit = state.navbarLimit.toString()
        navbarRootOrder = state.navbarRootOrder
    }

    val rootsOrdered =
        remember(state.categories, navbarRootOrder, languageCode) {
            val roots = state.categories.filter { it.parentId.isNullOrBlank() }
            val byId = roots.associateBy { it.id }
            val ordered = mutableListOf<CategoryDto>()
            navbarRootOrder.forEach { id -> byId[id]?.let { ordered += it } }
            roots
                .filter { root -> ordered.none { it.id == root.id } }
                .sortedBy { it.localizedName(languageCode) }
                .forEach { ordered += it }
            ordered
        }

    val parentOptions =
        remember(state.categories, languageCode, draft?.id) {
            val editingId = draft?.id.orEmpty()
            val excludedIds =
                if (editingId.isBlank()) {
                    emptySet()
                } else {
                    collectDescendantCategoryIds(
                        state.categories,
                        editingId,
                    )
                }
            buildCategorySelectOptions(
                categories = state.categories,
                languageCode = languageCode,
                onlyLeaf = false,
            ).filterNot { option -> excludedIds.contains(option.id) }
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
                    "Image exceeds $CATEGORY_IMAGE_MAX_FILE_SIZE_MB MB",
                    "La imagen excede $CATEGORY_IMAGE_MAX_FILE_SIZE_MB MB",
                )
            }
        }

    fun assignPickedImage(photo: GalleryPhotoResult) {
        val current = draft ?: return
        val bytes = photo.loadBytes()
        val validationError =
            validatePickedImage(bytes, maxFileSizeMb = CATEGORY_IMAGE_MAX_FILE_SIZE_MB)
        if (validationError != null) {
            localError = imageValidationMessage(validationError)
            return
        }
        val mimeType = detectImageMimeType(bytes)
        if (mimeType.isNullOrBlank()) {
            localError = imageValidationMessage(AdminImageValidationError.UnsupportedType)
            return
        }
        current.pickedImage =
            DraftCategoryImage(
                photo = photo,
                bytesCache = bytes,
                mimeType = mimeType,
            )
        draft = current.copy()
        localError = null
    }

    if (openImagePicker) {
        GalleryPickerLauncher(
            mimeTypes = listOf(MimeType.IMAGE_WEBP, MimeType.IMAGE_JPEG, MimeType.IMAGE_PNG),
            onPhotosSelected = { photos ->
                val selected = photos.firstOrNull()
                if (selected != null) {
                    assignPickedImage(selected)
                }
                openImagePicker = false
            },
            onError = {
                localError = it.message ?: tr("Image picker failed", "Fallo selector de imagen")
                openImagePicker = false
            },
            onDismiss = { openImagePicker = false },
            allowMultiple = false,
        )
    }

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("Manage Categories", "Gestionar categorias"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = !state.isLoading && !isSaving,
                    onClick = viewModel::refresh,
                ) {
                    Text(tr("Refresh", "Actualizar"))
                }
                Button(
                    enabled = !state.isLoading && !isSaving,
                    onClick = {
                        createMode = true
                        draft =
                            CategoryDraft(
                                id = "",
                                name = "",
                                nameEs = "",
                                isCue = false,
                                parentId = null,
                                currentImageUrl = "",
                                pickedImage = null,
                            )
                    },
                ) {
                    Text(tr("Add", "Agregar"))
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                tr("Navbar root order", "Orden de categorias raiz en navbar"),
                fontWeight = FontWeight.SemiBold,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = navbarLimit,
                    onValueChange = { navbarLimit = it },
                    singleLine = true,
                    label = { Text(tr("Visible roots", "Raices visibles")) },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    enabled = !isSaving,
                    onClick = {
                        val limit = navbarLimit.toIntOrNull() ?: 6
                        viewModel.saveNavbar(limit, rootsOrdered.map { it.id })
                    },
                ) {
                    Text(tr("Save", "Guardar"))
                }
            }
            rootsOrdered.forEachIndexed { index, category ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${index + 1}. ${category.localizedName(languageCode)} (${category.id})")
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            enabled = !isSaving,
                            onClick = {
                                if (index > 0) {
                                    navbarRootOrder =
                                        moveRoot(rootsOrdered, index, index - 1)
                                }
                            },
                        ) { Text(tr("Up", "Subir")) }
                        TextButton(
                            enabled = !isSaving,
                            onClick = {
                                if (index < rootsOrdered.lastIndex) {
                                    navbarRootOrder =
                                        moveRoot(rootsOrdered, index, index + 1)
                                }
                            },
                        ) { Text(tr("Down", "Bajar")) }
                    }
                }
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

            state.categories.isEmpty() -> {
                Text(tr("No categories", "No hay categorias"))
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.categories, key = { it.id }) { category ->
                        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                            Text(
                                "${category.localizedName(languageCode)} (${category.id})",
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text("${tr("Parent", "Padre")}: ${category.parentId ?: tr("Root", "Raiz")}")
                            Text(
                                "${tr("Cue", "Cue")}: ${
                                    if ((category.isCue ?: 0) == 1) {
                                        tr(
                                            "Yes",
                                            "Si",
                                        )
                                    } else {
                                        tr("No", "No")
                                    }
                                }",
                            )
                            Text(categoryBreadcrumb(category.id, state.categories, languageCode))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = {
                                    createMode = false
                                    draft =
                                        CategoryDraft(
                                            id = category.id,
                                            name = category.name,
                                            nameEs = category.nameEs,
                                            isCue = (category.isCue ?: 0) == 1,
                                            parentId = category.parentId,
                                            currentImageUrl = category.image,
                                            pickedImage = null,
                                        )
                                }, enabled = !isSaving) {
                                    Text(tr("Edit", "Editar"))
                                }
                                TextButton(onClick = {
                                    viewModel.deleteCategory(category.id)
                                }, enabled = !isSaving) {
                                    Text(
                                        tr("Delete", "Eliminar"),
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
                    if (createMode) {
                        tr(
                            "Add Category",
                            "Agregar categoria",
                        )
                    } else {
                        tr("Edit Category", "Editar categoria")
                    },
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (createMode) {
                        OutlinedTextField(
                            value = activeDraft.id,
                            onValueChange = {
                                activeDraft.id = it
                                draft = activeDraft.copy()
                            },
                            label = { Text("ID") },
                            singleLine = true,
                            enabled = !isSaving,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text("ID: ${activeDraft.id}")
                    }

                    OutlinedTextField(
                        value = activeDraft.name,
                        onValueChange = {
                            activeDraft.name = it
                            draft = activeDraft.copy()
                        },
                        label = { Text(tr("Name", "Nombre")) },
                        singleLine = true,
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
                        singleLine = true,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    var parentExpanded by remember(activeDraft.id) { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = parentExpanded,
                        onExpandedChange = {
                            if (!isSaving) parentExpanded = !parentExpanded
                        },
                    ) {
                        val selectedParentLabel =
                            parentOptions.firstOrNull { it.id == activeDraft.parentId }?.label
                                ?: tr("No parent (root)", "Sin padre (raiz)")
                        OutlinedTextField(
                            value = selectedParentLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = {
                                Text(
                                    tr(
                                        "Parent category (optional)",
                                        "Categoria padre (opcional)",
                                    ),
                                )
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = parentExpanded) },
                            enabled = !isSaving,
                            modifier =
                                Modifier
                                    .menuAnchor(
                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                    ).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = parentExpanded,
                            onDismissRequest = { parentExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text(tr("No parent (root)", "Sin padre (raiz)")) },
                                onClick = {
                                    parentExpanded = false
                                    activeDraft.parentId = null
                                    draft = activeDraft.copy()
                                },
                            )
                            parentOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        parentExpanded = false
                                        activeDraft.parentId = option.id
                                        draft = activeDraft.copy()
                                    },
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(tr("Is cue", "Es cue"))
                        Switch(
                            checked = activeDraft.isCue,
                            enabled = !isSaving,
                            onCheckedChange = {
                                activeDraft.isCue = it
                                draft = activeDraft.copy()
                            },
                        )
                    }

                    Button(
                        enabled = !isSaving,
                        onClick = { openImagePicker = true },
                    ) {
                        Text(tr("Pick image", "Seleccionar imagen"))
                    }

                    when {
                        activeDraft.pickedImage != null -> {
                            activeDraft.pickedImage!!.photo.loadPainter()?.let {
                                Image(
                                    painter = it,
                                    contentDescription =
                                        tr(
                                            "Category image preview",
                                            "Preview de imagen de categoria",
                                        ),
                                    contentScale = ContentScale.Crop,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(140.dp)
                                            .padding(top = 4.dp),
                                )
                            }
                        }

                        activeDraft.currentImageUrl.isNotBlank() -> {
                            AsyncImage(
                                model = activeDraft.currentImageUrl,
                                contentDescription =
                                    tr(
                                        "Current category image",
                                        "Imagen actual categoria",
                                    ),
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                        .padding(top = 4.dp),
                            )
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
                        val normalizedId = activeDraft.id.trim()
                        if (createMode && normalizedId.isBlank()) {
                            localError = tr("Category ID is required", "ID de categoria requerido")
                            return@TextButton
                        }
                        val normalizedParentId = activeDraft.parentId?.trim()?.ifBlank { null }
                        if (normalizedParentId != null && normalizedParentId == normalizedId) {
                            localError =
                                tr(
                                    "Category cannot be its own parent",
                                    "La categoria no puede ser su propio padre",
                                )
                            return@TextButton
                        }

                        val upload =
                            activeDraft.pickedImage?.let { image ->
                                val bytes = image.bytesCache
                                val validationError =
                                    validatePickedImage(
                                        bytes = bytes,
                                        maxFileSizeMb = CATEGORY_IMAGE_MAX_FILE_SIZE_MB,
                                    )
                                if (validationError != null) {
                                    localError = imageValidationMessage(validationError)
                                    return@TextButton
                                }
                                val mimeType =
                                    image.mimeType.ifBlank { detectImageMimeType(bytes).orEmpty() }
                                if (mimeType.isBlank()) {
                                    localError =
                                        imageValidationMessage(AdminImageValidationError.UnsupportedType)
                                    return@TextButton
                                }
                                val extension = fileExtensionForImageMimeType(mimeType)
                                AdminUploadImage(
                                    fileName = "category_${Clock.System.now()}.$extension",
                                    mimeType = mimeType,
                                    bytes = bytes,
                                )
                            }
                        if (createMode && upload == null) {
                            localError = tr("Image is required", "Imagen requerida")
                            return@TextButton
                        }

                        localError = null
                        viewModel.saveCategory(
                            createMode = createMode,
                            id = normalizedId,
                            name = cleanName,
                            nameEs = cleanNameEs,
                            isCue = activeDraft.isCue,
                            parentId = normalizedParentId,
                            image = upload,
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
                ) { Text(tr("Cancel", "Cancelar")) }
            },
        )
    }
}

private fun collectDescendantCategoryIds(
    categories: List<CategoryDto>,
    startId: String,
): Set<String> {
    if (startId.isBlank()) return emptySet()
    val byParent = categories.groupBy { it.parentId.orEmpty() }
    val result = mutableSetOf<String>()
    val stack = ArrayDeque<String>()
    stack.addLast(startId)
    while (stack.isNotEmpty()) {
        val current = stack.removeLast()
        if (!result.add(current)) continue
        val children = byParent[current].orEmpty()
        children.forEach { child -> stack.addLast(child.id) }
    }
    return result
}

fun moveRoot(
    list: List<CategoryDto>,
    from: Int,
    to: Int,
): List<String> {
    val next = list.map { it.id }.toMutableList()
    val moved = next.removeAt(from)
    next.add(to, moved)
    return next
}
