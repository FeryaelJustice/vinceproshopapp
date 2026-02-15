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

## Configuracion de red

Base URL por defecto:

- Android: `http://10.0.2.2:4000/api`
- iOS: `http://127.0.0.1:4000/api`

Puedes ajustar estos valores en:

- `composeApp/src/androidMain/kotlin/com/billiardsdraw/vinceproshop/NetworkPlatform.android.kt`
- `composeApp/src/iosMain/kotlin/com/billiardsdraw/vinceproshop/NetworkPlatform.ios.kt`

## Permisos

- Android: `INTERNET` en `composeApp/src/androidMain/AndroidManifest.xml`
- iOS: `NSAppTransportSecurity` (`NSAllowsArbitraryLoads`) en `iosApp/iosApp/Info.plist`

## Ejecutar

```bash
cd /Users/feryaeljustice/AndroidStudioProjects/vinceproshop-app
export JAVA_HOME="/Users/feryaeljustice/Library/Caches/JetBrains/Toolbox/backup/AndroidStudio-252.28238.7.2523.14688667-5590052531630616454/Contents/jbr/Contents/Home"
./gradlew :composeApp:assembleDebug
```

Para iOS, abrir `iosApp/iosApp.xcodeproj` y ejecutar el target `iosApp`.
