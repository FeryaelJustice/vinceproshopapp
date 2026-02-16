package com.billiardsdraw.vinceproshop.presentation.legal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.billiardsdraw.vinceproshop.presentation.common.tr

private sealed interface LegalContentBlock {
    data class Paragraph(val text: String) : LegalContentBlock

    data class BulletList(val items: List<String>) : LegalContentBlock

    data class NumberedList(val items: List<String>) : LegalContentBlock
}

private data class LegalSection(
    val heading: String,
    val blocks: List<LegalContentBlock>,
)

private data class LegalPageContent(
    val title: String,
    val intro: String? = null,
    val sections: List<LegalSection> = emptyList(),
    val footerNote: String? = null,
)

@Composable
fun LegalDocumentScreen(
    document: LegalDocument,
    languageCode: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content = legalPageContent(document = document, languageCode = languageCode)

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(contentType = "contentType1") {
            TextButton(
                onClick = onBack,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = tr("< Back", "< Volver"),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        item(contentType = "contentType2") {
            Text(
                text = content.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        content.intro?.takeIf { it.isNotBlank() }?.let { intro ->
            item {
                Text(
                    text = intro,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        content.sections.forEach { section ->
            item(contentType = "contentType1") {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = section.heading,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )

                    section.blocks.forEach { block ->
                        when (block) {
                            is LegalContentBlock.Paragraph -> Text(
                                text = block.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            is LegalContentBlock.BulletList -> BulletListBlock(block.items)
                            is LegalContentBlock.NumberedList -> NumberedListBlock(block.items)
                        }
                    }
                }
            }
        }

        content.footerNote?.takeIf { it.isNotBlank() }?.let { footerNote ->
            item(contentType = "contentType1") {
                Text(
                    text = footerNote,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun BulletListBlock(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NumberedListBlock(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "${index + 1}.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private fun legalPageContent(
    document: LegalDocument,
    languageCode: String,
): LegalPageContent {
    val isSpanish = languageCode.startsWith("es", ignoreCase = true)

    return when (document) {
        LegalDocument.About -> {
            LegalPageContent(
                title = tr("About Us", "Sobre nosotros"),
                intro =
                    tr(
                        "Vince Pro Shop is your trusted destination for high-quality billiards gear.",
                        "Vince Pro Shop es tu destino de confianza para equipos de billar de alta calidad.",
                    ),
                sections =
                    listOf(
                        LegalSection(
                            heading = tr("Who We Are", "Quienes somos"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "We provide premium cues and accessories for players who care about precision, durability, and performance.",
                                            "Ofrecemos tacos y accesorios premium para jugadores que buscan precision, durabilidad y rendimiento.",
                                        ),
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Our Mission", "Nuestra mision"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "Our mission is to deliver top-tier billiards products and an honest shopping experience.",
                                            "Nuestra mision es ofrecer productos de primer nivel y una experiencia de compra transparente.",
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )
        }

        LegalDocument.Shipping -> {
            LegalPageContent(
                title = tr("Shipping Information", "Informacion de Envios"),
                intro =
                    tr(
                        "We are committed to delivering your order quickly and safely.",
                        "Nos comprometemos a entregar tu pedido de forma rapida y segura.",
                    ),
                sections =
                    listOf(
                        LegalSection(
                            heading = tr("Shipping Times", "Tiempos de envio"),
                            blocks =
                                listOf(
                                    LegalContentBlock.BulletList(
                                        items =
                                            if (isSpanish) {
                                                listOf(
                                                    "Peninsula: 2-3 dias laborables",
                                                    "Baleares y Canarias: 1-2 dias laborables",
                                                    "Envios internacionales: 5-7 dias laborables",
                                                )
                                            } else {
                                                listOf(
                                                    "Mainland Spain: 2-3 business days",
                                                    "Balearic and Canary Islands: 1-2 business days",
                                                    "International shipping: 5-7 business days",
                                                )
                                            },
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Tracking", "Seguimiento"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "You will receive a tracking number by email once your order has shipped.",
                                            "Recibiras un numero de seguimiento por email cuando se envie tu pedido.",
                                        ),
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Packaging", "Embalaje"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "All products are shipped in secure packaging to protect them during transport.",
                                            "Todos los productos se envian en embalajes seguros para protegerlos durante el transporte.",
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )
        }

        LegalDocument.Returns -> {
            LegalPageContent(
                title = tr("Returns Policy", "Politica de Devoluciones"),
                intro =
                    tr(
                        "We want you to be completely satisfied with your purchase. If not, we offer a flexible return policy.",
                        "Queremos que estes completamente satisfecho con tu compra. Si no es asi, ofrecemos una politica de devolucion flexible.",
                    ),
                sections =
                    listOf(
                        LegalSection(
                            heading = tr("Return Period", "Plazo de devolucion"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "You have 30 days from order delivery to request a return.",
                                            "Tienes 30 dias desde la recepcion del pedido para solicitar una devolucion.",
                                        ),
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Conditions", "Condiciones"),
                            blocks =
                                listOf(
                                    LegalContentBlock.BulletList(
                                        items =
                                            if (isSpanish) {
                                                listOf(
                                                    "El producto debe estar sin usar y en su embalaje original.",
                                                    "Incluye el ticket o factura de compra.",
                                                    "Los productos personalizados no son reembolsables.",
                                                )
                                            } else {
                                                listOf(
                                                    "The product must be unused and in its original packaging.",
                                                    "Include the purchase receipt or invoice.",
                                                    "Personalized products are non-refundable.",
                                                )
                                            },
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Process", "Proceso"),
                            blocks =
                                listOf(
                                    LegalContentBlock.NumberedList(
                                        items =
                                            if (isSpanish) {
                                                listOf(
                                                    "Contacta con atencion al cliente en support@vinceproshop.com.",
                                                    "Indica tu numero de pedido y motivo de devolucion.",
                                                    "Te enviaremos una etiqueta de devolucion gratuita.",
                                                    "Procesaremos tu reembolso en 5-7 dias laborables tras recibir el producto.",
                                                )
                                            } else {
                                                listOf(
                                                    "Contact customer service at support@vinceproshop.com.",
                                                    "Provide your order number and return reason.",
                                                    "We will send you a free return label.",
                                                    "We will process your refund within 5-7 business days after receiving the product.",
                                                )
                                            },
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Refund", "Reembolso"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "The refund is made using the same payment method used in the original purchase.",
                                            "El reembolso se realizara por el mismo metodo de pago utilizado en la compra original.",
                                        ),
                                    ),
                                ),
                        ),
                    ),
                footerNote = tr("All displayed prices include VAT (Spain 21%).", "Todos los precios mostrados incluyen IVA (21% Espana)."),
            )
        }

        LegalDocument.Cookies -> {
            LegalPageContent(
                title = tr("Cookie Policy", "Politica de Cookies"),
                intro =
                    tr(
                        "This website uses cookies and local storage to provide essential storefront functions.",
                        "Este sitio utiliza cookies y almacenamiento local para el funcionamiento esencial de la tienda.",
                    ),
                sections =
                    listOf(
                        LegalSection(
                            heading = tr("What We Use Cookies For", "Para que usamos cookies"),
                            blocks =
                                listOf(
                                    LegalContentBlock.BulletList(
                                        items =
                                            if (isSpanish) {
                                                listOf(
                                                    "Mantener el carrito entre sesiones.",
                                                    "Recordar tu sesion autenticada.",
                                                    "Guardar preferencias funcionales como idioma.",
                                                )
                                            } else {
                                                listOf(
                                                    "Keep your shopping cart between sessions.",
                                                    "Remember your authenticated session.",
                                                    "Store functional preferences like language.",
                                                )
                                            },
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Cookie Categories", "Categorias de cookies"),
                            blocks =
                                listOf(
                                    LegalContentBlock.NumberedList(
                                        items =
                                            if (isSpanish) {
                                                listOf(
                                                    "Necesarias: imprescindibles para comprar e iniciar sesion.",
                                                    "Funcionales: mejoran la experiencia de uso.",
                                                    "Analiticas: ayudan a mejorar el producto.",
                                                )
                                            } else {
                                                listOf(
                                                    "Necessary: required to shop and log in.",
                                                    "Functional: improve product experience.",
                                                    "Analytics: help us improve the store.",
                                                )
                                            },
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Management", "Gestion"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "You can accept or reject optional cookies through the cookie settings controls.",
                                            "Puedes aceptar o rechazar cookies opcionales desde la configuracion de cookies.",
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )
        }

        LegalDocument.Terms -> {
            LegalPageContent(
                title = tr("Terms of Use", "Terminos de Uso"),
                intro =
                    tr(
                        "By using this app and website, you accept these terms and conditions.",
                        "Al usar esta app y web, aceptas estos terminos y condiciones.",
                    ),
                sections =
                    listOf(
                        LegalSection(
                            heading = tr("Intellectual Property", "Propiedad intelectual"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "All product images, text, and branding are property of Vince Pro Shop unless otherwise noted.",
                                            "Las imagenes, textos y marca son propiedad de Vince Pro Shop salvo indicacion expresa.",
                                        ),
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Use Restrictions", "Restricciones de uso"),
                            blocks =
                                listOf(
                                    LegalContentBlock.BulletList(
                                        items =
                                            if (isSpanish) {
                                                listOf(
                                                    "No reproducir contenido sin autorizacion escrita.",
                                                    "No usar la plataforma con fines fraudulentos o abusivos.",
                                                )
                                            } else {
                                                listOf(
                                                    "Do not reproduce content without written permission.",
                                                    "Do not use the platform for fraudulent or abusive activity.",
                                                )
                                            },
                                    ),
                                ),
                        ),
                    ),
            )
        }

        LegalDocument.Notice -> {
            LegalPageContent(
                title = tr("Legal Notice", "Aviso Legal"),
                sections =
                    listOf(
                        LegalSection(
                            heading = tr("Company Information", "Informacion de la empresa"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "Vince Pro Shop is a dedicated provider of high-quality billiards equipment.",
                                            "Vince Pro Shop es un proveedor dedicado de equipos de billar de alta calidad.",
                                        ),
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Contact", "Contacto"),
                            blocks =
                                listOf(
                                    LegalContentBlock.BulletList(
                                        items =
                                            if (isSpanish) {
                                                listOf(
                                                    "Correo de contacto: support@vinceproshop.com",
                                                    "Telefono: +34651727480",
                                                )
                                            } else {
                                                listOf(
                                                    "Contact email: support@vinceproshop.com",
                                                    "Phone: +34651727480",
                                                )
                                            },
                                    ),
                                ),
                        ),
                    ),
                footerNote = tr("This website is operated by Vince Pro Shop. All rights reserved.", "Este sitio web es operado por Vince Pro Shop. Todos los derechos reservados."),
            )
        }

        LegalDocument.Privacy -> {
            LegalPageContent(
                title = tr("Privacy Policy", "Politica de Privacidad"),
                sections =
                    listOf(
                        LegalSection(
                            heading = tr("Data We Collect", "Datos que recopilamos"),
                            blocks =
                                listOf(
                                    LegalContentBlock.BulletList(
                                        items =
                                            if (isSpanish) {
                                                listOf(
                                                    "Datos necesarios para procesar pedidos (nombre, email, direccion y envio).",
                                                    "No almacenamos datos completos de tarjeta de credito en nuestros servidores.",
                                                )
                                            } else {
                                                listOf(
                                                    "Data required to process orders (name, email, address and shipping details).",
                                                    "We do not store full credit-card data on our servers.",
                                                )
                                            },
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("How We Use Data", "Como usamos los datos"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "We use your data only for order fulfillment, customer support, and legal obligations.",
                                            "Usamos tus datos solo para gestionar pedidos, soporte y obligaciones legales.",
                                        ),
                                    ),
                                ),
                        ),
                        LegalSection(
                            heading = tr("Third Parties", "Terceros"),
                            blocks =
                                listOf(
                                    LegalContentBlock.Paragraph(
                                        tr(
                                            "Payment processing is handled by secure providers such as Stripe under their own privacy terms.",
                                            "El pago es procesado por proveedores seguros como Stripe bajo sus propias politicas de privacidad.",
                                        ),
                                    ),
                                ),
                        ),
                    ),
            )
        }
    }
}
