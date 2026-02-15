package com.billiardsdraw.vinceproshop.presentation.admin.outofstock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.presentation.common.tr
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdminOutOfStockInterestedScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminOutOfStockInterestedViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("Out Of Stock Interested", "Interesados sin stock"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = viewModel::refresh) {
                Text(tr("Refresh", "Actualizar"))
            }
        }

        when {
            state.isLoading -> {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
            state.rows.isEmpty() -> Text(tr("No requests yet.", "Aun no hay solicitudes."))
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.rows, key = { it.id }) { row ->
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
                            Text(row.productName, fontWeight = FontWeight.SemiBold)
                            Text("${tr("Date", "Fecha")}: ${row.createdAt}")
                            Text("${tr("Size", "Talla")}: ${row.sizeLabel}")
                            Text("${tr("Name", "Nombre")}: ${row.requesterName}")
                            Text("${tr("Email", "Correo")}: ${row.requesterEmail}")
                            Text("${tr("Phone", "Telefono")}: ${row.requesterPhone ?: "-"}")
                        }
                    }
                }
            }
        }
    }
}
