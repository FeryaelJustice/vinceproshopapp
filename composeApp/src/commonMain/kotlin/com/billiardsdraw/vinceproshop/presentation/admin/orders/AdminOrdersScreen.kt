package com.billiardsdraw.vinceproshop.presentation.admin.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.core.formatEuro
import com.billiardsdraw.vinceproshop.data.remote.OrderDto
import com.billiardsdraw.vinceproshop.presentation.common.tr
import org.koin.compose.viewmodel.koinViewModel

private val orderStatuses = listOf("pending", "completed", "cancelled")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    modifier: Modifier = Modifier,
    viewModel: AdminOrdersViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    Column(modifier = modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("Admin Orders", "Pedidos Admin"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = viewModel::refresh, enabled = !state.isUpdating) {
                Text(tr("Refresh", "Actualizar"))
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

            state.orders.isEmpty() -> {
                Text(tr("No orders yet.", "Aun no hay pedidos."))
            }

            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.orders, key = { it.id }) { order ->
                        AdminOrderCard(
                            order = order,
                            onStatusChange = { nextStatus ->
                                viewModel.updateOrderStatus(order.id, nextStatus)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminOrderCard(
    order: OrderDto,
    onStatusChange: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("${tr("Order", "Pedido")} #${order.id}", fontWeight = FontWeight.SemiBold)
        Text("${tr("Customer", "Cliente")}: ${order.customer_name}")
        Text("${tr("Email", "Correo")}: ${order.customer_email}")
        Text("${tr("Address", "Direccion")}: ${order.address}, ${order.country}")
        Text("${tr("Total", "Total")}: ${formatEuro(order.total)}")

        var expanded by remember(order.id) { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            OutlinedTextField(
                value = order.status,
                onValueChange = {},
                readOnly = true,
                label = { Text(tr("Status", "Estado")) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                orderStatuses.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status) },
                        onClick = {
                            expanded = false
                            if (status != order.status) {
                                onStatusChange(status)
                            }
                        },
                    )
                }
            }
        }

        if (order.items.isNotEmpty()) {
            Text(tr("Items", "Items"), fontWeight = FontWeight.Medium)
            order.items.forEach { item ->
                Text("- ${item.quantity}x ${item.product_name} (${item.size}) - ${formatEuro(item.price)}")
            }
        }
    }
}
