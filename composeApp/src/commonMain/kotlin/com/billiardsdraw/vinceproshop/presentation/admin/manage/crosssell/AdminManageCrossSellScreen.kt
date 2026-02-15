package com.billiardsdraw.vinceproshop.presentation.admin.manage.crosssell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.billiardsdraw.vinceproshop.data.remote.AdminCrossSellRuleUpsertItemDto
import com.billiardsdraw.vinceproshop.data.remote.AdminCrossSellRuleUpsertRequestDto
import com.billiardsdraw.vinceproshop.presentation.admin.buildCategorySelectOptions
import com.billiardsdraw.vinceproshop.presentation.admin.localizedName
import com.billiardsdraw.vinceproshop.presentation.common.tr
import org.koin.compose.viewmodel.koinViewModel

private data class RuleItemDraft(
    var productId: String,
    var score: String,
    var isActive: Boolean,
)

private data class CrossSellDraft(
    val id: Int,
    var sourceType: String,
    var triggerType: String,
    var triggerProductId: String,
    var triggerCategoryId: String,
    var name: String,
    var description: String,
    var maxSuggestions: String,
    var priority: String,
    var validFrom: String,
    var validTo: String,
    var isActive: Boolean,
    var analyticsLocked: Boolean,
    var items: List<RuleItemDraft>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageCrossSellScreen(
    languageCode: String,
    modifier: Modifier = Modifier,
    viewModel: AdminManageCrossSellViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    var draft by remember { mutableStateOf<CrossSellDraft?>(null) }

    val categoryOptions = remember(state.categories, languageCode) {
        buildCategorySelectOptions(state.categories, languageCode = languageCode, onlyLeaf = false)
    }

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("Manage Cross Sell", "Gestionar cross sell"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = viewModel::refresh, enabled = !state.isSaving) { Text(tr("Refresh", "Actualizar")) }
                Button(enabled = !state.isSaving, onClick = {
                    draft = CrossSellDraft(
                        id = 0,
                        sourceType = "analytics",
                        triggerType = "product",
                        triggerProductId = "",
                        triggerCategoryId = "",
                        name = "",
                        description = "",
                        maxSuggestions = "10",
                        priority = "100",
                        validFrom = "",
                        validTo = "",
                        isActive = true,
                        analyticsLocked = false,
                        items = listOf(RuleItemDraft(productId = "", score = "", isActive = true)),
                    )
                }) { Text(tr("Add", "Agregar")) }
            }
        }

        when {
            state.isLoading -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }

            state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
            state.rules.isEmpty() -> Text(tr("No cross-sell rules.", "No hay reglas cross-sell."))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(state.rules, key = { _, item -> item.id }) { _, rule ->
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("#${rule.id} ${rule.name}", fontWeight = FontWeight.SemiBold)
                        Text("${tr("Source", "Fuente")}: ${rule.sourceType}")
                        Text("${tr("Trigger", "Disparador")}: ${rule.triggerType}")
                        Text("${tr("Items", "Items")}: ${rule.items.size}")
                        Text("${tr("Priority", "Prioridad")}: ${rule.priority}")
                        Text("${tr("Active", "Activo")}: ${if (rule.isActive == 1) tr("Yes", "Si") else tr("No", "No")}")

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (rule.sourceType == "analytics" && rule.triggerProductId != null) {
                                TextButton(onClick = {
                                    viewModel.recomputeAnalytics(rule.triggerProductId)
                                }) {
                                    Text(tr("Recompute", "Recalcular"))
                                }
                            }

                            TextButton(onClick = {
                                draft = CrossSellDraft(
                                    id = rule.id,
                                    sourceType = rule.sourceType,
                                    triggerType = rule.triggerType,
                                    triggerProductId = rule.triggerProductId?.toString().orEmpty(),
                                    triggerCategoryId = rule.triggerCategoryId.orEmpty(),
                                    name = rule.name,
                                    description = rule.description.orEmpty(),
                                    maxSuggestions = rule.maxSuggestions.toString(),
                                    priority = rule.priority.toString(),
                                    validFrom = rule.validFrom.orEmpty(),
                                    validTo = rule.validTo.orEmpty(),
                                    isActive = rule.isActive == 1,
                                    analyticsLocked = (rule.isLocked ?: 0) == 1,
                                    items = (if (rule.items.isEmpty()) {
                                        listOf(RuleItemDraft(productId = "", score = "", isActive = true))
                                    } else {
                                        rule.items.map { item ->
                                            RuleItemDraft(
                                                productId = item.productId.toString(),
                                                score = item.score?.toString().orEmpty(),
                                                isActive = item.isActive == 1,
                                            )
                                        }
                                    }),
                                )
                            }) { Text(tr("Edit", "Editar")) }

                            TextButton(onClick = {
                                viewModel.deleteRule(rule.id)
                            }) { Text(tr("Delete", "Eliminar"), color = MaterialTheme.colorScheme.error) }
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
                Text(if (activeDraft.id == 0) tr("Add Cross-Sell Rule", "Agregar regla cross-sell") else tr("Edit Cross-Sell Rule", "Editar regla cross-sell"))
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
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.description,
                        onValueChange = {
                            activeDraft.description = it
                            draft = activeDraft.copy()
                        },
                        label = { Text(tr("Description", "Descripcion")) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    var sourceExpanded by remember(activeDraft.id) { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = sourceExpanded, onExpandedChange = { sourceExpanded = !sourceExpanded }) {
                        OutlinedTextField(
                            value = activeDraft.sourceType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(tr("Source type", "Tipo fuente")) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = sourceExpanded, onDismissRequest = { sourceExpanded = false }) {
                            listOf("manual", "analytics").forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        sourceExpanded = false
                                        activeDraft.sourceType = option
                                        if (option == "analytics") {
                                            activeDraft.triggerType = "product"
                                            activeDraft.triggerCategoryId = ""
                                        }
                                        draft = activeDraft.copy()
                                    }
                                )
                            }
                        }
                    }

                    if (activeDraft.sourceType == "manual") {
                        var triggerExpanded by remember(activeDraft.id) { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = triggerExpanded, onExpandedChange = { triggerExpanded = !triggerExpanded }) {
                            OutlinedTextField(
                                value = activeDraft.triggerType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(tr("Trigger type", "Tipo disparador")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = triggerExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            )
                            ExposedDropdownMenu(expanded = triggerExpanded, onDismissRequest = { triggerExpanded = false }) {
                                listOf("product", "category").forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            triggerExpanded = false
                                            activeDraft.triggerType = option
                                            draft = activeDraft.copy()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (activeDraft.triggerType == "product") {
                        var productExpanded by remember(activeDraft.id) { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = productExpanded, onExpandedChange = { productExpanded = !productExpanded }) {
                            val selected = state.products.firstOrNull { it.id.toString() == activeDraft.triggerProductId }
                            OutlinedTextField(
                                value = selected?.localizedName(languageCode) ?: tr("Select product", "Selecciona producto"),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(tr("Trigger product", "Producto disparador")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            )
                            ExposedDropdownMenu(expanded = productExpanded, onDismissRequest = { productExpanded = false }) {
                                state.products.sortedBy { it.localizedName(languageCode) }.forEach { product ->
                                    DropdownMenuItem(
                                        text = { Text("${product.localizedName(languageCode)} (${product.slug})") },
                                        onClick = {
                                            productExpanded = false
                                            activeDraft.triggerProductId = product.id.toString()
                                            draft = activeDraft.copy()
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        var categoryExpanded by remember(activeDraft.id) { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = !categoryExpanded }) {
                                val selected = categoryOptions.firstOrNull { it.id == activeDraft.triggerCategoryId }
                            OutlinedTextField(
                                value = selected?.label ?: tr("Select category", "Selecciona categoria"),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(tr("Trigger category", "Categoria disparador")) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                            )
                            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                                categoryOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            categoryExpanded = false
                                            activeDraft.triggerCategoryId = option.id
                                            draft = activeDraft.copy()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = activeDraft.maxSuggestions,
                        onValueChange = {
                            activeDraft.maxSuggestions = it
                            draft = activeDraft.copy()
                        },
                        label = { Text(tr("Max suggestions", "Max sugerencias")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.priority,
                        onValueChange = {
                            activeDraft.priority = it
                            draft = activeDraft.copy()
                        },
                        label = { Text(tr("Priority", "Prioridad")) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.validFrom,
                        onValueChange = {
                            activeDraft.validFrom = it
                            draft = activeDraft.copy()
                        },
                        label = { Text("Valid From") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = activeDraft.validTo,
                        onValueChange = {
                            activeDraft.validTo = it
                            draft = activeDraft.copy()
                        },
                        label = { Text("Valid To") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(tr("Rule active", "Regla activa"))
                        Switch(
                            checked = activeDraft.isActive,
                            onCheckedChange = {
                                activeDraft.isActive = it
                                draft = activeDraft.copy()
                            },
                        )
                    }

                    if (activeDraft.sourceType == "analytics" && activeDraft.triggerProductId.toIntOrNull() != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(tr("Lock analytics control", "Bloquear control analytics"))
                            Switch(
                                checked = activeDraft.analyticsLocked,
                                onCheckedChange = {
                                    activeDraft.analyticsLocked = it
                                    draft = activeDraft.copy()
                                },
                            )
                        }
                    }

                    Text(tr("Rule items", "Items de la regla"), fontWeight = FontWeight.SemiBold)
                    activeDraft.items.forEachIndexed { index, item ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                            var itemProductExpanded by remember(activeDraft.id, index) { mutableStateOf(false) }
                            ExposedDropdownMenuBox(expanded = itemProductExpanded, onExpandedChange = { itemProductExpanded = !itemProductExpanded }) {
                                val selected = state.products.firstOrNull { it.id.toString() == item.productId }
                                OutlinedTextField(
                                    value = selected?.localizedName(languageCode) ?: tr("Select product", "Selecciona producto"),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(tr("Product", "Producto")) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemProductExpanded) },
                                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                                )
                                ExposedDropdownMenu(expanded = itemProductExpanded, onDismissRequest = { itemProductExpanded = false }) {
                                state.products.sortedBy { it.localizedName(languageCode) }.forEach { product ->
                                        DropdownMenuItem(
                                            text = { Text("${product.localizedName(languageCode)} (${product.slug})") },
                                            onClick = {
                                                itemProductExpanded = false
                                                activeDraft.items = activeDraft.items.toMutableList().also {
                                                    it[index] = it[index].copy(productId = product.id.toString())
                                                }
                                                draft = activeDraft.copy()
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = item.score,
                                onValueChange = { newScore ->
                                    activeDraft.items = activeDraft.items.toMutableList().also {
                                        it[index] = it[index].copy(score = newScore)
                                    }
                                    draft = activeDraft.copy()
                                },
                                label = { Text(tr("Score (optional)", "Score (opcional)")) },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = item.isActive,
                                    onCheckedChange = { checked ->
                                        activeDraft.items = activeDraft.items.toMutableList().also {
                                            it[index] = it[index].copy(isActive = checked)
                                        }
                                        draft = activeDraft.copy()
                                    },
                                )
                                Text(tr("Item active", "Item activo"))
                                TextButton(onClick = {
                                    if (index > 0) {
                                        val next = activeDraft.items.toMutableList()
                                        val moved = next.removeAt(index)
                                        next.add(index - 1, moved)
                                        activeDraft.items = next
                                        draft = activeDraft.copy()
                                    }
                                }) { Text(tr("Up", "Subir")) }
                                TextButton(onClick = {
                                    if (index < activeDraft.items.lastIndex) {
                                        val next = activeDraft.items.toMutableList()
                                        val moved = next.removeAt(index)
                                        next.add(index + 1, moved)
                                        activeDraft.items = next
                                        draft = activeDraft.copy()
                                    }
                                }) { Text(tr("Down", "Bajar")) }
                                TextButton(onClick = {
                                    val next = activeDraft.items.toMutableList()
                                    next.removeAt(index)
                                    if (next.isEmpty()) next += RuleItemDraft(productId = "", score = "", isActive = true)
                                    activeDraft.items = next
                                    draft = activeDraft.copy()
                                }) { Text(tr("Delete", "Eliminar"), color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }

                    TextButton(onClick = {
                        activeDraft.items = activeDraft.items + RuleItemDraft(productId = "", score = "", isActive = true)
                        draft = activeDraft.copy()
                    }) {
                        Text(tr("Add item", "Agregar item"))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val triggerProductId = activeDraft.triggerProductId.toIntOrNull()
                    val maxSuggestions = activeDraft.maxSuggestions.toIntOrNull() ?: 10
                    val priority = activeDraft.priority.toIntOrNull() ?: 100
                    val items = activeDraft.items.mapIndexedNotNull { index, item ->
                        val itemProductId = item.productId.toIntOrNull() ?: return@mapIndexedNotNull null
                        AdminCrossSellRuleUpsertItemDto(
                            productId = itemProductId,
                            sortOrder = index,
                            score = item.score.toDoubleOrNull(),
                            isActive = if (item.isActive) 1 else 0,
                        )
                    }
                    if (items.isEmpty()) {
                        viewModel.setError(tr("At least one item is required", "Se requiere al menos un item"))
                        return@TextButton
                    }

                    val payload = AdminCrossSellRuleUpsertRequestDto(
                        sourceType = activeDraft.sourceType,
                        triggerType = activeDraft.triggerType,
                        triggerProductId = if (activeDraft.triggerType == "product") triggerProductId else null,
                        triggerCategoryId = if (activeDraft.triggerType == "category") activeDraft.triggerCategoryId.ifBlank { null } else null,
                        name = activeDraft.name.trim(),
                        description = activeDraft.description.trim().ifBlank { null },
                        maxSuggestions = maxSuggestions,
                        priority = priority,
                        isActive = if (activeDraft.isActive) 1 else 0,
                        validFrom = activeDraft.validFrom.ifBlank { null },
                        validTo = activeDraft.validTo.ifBlank { null },
                        items = items,
                    )

                    viewModel.saveRule(
                        ruleId = activeDraft.id,
                        payload = payload,
                        analyticsControlProductId = if (activeDraft.sourceType == "analytics") triggerProductId else null,
                        analyticsLocked = activeDraft.analyticsLocked,
                    )
                    draft = null
                }, enabled = !state.isSaving) { Text(tr("Save", "Guardar")) }
            },
            dismissButton = {
                TextButton(onClick = { draft = null }, enabled = !state.isSaving) { Text(tr("Cancel", "Cancelar")) }
            },
        )
    }
}
