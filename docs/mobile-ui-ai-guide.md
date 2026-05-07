# Guia para IA local: UI movil

## Objetivo

Mejorar vistas en Jetpack Compose sin romper la logica de negocio, sincronizacion offline, autenticacion ni contratos API.

## Estilo actual

- Fondo general claro: gris/azulado suave.
- Tarjetas blancas con radios amplios entre `20.dp` y `26.dp`.
- Azul primario para acciones principales.
- Verde para exito/cobros completados.
- Rojo para errores, mora o conflictos.
- Amarillo/ambar para pendientes o advertencias.
- Botones grandes, minimo `48.dp` de alto.
- Iconos Material/Lucide equivalentes de `Icons.Outlined` cuando existan.
- Titulos cortos, texto secundario discreto y escaneable.
- Evitar cards dentro de cards salvo secciones de contenido claramente separadas.
- Mantener densidad operativa: es una app de cobros, no una landing page.

## Archivos donde puede trabajar la IA local

Puede modificar:

- `app/src/main/java/com/sistemaprestamista/mobile/ui/*Screen.kt`
- `app/src/main/java/com/sistemaprestamista/mobile/ui/components/*.kt`
- `app/src/main/java/com/sistemaprestamista/mobile/ui/theme/*.kt`

Con cuidado:

- `app/src/main/java/com/sistemaprestamista/mobile/ui/PrestamistaApp.kt`
- `app/src/main/java/com/sistemaprestamista/mobile/ui/navigation/AppDestination.kt`

No debe modificar sin revision senior:

- `app/src/main/java/com/sistemaprestamista/mobile/data/**`
- `app/src/main/java/com/sistemaprestamista/mobile/sync/**`
- `app/src/main/java/com/sistemaprestamista/mobile/printing/**`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`

## Reglas de botones

Todo boton debe tener accion real:

- Login: `onLogin`
- Huella: `onBiometricLogin`
- Recuperar/cambiar contrasena: `onForgotPassword` / `onResetPassword`
- Cobrar: confirmar antes de llamar `onRegisterPayment`
- Cliente: `onOpenClient`
- Prestamo: `onOpenLoan`
- Cuota: `onOpenInstallment`
- Recibo: `onOpenReceipt`
- Impresora: buscar, seleccionar, guardar, limpiar o imprimir prueba
- Pendientes: sincronizar, reintentar o descartar

Prohibido dejar:

- `onClick = { }`
- `TODO()`
- `NotImplementedError`
- Botones visibles que solo cambien color o no hagan nada

## Estados obligatorios

Cada pantalla operativa debe cubrir:

- Loading
- Empty state
- Error o conflicto cuando aplique
- Accion principal habilitada/deshabilitada correctamente
- Texto que no se corte en pantallas pequenas

## Flujo critico que no se debe romper

1. Login con correo y contrasena.
2. Login con huella si existe sesion guardada.
3. Recuperar contrasena y cambiarla con token.
4. Ver dashboard.
5. Ver lista de cobros/cuotas.
6. Registrar cobro.
7. Si no hay red, guardar cobro pendiente.
8. Ver cobros pendientes/fallidos.
9. Reintentar o descartar cobros fallidos.
10. Ver recibo.
11. Imprimir recibo termico o A4.
12. Configurar impresora.

## Validacion antes de entregar cambios

Ejecutar:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest assembleDebug
```

Buscar botones vacios:

```powershell
Get-ChildItem -Path app\src\main\java\com\sistemaprestamista\mobile\ui -Recurse -File |
    Select-String -Pattern 'onClick = \{ \}','onClick = \{\}','TODO','NotImplemented'
```

La entrega no es aceptable si la compilacion falla o si quedan botones sin accion.
