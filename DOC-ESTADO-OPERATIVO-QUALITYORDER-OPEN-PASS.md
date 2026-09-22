# Estado Operativo — QualityOrder Open/Pass (v1.6.x)

**Fecha:** 28 de julio de 2026  
**Alcance:** Solo `OperationalStatusResolver` + wiring en `DynamicsLookupService` + tests.  
**Sin cambios:** endpoints, contratos REST, frontend, Android, ApprovalService, PlatformStatus, sync.

---

## 1. Evidencia utilizada

Consulta OData live contra `https://olnatura-produccion.operations.dynamics.com`:

| Entidad | Campos |
|---------|--------|
| `ItemBatches` | `BatchNumber`, `BatchDispositionCode` |
| `InventDimBiEntities` | `inventBatchId`, `InventLocationId` |
| `QualityOrderHeaders` | `ItemBatchNumber`, `QualityOrderStatus`, `ValidatedDateTime`, `WarehouseId` |

Artefacto de validación: `scripts/out/operational-status-qo-validation.json`

Contexto de negocio confirmado: en Materia Prima la cuarentena **no** implica mover el lote al almacén `CUARENTENA`; el lote permanece en `MPS`/`Disponible` con orden de calidad `Open` hasta la liberación (`Pass` + `Aprobado`).

---

## 2. Tabla de validación (12 lotes)

| Batch | InventLocation | QualityOrderStatus | ValidatedDateTime | BatchDispositionCode | Estado actual (antes) | Estado esperado (nuevo) | Match |
|-------|----------------|--------------------|-------------------|----------------------|-----------------------|-------------------------|-------|
| 260206-MPM0003363 | MPM,REM | Pass | 2026-02-17 | Aprobado | RECHAZADO | RECHAZADO | Sí |
| 240923-MPM0003109 | MPM,REM | (vacío) | (vacío) | (vacío) | RECHAZADO | RECHAZADO | Sí |
| 260619-MEM0003625 | MEM,REM | Pass | 2026-06-26 | (vacío) | RECHAZADO | RECHAZADO | Sí |
| 240412-MPS0005078 | MPS,RES | Pass | 2024-04-23 | Aprobado | RECHAZADO | RECHAZADO | Sí |
| 240419-MPS0005089 | MPS,RES | Pass | 2024-04-25 | Aprobado | RECHAZADO | RECHAZADO | Sí |
| 260727-MPS0006649 | MPS | **Open** | 1900-01-01 | (vacío) | **APROBADO** | **CUARENTENA** | Sí |
| 260720-MPS0006641 | MPS | Pass | 2026-07-27 | Aprobado | APROBADO | APROBADO | Sí |
| 260203-MPS0006246 | MPS | Pass | 2026-02-20 | Aprobado | APROBADO | APROBADO | Sí |
| 260410-MPS0006465 | MPS | (sin QO) | — | (vacío) | APROBADO | **DESCONOCIDO** | Sí* |
| 260716-MEM0003666 | MEM | Pass | 2026-07-20 | Aprobado | APROBADO | APROBADO | Sí |
| 260406-MEM0003559 | MEM | Pass | 2026-04-08 | Aprobado | APROBADO | APROBADO | Sí |
| 260525-MPM0003416 | MPM | Pass | 2026-06-02 | Aprobado | APROBADO | APROBADO | Sí |

\*Excepción documentada: sin `QualityOrderStatus` ni disposición, la regla nueva ya **no** asume APROBADO solo por existir almacén operativo (antes sí lo hacía). Pasa a `DESCONOCIDO`.

Patrón propuesto vs esperado: **12/12**.

---

## 3. Regla final implementada

Prioridad en `OperationalStatusResolver.resolve(...)`:

1. Almacén **REM** → `RECHAZADO` (`Almacén REM`)
2. Almacén **RES** → `RECHAZADO` (`Almacén RES`)
3. Almacén **CUARENTENA** → `CUARENTENA` (`Almacén CUARENTENA`) — compatibilidad
4. `QualityOrderStatus` pendiente (`Open`, `Opened`, `Pending`, `InProgress`, `Started`, `Draft`) → `CUARENTENA` (`QualityOrderStatus Open`)
5. `QualityOrderStatus` = `Pass`/`Passed` **y** `BatchDispositionCode` aprobado → `APROBADO` (`QualityOrder Pass + BatchDispositionCode`)
6. Si nada aplica → `DESCONOCIDO` (`Información insuficiente`)

Firma ampliada (centralizada):

```text
resolve(inventLocationIds, qualityWarehouseId, batchDispositionCode, qualityOrderStatus, dynamicsPresent)
```

Se conserva overload de 4 argumentos (pasa `qualityOrderStatus = null`) por compatibilidad interna.

Wiring: `DynamicsLookupService` envía el `qualityOrderStatus` ya leído de Dynamics (sin cambios de contrato REST).

---

## 4. Casos de prueba

Archivo: `OperationalStatusResolverTest.java`

| Caso | Entrada clave | Esperado |
|------|---------------|----------|
| REM gana con Pass+Aprobado | REM + Pass + Aprobado | RECHAZADO |
| RES gana con Pass+Aprobado | RES + Pass + Aprobado | RECHAZADO |
| Almacén CUARENTENA | CUARENTENA | CUARENTENA |
| MPS0006649 Open | MPS + Open | CUARENTENA |
| MPS0006641 Pass+Aprobado | MPS + Pass + Aprobado | APROBADO |
| Pass sin disposición | Pass + disp vacío | DESCONOCIDO |
| Solo almacén sin QO | MPS + null QO | DESCONOCIDO |
| Open con disposición Aprobado | Open + Aprobado | CUARENTENA (Open gana) |
| Sin Dynamics | dynamicsPresent=false | DESCONOCIDO |

---

## 5. Resultado de regresión

Lotes de control de la investigación:

| Lote | Señal | Resultado nuevo |
|------|-------|-----------------|
| MPM0003363 | REM | RECHAZADO |
| MPS0005078 | RES | RECHAZADO |
| MPS0005089 | RES | RECHAZADO |
| MPS0006649 | Open | CUARENTENA |
| MPS0006641 | Pass + Aprobado | APROBADO |

---

## 6. Arquitectura no alterada

| Componente | ¿Modificado? |
|------------|--------------|
| Endpoints / controllers | No |
| Contratos REST / DTOs públicos | No |
| Frontend | No |
| Android | No |
| ApprovalService / PlatformStatus | No |
| Sync Dynamics | No |
| `OperationalStatusResolver` | **Sí** (única fuente de verdad del Estado Operativo) |
| `DynamicsLookupService` | Solo pasa `qualityOrderStatus` al resolver |
| Tests del resolver | **Sí** |

Confirmación: el Estado Operativo sigue calculándose exclusivamente en `OperationalStatusResolver`.
