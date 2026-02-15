# Documentacion Tecnica Del Proyecto KMP (Shared + Android + iOS)

## 1) Objetivo Del Proyecto
Aplicacion Kotlin Multiplatform + Compose Multiplatform para Vince Pro Shop con:
- catalogo de productos,
- detalle y carrito persistente,
- checkout Stripe equivalente al flujo web,
- cuenta de usuario (login/sesion/pedidos) y vista admin de pedidos/secciones.

El proyecto sigue Clean Architecture + MVVM:
- `domain`: reglas de negocio y contratos.
- `data`: implementaciones concretas (API remota, Room, mappers).
- `presentation`: UI Compose y ViewModels.

## 2) Modulos Y Responsabilidades
- `composeApp`:
  - modulo compartido principal (common + androidMain + iosMain).
  - contiene casi toda la logica de negocio, UI y datos.
- `androidApp`:
  - cascaron Android (Activity, Manifest, recursos).
  - carga `App()` de `composeApp`.
- `iosApp`:
  - app iOS SwiftUI host.
  - integra `ComposeApp` framework y bridge nativo para PaymentSheet.

## 3) Arquitectura (Clean + MVVM)
### 3.1 Domain
Contiene modelos puros, interfaces de repositorio y casos de uso.

Regla:
- `domain` no conoce Ktor, Room, Compose UI, ni frameworks de plataforma.

### 3.2 Data
Contiene:
- `data/remote`: contratos API + DTOs + implementaciones Ktor.
- `data/local`: entidades Room, DAOs y DB.
- `data/repository`: implementaciones de repositorios domain.
- `data/mapper`: conversion DTO <-> Domain <-> Entity.

### 3.3 Presentation (MVVM)
- `ViewModel` contiene estado y acciones.
- `Screen`/`Composable` renderiza `UiState` y dispara callbacks.
- `AppNavigator` maneja estado de navegacion simple en memoria.

### 3.4 DI (Koin)
- `di/AppModules.kt` define grafo principal.
- `platformModule` (`androidMain`/`iosMain`) aporta engine Ktor por plataforma.

## 4) Shared (commonMain) En Detalle

### 4.1 Core
- `core/DispatchersProvider.kt`
  - `DispatchersProvider`: interfaz para `main/io/default`.
  - `StandardDispatchers`: implementacion real.
- `core/Localization.kt`
  - `expect fun currentLanguageCode()`
  - `fun isSpanishLanguage()` helper.
- `core/CurrencyFormatter.kt`
  - `fun formatEuro(value)` para mostrar importes.

Nota: `LocalSecrets.kt` se genera en build (`composeApp/build.gradle.kts`) desde `local.properties`.
Funciones generadas:
- `localApiBaseUrl()`
- `localStripePublishableKey()`
- `localStripeMerchantDisplayName()`
- `localHttpLogsEnabled()`

### 4.2 Domain Models
- `domain/model/Product.kt`
  - `Product`, `ProductOption`, `ProductMedia`
  - helpers `localizedName`, `localizedDescription`.
- `domain/model/Category.kt`
  - `Category`, helper `localizedName`.
- `domain/model/FeaturedSlide.kt`
  - `FeaturedSlide`, helpers `localizedTitle/localizedSubtitle`.
- `domain/model/CartItem.kt`
  - `CartItem`, extension `finalPrice`.
- `domain/model/AccountModels.kt`
  - `AccountUser`, `AuthSession`, `Order`, `OrderItem`.
- `domain/model/CheckoutModels.kt`
  - `CheckoutCustomerInfo`, `CheckoutLineItem`, `CheckoutPaymentIntentPayload`, `CheckoutPaymentIntent`.
  - helpers: `isValid`, `fullAddress`, `paymentIntentIdFromClientSecret`.

### 4.3 Domain Repositories (Contratos)
- `domain/repository/CatalogRepository.kt`
- `domain/repository/CartRepository.kt`
- `domain/repository/AccountRepository.kt`
- `domain/repository/CheckoutRepository.kt`

### 4.4 Domain Use Cases
Catalogo:
- `RefreshCatalogUseCase`
- `ObserveHomeFeedUseCase`
- `ObserveCatalogUseCase`
- `ObserveProductDetailUseCase`
- `RefreshProductUseCase`
- `SearchProductsUseCase`

Carrito:
- `ObserveCartUseCase`
- `AddCartItemUseCase`
- `UpdateCartQuantityUseCase`
- `RemoveCartItemUseCase`
- `ClearCartUseCase`

Cuenta:
- `RefreshSessionUseCase`
- `LoginUseCase`
- `LogoutUseCase`
- `GetUserOrdersUseCase`
- `GetAdminOrdersUseCase`

Checkout:
- `CreatePaymentIntentUseCase`

### 4.5 Data Local (Room)
Archivos:
- `data/local/Entities.kt`
  - `ProductEntity`, `CategoryEntity`, `FeaturedSlideEntity`, `CartItemEntity`.
- `data/local/Daos.kt`
  - `ProductDao`, `CategoryDao`, `FeaturedDao`, `CartDao`.
- `data/local/VinceProShopDatabase.kt`
  - clase `RoomDatabase` + constructor expect/actual.
- `data/local/DatabaseFactory.kt`
  - `platformDatabaseBuilder(...)` expect
  - `createRoomDatabase()` shared.

Funcionamiento:
- cache local de catalogo + featured + categorias.
- carrito persistente local en tabla `cart_items`.

### 4.6 Data Remote (Ktor + DTO)
#### Catalogo
- `data/remote/CatalogApi.kt`
- `data/remote/CatalogDto.kt`
- `data/remote/KtorCatalogApi.kt`

Endpoints:
- `GET /products`
- `GET /products/categories`
- `GET /products/featured`
- `GET /products/{slug}`

#### Cuenta
- `data/remote/AccountApi.kt`
- `data/remote/AccountDto.kt`
- `data/remote/KtorAccountApi.kt`

Endpoints:
- `POST /auth/login`
- `POST /auth/logout`
- `GET /auth/me`
- `GET /users/orders`
- `GET /admin/orders`

#### Checkout
- `data/remote/CheckoutApi.kt`
- `data/remote/CheckoutDto.kt`
- `data/remote/KtorCheckoutApi.kt`

Endpoint:
- `POST /payments/create-intent`

Comportamiento importante:
- `KtorCheckoutApi` inspecciona status HTTP y parsea `ApiErrorDto` para propagar error de backend.

#### Networking Platform Abstraction
- `data/remote/NetworkPlatform.kt` (expect)
- `androidMain` y `iosMain` dan `actual`.

Configuracion aplicada:
- ContentNegotiation JSON
- HttpTimeout
- HttpCookies (`AcceptAllCookiesStorage`) para sesion JWT cookie-compatible con backend
- Logging opcional controlado por `VINCE_HTTP_LOGS_ENABLED`

### 4.7 Repositories Impl
- `data/repository/CatalogRepositoryImpl.kt`
  - sincroniza remoto -> cache Room.
  - expone flujos observables para UI.
- `data/repository/CartRepositoryImpl.kt`
  - merge de items por clave (`slug`,`size`) y persistencia local.
- `data/repository/AccountRepositoryImpl.kt`
  - login/logout/sesion/pedidos via API.
- `data/repository/CheckoutRepositoryImpl.kt`
  - crea PaymentIntent para checkout.

### 4.8 Mappers
- `data/mapper/CatalogMappers.kt`
  - transforma DTO a Domain y Domain a Entity.
  - resuelve rutas de assets relativas con `API_BASE_URL`.

### 4.9 Presentation Shared
#### Entrada App
- `App.kt`
  - configura Koin (`appModule`) y tema.
  - crea VMs (`Home`, `Catalog`, `Search`, `Cart`, `Account`).
  - renderiza scaffold con top bar + bottom bar.
  - abre account en `ModalBottomSheet`.

#### Navegacion
- `presentation/navigation/AppNavigator.kt`
  - `RootSection`: `Home`, `Catalog`, `Search`, `Cart`.
  - `AppDestination`: Root o ProductDetail.

#### Home
- `presentation/home/HomeViewModel.kt`
- `presentation/home/HomeScreen.kt`

#### Catalogo
- `presentation/catalog/CatalogViewModel.kt`
- `presentation/catalog/CatalogScreen.kt`

#### Busqueda
- `presentation/search/SearchViewModel.kt`
- `presentation/search/SearchScreen.kt`

#### Detalle Producto
- `presentation/product/ProductDetailViewModel.kt`
- `presentation/product/ProductDetailScreen.kt`

#### Carrito + Checkout
- `presentation/cart/CartViewModel.kt`
  - estado `CartUiState`.
  - `CheckoutStep`: `Shipping`, `Payment`, `Completed`.
  - crea intent con backend y controla flujo multi-step.
  - guard de fingerprint: si cambia carrito en `Payment`, invalida client secret y vuelve a `Shipping`.
- `presentation/cart/CartScreen.kt`
  - formulario shipping
  - boton de pago nativo
  - pantalla de exito

#### Cuenta/Admin
- `presentation/account/AccountViewModel.kt`
- `presentation/account/AccountSheet.kt`

Comportamiento:
- login/logout
- refresco de sesion
- pedidos de usuario
- pedidos admin + lista de secciones admin

#### Componentes Compartidos
- `presentation/components/AppBars.kt`
  - top bar + bottom bar (incluye tab Cuenta)
- `presentation/components/ProductCard.kt`
- `presentation/components/HeroCarousel.kt`
- `presentation/components/EmptyState.kt`
- `presentation/common/Strings.kt` (`tr(en,es)`)
- `presentation/theme/*`

#### Pago (expect)
- `presentation/payment/StripePaymentButton.kt`
  - contrato multiplataforma del boton de pago.

## 5) Android (androidMain + androidApp)

### 5.1 composeApp androidMain
- `AndroidAppContext.kt`
  - guarda `applicationContext` para Room.
- `VinceProShopApp.kt`
  - inicializa `AndroidAppContext`.
- `data/local/DatabaseFactory.android.kt`
  - builder Room Android.
- `data/remote/NetworkPlatform.android.kt`
  - cliente Ktor con cookies, json, timeout, logs.
- `di/AppModules.android.kt`
  - provee `OkHttp.create()`.
- `presentation/payment/StripePaymentButton.android.kt`
  - PaymentSheet Android oficial.
  - usa `PaymentConfiguration.init(publishableKey)`.
  - `presentWithPaymentIntent(clientSecret, configuration)`.

### 5.2 androidApp modulo host
- `MainActivity.kt`
  - monta Compose con `setContent { App() }`.
- `AndroidManifest.xml`
  - permisos de red y registro de `VinceProShopApp`.

## 6) iOS (iosMain + iosApp)

### 6.1 composeApp iosMain
- `MainViewController.kt` expone `ComposeUIViewController { App() }`.
- `data/local/DatabaseFactory.ios.kt` crea DB en Documents.
- `data/remote/NetworkPlatform.ios.kt` Ktor Darwin + cookies + timeout + logs.
- `di/AppModules.ios.kt` provee engine Darwin.
- `presentation/payment/StripePaymentButton.ios.kt`
  - implementa boton de pago iOS.
  - usa bridge con NotificationCenter hacia Swift.

### 6.2 iosApp host + bridge nativo
- `iOSApp.swift`
  - arranca `StripePaymentBridgeService.shared.start()`.
- `ContentView.swift`
  - host SwiftUI del compose VC.
- `StripePaymentBridge.swift`
  - integra `StripePaymentSheet` nativo.
  - recibe request desde Kotlin:
    - accion `presentPaymentSheet`
    - `clientSecret` + billing basico
  - configura:
    - publishable key (`VINCE_STRIPE_PUBLISHABLE_KEY`)
    - merchant display name (`VINCE_STRIPE_MERCHANT_DISPLAY_NAME`)
  - presenta PaymentSheet y responde estado a Kotlin (`completed/canceled/failed`).

## 7) Flujo End-To-End De Checkout
1. Usuario completa shipping en `CartScreen`.
2. `CartViewModel.continueToPayment(...)` valida datos.
3. `CreatePaymentIntentUseCase` -> `CheckoutRepositoryImpl` -> `KtorCheckoutApi` -> `POST /payments/create-intent`.
4. Backend devuelve `clientSecret` + `amount`.
5. UI pasa a step `Payment`.
6. Boton de pago nativo:
   - Android: PaymentSheet.
   - iOS: bridge Swift PaymentSheet.
7. Resultado:
   - completed -> `onPaymentCompleted` limpia carrito local y muestra success.
   - canceled/failed -> feedback en UI.
8. Estado final de transaccion en backend lo define webhook Stripe (`/api/webhook`).

## 8) APIs Y Data Sources En El Proyecto
### Data sources remotos (Ktor)
- `KtorCatalogApi`
- `KtorAccountApi`
- `KtorCheckoutApi`

### Data source local
- Room (`VinceProShopDatabase`) con DAOs:
  - `ProductDao`, `CategoryDao`, `FeaturedDao`, `CartDao`.

### Repositorios (capa de acceso unificada)
- `CatalogRepositoryImpl`
- `CartRepositoryImpl`
- `AccountRepositoryImpl`
- `CheckoutRepositoryImpl`

## 9) Secrets Y Configuracion
`composeApp/build.gradle.kts` genera `LocalSecrets.kt` desde `local.properties`.

Llaves:
- `VINCE_API_BASE_URL`
- `VINCE_STRIPE_PUBLISHABLE_KEY`
- `VINCE_STRIPE_MERCHANT_DISPLAY_NAME`
- `VINCE_HTTP_LOGS_ENABLED`

Importante:
- no usar `sk_*` (secret keys) en mobile.
- solo publishable key (`pk_*`) en cliente.

## 10) Estado Actual Y Nota Operativa
- Arquitectura limpia y flujo Stripe alineado con backend web.
- Stripe Terminal eliminado del codigo KMP.
- Cuenta/admin integrada como sheet desde bottom tab.
- No se ejecuto build final (siguiendo instruccion).

Para ejecutar pagos en iOS:
- verificar que el target `iosApp` tenga producto SPM `StripePaymentSheet`.

## 11) Anexos
Los anexos siguientes listan inventario completo de archivos y declaraciones encontradas.

## Anexo A: Inventario Completo De Archivos De Codigo
`	ext
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\androidTest\java\com\billiardsdraw\androidApp\ExampleInstrumentedTest.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\AndroidManifest.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\java\com\billiardsdraw\androidApp\MainActivity.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\drawable\ic_launcher_background.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\drawable\ic_launcher_foreground.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-anydpi-v26\ic_launcher_round.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-anydpi-v26\ic_launcher.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-hdpi\ic_launcher_round.webp
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-hdpi\ic_launcher.webp
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-mdpi\ic_launcher_round.webp
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-mdpi\ic_launcher.webp
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-xhdpi\ic_launcher_round.webp
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-xhdpi\ic_launcher.webp
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-xxhdpi\ic_launcher_round.webp
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-xxhdpi\ic_launcher.webp
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-xxxhdpi\ic_launcher_round.webp
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\mipmap-xxxhdpi\ic_launcher.webp
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\values-night\themes.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\values\colors.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\values\strings.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\res\values\themes.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\test\java\com\billiardsdraw\androidApp\ExampleUnitTest.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\AndroidAppContext.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\core\Localization.android.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\data\local\DatabaseFactory.android.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\NetworkPlatform.android.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\di\AppModules.android.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\presentation\payment\StripePaymentButton.android.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\VinceProShopApp.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\drawable-v24\ic_launcher_foreground.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\drawable\ic_launcher_background.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-anydpi-v26\ic_launcher_round.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-anydpi-v26\ic_launcher.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-hdpi\ic_launcher_round.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-hdpi\ic_launcher.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-mdpi\ic_launcher_round.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-mdpi\ic_launcher.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-xhdpi\ic_launcher_round.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-xhdpi\ic_launcher.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-xxhdpi\ic_launcher_round.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-xxhdpi\ic_launcher.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-xxxhdpi\ic_launcher_round.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\mipmap-xxxhdpi\ic_launcher.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\res\values\strings.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\composeResources\values\strings.xml
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\App.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\core\CurrencyFormatter.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\core\DispatchersProvider.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\core\Localization.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\Daos.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\DatabaseFactory.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\Entities.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\VinceProShopDatabase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountApi.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountDto.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CatalogApi.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CatalogDto.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CheckoutApi.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CheckoutDto.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorAccountApi.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCatalogApi.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCheckoutApi.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\NetworkPlatform.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\repository\AccountRepositoryImpl.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\repository\CartRepositoryImpl.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\repository\CatalogRepositoryImpl.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\repository\CheckoutRepositoryImpl.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\di\AppModules.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\AccountModels.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\CartItem.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\Category.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\CheckoutModels.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\FeaturedSlide.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\Product.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\repository\AccountRepository.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\repository\CartRepository.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\repository\CatalogRepository.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\repository\CheckoutRepository.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\AddCartItemUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ClearCartUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\CreatePaymentIntentUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\GetAdminOrdersUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\GetUserOrdersUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\LoginUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\LogoutUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveCartUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveCatalogUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveHomeFeedUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveProductDetailUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\RefreshCatalogUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\RefreshProductUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\RefreshSessionUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\RemoveCartItemUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\SearchProductsUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\UpdateCartQuantityUseCase.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\account\AccountSheet.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\account\AccountViewModel.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\cart\CartScreen.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\cart\CartViewModel.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\catalog\CatalogScreen.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\catalog\CatalogViewModel.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\common\Strings.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\components\AppBars.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\components\EmptyState.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\components\HeroCarousel.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\components\ProductCard.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\home\HomeScreen.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\home\HomeViewModel.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\navigation\AppNavigator.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\payment\StripePaymentButton.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\product\ProductDetailScreen.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\product\ProductDetailViewModel.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\search\SearchScreen.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\search\SearchViewModel.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\theme\Color.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\theme\Theme.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\theme\Typography.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\core\Localization.ios.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\data\local\DatabaseFactory.ios.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\NetworkPlatform.ios.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\di\AppModules.ios.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\MainViewController.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\presentation\payment\StripePaymentButton.ios.kt
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\iosApp\iosApp\Assets.xcassets\AccentColor.colorset\Contents.json
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\iosApp\iosApp\Assets.xcassets\AppIcon.appiconset\app-icon-1024.png
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\iosApp\iosApp\Assets.xcassets\AppIcon.appiconset\Contents.json
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\iosApp\iosApp\Assets.xcassets\Contents.json
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\iosApp\iosApp\ContentView.swift
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\iosApp\iosApp\Info.plist
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\iosApp\iosApp\iOSApp.swift
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\iosApp\iosApp\Preview Content\Preview Assets.xcassets\Contents.json
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\iosApp\iosApp\StripePaymentBridge.swift
```

## Anexo B: Inventario De Clases/Interfaces/Funciones (extraido automaticamente)
`	ext
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\build.gradle.kts:21:fun secretProperty(key: String, defaultValue: String = ""): String {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\build.gradle.kts:30:fun kotlinEscaped(value: String): String {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\build.gradle.kts:64:fun writeLocalSecretsFile() {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\App.kt:45:fun App() {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\core\Localization.kt:3:expect fun currentLanguageCode(): String
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\core\Localization.kt:5:fun isSpanishLanguage(): Boolean = currentLanguageCode().startsWith("es", ignoreCase = true)
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\androidTest\java\com\billiardsdraw\androidApp\ExampleInstrumentedTest.kt:17:class ExampleInstrumentedTest {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\core\DispatchersProvider.kt:6:interface DispatchersProvider {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\core\DispatchersProvider.kt:12:class StandardDispatchers : DispatchersProvider {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\core\CurrencyFormatter.kt:5:fun formatEuro(value: Double): String {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\main\java\com\billiardsdraw\androidApp\MainActivity.kt:8:class MainActivity : ComponentActivity() {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\common\Strings.kt:5:fun tr(en: String, es: String): String = if (isSpanishLanguage()) es else en
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\cart\CartViewModel.kt:29:data class CartUiState(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\cart\CartViewModel.kt:46:class CartViewModel(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\account\AccountViewModel.kt:18:data class AdminPanelSection(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\account\AccountViewModel.kt:23:data class AccountUiState(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\account\AccountViewModel.kt:35:class AccountViewModel(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\cart\CartScreen.kt:38:fun CartScreen(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\theme\Theme.kt:32:fun VinceTheme(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\account\AccountSheet.kt:36:fun AccountSheet(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\androidApp\src\test\java\com\billiardsdraw\androidApp\ExampleUnitTest.kt:12:class ExampleUnitTest {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\repository\CheckoutRepository.kt:6:interface CheckoutRepository {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\catalog\CatalogViewModel.kt:28:data class CatalogUiState(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\catalog\CatalogViewModel.kt:43:class CatalogViewModel(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\repository\CheckoutRepositoryImpl.kt:12:class CheckoutRepositoryImpl(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\presentation\payment\StripePaymentButton.ios.kt:28:actual fun StripePaymentButton(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\catalog\CatalogScreen.kt:30:data class CatalogRenderItem(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\catalog\CatalogScreen.kt:36:fun CatalogScreen(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\repository\CatalogRepository.kt:8:interface CatalogRepository {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\payment\StripePaymentButton.kt:7:sealed interface StripePaymentResult {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\payment\StripePaymentButton.kt:14:expect fun StripePaymentButton(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\repository\CatalogRepositoryImpl.kt:20:class CatalogRepositoryImpl(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\home\HomeViewModel.kt:15:data class HomeUiState(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\home\HomeViewModel.kt:22:class HomeViewModel(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\repository\CartRepository.kt:6:interface CartRepository {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\MainViewController.kt:5:fun MainViewController() = ComposeUIViewController { App() }
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\UpdateCartQuantityUseCase.kt:5:class UpdateCartQuantityUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\repository\CartRepositoryImpl.kt:13:class CartRepositoryImpl(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\home\HomeScreen.kt:22:fun HomeScreen(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\VinceProShopApp.kt:5:class VinceProShopApp : Application() {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\SearchProductsUseCase.kt:5:class SearchProductsUseCase {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\repository\AccountRepository.kt:6:interface AccountRepository {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\repository\AccountRepositoryImpl.kt:12:class AccountRepositoryImpl(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\RemoveCartItemUseCase.kt:5:class RemoveCartItemUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\product\ProductDetailViewModel.kt:16:data class ProductDetailUiState(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\product\ProductDetailViewModel.kt:25:class ProductDetailViewModel(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\navigation\AppNavigator.kt:15:sealed interface AppDestination {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\navigation\AppNavigator.kt:20:class AppNavigator {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\RefreshSessionUseCase.kt:6:class RefreshSessionUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\search\SearchViewModel.kt:14:data class SearchUiState(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\search\SearchViewModel.kt:21:class SearchViewModel(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\product\ProductDetailScreen.kt:38:fun ProductDetailScreen(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\search\SearchScreen.kt:21:fun SearchScreen(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\RefreshProductUseCase.kt:5:class RefreshProductUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\AccountModels.kt:3:data class AccountUser(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\AccountModels.kt:10:data class AuthSession(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\AccountModels.kt:15:data class OrderItem(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\AccountModels.kt:22:data class Order(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\RefreshCatalogUseCase.kt:5:class RefreshCatalogUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountApi.kt:3:interface AccountApi {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveProductDetailUseCase.kt:7:class ObserveProductDetailUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\components\ProductCard.kt:30:fun ProductCard(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\NetworkPlatform.ios.kt:16:actual fun createPlatformHttpClient(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\NetworkPlatform.ios.kt:46:actual fun defaultApiBaseUrl(): String = localApiBaseUrl()
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\Product.kt:6:data class ProductOption(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\Product.kt:15:data class ProductMedia(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\Product.kt:20:data class Product(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\Product.kt:37:fun Product.localizedName(languageCode: String): String {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\Product.kt:41:fun Product.localizedDescription(languageCode: String): String {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CatalogApi.kt:3:interface CatalogApi {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\core\Localization.ios.kt:3:actual fun currentLanguageCode(): String = "en"
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveHomeFeedUseCase.kt:9:data class HomeFeed(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveHomeFeedUseCase.kt:14:class ObserveHomeFeedUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\Daos.kt:9:interface ProductDao {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\Daos.kt:21:interface CategoryDao {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\Daos.kt:30:interface FeaturedDao {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\Daos.kt:39:interface CartDao {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:20:fun ProductDto.toDomain(apiBaseUrl: String): Product {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:41:fun ProductOptionDto.toDomain(): ProductOption {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:51:fun ProductMediaDto.toDomain(apiBaseUrl: String): ProductMedia {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:55:fun CategoryDto.toDomain(apiBaseUrl: String): Category {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:66:fun FeaturedSlideDto.toDomain(apiBaseUrl: String): FeaturedSlide {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:85:fun Product.toEntity(json: Json): ProductEntity {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:100:fun ProductEntity.toDomain(json: Json): Product {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:123:fun Category.toEntity(): CategoryEntity {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:134:fun CategoryEntity.toDomain(): Category {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:145:fun FeaturedSlide.toEntity(json: Json): FeaturedSlideEntity {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:154:fun FeaturedSlideEntity.toDomain(json: Json): FeaturedSlide {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:158:fun CartItemEntity.toDomain(): CartItem {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\mapper\CatalogMappers.kt:172:fun CartItem.toEntity(): CartItemEntity {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\NetworkPlatform.android.kt:16:actual fun createPlatformHttpClient(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\NetworkPlatform.android.kt:45:actual fun defaultApiBaseUrl(): String = localApiBaseUrl()
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\components\HeroCarousel.kt:38:fun HeroCarousel(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\FeaturedSlide.kt:6:data class FeaturedSlide(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\FeaturedSlide.kt:23:fun FeaturedSlide.localizedTitle(languageCode: String): String {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\FeaturedSlide.kt:27:fun FeaturedSlide.localizedSubtitle(languageCode: String): String {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveCatalogUseCase.kt:9:data class CatalogFeed(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveCatalogUseCase.kt:14:class ObserveCatalogUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountDto.kt:17:data class LoginRequestDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountDto.kt:23:data class LoginResponseDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountDto.kt:28:data class AuthUserDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountDto.kt:36:data class AuthSessionDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountDto.kt:42:data class OrderItemDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountDto.kt:50:data class OrderDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountDto.kt:63:object LenientDoubleSerializer : KSerializer<Double> {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\AccountDto.kt:77:object LenientIntSerializer : KSerializer<Int> {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\components\EmptyState.kt:16:fun EmptyState(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveCartUseCase.kt:9:data class CartSnapshot(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ObserveCartUseCase.kt:15:class ObserveCartUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\CheckoutModels.kt:3:data class CheckoutCustomerInfo(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\CheckoutModels.kt:13:data class CheckoutLineItem(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\CheckoutModels.kt:19:data class CheckoutPaymentIntentPayload(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\CheckoutModels.kt:30:data class CheckoutPaymentIntent(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\CheckoutModels.kt:35:fun CheckoutCustomerInfo.isValid(): Boolean {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\CheckoutModels.kt:45:fun CheckoutCustomerInfo.fullAddress(): String {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\CheckoutModels.kt:52:fun paymentIntentIdFromClientSecret(clientSecret: String): String {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\components\AppBars.kt:38:fun VinceTopBar(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\presentation\components\AppBars.kt:95:fun VinceBottomBar(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\Entities.kt:7:data class ProductEntity(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\Entities.kt:21:data class CategoryEntity(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\Entities.kt:31:data class FeaturedSlideEntity(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\Entities.kt:39:data class CartItemEntity(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\LogoutUseCase.kt:5:class LogoutUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CheckoutDto.kt:6:data class CreatePaymentIntentItemDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CheckoutDto.kt:13:data class CreatePaymentIntentRequestDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CheckoutDto.kt:25:data class CreatePaymentIntentResponseDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CheckoutDto.kt:31:data class ApiErrorDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\Category.kt:6:data class Category(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\Category.kt:15:fun Category.localizedName(languageCode: String): String {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\DatabaseFactory.kt:9:expect fun platformDatabaseBuilder(databaseFileName: String): RoomDatabase.Builder<VinceProShopDatabase>
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\local\DatabaseFactory.kt:11:fun createRoomDatabase(): VinceProShopDatabase {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCatalogApi.kt:7:class KtorCatalogApi(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\data\local\DatabaseFactory.android.kt:7:actual fun platformDatabaseBuilder(databaseFileName: String): RoomDatabase.Builder<VinceProShopDatabase> {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CatalogDto.kt:7:data class CategoryDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CatalogDto.kt:17:data class ProductOptionDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CatalogDto.kt:26:data class ProductMediaDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CatalogDto.kt:31:data class ProductDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CatalogDto.kt:49:data class FeaturedSlideDto(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\model\CartItem.kt:3:data class CartItem(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\LoginUseCase.kt:6:class LoginUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\NetworkPlatform.kt:7:expect fun createPlatformHttpClient(engine: HttpClientEngine, json: Json, enableLogs: Boolean = true): HttpClient
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\NetworkPlatform.kt:8:expect fun defaultApiBaseUrl(): String
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\iosMain\kotlin\com\billiardsdraw\vinceproshop\data\local\DatabaseFactory.ios.kt:11:actual fun platformDatabaseBuilder(databaseFileName: String): RoomDatabase.Builder<VinceProShopDatabase> {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\GetUserOrdersUseCase.kt:6:class GetUserOrdersUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\CreatePaymentIntentUseCase.kt:7:class CreatePaymentIntentUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\GetAdminOrdersUseCase.kt:6:class GetAdminOrdersUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorAccountApi.kt:9:class KtorAccountApi(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCheckoutApi.kt:9:class KtorCheckoutApi(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\CheckoutApi.kt:3:interface CheckoutApi {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\AddCartItemUseCase.kt:6:class AddCartItemUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\domain\usecase\ClearCartUseCase.kt:5:class ClearCartUseCase(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\presentation\payment\StripePaymentButton.android.kt:21:actual fun StripePaymentButton(
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\androidMain\kotlin\com\billiardsdraw\vinceproshop\core\Localization.android.kt:5:actual fun currentLanguageCode(): String = Locale.getDefault().language
```

## Anexo C: Endpoints HTTP Consumidos Desde KMP
`	ext
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCheckoutApi.kt:15:        val response = httpClient.post(url("payments/create-intent")) {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCheckoutApi.kt:30:    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/$path"
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCatalogApi.kt:13:        return httpClient.get(url("products")).body()
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCatalogApi.kt:17:        return httpClient.get(url("products/categories")).body()
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCatalogApi.kt:21:        return httpClient.get(url("products/featured")).body()
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCatalogApi.kt:26:            httpClient.get(url("products/$slug")).body<ProductDto>()
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorCatalogApi.kt:30:    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/$path"
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorAccountApi.kt:15:        return httpClient.post(url("auth/login")) {
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorAccountApi.kt:21:        httpClient.post(url("auth/logout"))
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorAccountApi.kt:25:        return httpClient.get(url("auth/me")).body()
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorAccountApi.kt:29:        return httpClient.get(url("users/orders")).body()
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorAccountApi.kt:33:        return httpClient.get(url("admin/orders")).body()
C:\Users\nano9\AndroidStudioProjects\vinceproshop-app\composeApp\src\commonMain\kotlin\com\billiardsdraw\vinceproshop\data\remote\KtorAccountApi.kt:36:    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/$path"
```
