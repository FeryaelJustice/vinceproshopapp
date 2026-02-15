package com.billiardsdraw.vinceproshop.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.presentation.common.tr
import com.billiardsdraw.vinceproshop.presentation.navigation.RootSection
import org.jetbrains.compose.resources.vectorResource
import vinceproshop_app.composeapp.generated.resources.Res
import vinceproshop_app.composeapp.generated.resources.home
import vinceproshop_app.composeapp.generated.resources.person
import vinceproshop_app.composeapp.generated.resources.search
import vinceproshop_app.composeapp.generated.resources.shopping_cart
import vinceproshop_app.composeapp.generated.resources.storefront

@Composable
fun VinceTopBar(
    cartCount: Int,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant,
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Vince Pro Shop",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = tr("Billiards gear for serious players", "Equipamiento premium de billar"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Box(modifier = Modifier.clickable(onClick = onCartClick)) {
                Icon(
                    imageVector = vectorResource(Res.drawable.shopping_cart),
                    contentDescription = tr("Cart", "Carrito"),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
                if (cartCount > 0) {
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Text(text = cartCount.toString())
                    }
                }
            }
        }
    }
}

@Composable
fun VinceBottomBar(
    selectedRoot: RootSection,
    isAccountSelected: Boolean,
    onSelectRoot: (RootSection) -> Unit,
    onAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BottomItem(
                icon = vectorResource(Res.drawable.home),
                label = tr("Home", "Inicio"),
                selected = selectedRoot == RootSection.Home,
                onClick = { onSelectRoot(RootSection.Home) },
            )
            BottomItem(
                icon = vectorResource(Res.drawable.storefront),
                label = tr("Catalog", "Productos"),
                selected = selectedRoot == RootSection.Catalog,
                onClick = { onSelectRoot(RootSection.Catalog) },
            )
            BottomItem(
                icon = vectorResource(Res.drawable.search),
                label = tr("Search", "Buscar"),
                selected = selectedRoot == RootSection.Search,
                onClick = { onSelectRoot(RootSection.Search) },
            )
            BottomItem(
                icon = vectorResource(Res.drawable.shopping_cart),
                label = tr("Cart", "Carrito"),
                selected = selectedRoot == RootSection.Cart,
                onClick = { onSelectRoot(RootSection.Cart) },
            )
            BottomItem(
                icon = vectorResource(Res.drawable.person),
                label = tr("Account", "Cuenta"),
                selected = isAccountSelected,
                onClick = onAccountClick,
            )
        }
    }
}

@Composable
private fun BottomItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
