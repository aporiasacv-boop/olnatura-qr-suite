# Diseño — Trazabilidad del Estado Operativo (sin implementación)

**Fecha:** 28 de julio de 2026  
**Estado:** Diseño / preparación arquitectónica  
**Alcance:** Consulta por lote (`GET /api/v1/qr/{lote}`)  
**Restricciones de esta fase:** sin consultas nuevas a Dynamics, sin cambios de endpoints, sin UI, sin implementación.

**Relacionado:** `ARQUITECTURA-ESTADOS-LOTE.md`, `INVESTIGACION-DYNAMICS-ESTADO-Y-TRAZABILIDAD-LOTES.md`, `OperationalStatusResolver`

---

## 1. Punto único de construcción (Consulta por lote)

La respuesta de Consulta por lote se ensambla en **una cadena clara** con dos niveles:

```
GET /api/v1/qr/{lote}
    └── QrController.getByLote()
            └── QrQueryService.getByLote()          ← ensamblaje final (label BD + Dynamics + permisos)
                    ├── qrLabelRepository           ← datos de etiqueta (PostgreSQL)
                    ├── dynamicsLookupService.lookupByBatchNumber()
                    │       └── DynamicsLookupService.executeLookup()   ← ★ PUNTO ÚNICO Dynamics
                    │               ├── DynamicsClient (OData HTTP)
                    │               └── OperationalStatusResolver.resolve()  ← Estado Operativo (no mutar)
                    └── toDynamicDto()            ← mapeo DynamicsLookupDto → QrDto.Dynamic
```

### 1.1 Punto único para enriquecer trazabilidad Dynamics

| Rol | Clase | Método | Responsabilidad |
|-----|-------|--------|-----------------|
| **Orquestación Dynamics** | `DynamicsLookupService` | `executeLookup()` | Único lugar donde se consultan entidades OData, se interpreta el estado y se construye `DynamicsLookupDto` |
| **Cliente HTTP** | `RealDynamicsClient` / `MockDynamicsClient` | métodos `find*` | Solo transporte OData; sin reglas de negocio |
| **Estado Operativo** | `OperationalStatusResolver` | `resolve()` | **Fuera de alcance** de trazabilidad; no modificar reglas |
| **Fusión API** | `QrQueryService` | `getByLote()` + `toDynamicDto()` | Agrega `platformStatus` desde BD; expone JSON al cliente |

**Regla de diseño:** cualquier dato nuevo de trazabilidad del Estado Operativo debe **entrar en `DynamicsLookupDto` dentro de `executeLookup()`** y propagarse hacia arriba vía `toDynamicDto()`. No duplicar lógica en Web/Android ni en otros controladores.

### 1.2 Consumidores actuales (sin cambiar en esta fase)

| Cliente | Ruta | Uso |
|---------|------|-----|
| Web | `BatchLookupPage` → `GET /qr/{lote}` | Centro de información del lote |
| Android | `ResultViewModel` → `OlnaturaApi.getQr()` | Pantalla post-escaneo |
| Web (aux.) | `GenerateQrPage` | Reimpresión (solo datos de etiqueta) |
| API aux. | `DynamicsLookupController` | Lookup Dynamics aislado (no es Consulta por lote) |

---

## 2. Separación de responsabilidades (vigente)

| Bloque | Fuente | Mutable desde app |
|--------|--------|-------------------|
| **Estado Operativo** (`dynamic.status`) | `OperationalStatusResolver` ← Dynamics | **No** |
| **Trazabilidad operativa** (propuesta) | Dynamics (solo lectura) | **No** |
| **Estado de plataforma** (`dynamic.platformStatus`) | `qr_labels.status` | Sí (workflow / corrección admin) |
| **Datos de etiqueta** (`label.*`) | PostgreSQL | Sí (registro / corrección admin) |

La trazabilidad propuesta es **informativa**: complementa el banner de Estado Operativo; no lo sustituye ni lo modifica.

---

## 3. Estructura propuesta (DTO anidado)

Introducir un bloque opcional **`operationalTraceability`** (nombre tentativo) en la cadena de DTOs, **sin romper contratos existentes** (campos nuevos nullable / objeto anidado opcional).

### 3.1 Modelo lógico (Java / TypeScript equivalente)

```text
OperationalTraceability
├── qualityValidation
│   ├── validatedAt              ← QualityOrderHeaders.ValidatedDateTime (ISO-8601)
│   ├── validatingPersonnelNumber ← QualityOrderHeaders.ValidatingPersonnelNumber
│   ├── validatingEmployeeName    ← BaseWorkers (resuelto por personnel number)
│   └── qualityOrderNumber        ← QualityOrderHeaders.QualityOrderNumber (contexto; lotes con >1 orden)
├── warehouseMovement             ← solo relevante si regla = Almacén REM|RES|CUARENTENA
│   ├── targetWarehouse           ← InventDim.InventLocationId (REM / RES / CUARENTENA)
│   ├── movedAt                   ← pendiente investigación (InventTrans / InventDim)
│   └── source                    ← "Dynamics" | null si no resuelto
└── dataAvailability              ← metadatos de completitud (opcional, UX futura)
    ├── qualityValidationAvailable: boolean
    └── remResMovementAvailable: boolean
```

### 3.2 Ubicación en la cadena de DTOs

| Capa | Tipo actual | Extensión propuesta |
|------|-------------|---------------------|
| Infra Dynamics | `DynamicsLookupDto` | `OperationalTraceability operationalTraceability` |
| API REST | `QrDto.Dynamic` | mismo campo anidado |
| Web | `types.ts` / `QrResponse.dynamic` | tipo `OperationalTraceability` |
| Android | `Models.kt` → `DynamicDto` | `OperationalTraceabilityDto?` |

### 3.3 Reglas de selección de la orden de calidad

Hoy `RealDynamicsClient.findQualityOrderByItemBatch` usa `$top=1` sobre `QualityOrderHeaders`.

Para trazabilidad, en la **fase de implementación** habrá que definir:

1. Si el lote tiene **varias** órdenes (ej. 3447 tiene OCL0015574 y OCL0015575), elegir la fila con:
   - `QualityOrderStatus = Pass`, y
   - `PassedBatchDispositionCode` poblado **o** `ValidatedDateTime` más reciente entre las órdenes `Pass`.
2. Documentar la regla en código (clase dedicada p.ej. `QualityOrderTraceabilitySelector`) **dentro de** `infra.dynamics`, invocada desde `executeLookup()`.

> Evidencia live: `scripts/out/dynamics-estado-lotes-20260727-193858/3447-11-Quality-full.json`

### 3.4 Regla para fecha movimiento REM/RES

**Estado (2026-07-28):** fecha REM **validada** en 3 lotes reales → `InventTransCDSEntities.DatePhysical`. RES/CUARENTENA pendientes de muestra.

Algoritmo confirmado para REM:

1. Obtener `inventDimId` de dims con `InventLocationId = REM` (vía `findInventDimsByBatch`).
2. Consultar `InventTransCDSEntities` por esos `inventDimId`.
3. Tomar `MIN(DatePhysical)` donde fecha ≠ sentinel `1900-01-01` y preferir filas con `Qty ≠ 0`.
4. En la muestra: `StatusReceipt=Purchased`, `StatusIssue=None`, voucher `DTI*`.

**Usuario del movimiento:** no existe en InventTrans OData. Opcional no certificado: `InventDimBiEntities.SysModifiedBy`.

Detalle: `INVESTIGACION-TRAZABILIDAD-ESTADO-OPERATIVO-8-LOTES.md` §3–4.

---

## 4. Entidades Dynamics participantes

### 4.1 Ya consultadas hoy (lookup existente)

| Entidad OData | Uso actual | Campos relevantes hoy |
|---------------|------------|------------------------|
| `ItemBatches` | Lote / disposición | `BatchNumber`, `BatchDispositionCode`, `BatchExpirationDate` |
| `InventorySitesOnHand` | Inventario / nombre | `AvailableOnHandQuantity`, producto |
| `ReleasedProductsV2` | Unidad | `InventoryUnitSymbol` |
| `InventDimBiEntities` | Almacenes del lote | `inventDimId`, `InventLocationId`, `wMSLocationId` |
| `InventTransCDSEntities` | Fecha entrada | `DatePhysical`, `StatusReceipt` |
| `QualityOrderHeaders` | Calidad / almacén orden | `QualityOrderStatus`, `PassedBatchDispositionCode`, `WarehouseId`, `WarehouseLocationId` |

### 4.2 A enriquecer en la misma consulta (sin entidad nueva)

Extender el `$select` de la consulta **ya existente** a `QualityOrderHeaders`:

| Campo OData | Destino DTO | Notas |
|-------------|-------------|-------|
| `ValidatedDateTime` | `qualityValidation.validatedAt` | Evidencia en probes (3363, 3447, 3390) |
| `ValidatingPersonnelNumber` | `qualityValidation.validatingPersonnelNumber` | Ej. `3960`; no es usuario FO |
| `QualityOrderNumber` | `qualityValidation.qualityOrderNumber` | Desambiguación multi-orden |

**Archivo a tocar en implementación:** `RealDynamicsClient.findQualityOrderByItemBatch` (+ record `QualityOrderRecord`).

### 4.3 Consulta adicional prevista (implementación futura)

| Entidad OData | Propósito | Join |
|---------------|-----------|------|
| **`BaseWorkers`** (o entidad equivalente publicada en FO) | Nombre del empleado | `PersonnelNumber` = `ValidatingPersonnelNumber` |

> Validar nombre exacto de entidad y campo de nombre en metadatos OData de producción (`$metadata`).  
> Si `BaseWorkers` no está expuesta, alternativas: `HcmWorkers`, `Workers`, o mostrar solo número de personal.

### 4.4 Investigación pendiente — movimiento REM/RES

| Entidad | Rol |
|---------|-----|
| `InventDimBiEntities` | Identificar dims en REM/RES/CUARENTENA |
| `InventTransCDSEntities` | Fecha física del movimiento (filtros por confirmar) |
| *(opcional)* `InventTransfer*` / journals | Solo si OData estándar no alcanza — **fuera de v1** |

**No usar** como trazabilidad de negocio fiable:

- `InventDimBiEntities.SysModifiedBy` / `SysModifiedDateTime` (metadata técnica, no evento de rechazo).

---

## 5. Componentes a modificar (checklist implementación futura)

Cuando se autorice la funcionalidad, el orden recomendado es **de abajo hacia arriba** (infra → API → clientes → UI).

### 5.1 Backend — Infra Dynamics

| # | Archivo | Cambio |
|---|---------|--------|
| 1 | `DynamicsClient.java` | Ampliar `QualityOrderRecord`; opcional `WorkerRecord`; record `OperationalTraceability` |
| 2 | `RealDynamicsClient.java` | Extender `$select` QualityOrder; método `findWorkerByPersonnelNumber`; lógica REM/Trans (fase 2) |
| 3 | `MockDynamicsClient.java` | Datos mock alineados a lotes 3390/3447/3363/3109 |
| 4 | `DynamicsLookupDto.java` | Campo `OperationalTraceability operationalTraceability` |
| 5 | `DynamicsLookupService.java` | En `executeLookup()`: construir trazabilidad; selector multi-orden |
| 6 | *(nuevo, opcional)* `QualityOrderTraceabilitySelector.java` | Regla de elección de orden Pass / ValidatedDateTime |
| 7 | *(nuevo, fase REM)* `RemResMovementResolver.java` | Algoritmo fecha movimiento (post-investigación) |

**No modificar:** `OperationalStatusResolver.java` (reglas de estado).

### 5.2 Backend — API / ensamblaje

| # | Archivo | Cambio |
|---|---------|--------|
| 8 | `QrDto.java` | Añadir record anidado en `Dynamic` (o record top-level reutilizable) |
| 9 | `QrQueryService.java` | Propagar en `toDynamicDto()`; fallback `null` si Dynamics ausente |
| 10 | `QrController.java` | **Sin cambio de ruta** — mismo `GET /{lote}` |

**No modificar:** `ApprovalService`, `AdminStatusCorrectionService` (solo plataforma).

### 5.3 Backend — Tests

| # | Archivo | Cambio |
|---|---------|--------|
| 11 | `OperationalStatusResolverTest.java` | Sin cambios |
| 12 | *(nuevo)* `QualityOrderTraceabilitySelectorTest.java` | Multi-orden 3447 |
| 13 | *(nuevo)* `DynamicsLookupServiceTraceabilityTest.java` | Mock client + DTO completo |

### 5.4 Web

| # | Archivo | Cambio (cuando haya UI) |
|---|---------|-------------------------|
| 14 | `api/types.ts` | Tipo `OperationalTraceability` |
| 15 | `utils/displayLabels.ts` | Etiquetas ES |
| 16 | `pages/BatchLookupPage.tsx` | Bloque informativo bajo Estado Operativo o en técnico colapsable |

### 5.5 Android

| # | Archivo | Cambio (cuando haya UI) |
|---|---------|-------------------------|
| 17 | `Models.kt` | `OperationalTraceabilityDto` |
| 18 | `ResultScreen.kt` | Presentación (sin confundir con `platformStatus`) |

### 5.6 Documentación

| # | Archivo | Cambio |
|---|---------|--------|
| 19 | `INVESTIGACION-DYNAMICS-ESTADO-Y-TRAZABILIDAD-LOTES.md` | Cerrar § REM/RES + entidad BaseWorkers |
| 20 | `IMPLEMENTACION-ESTADO-OPERATIVO-DYNAMICS-v1.6.0.md` | Apéndice trazabilidad |
| 21 | Este documento | Marcar fases implementadas |

---

## 6. Flujo de datos propuesto (fase implementación)

```text
QualityOrderHeaders ──► ValidatedDateTime, ValidatingPersonnelNumber, QualityOrderNumber
                              │
                              ▼
                    QualityOrderTraceabilitySelector
                              │
BaseWorkers ◄── PersonnelNumber ──┘
                              │
                              ▼
              OperationalTraceability.qualityValidation
                              │
InventDim (REM/RES) ──► inventDimId ──► InventTrans (filtro TBD)
                              │
                              ▼
              OperationalTraceability.warehouseMovement.movedAt
                              │
                              ▼
                   DynamicsLookupDto (+ operationalStatus sin cambios)
                              │
                              ▼
                      QrDto.Dynamic
                              │
              ┌─────────────────┴─────────────────┐
              ▼                                   ▼
      BatchLookupPage (Web)              ResultScreen (Android)
```

---

## 7. Qué NO hacer

| Acción | Motivo |
|--------|--------|
| Escribir trazabilidad en PostgreSQL | Fuente = Dynamics; evitar divergencia |
| Mezclar trazabilidad con `platformStatus` | Workflow interno ≠ Estado Operativo |
| Cambiar `OperationalStatusResolver` | Fuera de alcance |
| Prometer “usuario que movió a REM” sin regla FO | No confirmado en OData estándar |
| Nueva ruta REST (`/qr/{lote}/trace`) | Mantener un solo payload en Consulta por lote |

---

## 8. Criterios de aceptación (implementación futura)

1. `GET /api/v1/qr/{lote}` incluye `dynamic.operationalTraceability` cuando Dynamics responde.
2. Sin Dynamics: trazabilidad `null` o objeto con flags `*Available = false`; **Estado Operativo** sigue igual.
3. `ValidatedDateTime` y `ValidatingPersonnelNumber` provienen de la orden de calidad seleccionada por regla documentada.
4. Nombre de empleado solo si `BaseWorkers` responde; si no, UI muestra número de personal.
5. `movedAt` solo se rellena cuando la investigación REM/RES esté cerrada y probada con lotes 3363/3109.
6. Web y Android muestran los mismos campos del mismo endpoint.

---

## 9. Estado de esta fase

| Entregable | Estado |
|------------|--------|
| Punto único identificado | ✅ `DynamicsLookupService.executeLookup()` |
| Estructura DTO diseñada | ✅ §3 |
| Componentes listados | ✅ §5 |
| Entidades Dynamics listadas | ✅ §4 |
| Consultas nuevas | ❌ No ejecutadas (por diseño) |
| Endpoints / UI / código | ❌ Sin cambios |

**Próximo paso recomendado (negocio + TI FO):** validar entidad `BaseWorkers` en `$metadata` y cerrar filtros InventTrans para movimiento a REM/RES antes de implementar §3.4.

---

## Actualización — investigación live 2026-07-28 (8 lotes)

Evidencia completa: `INVESTIGACION-TRAZABILIDAD-ESTADO-OPERATIVO-8-LOTES.md`.

| Tema | Resultado live |
|------|----------------|
| `BaseWorkers` | **Confirmada** — `PersonnelNumber`, `Name`, `FirstName`, `LastName` |
| `ValidationStatus` / `InventoryBatchId` | **No publicados** — usar `QualityOrderStatus` / `ItemBatchNumber` |
| Fecha movimiento REM | **Candidato definitivo:** `InventTransCDSEntities.DatePhysical` (3/3) |
| Usuario/empleado del movimiento REM | **No** en InventTrans; señal débil `InventDimBiEntities.SysModifiedBy` (login FO) |
| RES / CUARENTENA | **Sin lotes** en la muestra de 8 — patrón pendiente |

### Referencia cruzada

- Arquitectura de estados (Dynamics vs plataforma): `ARQUITECTURA-ESTADOS-LOTE.md`
- Evidencia OData live (estado): `INVESTIGACION-DYNAMICS-ESTADO-Y-TRAZABILIDAD-LOTES.md`
- Evidencia OData live (trazabilidad 8 lotes): `INVESTIGACION-TRAZABILIDAD-ESTADO-OPERATIVO-8-LOTES.md`
