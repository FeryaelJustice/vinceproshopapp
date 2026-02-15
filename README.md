# Vince Pro Shop App (KMP + Compose)

Aplicacion Kotlin Multiplatform (Android + iOS) para Vince Pro Shop, basada en el frontend de `vince-billiards-pro-shop` y su backend REST.

## Stack

- Kotlin Multiplatform + Compose Multiplatform
- Clean Architecture + MVVM
- Ktor Client
- Kotlinx Serialization
- Koin DI
- Coil KMP (`coil-compose`, `coil-network-ktor3`)
- Room Multiplatform + KSP + `sqlite-bundled`
- ImagePickerKMP (`io.github.ismoy:imagepickerkmp:1.0.32`)

## Package / Company

- `com.billiardsdraw.vinceproshop`

## Arquitectura

- `domain/`: modelos, repositorios (contratos), casos de uso
- `data/`: API remota (Ktor), persistencia local (Room), mappers, repositorios impl
- `presentation/`: UI Compose, navegacion interna, ViewModels
- `di/`: modulos Koin

## Features implementadas

- Home con hero de destacados y quick view
- Catalogo con filtros (categoria, marca, precio, disponibilidad) y ordenamiento
- Regla visual especial para tacos/cues (cards en fila completa)
- Busqueda por nombre y marca
- Detalle de producto (galeria, talla, cantidad, add-to-cart)
- Carrito persistente en Room con subtotal
- Checkout por steps (`shipping -> payment -> success`) usando Stripe
- Tab de cuenta en bottom bar con login, pedidos usuario y secciones admin
- Modulo admin completo en app KMP con rutas reales `admin/*`:
  - `admin/orders`
  - `admin/inventory`
  - `admin/out-of-stock-interested`
  - `admin/inventory/manage`
  - `admin/categories/manage`
  - `admin/cross-sell/manage`
  - `admin/sizes/manage`
  - `admin/featured/manage`
  con tablas/listados y modales add/edit/delete.

## Admin media upload (multiplatform)

- Integrado `ImagePickerKMP` (`io.github.ismoy:imagepickerkmp:1.0.32`).
- `Manage Categories`: seleccion de imagen unica + preview.
- `Manage Inventory`: seleccion multiple, preview, reordenamiento y envio en orden exacto.
- El envio de inventario replica el frontend web con:
  - `multipart/form-data`
  - campo `data` (JSON)
  - archivos `images[]`
  - `mediaPlan` (`existing` / `new(fileIndex)`) para preservar orden.
- Hardening aplicado en KMP:
  - limites equivalentes a web/backend (`min=1`, `max=20`, `max 20MB` por imagen de producto, `max 10MB` en categoria)
  - validacion de tipo por firma binaria (`jpeg/png/webp/avif`)
  - bloqueo de UI durante guardado y el modal no se cierra si falla el request
  - soporte de categoria padre por selector jerarquico (no texto libre)

## i18n KMP + RTL

- Selector de idioma en runtime: `System`, `EN`, `ES`, `AR`.
- Persistencia de preferencia por plataforma:
  - Android: `SharedPreferences`.
  - iOS: `NSUserDefaults`.
- `LayoutDirection` dinamico por idioma resuelto (`RTL` para `ar/fa/he/ur`).
- Integrado desde shared con `LocalizationManager` + `expect/actual`.

## Configuracion segura (`local.properties`)

Este proyecto genera `LocalSecrets.kt` automaticamente desde `local.properties` (sin plugin custom), siguiendo un enfoque de automatizacion de secretos para KMP.

Guia base utilizada:

- `https://medium.com/@vptarasov/automating-work-with-secrets-in-kotlin-multiplatform-a2b4c587180b`

Llaves soportadas:

- `VINCE_API_BASE_URL` (ej. `https://vinceproshop.com/api`)
- `VINCE_STRIPE_PUBLISHABLE_KEY` (`pk_test_...` / `pk_live_...`)
- `VINCE_STRIPE_MERCHANT_DISPLAY_NAME` (opcional)
- `VINCE_HTTP_LOGS_ENABLED` (`true` / `false`)

Referencia base: usa `local.properties.example` como plantilla local.

## Permisos

- Android: `INTERNET` y `ACCESS_NETWORK_STATE` en `androidApp/src/main/AndroidManifest.xml`
- iOS: `NSAppTransportSecurity` (`NSAllowsArbitraryLoads`) en `iosApp/iosApp/Info.plist`

## Stripe Checkout (Android + iOS)

- Android:
  - SDK oficial `com.stripe:stripe-android`.
  - flujo nativo con `PaymentSheet` en `composeApp/src/androidMain/.../StripePaymentButton.android.kt`.
- iOS:
  - agrega `StripePaymentSheet` por Swift Package Manager al target `iosApp`.
  - puente Swift/KMP en `iosApp/iosApp/StripePaymentBridge.swift` via `NotificationCenter`.
- Backend requerido para checkout (igual que web):
  - `POST /api/payments/create-intent` (crea PaymentIntent + transaccion `pending`)
  - `POST /api/webhook` (actualiza `paid/failed` por eventos Stripe)
- referencia oficial:
  - Android: `https://docs.stripe.com/payments/mobile/accept-payment?platform=android`
  - iOS: `https://docs.stripe.com/payments/mobile/accept-payment?platform=ios`

## JWT Session Storage

- Backend admite autenticacion por cookie `token` y por header `Authorization: Bearer ...` en rutas protegidas.
- En Android, el JWT se guarda en DataStore cifrado manualmente (AES/GCM) con clave en Android Keystore:
  - `composeApp/src/androidMain/kotlin/com/billiardsdraw/vinceproshop/data/security/EncryptedDataStoreTokenStore.kt`
- En iOS, el JWT se guarda en DataStore Multiplatform cifrado manualmente (AES-CBC / CommonCrypto) con clave AES en Keychain:
  - `composeApp/src/iosMain/kotlin/com/billiardsdraw/vinceproshop/data/security/IosEncryptedDataStoreTokenStore.kt`
- En requests protegidas desde KMP se envian ambos headers para compatibilidad (`Authorization` + `Cookie token=...`).

## Ejecutar

```bash
cd /Users/feryaeljustice/AndroidStudioProjects/vinceproshop-app
export JAVA_HOME="/Users/feryaeljustice/Library/Caches/JetBrains/Toolbox/backup/AndroidStudio-252.28238.7.2523.14688667-5590052531630616454/Contents/jbr/Contents/Home"
./gradlew :composeApp:assembleDebug
```

Para iOS, abrir `iosApp/iosApp.xcodeproj` y ejecutar el target `iosApp`.
