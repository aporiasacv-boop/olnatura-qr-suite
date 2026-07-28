# Investigación — Trazabilidad completa del Estado Operativo (8 lotes reales)

**Fecha OData:** 28 de julio de 2026  
**Entorno:** `https://olnatura-produccion.operations.dynamics.com`  
**Artefactos:** `scripts/out/dynamics-trazabilidad-full-20260728-102937/`  
**Scripts:** `scripts/dynamics-traceability-probe.ps1`, `scripts/dynamics-traceability-probe-full.ps1`  
**Alcance:** Solo investigación. Sin cambios de UI, endpoints ni `OperationalStatusResolver`.

---

## Identificación de lotes (BatchNumber completo)

OData **no soporta** `endswith`/`contains` sobre `ItemBatches.BatchNumber` (HTTP 400). Los sufijos se resolvieron a BatchNumber exactos:

| Sufijo pedido | BatchNumber Dynamics | ItemNumber |
|---------------|----------------------|------------|
| MEM0003675 | `260724-MEM0003675` | 400626402000 |
| MEM0003559 | `260406-MEM0003559` | 400615401502 |
| MEM0003666 | `260716-MEM0003666` | 400631402300 |
| MEM0003625 | `260619-MEM0003625` | 400610160202 |
| MPM0003416 | `260525-MPM0003416` | 101023130400 |
| MPM0003395 | `260410-MPM0003395` | 106623850000 |
| MPM0003109 | `240923-MPM0003109` | 106623700100 |
| MPM0003363 | `260206-MPM0003363` | 100623401100 |

---

## PARTE 1 — Quality Order Headers

### Disponibilidad de campos pedidos

| Campo pedido | ¿Existe en `QualityOrderHeaders`? | Evidencia |
|--------------|-----------------------------------|-----------|
| `QualityOrderNumber` | **Sí** | Presente en todas las órdenes halladas |
| `InventoryBatchId` | **No** | No aparece en `$metadata`/fila completa; filtrar por él → HTTP 400 |
| `ValidationStatus` | **No** | No publicado. Usar **`QualityOrderStatus`** (`Open` / `Pass` / `Fail`…) |
| `ValidatedDateTime` | **Sí** | ISO-8601; sentinel `1900-01-01T00:00:00Z` si aún no validada |
| `ValidatingPersonnelNumber` | **Sí** | String; vacío si no validada |
| `PassedBatchDispositionCode` | **Sí** | |
| `FailedBatchDispositionCode` | **Sí** | A menudo `Rechazado` aunque status sea `Pass` (plantilla de la orden) |

Campo adicional útil: `QMSAssignedToPersonnelNumber` (vacío en estos 8 lotes).

### Resultados por lote

| Lote | QO # | QualityOrderStatus | ValidatedDateTime | ValidatingPersonnelNumber | Passed | Failed | Estado Operativo actual* | ¿Calidad = Estado Operativo? |
|------|------|--------------------|-------------------|---------------------------|--------|--------|--------------------------|------------------------------|
| MEM0003675 | OCL0015656 | **Open** | 1900-01-01 (sin validar) | *(vacío)* | *(vacío)* | *(vacío)* | **APROBADO** (MEM + disp. vacía) | **No** — orden abierta; EO por almacén/disposición |
| MEM0003559 | OCL0015068 | Pass | 2026-04-08T00:09:32Z | **3726** | Aprobado | Rechazado | **APROBADO** | Alineado (Pass + Aprobado + sin REM) |
| MEM0003666 | OCL0015616 | Pass | 2026-07-20T16:47:13Z | **3726** | Aprobado | Rechazado | **APROBADO** | Alineado |
| MEM0003625 | OCL0015476 | Pass | 2026-06-26T20:25:24Z | **3726** | *(vacío)* | *(vacío)* | **RECHAZADO** (REM) | **No** — calidad Pass; EO por **REM** |
| MPM0003416 | OCL0015337 | Pass | 2026-06-02T20:03:39Z | **3960** | Aprobado | Rechazado | **APROBADO** | Alineado |
| MPM0003395 | *(ninguna)* | — | — | — | — | — | **APROBADO** (MPM + disp. vacía) | N/A — sin orden de calidad |
| MPM0003109 | *(ninguna)* | — | — | — | — | — | **RECHAZADO** (REM) | N/A — sin orden; EO por **REM** |
| MPM0003363 | OCL0014762 | Pass | 2026-02-17T00:07:56Z | **3960** | Aprobado | Rechazado | **RECHAZADO** (REM) | **No** — calidad Pass/Aprobado; EO por **REM** |

\*Estado Operativo calculado con la misma lógica que `OperationalStatusResolver` (InventDim + BatchDispositionCode). **No se modificó el resolver.**

### Conclusión Parte 1

1. La trazabilidad de **validación de calidad** sí se puede construir con:  
   `QualityOrderNumber` + `QualityOrderStatus` + `ValidatedDateTime` + `ValidatingPersonnelNumber` + Passed/Failed disposition.
2. **No** usar Quality Order como espejo 1:1 del Estado Operativo: en lotes REM el EO es RECHAZADO aunque la orden esté en Pass/Aprobado.
3. Lotes sin Quality Order (3395, 3109) son válidos: EO se determina solo con InventDim / BatchDisposition.
4. Sustituir `ValidationStatus` → `QualityOrderStatus`; omitir `InventoryBatchId` (no existe en OData publicado).

---

## PARTE 2 — BaseWorkers

### Entidad

| Entidad probada | Resultado |
|-----------------|-----------|
| **`BaseWorkers`** | **Publicada** (HTTP 200) |
| `Workers`, `HcmWorkers`, `DirPeopleV2`, `Employees` | 404 |

### Campos

| Campo | ¿Disponible? | Observado |
|-------|--------------|-----------|
| `PersonnelNumber` | Sí | Coincide con el filtro |
| `Name` | Sí | Nombre completo |
| `FirstName` | Sí | |
| `LastName` | Sí | Puede incluir apellidos compuestos |

### Resoluciones live

| PersonnelNumber | Name | FirstName | LastName | ¿Hallado? |
|-----------------|------|-----------|----------|-----------|
| **3726** | GIOVANA AVILES ZAGAL | GIOVANA | AVILES ZAGAL | Sí |
| **3960** | VIRGINIA AMARO VILLEGAS | VIRGINIA | AMARO VILLEGAS | Sí |

**Sin coincidencias fallidas** en este set: los dos números encontrados en Quality Orders resolvieron en `BaseWorkers`.

### Conclusión Parte 2

Candidato definitivo para nombre de empleado:  
`BaseWorkers` filtrado por `PersonnelNumber eq '{ValidatingPersonnelNumber}'` → preferir `Name` (o `FirstName` + `LastName`).

---

## PARTE 3 — InventTransCDSEntities (movimiento REM / RES / CUARENTENA)

### Campos pedidos vs publicados (fila completa, 67 propiedades)

| Campo pedido | ¿En InventTransCDSEntities? |
|--------------|------------------------------|
| `DatePhysical` | **Sí** |
| `DateFinancial` | **Sí** |
| `DateInvent` / `DateStatus` / `DateClosed` | **Sí** (adicionales) |
| `ModifiedDateTime` / `CreatedDateTime` | **No** |
| `ModifiedBy` / `CreatedBy` | **No** |
| `Worker` / `PersonnelNumber` / `UserId` | **No** |
| `InventLocationId` / `InventSiteId` | **No** (solo vía `inventDimId` → `InventDimBiEntities`) |
| `InventDimId` | **Sí** (`inventDimId`) |
| `ReferenceId` / `ReferenceCategory` / `ReferenceType` | **No** |
| `InventTransType` | **No** |
| `StatusIssue` / `StatusReceipt` | **Sí** |
| `InventTransOrigin` / `Voucher` | **Sí** (útiles como referencia, no usuario) |

Entidades de journals de transferencia (`InventTransfer*`, `InventJournal*`, etc.): **404** en este entorno — no hay puente OData estándar a usuario de transferencia.

### Cobertura de almacenes en los 8 lotes

| Almacén objetivo | ¿Presente en los 8? |
|------------------|---------------------|
| **REM** | Sí — MEM0003625, MPM0003109, MPM0003363 |
| **RES** | **No** en esta muestra |
| **CUARENTENA** | **No** en esta muestra |

No se puede validar con datos live el patrón RES/CUARENTENA en este set; la hipótesis de implementación se basa en el patrón REM observado (misma mecánica de InventDim).

### Movimientos REM observados (patrón consistente)

Método: `InventDimBiEntities` donde `InventLocationId = REM` → `InventTransCDSEntities` por `inventDimId`.

| Lote | inventDimId REM (con stock) | DatePhysical | DateFinancial | DateStatus | StatusReceipt | StatusIssue | Qty | Voucher |
|------|-----------------------------|--------------|---------------|------------|---------------|-------------|-----|---------|
| MEM0003625 | `#00000001500B0CC9` | **2026-07-27T12:00:00Z** | mismo día | mismo día | Purchased | None | 2146 | DTI0006266 |
| MPM0003109 | `#00000001500AC2DD` | **2026-05-07T12:00:00Z** | mismo día | mismo día | Purchased | None | 247675 | DTI0005865 |
| MPM0003363 | `#00000001500ABFD2` | **2026-04-22T12:00:00Z** | mismo día | mismo día | Purchased | None | 41115 | DTI0005793 |

Patrón en los 3/3 casos REM con transacción:

1. Una sola fila relevante en el dim REM con ubicación `General`.
2. `DatePhysical` = `DateFinancial` = `DateStatus` (hora tipicamente `12:00:00Z`).
3. `DateInvent` / `DateClosed` suelen ser sentinel `1900-01-01` o cierre contable posterior — **no** usar como fecha de movimiento a REM.
4. `StatusReceipt = Purchased`, `StatusIssue = None`, `Qty > 0`.
5. Voucher serie `DTI*`.

### ¿Quién hizo el movimiento?

**No existe** en `InventTransCDSEntities` ningún campo de usuario/empleado.

Señal colateral en **`InventDimBiEntities`** (fila completa, no en el `$select` actual de la app):

| Lote | DatePhysical (día) | SysCreatedDateTime | SysModifiedBy |
|------|--------------------|--------------------|---------------|
| MEM0003625 | 2026-07-27 | 2026-07-27T15:48:47Z | **marco.anguiano** |
| MPM0003109 | 2026-05-07 | 2026-05-07T15:16:04Z | **marco.anguiano** |
| MPM0003363 | 2026-04-22 | 2026-04-22T18:09:05Z | **marisol.ruiz** |

- Coincide el **día** de creación de la dimensión REM con `DatePhysical`.
- `SysModifiedBy` es **usuario FO** (login), **no** `PersonnelNumber` / `BaseWorkers`.
- Es metadata técnica de la dimensión (primera vez que existe esa combinación batch+almacén+ubicación), **no** un evento de auditoría de negocio certificado.

### Candidatos definitivos (Parte 3)

| Pregunta de negocio | Campo / método | Confianza |
|---------------------|----------------|-----------|
| **Fecha movimiento a REM** | `InventTransCDSEntities.DatePhysical` (MIN ≠ 1900) del `inventDimId` con `InventLocationId=REM` | **Alta** (3/3) |
| Fecha alternativa | `DateFinancial` / `DateStatus` | Alta correlación, redundante con Physical en esta muestra |
| Fecha movimiento a RES / CUARENTENA | Misma mecánica (no muestreada aquí) | **Pendiente** de lotes RES/CUARENTENA |
| **Usuario del movimiento** | **No disponible** en InventTrans OData estándar | — |
| Señal débil de usuario FO | `InventDimBiEntities.SysModifiedBy` + `SysCreatedDateTime` | Baja / no certificable |
| Empleado (PersonnelNumber) del movimiento | **No existe** en estas entidades | — |

---

## PARTE 4 — Comparativa y candidatos definitivos

### Consistencias

1. **Estado Operativo ≠ Quality Order** cuando hay REM (3 lotes).
2. Cuando hay validación Pass + sin REM → EO APROBADO alineado con Passed/BatchDisposition.
3. `ValidatingPersonnelNumber` → `BaseWorkers` resolvió **100%** (2/2).
4. Fecha REM → **`DatePhysical`** estable en 3/3.
5. InventTrans **nunca** expone actor humano en este tenant.

### Excepciones / casos borde

| Caso | Detalle |
|------|---------|
| MEM0003675 | QO **Open**, sin validación; EO igual APROBADO |
| MPM0003395 | Sin Quality Order; EO APROBADO por MPM + disp. vacía |
| MPM0003109 | Sin Quality Order; EO RECHAZADO por REM |
| MEM0003625 | QO Pass pero Passed vacío; EO por REM |
| Dims REM “vacíos” | Algunos `inventDimId` REM sin filas InventTrans útiles — usar el dim con `Qty`/`DatePhysical` reales |
| RES / CUARENTENA | **Ausentes** en los 8 lotes — no concluir patrón de fecha para ellos aún |

### Mapa de implementación recomendado (sin implementar ahora)

```text
operationalTraceability
├── qualityValidation
│   ├── qualityOrderNumber          ← QualityOrderHeaders.QualityOrderNumber
│   ├── qualityOrderStatus          ← QualityOrderHeaders.QualityOrderStatus  (NO ValidationStatus)
│   ├── validatedAt                 ← QualityOrderHeaders.ValidatedDateTime
│   │                                 (null si 1900-01-01 o status Open)
│   ├── validatingPersonnelNumber   ← QualityOrderHeaders.ValidatingPersonnelNumber
│   └── validatingEmployeeName      ← BaseWorkers.Name
└── warehouseMovement                 (solo si EO rule = Almacén REM|RES|CUARENTENA)
    ├── targetWarehouse             ← InventDim.InventLocationId
    ├── movedAt                     ← MIN(InventTrans.DatePhysical) del dim objetivo
    ├── voucher                     ← InventTrans.Voucher (opcional, referencia)
    ├── movedByFoUser               ← InventDim.SysModifiedBy (OPCIONAL, advertir metadata)
    └── movedByPersonnelNumber      ← NO DISPONIBLE en OData estándar
```

**Punto de ensamblaje (sin cambiar hoy):** `DynamicsLookupService.executeLookup()` → extender `$select` QualityOrder + lookup `BaseWorkers` + InventTrans sobre dims REM/RES/CUARENTENA.

---

## Qué queda pendiente antes de implementar

1. Probar **≥1 lote RES** y **≥1 lote CUARENTENA** con la misma mecánica `InventDim` + `DatePhysical`.
2. Decisión de negocio: ¿mostrar `SysModifiedBy` como “usuario FO (metadata)” o omitirlo por no ser auditoría formal?
3. Regla multi-orden Quality (no apareció en estos 8; sí en 3447 histórico): elegir orden Pass con `ValidatedDateTime` más reciente.
4. Ampliar `$select` de `InventDimBiEntities` solo si se aprueba usar Sys* fields.

---

## Referencias

- Diseño previo: `DISENO-TRAZABILIDAD-ESTADO-OPERATIVO.md`
- Investigación estado/REM: `INVESTIGACION-DYNAMICS-ESTADO-Y-TRAZABILIDAD-LOTES.md`
- Arquitectura EO: `ARQUITECTURA-ESTADOS-LOTE.md`
- Raw JSON: `scripts/out/dynamics-trazabilidad-full-20260728-102937/`
