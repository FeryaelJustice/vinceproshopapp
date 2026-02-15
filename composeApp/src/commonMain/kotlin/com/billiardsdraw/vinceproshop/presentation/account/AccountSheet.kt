package com.billiardsdraw.vinceproshop.presentation.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.core.formatEuro
import com.billiardsdraw.vinceproshop.domain.model.Order
import com.billiardsdraw.vinceproshop.presentation.common.tr
import org.jetbrains.compose.resources.vectorResource
import vinceproshop_app.composeapp.generated.resources.Res
import vinceproshop_app.composeapp.generated.resources.visibility
import vinceproshop_app.composeapp.generated.resources.visibility_off

private enum class AccountItemType {
    Header,          // Row con título "Account" + botón Refresh
    LanguageLabel,   // Text "Language"
    LanguageChips,   // Row con los 4 FilterChip
    Loading,         // CircularProgressIndicator (sesión Y pedidos, mismo composable)
    LoginHint,       // Text explicativo de login
    IdentifierField, // OutlinedTextField email/usuario
    PasswordField,   // OutlinedTextField contraseña
    ForgetCredential,// TextButton "Forget saved login"
    LoginButton,     // Button "Log in"
    AuthError,       // Text error de autenticación
    UserCard,        // Surface con username/email/role
    LogoutButton,    // Row con TextButton "Logout"
    OrdersError,     // Text error cargando pedidos
    SectionTitle,    // Text de título de sección (Admin Sections, Recent Orders, My Orders)
    AdminSection,    // Row clickable de cada sección admin
    Order,           // OrderCard (admin y user comparten el mismo composable)
    EmptyOrders,     // Text "No orders yet" / "No hay pedidos"
}

@Composable
fun AccountSheet(
    state: AccountUiState,
    languageCode: String,
    languageOption: String,
    onLanguageOptionChange: (String) -> Unit,
    onLogin: (identifier: String, password: String) -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onClearSavedCredential: () -> Unit,
    onDismissAuthError: () -> Unit,
    onOpenAdminRoute: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var identifier by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
    val isSpanish = languageCode.startsWith("es", true)

    LaunchedEffect(state.savedCredential) {
        val credential = state.savedCredential ?: return@LaunchedEffect
        if (identifier.isBlank()) {
            identifier = credential.identifier
        }
        if (password.isBlank()) {
            password = credential.password
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(contentType = AccountItemType.Header) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = tr("Account", "Cuenta"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                TextButton(onClick = onRefresh) {
                    Text(tr("Refresh", "Actualizar"))
                }
            }
        }
        item(contentType = AccountItemType.LanguageLabel) {
            Text(
                text = tr("Language", "Idioma", "اللغة"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item(contentType = AccountItemType.LanguageChips) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LanguageChip(
                    label = tr("System", "Sistema", "النظام"),
                    selected = languageOption.equals("system", ignoreCase = true),
                    onClick = { onLanguageOptionChange("system") },
                )
                LanguageChip(
                    label = "EN",
                    selected = languageOption.equals("en", ignoreCase = true),
                    onClick = { onLanguageOptionChange("en") },
                )
                LanguageChip(
                    label = "ES",
                    selected = languageOption.equals("es", ignoreCase = true),
                    onClick = { onLanguageOptionChange("es") },
                )
                LanguageChip(
                    label = "AR",
                    selected = languageOption.equals("ar", ignoreCase = true),
                    onClick = { onLanguageOptionChange("ar") },
                )
            }
        }

        if (state.isLoadingSession) {
            item(contentType = AccountItemType.Loading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        val user = state.user
        if (user == null) {
            item(contentType = AccountItemType.LoginHint) {
                Text(
                    text = tr(
                        "Log in to see your orders or admin panel.",
                        "Inicia sesion para ver tus pedidos o panel admin."
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item(contentType = AccountItemType.IdentifierField) {
                OutlinedTextField(
                    value = identifier,
                    onValueChange = {
                        identifier = it
                        if (state.authError != null) onDismissAuthError()
                    },
                    singleLine = true,
                    label = { Text(tr("Email or username", "Email o usuario")) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    supportingText = {
                        if (state.savedCredential != null) {
                            Text(
                                tr(
                                    "Autofilled from secure credential store",
                                    "Autocompletado desde almacen seguro"
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription =
                                tr("Email or username input", "Campo de email o usuario")
                        },
                )
            }
            item(contentType = AccountItemType.PasswordField) {
                CompositionLocalProvider(LocalTextToolbar provides DisabledTextToolbar) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (state.authError != null) onDismissAuthError()
                        },
                        singleLine = true,
                        label = { Text(tr("Password", "Contraseña")) },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Unspecified,
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            platformImeOptions = null,
                            showKeyboardOnFocus = null,
                            hintLocales = null
                        ),
                        visualTransformation = if (isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = vectorResource(
                                        if (isPasswordVisible) {
                                            Res.drawable.visibility_off
                                        } else {
                                            Res.drawable.visibility
                                        }
                                    ),
                                    contentDescription = if (isPasswordVisible) {
                                        tr("Hide password", "Ocultar contraseña")
                                    } else {
                                        tr("Show password", "Mostrar contraseña")
                                    },
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = tr("Password input", "Campo de contraseña")
                                password()
                            },
                    )
                }
            }
            if (state.savedCredential != null) {
                item(contentType = AccountItemType.ForgetCredential) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onClearSavedCredential) {
                            Text(tr("Forget saved login", "Olvidar login guardado"))
                        }
                    }
                }
            }
            item(contentType = AccountItemType.LoginButton) {
                Button(
                    onClick = { onLogin(identifier, password) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmittingAuth,
                ) {
                    if (state.isSubmittingAuth) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(tr("Log in", "Iniciar sesion"))
                    }
                }
            }
            if (state.authError != null) {
                item(contentType = AccountItemType.AuthError) {
                    Text(
                        text = if (isSpanish) "Error de login: ${state.authError}" else "Login error: ${state.authError}",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            return@LazyColumn
        }

        item(contentType = AccountItemType.UserCard) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = user.username.ifBlank { user.email },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (isSpanish) "Rol: ${user.role}" else "Role: ${user.role}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item(contentType = AccountItemType.LogoutButton) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onLogout) {
                    Text(tr("Logout", "Cerrar sesion"), color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (state.isLoadingOrders) {
            item(contentType = AccountItemType.Loading) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        if (state.ordersError != null) {
            item(contentType = AccountItemType.OrdersError) {
                Text(
                    text = if (isSpanish) "Error cargando pedidos: ${state.ordersError}" else "Orders load error: ${state.ordersError}",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (user.role.equals("admin", ignoreCase = true)) {
            item(contentType = AccountItemType.SectionTitle) {
                Text(
                    text = tr("Admin Sections", "Secciones Admin"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(
                state.adminSections,
                key = { it.route },
                contentType = { AccountItemType.AdminSection }) { section ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAdminRoute(section.route) }
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = section.label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = section.route,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(150.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            item(contentType = AccountItemType.SectionTitle) {
                Text(
                    text = tr("Recent Orders", "Pedidos recientes"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            if (state.adminOrders.isEmpty()) {
                item(contentType = AccountItemType.EmptyOrders) {
                    Text(
                        text = tr("No admin orders yet.", "Sin pedidos admin aun."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    state.adminOrders.take(8),
                    key = { it.id },
                    contentType = { AccountItemType.Order }) { order ->
                    OrderCard(order = order, languageCode = languageCode)
                }
            }
        } else {
            item(contentType = AccountItemType.SectionTitle) {
                Text(
                    text = tr("My Orders", "Mis pedidos"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (state.userOrders.isEmpty()) {
                item(contentType = AccountItemType.EmptyOrders) {
                    Text(
                        text = tr("No orders found.", "No hay pedidos."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(state.userOrders, key = { it.id }, contentType = { AccountItemType.Order }) { order ->
                    OrderCard(order = order, languageCode = languageCode)
                }
            }
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

private object DisabledTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun hide() = Unit

    override fun showMenu(
        rect: androidx.compose.ui.geometry.Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit
}

@Composable
private fun OrderCard(
    order: Order,
    languageCode: String,
) {
    val isSpanish = languageCode.startsWith("es", true)
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "#${order.id} ${order.status}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = formatEuro(order.total),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = if (isSpanish) "Fecha: ${order.createdAt}" else "Date: ${order.createdAt}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (order.address.isNotBlank()) {
                Text(
                    text = if (isSpanish) "Direccion: ${order.address}" else "Address: ${order.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            order.items.take(3).forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "${item.quantity}x ${item.productName} (${item.size})",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatEuro(item.price),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (order.items.size > 3) {
                Text(
                    text = if (isSpanish) "+${order.items.size - 3} productos" else "+${order.items.size - 3} items",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
