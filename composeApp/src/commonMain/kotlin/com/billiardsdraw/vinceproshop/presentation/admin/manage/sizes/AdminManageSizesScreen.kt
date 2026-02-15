package com.billiardsdraw.vinceproshop.presentation.admin.manage.sizes

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.billiardsdraw.vinceproshop.presentation.common.tr
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminManageSizesScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminManageSizesViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("Manage Sizes", "Gestionar tallas"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Button(onClick = viewModel::startAdd, enabled = !state.isSaving) {
                Text(tr("Add", "Agregar"))
            }
        }

        when {
            state.isLoading -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }

            state.sizes.isEmpty() -> {
                Text(tr("No sizes configured.", "No hay tallas configuradas."))
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.sizes, key = { it.id }) { size ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("#${size.id} ${size.name}")
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { viewModel.startEdit(size) }, enabled = !state.isSaving) {
                                    Text(tr("Edit", "Editar"))
                                }
                                TextButton(
                                    onClick = { viewModel.deleteSize(size.id) },
                                    enabled = !state.isSaving,
                                ) { Text(tr("Delete", "Eliminar"), color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.editing != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEditor,
            title = {
                Text(
                    if (state.editing.id == 0) {
                        tr("Add Size", "Agregar talla")
                    } else {
                        tr("Edit Size", "Editar talla")
                    },
                )
            },
            text = {
                OutlinedTextField(
                    value = state.draftName,
                    onValueChange = viewModel::updateDraftName,
                    singleLine = true,
                    label = { Text(tr("Name", "Nombre")) },
                    enabled = !state.isSaving,
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::saveDraft, enabled = !state.isSaving) {
                    Text(tr("Save", "Guardar"))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEditor, enabled = !state.isSaving) {
                    Text(tr("Cancel", "Cancelar"))
                }
            },
        )
    }
}
