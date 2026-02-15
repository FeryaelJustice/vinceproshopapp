package com.billiardsdraw.vinceproshop.presentation.admin.manage.featured

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billiardsdraw.vinceproshop.data.remote.AdminFeaturedUpsertRequestDto
import com.billiardsdraw.vinceproshop.presentation.admin.buildCategorySelectOptions
import com.billiardsdraw.vinceproshop.presentation.admin.localizedName
import com.billiardsdraw.vinceproshop.presentation.common.tr
import org.koin.compose.viewmodel.koinViewModel

private data class FeaturedDraft(
    val id: Int,
    var targetType: String,
    var productId: Int?,
    var categoryId: String?,
    var title: String,
    var titleEs: String,
    var subtitle: String,
    var subtitleEs: String,
    var sortOrder: String,
    var isActive: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageFeaturedScreen(
    languageCode: String,
    modifier: Modifier = Modifier,
    viewModel: AdminManageFeaturedViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    var draft by remember { mutableStateOf<FeaturedDraft?>(null) }

    val categoryOptions = remember(state.categories, languageCode) {
        buildCategorySelectOptions(state.categories, languageCode = languageCode, onlyLeaf = true)
    }

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("Manage Featured", "Gestionar destacados"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = viewModel::refresh,
                    enabled = !state.isSaving
                ) { Text(tr("Refresh", "Actualizar")) }
                Button(enabled = !state.isSaving, onClick = {
                    draft = FeaturedDraft(
                        id = 0,
                        targetType = "product",
                        productId = null,
                        categoryId = null,
                        title = "",
                        titleEs = "",
                        subtitle = "",
                        subtitleEs = "",
                        sortOrder = "0",
                        isActive = true,
                    )
                }) {
                    Text(tr("Add", "Agregar"))
                }
            }
        }

        when {
            state.isLoading -> Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }

            state.error != null -> Text(
                state.error,
                color = MaterialTheme.colorScheme.error
            )

            state.featured.isEmpty() -> Text(tr("No featured items.", "No hay destacados."))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.featured, key = { it.id }) { item ->
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                        Text("#${item.id} - ${item.title}", fontWeight = FontWeight.SemiBold)
                        Text("${tr("Target", "Objetivo")}: ${item.targetType}")
                        Text(
                            "${tr("Destination", "Destino")}: " +
                                    if (item.targetType == "category") {
                                        item.categoryId.orEmpty()
                                    } else {
                                        item.slug.orEmpty()
                                    }
                        )
                        Text("${tr("Order", "Orden")}: ${item.sortOrder}")
                        Text(
                            "${tr("Status", "Estado")}: ${
                                if (item.isActive == 1) tr(
                                    "Active",
                                    "Activo"
                                ) else tr("Inactive", "Inactivo")
                            }"
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                draft = FeaturedDraft(
                                    id = item.id,
                                    targetType = item.targetType,
                                    productId = item.productId,
                                    categoryId = item.categoryId,
                                    title = item.title,
                                    titleEs = item.titleEs,
                                    subtitle = item.subtitle,
                                    subtitleEs = item.subtitleEs,
                                    sortOrder = item.sortOrder.toString(),
                                    isActive = item.isActive == 1,
                                )
                            }) { Text(tr("Edit", "Editar")) }
                            TextButton(onClick = {
                                viewModel.deleteFeatured(item.id)
                            }, enabled = !state.isSaving) {
                                Text(
                                    tr("Delete", "Eliminar"),
                                    color = MaterialTheme.colorScheme.error
                                )
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
            onDismissRequest = { draft = null },
            title = {
                Text(
                    if (activeDraft.id == 0) tr(
                        "Add Featured",
                        "Agregar destacado"
                    ) else tr("Edit Featured", "Editar destacado")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var targetExpanded by remember(activeDraft.id) { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = targetExpanded,
                        onExpandedChange = { targetExpanded = !targetExpanded }) {
                        OutlinedTextField(
                            value = activeDraft.targetType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(tr("Target type", "Tipo de objetivo")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = targetExpanded,
                            onDismissRequest = { targetExpanded = false }) {
                            listOf("product", "category").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        targetExpanded = false
                                        activeDraft.targetType = option
                                        if (option == "product") {
                                            activeDraft.categoryId = null
                                        } else {
                                            activeDraft.productId = null
                                        }
                                        draft = activeDraft.copy()
                                    }
                                )
                            }
                        }
                    }

                    if (activeDraft.targetType == "product") {
                        var productExpanded by remember(activeDraft.id) { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = productExpanded,
                            onExpandedChange = { productExpanded = !productExpanded }) {
                            val selectedLabel =
                                state.products.firstOrNull { it.id == activeDraft.productId }
                                    ?.localizedName(languageCode)
                                    ?: tr("Select product", "Selecciona producto")
                            OutlinedTextField(
                                value = selectedLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(tr("Product", "Producto")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = productExpanded,
                                onDismissRequest = { productExpanded = false }) {
                                state.products.sortedBy { it.localizedName(languageCode) }
                                    .forEach { product ->
                                        DropdownMenuItem(
                                            text = { Text("${product.localizedName(languageCode)} (${product.slug})") },
                                            onClick = {
                                                productExpanded = false
                                                activeDraft.productId = product.id
                                                draft = activeDraft.copy()
                                            }
                                        )
                                    }
                            }
                        }
                    } else {
                        var categoryExpanded by remember(activeDraft.id) { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                            val selectedLabel =
                                categoryOptions.firstOrNull { it.id == activeDraft.categoryId }?.label
                                    ?: tr("Select category", "Selecciona categoria")
                            OutlinedTextField(
                                value = selectedLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(tr("Category", "Categoria")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }) {
                                categoryOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            categoryExpanded = false
                                            activeDraft.categoryId = option.id
                                            draft = activeDraft.copy()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = activeDraft.title,
                        onValueChange = {
                            activeDraft.title = it
                            draft = activeDraft.copy()
                        },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.titleEs,
                        onValueChange = {
                            activeDraft.titleEs = it
                            draft = activeDraft.copy()
                        },
                        label = { Text("Title ES") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.subtitle,
                        onValueChange = {
                            activeDraft.subtitle = it
                            draft = activeDraft.copy()
                        },
                        label = { Text("Subtitle") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.subtitleEs,
                        onValueChange = {
                            activeDraft.subtitleEs = it
                            draft = activeDraft.copy()
                        },
                        label = { Text("Subtitle ES") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.sortOrder,
                        onValueChange = {
                            activeDraft.sortOrder = it
                            draft = activeDraft.copy()
                        },
                        label = { Text(tr("Sort order", "Orden")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(tr("Active", "Activo"))
                        Switch(
                            checked = activeDraft.isActive,
                            onCheckedChange = {
                                activeDraft.isActive = it
                                draft = activeDraft.copy()
                            },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val sort = activeDraft.sortOrder.toIntOrNull() ?: 0
                    val payload = AdminFeaturedUpsertRequestDto(
                        targetType = activeDraft.targetType,
                        productId = if (activeDraft.targetType == "product") activeDraft.productId else null,
                        categoryId = if (activeDraft.targetType == "category") activeDraft.categoryId else null,
                        title = activeDraft.title.trim(),
                        titleEs = activeDraft.titleEs.trim(),
                        subtitle = activeDraft.subtitle.trim(),
                        subtitleEs = activeDraft.subtitleEs.trim(),
                        sortOrder = sort,
                        isActive = if (activeDraft.isActive) 1 else 0,
                    )
                    viewModel.saveFeatured(activeDraft.id, payload)
                    draft = null
                }, enabled = !state.isSaving) {
                    Text(tr("Save", "Guardar"))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { draft = null },
                    enabled = !state.isSaving
                ) { Text(tr("Cancel", "Cancelar")) }
            },
        )
    }
}
