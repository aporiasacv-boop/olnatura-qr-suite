# Implementación Estado Operativo desde Dynamics — v1.6.0

**Fecha:** 27 de julio de 2026  
**Versión:** 1.6.0 (API `olnaturaqr-1.6.0.jar`, Android `versionCode` 10 / `versionName` 1.6.0)  
**Alcance:** Estado Operativo del lote con Dynamics 365 como única fuente de verdad del banner.

---

## 1. Resumen

Se eliminó el uso de `qr_labels.status` como fuente del **banner / Estado Operativo** en Consulta por lote (Web y Android).

A partir de v1.6.0 el sistema **interpreta** señales de Dynamics (almacén + disposición) y muestra un Estado Operativo:

| Presentación | Código |
|--------------|--------|
| 🟢 Aprobado | `APROBADO` |
| 🟡 Cuarentena | `CUARENTENA` |
| 🔴 Rechazado | `RECHAZADO` |
| ⚪ Estado no determinado | `DESCONOCIDO` |

`qr_labels.status` se conserva solo como **estado de plataforma** (aprobaciones / corrección admin / auditoría interna), expuesto como `dynamic.platformStatus` (no controla el banner).

**Consolidación (post v1.6.1):** ninguna acción de la app puede modificar el Estado Operativo.  
Ver `ARQUITECTURA-ESTADOS-LOTE.md`.

**Trazabilidad operativa (diseño futuro):** `DISENO-TRAZABILIDAD-ESTADO-OPERATIVO.md`.

**Sincronización manual (v1.6.3):** `SYNC-DYNAMICS-MANUAL.md` — solo lectura OData; no escribe en Dynamics.

---

## 2. Nueva lógica implementada

Clase: `OperationalStatusResolver`

### Tabla de reglas

| Prioridad | Condición | Estado Operativo | Regla aplicada (UI) |
|-----------|-----------|------------------|---------------------|
| 1 | Algún almacén = `REM` (InventDim `InventLocationId` o Quality `WarehouseId`) | **RECHAZADO** | Almacén REM |
| 2 | Algún almacén = `RES` | **RECHAZADO** | Almacén RES |
| 3 | Algún almacén = `CUARENTENA` | **CUARENTENA** | Almacén CUARENTENA |
| 4 | Otro almacén + `BatchDispositionCode` aprobado (`Aprobado`, `Disponible`, …) | **APROBADO** | BatchDispositionCode |
| 4b | Otro almacén + disposición rechazo / cuarentena | RECHAZADO / CUARENTENA | BatchDispositionCode |
| 4c | Otro almacén + disposición vacía (p.ej. MPM) | **APROBADO** (liberado implícito) | BatchDispositionCode |
| 5 | Sin Dynamics / datos insuficientes / disposición no reconocida | **DESCONOCIDO** | Información insuficiente |

**Importante:** el almacén decisivo se lee de **InventDim** (`InventLocationId`), porque Quality `WarehouseId` suele seguir en `MPM` aunque el lote ya tenga dimensión en `REM`.

---

## 3. Cambios realizados

### Backend
- Nuevo `OperationalStatusResolver` + tests unitarios.
- `InventDimBiEntities` ahora selecciona `InventLocationId`, `wMSLocationId`.
- `DynamicsLookupService` calcula Estado Operativo y enriquece el DTO.
- `QrQueryService`: `dynamic.status` = Estado Operativo Dynamics (ya **no** `WorkflowStatus.normalize(label.getStatus())`).
- Respuesta enriquecida: `operationalStatusRule`, `statusSource`, `platformStatus`.
- Mock Dynamics ampliado con los 4 lotes de validación.

### Web
- Banner: **Estado Operativo** con emoji + color.
- Transparencia: Fuente + Regla aplicada.
- Campos técnicos (`QualityOrderStatus`, disposiciones, Open/Pass/Fail, almacén/ubicación) en bloque **colapsable**.
- Corrección admin / permisos usan `platformStatus`.

### Android
- Banner con etiqueta operativa + Fuente / Regla.
- Modelo `DynamicDto` ampliado.
- Corrección admin usa `platformStatus`.

### No modificado
- Auditoría, historial de escaneos, comentarios, roles, seguridad, BD, OAuth.
- Endpoints existentes (solo enriquecimiento de respuesta).

---

## 4. Archivos modificados / nuevos

| Archivo | Cambio |
|---------|--------|
| `.../support/workflow/OperationalStatusResolver.java` | **Nuevo** — reglas |
| `.../support/workflow/OperationalStatusResolverTest.java` | **Nuevo** — tests |
| `.../infra/dynamics/DynamicsClient.java` | `InventDimRecord` + `findInventDimsByBatch` |
| `.../infra/dynamics/RealDynamicsClient.java` | Select InventDim ampliado |
| `.../infra/dynamics/MockDynamicsClient.java` | Lotes 3390/3447/3363/3109 |
| `.../infra/dynamics/DynamicsLookupDto.java` | Campos operativos |
| `.../infra/dynamics/DynamicsLookupService.java` | Orquestación + resolve |
| `.../api/QrDto.java` | `status` operativo + rule/source/platform |
| `.../support/qr/QrQueryService.java` | Banner desde Dynamics |
| `.../web/.../BatchLookupPage.tsx` | UI Estado Operativo |
| `.../web/.../StatusTag.tsx` | DESCONOCIDO + emojis |
| `.../web/.../displayLabels.ts` | Etiquetas |
| `.../web/.../api/types.ts` | Tipos Dynamics |
| `OlnaturaQR/.../Models.kt` | DTO Android |
| `OlnaturaQR/.../StatusBanner.kt` | Labels operativos |
| `OlnaturaQR/.../ResultScreen.kt` | Banner + transparencia |
| `olnaturaqr/build.gradle` | `1.6.0` |
| `OlnaturaQR/app/build.gradle.kts` | `1.6.0` / code 10 |

---

## 5. Evidencia con los cuatro lotes

Basada en OData live (investigación 2026-07-27) + reglas unitarias / mock.

| Lote | Señales Dynamics | Estado Operativo | Regla | ¿Coincide validación? |
|------|------------------|------------------|-------|------------------------|
| `260406-MPM0003390` | InventDim `MPM`; `BatchDispositionCode` vacío | **APROBADO** | BatchDispositionCode | Sí |
| `260206-MPM0003363` | InventDim `MPM` + **`REM`**; disp. `Aprobado` | **RECHAZADO** | Almacén REM | Sí |
| `240923-MPM0003109` | InventDim `MPM` + **`REM`**; disp. vacío | **RECHAZADO** | Almacén REM | Sí |
| `260707-MPM0003447` | InventDim `MPM`; disp. `Aprobado` | **APROBADO** | BatchDispositionCode | Sí (Dynamics liberado; no CUARENTENA en FO) |

### Confirmación `qr_labels.status`

| Campo | ¿Controla el banner? |
|-------|----------------------|
| `qr_labels.status` / `dynamic.platformStatus` | **No** |
| `dynamic.status` (Estado Operativo) | **Sí** — Dynamics |

---

## 6. Capturas (mockups de la nueva presentación)

Ubicación: `docs/capturas-v1.6.0/`

| Archivo | Caso |
|---------|------|
| `captura-estado-operativo-3390-aprobado.png` | Aprobado |
| `captura-estado-operativo-3363-rechazado.png` | Rechazado (REM) |
| `captura-estado-operativo-3109-rechazado.png` | Rechazado (REM) |
| `captura-estado-operativo-3447-aprobado.png` | Aprobado (liberado en Dynamics) |

> Son mockups de la nueva UI (Estado Operativo + Fuente + Regla + detalle técnico colapsable). Validar en runtime con Web/Android contra producción o mock.

---

## 7. Artefactos

| Artefacto | Ruta |
|-----------|------|
| JAR API | `olnatura-qr/api/olnaturaqr/build/libs/olnaturaqr-1.6.0.jar` |
| APK | `OlnaturaQR/app/build/outputs/apk/release/app-release.apk` (también `OlnaturaQR-1.6.0.apk`) |
| Tests | `OperationalStatusResolverTest` — OK |

---

## 8. Transparencia en UI

Debajo del Estado Operativo:

```
Fuente: Dynamics 365 Finance & Operations
Regla aplicada: Almacén REM | Almacén RES | Almacén CUARENTENA | BatchDispositionCode
```

Bloque colapsable **Detalle técnico Dynamics**:

- Estado de Dynamics / QualityOrderStatus  
- PassedBatchDispositionCode / BatchDispositionCode  
- Almacén / Ubicación  
- Fuente de datos  
- Estado plataforma (histórico)

---

## 9. Notas

1. Lote **3447**: Dynamics lo reporta liberado (`Aprobado` + `MPM`). El Estado Operativo es **APROBADO**, no CUARENTENA.  
2. Si en el futuro se requiere `REM-D` / `RES-D`, ampliar la regla 1–2.  
3. Sin respuesta Dynamics → **DESCONOCIDO** (no se asume CUARENTENA desde BD).
