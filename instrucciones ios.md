# Instrucciones iOS (KMP + Compose)

## Flujo recomendado

1. Usa Android Studio para editar código Kotlin/KMP y sincronizar Gradle.
2. Usa Xcode para compilar y ejecutar la app iOS (simulador o dispositivo).
3. No es obligatorio compilar `composeApp` antes de abrir Xcode.
4. Sí es recomendable prevalidar `composeApp` cuando hay cambios grandes en KMP, para detectar errores antes.

## ¿Compilar iOS desde Android Studio o Xcode?

- Recomendado: **siempre desde Xcode** para el build/run final de iOS.
- Android Studio sirve para:
  - editar código,
  - sincronizar Gradle,
  - lanzar tareas Gradle de validación.

## ¿Hace falta compilar primero `composeApp`?

- **No es obligatorio**.
- **Sí recomendable** como validación previa cuando hubo cambios en:
  - `commonMain`,
  - `iosMain`,
  - DI,
  - networking/Stripe/security.

Comandos de validación previa (opcionales):

```bash
./gradlew :composeApp:compileKotlinIosSimulatorArm64 --no-configuration-cache
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 --no-configuration-cache
```

## Configuración mínima que debe estar bien

### 1) `iosApp/Configuration/Config.xcconfig`

- `TEAM_ID=TU_TEAM_ID`
- `BUNDLE_ID=com.billiardsdraw.vinceproshop` (o tu bundle único)
- `APP_NAME=Vince Pro Shop`

### 2) `local.properties` (raíz del proyecto)

- `VINCE_API_BASE_URL=https://.../api`
- `VINCE_STRIPE_PUBLISHABLE_KEY=pk_...`
- `VINCE_STRIPE_MERCHANT_DISPLAY_NAME=Vince Pro Shop` (opcional)
- `VINCE_HTTP_LOGS_ENABLED=true` (opcional)

### 3) Xcode (target `iosApp`)

- `Signing & Capabilities` con Team correcto.
- Bundle Identifier válido y único.
- Stripe iOS agregado por Swift Package Manager:
  - repo: `https://github.com/stripe/stripe-ios`
  - producto: `StripePaymentSheet` enlazado al target `iosApp`.

## Orden práctico recomendado para trabajar

1. Edita en Android Studio.
2. (Opcional) corre validación de `composeApp` para iOS.
3. Abre `iosApp/iosApp.xcodeproj`.
4. `Product > Clean Build Folder` si hiciste cambios grandes.
5. Ejecuta en simulador.
6. Después prueba en dispositivo físico (con firma válida).

## Buenas prácticas para evitar errores frecuentes

1. No dejes `TEAM_ID` vacío.
2. Verifica que Stripe SPM esté realmente agregado al target `iosApp`.
3. Evita valores mal formateados en `local.properties` (saltos de línea/comillas extrañas).
4. Tras cambios de Gradle/KMP, vuelve a sincronizar en Android Studio antes de ejecutar en Xcode.
