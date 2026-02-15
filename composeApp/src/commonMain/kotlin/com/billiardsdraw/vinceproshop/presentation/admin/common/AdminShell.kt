package com.billiardsdraw.vinceproshop.presentation.admin.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.presentation.admin.navigation.AdminDestinationItem
import com.billiardsdraw.vinceproshop.presentation.admin.navigation.AdminSection
import com.billiardsdraw.vinceproshop.presentation.admin.navigation.adminDestinations
import com.billiardsdraw.vinceproshop.presentation.common.tr

@Composable
fun AdminShell(
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = onBack) {
                Text(tr("Back", "Volver"))
            }
            Text(
                text = tr("Admin Panel", "Panel Admin"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 6.dp),
            )
        }

        AdminRouteRow(
            title = tr("Overview", "Resumen"),
            routes = adminDestinations.filter { it.section == AdminSection.Overview },
            currentRoute = currentRoute,
            onSelectRoute = onSelectRoute,
        )

        AdminRouteRow(
            title = tr("Management", "Gestion"),
            routes = adminDestinations.filter { it.section == AdminSection.Manage },
            currentRoute = currentRoute,
            onSelectRoute = onSelectRoute,
        )

        HorizontalDivider()
        content()
    }
}

@Composable
private fun AdminRouteRow(
    title: String,
    routes: List<AdminDestinationItem>,
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            routes.forEach { destination ->
                AssistChip(
                    onClick = { onSelectRoute(destination.route) },
                    label = { Text(destination.label) },
                    enabled = destination.route != currentRoute,
                )
            }
        }
    }
}
