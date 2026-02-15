# Bitacora De Implementacion (Pasos Realizados)

## Contexto Y Restricciones Aplicadas
- Se uso como referencia funcional el repo web/backend: `C:\Users\nano9\vince-billiards-pro-shop`.
- No se modifico `backend` ni `frontend` web.
- Se adapto solo este proyecto KMP: `C:\Users\nano9\AndroidStudioProjects\vinceproshop-app`.
- Secrets mantenidos en `local.properties` (enfoque seguro), sin crear plugin Gradle custom.
- No se hizo build final por instruccion explicita.

## Paso 1: Investigacion Del Flujo Real De Stripe En Web/Backend
Se revisaron rutas y contrato real:
- Backend:
  - `POST /api/payments/create-intent`
  - `POST /api/webhook` (eventos `payment_intent.succeeded` y `payment_intent.payment_failed`)
- Frontend web:
  - crea intent en backend
  - confirma en cliente con Stripe Elements
  - limpia carrito en UI al exito
  - estado final del pedido lo resuelve webhook backend

Conclusiones:
- El sistema real de la web NO usa Stripe Terminal.
- El flujo correcto para la app movil es PaymentIntent (online checkout), alineado a web.

## Paso 2: Estado Inicial Del Proyecto KMP Y Gap Detectado
- El proyecto KMP estaba migrado a un flujo `Stripe Terminal` (reader discovery/connect/process), con capas `Terminal*`.
- Ese flujo no correspondia al backend real disponible.

## Paso 3: Decision Arquitectonica
Se reemplazo el dominio `Terminal` por un dominio `Checkout` modular y alineado al backend real.

Objetivo del refactor:
- Mantener Clean Architecture + MVVM.
- Mantener expect/actual para pago nativo Android/iOS.
- Replicar semantica de web (`shipping -> payment -> success`) y guard de cambio de carrito.

## Paso 4: Nueva Capa Domain De Checkout
Se agregaron:
- `composeApp/src/commonMain/kotlin/com/billiardsdraw/vinceproshop/domain/model/CheckoutModels.kt`
  - `CheckoutCustomerInfo`
  - `CheckoutLineItem`
  - `CheckoutPaymentIntentPayload`
  - `CheckoutPaymentIntent`
  - helpers:
    - `isValid()`
    - `fullAddress()`
    - `paymentIntentIdFromClientSecret(...)`
- `composeApp/src/commonMain/kotlin/com/billiardsdraw/vinceproshop/domain/repository/CheckoutRepository.kt`
- `composeApp/src/commonMain/kotlin/com/billiardsdraw/vinceproshop/domain/usecase/CreatePaymentIntentUseCase.kt`

Ajuste adicional:
- Se movio `CheckoutLineItem` fuera de `AccountModels.kt` para separar responsabilidades.

## Paso 5: Nueva Capa Data Remota De Checkout
Se agregaron:
- `data/remote/CheckoutApi.kt`
- `data/remote/CheckoutDto.kt`
- `data/remote/KtorCheckoutApi.kt`
- `data/repository/CheckoutRepositoryImpl.kt`

Contrato implementado:
- Llamada a `payments/create-intent` con payload equivalente al web.
- Manejo de errores HTTP devolviendo mensaje de backend si existe (`error`/`message`).

## Paso 6: Refactor DI (Koin)
Archivo:
- `composeApp/src/commonMain/kotlin/com/billiardsdraw/vinceproshop/di/AppModules.kt`

Cambios:
- Se registro `CheckoutApi`, `CheckoutRepository`, `CreatePaymentIntentUseCase`.
- Se elimino wiring de `TerminalApi/TerminalRepository/TerminalUseCases`.
- Se actualizo constructor de `CartViewModel` al nuevo flujo.

## Paso 7: Refactor CartViewModel A Checkout Por Steps
Archivo:
- `composeApp/src/commonMain/kotlin/com/billiardsdraw/vinceproshop/presentation/cart/CartViewModel.kt`

Cambios funcionales:
- Nuevo estado con:
  - `checkoutStep` (`Shipping`, `Payment`, `Completed`)
  - `customerInfo`
  - `paymentClientSecret`, `paymentIntentId`, `paymentAmount`
  - flags de loading y errores/mensajes
- `continueToPayment(languageCode)`:
  - valida carrito + shipping
  - crea PaymentIntent via backend
  - guarda fingerprint del carrito
  - avanza a step `Payment`
- Guard de integridad:
  - si carrito cambia mientras esta en `Payment`, vuelve a `Shipping` y fuerza regenerar intent.
- Manejo de resultados de pago:
  - `onPaymentStarted`
  - `onPaymentCompleted(paymentIntentId)` limpia carrito y pasa a `Completed`
  - `onPaymentCanceled`
  - `onPaymentFailed`
- Reset/dismiss del flujo:
  - `backToShipping`
  - `resetCheckoutFlow`
  - `dismissCheckoutFeedback`

## Paso 8: Refactor CartScreen A UI Checkout Multi-Step
Archivo:
- `composeApp/src/commonMain/kotlin/com/billiardsdraw/vinceproshop/presentation/cart/CartScreen.kt`

Cambios:
- Se quito UI de Terminal readers.
- Se implemento panel de checkout:
  - formulario shipping
  - paso payment con boton Stripe nativo
  - pantalla success con `paymentIntentId`
- Se conectaron callbacks al `CartViewModel`.

## Paso 9: expect/actual Para Stripe Pago Nativo
### Common
- `presentation/payment/StripePaymentButton.kt`
  - `expect fun StripePaymentButton(...)`
  - `StripePaymentResult` (`Completed`, `Canceled`, `Failed`)

### Android (SDK oficial)
- `composeApp/src/androidMain/.../presentation/payment/StripePaymentButton.android.kt`
- Integracion con `com.stripe:stripe-android` + `PaymentSheet`.
- `PaymentConfiguration.init(...)` con publishable key local.
- `presentWithPaymentIntent(clientSecret, configuration)`.

### iOS (bridge Swift/Kotlin)
- `composeApp/src/iosMain/.../presentation/payment/StripePaymentButton.ios.kt`
- Bridge por `NotificationCenter`:
  - request: `StripePaymentBridgeRequest`
  - response: `StripePaymentBridgeResponse`
- `iosApp/iosApp/StripePaymentBridge.swift`:
  - integra `StripePaymentSheet`
  - inicializa publishable key
  - presenta `PaymentSheet`
  - devuelve estado (`completed`/`canceled`/`failed`) a Kotlin.

## Paso 10: Limpieza Completa De Stripe Terminal
Se eliminaron archivos de capa `Terminal*` en common/android/ios:
- `data/remote/KtorTerminalApi.kt`
- `data/remote/TerminalApi.kt`
- `data/remote/TerminalDto.kt`
- `data/repository/TerminalRepositoryImpl.kt`
- `data/terminal/StripeTerminalBridge.kt`
- `domain/model/TerminalModels.kt`
- `domain/repository/TerminalRepository.kt`
- `domain/usecase/TerminalUseCases.kt`
- `androidMain/data/terminal/StripeTerminalBridge.android.kt`
- `iosMain/data/terminal/StripeTerminalBridge.ios.kt`

Tambien se limpiaron hooks/plataforma:
- Android:
  - se quito `TerminalApplicationDelegate.onCreate(...)` de `VinceProShopApp.kt`
  - se removieron permisos runtime Bluetooth/Location en `MainActivity.kt`
  - se removieron permisos Terminal del `AndroidManifest.xml`
- iOS:
  - se removieron descripciones Bluetooth/Location de `Info.plist`
  - `iOSApp.swift` ahora arranca `StripePaymentBridgeService`

## Paso 11: Secrets En local.properties (sin plugin custom)
Archivo:
- `composeApp/build.gradle.kts`

Implementado:
- lectura segura de:
  - gradle property
  - env var
  - `local.properties`
- generacion de `LocalSecrets.kt` en build dir y agregado al `commonMain` source set.

Nuevas llaves soportadas:
- `VINCE_API_BASE_URL`
- `VINCE_STRIPE_PUBLISHABLE_KEY`
- `VINCE_STRIPE_MERCHANT_DISPLAY_NAME`
- `VINCE_HTTP_LOGS_ENABLED`

Plantilla actualizada:
- `local.properties.example`

Local real actualizado (archivo local, no para commit):
- `local.properties`

## Paso 12: Dependencias Stripe
Archivo:
- `gradle/libs.versions.toml`

Cambios:
- removida dependencia de `stripeterminal`
- agregada dependencia `stripe-android`

## Paso 13: Wiring De App Y Navegacion
Archivo:
- `composeApp/src/commonMain/kotlin/com/billiardsdraw/vinceproshop/App.kt`

Cambios:
- Cart screen conectado al nuevo flujo checkout/payment.
- Se mantiene tab/sheet de cuenta/admin en bottom bar sin bloquear toda la app.

## Paso 14: Docs Internas
Se actualizo:
- `README.md`

Con:
- flujo Stripe Checkout real
- llaves de `local.properties`
- backend requerido (`/api/payments/create-intent`, webhook)

## Resultado Final
- La app KMP queda alineada al flujo real del frontend web para pagos.
- Se removio acoplamiento con Stripe Terminal.
- Se mantiene arquitectura modular (clean architecture + MVVM + DI).
- Secrets en local properties + codigo generado.
- Sin build final ejecutado (por instruccion).

## Pendiente Operativo (Manual, no codigo)
En iOS/Xcode debes confirmar que el target `iosApp` tenga agregado el producto SPM `StripePaymentSheet`.
