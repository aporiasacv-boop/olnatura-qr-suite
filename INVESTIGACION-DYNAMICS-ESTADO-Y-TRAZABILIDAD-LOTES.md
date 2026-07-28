# Investigación Dynamics 365 — Estado operativo y trazabilidad de lotes (OData)

**Fecha:** 27 de julio de 2026  
**Alcance:** Solo investigación y documentación.  
**Restricciones cumplidas:** sin código, sin Backend/Web/Android/BD/endpoints.

**Entorno Dynamics conocido (probes previos del repo):**  
`https://olnatura-produccion.operations.dynamics.com`

---

## 0. Estado de la investigación live

| Ítem | Resultado |
|------|-----------|
| Credenciales OAuth en la sesión | **Disponibles** (consulta 2026-07-27; secret **no** guardado en repo) |
| Consulta OData de los 4 lotes de referencia | **Ejecutada** — carpeta `scripts/out/dynamics-estado-lotes-20260727-193858` |
| Evidencia OData real previa en el repo | Sí — lote `260713-MEM0003662` (probe 2026-07-14) |
| Evidencia mock del caso rechazo 3625 | Sí — `MockDynamicsClient` |
| Fuentes usadas | OData producción live + código Olnatura QR + Microsoft Learn |

> **Hallazgo crítico live:** en los 4 lotes de referencia, **`BatchDispositionCode` solo no discrimina** los estados esperados por negocio.  
> Los rechazos esperados (3363 / 3109) se detectan por **`InventDimBiEntities.InventLocationId = REM`**, mientras la disposición del lote sigue vacía o en `Aprobado`.  
> El lote esperado en CUARENTENA (3447) aparece en OData como **`Aprobado` + almacén `MPM` / ubicación `Disponible`** (sin `CUARENTENA` ni `REM`).

Lotes consultados:

1. `260406-MPM0003390` → esperado APROBADO  
2. `260707-MPM0003447` → esperado CUARENTENA  
3. `260206-MPM0003363` → esperado RECHAZADO  
4. `240923-MPM0003109` → esperado RECHAZADO  

---

## 1. Objetivo de negocio

Determinar si **Olnatura QR puede dejar de ser dueño del estado del lote** y convertirse en **visor inteligente** de Dynamics 365 F&O vía OData.

Pregunta central:

> ¿Existe en Dynamics una fuente oficial del estado operativo (APROBADO / CUARENTENA / RECHAZADO) consultable por OData, con suficiente trazabilidad (fechas, usuario, movimientos REM/RES)?

---

## 2. Entidades OData analizadas

### 2.1 Ya usadas por Olnatura QR (confirmadas en código + probe real)

| Entidad OData | Entidad técnica | Pública | Descripción | Clave / filtro típico | Campos relevantes ya leídos | Relaciones |
|---------------|-----------------|---------|-------------|------------------------|-----------------------------|------------|
| **ItemBatches** | `InventItemBatchEntity` | **Sí** | Maestro de lote (batch) del ítem | `BatchNumber`, `ItemNumber` | `ItemNumber`, `BatchNumber`, `BatchExpirationDate`, **`BatchDispositionCode`** | Relaciona a producto y a disposición del lote |
| **QualityOrderHeaders** | `InventQualityOrderHeaderEntity` | **Sí** | Cabecera de orden de calidad | `ItemBatchNumber` / `ItemNumber` | `QualityOrderStatus`, `PassedBatchDispositionCode`, `WarehouseId`, `WarehouseLocationId` | Liga lote ↔ resultado de calidad ↔ almacén/ubicación de la orden |
| **InventorySitesOnHand** | (on-hand por sitio) | **Sí** | Existencias por sitio | `ItemNumber` | `ProductName`, `AvailableOnHandQuantity`, `InventorySiteId` | Sitio/almacén de stock (fallback de almacén) |
| **InventorySitesOnHandV2** | — | **Sí** (fallback) | Misma familia | `ItemNumber` | Igual | Fallback del probe |
| **ReleasedProductsV2** | — | **Sí** | Producto liberado | `ItemNumber` | `InventoryUnitSymbol` | Unidad de inventario |
| **InventDimBiEntities** | — | **Sí** (usada) | Dimensiones de inventario | `inventBatchId` | `inventDimId`, `inventBatchId` | Puente lote → dimensiones (incluye ubicación/almacén en FO) |
| **InventTransCDSEntities** | — | **Sí** (usada) | Transacciones de inventario (CDS) | `inventDimId` | `StatusReceipt`, `DatePhysical` | Movimientos físicos; hoy solo para **fecha de entrada** |

### 2.2 Relacionadas / candidatas (documentación + patrón FO)

| Entidad / concepto | OData público | Descripción | Relevancia para estado/trazabilidad | Disponibilidad vía OData |
|--------------------|---------------|-------------|--------------------------------------|---------------------------|
| **Batch dispositions** (`PdsBatchDispositionEntity`) | En catálogo dynamics.fo aparece **Public: False** | Maestro de códigos de disposición (Available/Unavailable + reglas de bloqueo) | Catálogo oficial de códigos y su semántica Available/Unavailable | **Probablemente NO** expuesto por OData estándar sin personalización |
| **InventBatch** (tabla) | No es entidad OData directa | Tabla base de lotes | Fuente subyacente de ItemBatches | Solo vía entidad pública `ItemBatches` |
| **Quality orders / líneas / resultados** | Parcial (`QualityOrderHeaders`) | Ciclo de inspección | Status Open/Pass/Fail; puede actualizar disposición del lote | Cabecera sí; fechas/usuario dependen de si la entidad los expone |
| **InventMovement / journals** | Variable según entidad publicada | Diarios de movimiento | Posible reconstrucción de REM/RES | **No usada hoy**; hay que validar qué entidades de movimiento están publicadas en el entorno Olnatura |
| **Warehouses / Warehouse locations** | Varias entidades de almacén | Catálogo de almacenes y ubicaciones | Interpretar REM/RES | Consultar maestros de almacén/ubicación por OData si están públicos |
| **Database log / Sys* audit** | Generalmente **no** vía OData app | Auditoría de cambios de tabla | Quién cambió disposición | Normalmente **requiere personalización** o BYOD/Data Lake |

---

## 3. ¿Dónde vive el estado oficial del lote?

### 3.1 Respuesta técnica (ajustada con evidencia live)

| Capa | Campo | ¿Fuente oficial del lote? |
|------|-------|---------------------------|
| **Lote (batch)** | **`ItemBatches.BatchDispositionCode`** | **Candidata oficial FO**, pero **incompleta / desalineada** en los 4 lotes de referencia |
| **Dimensión inventario** | **`InventDimBiEntities.InventLocationId`** (+ `wMSLocationId`) | **Señal operativa fuerte** de rechazo (`REM`/`RES`) y ubicación física actual |
| Orden de calidad | `QualityOrderHeaders.QualityOrderStatus` | Estado de la **orden**, no del lote (`Pass` aparece también en lotes “rechazados” por REM) |
| Orden de calidad | `QualityOrderHeaders.PassedBatchDispositionCode` / `FailedBatchDispositionCode` | Disposición **de la orden**; en live `Failed=Rechazado` aunque el status sea `Pass` |
| Almacén en Quality | `WarehouseId`, `WarehouseLocationId` | Contexto de la **orden** (en los 4 lotes: `MPM` / `Disponible`) |
| Maestro almacenes | `Warehouses` | Confirma semántica: `REM`/`RES`/`CUARENTENA` son **almacenes** |
| Olnatura QR | `qr_labels.status` | Estado **paralelo interno** (hoy es lo que ve el usuario) |

### 3.2 Fundamento Microsoft

Según Microsoft Learn (*Batch disposition codes*):

- Cada lote tiene un **Batch disposition code**.
- Cada código tiene un **Batch disposition status** binario de sistema: **Available** o **Unavailable**.
- Los códigos concretos (nombres) son **configurables por compañía** (no hay un enum fijo global “Approved/Rejected”).
- La asignación se hace en FO: *Reset batch disposition code* sobre el lote.
- Las órdenes de calidad (Test groups) pueden **actualizar automáticamente** la disposición del lote al Pass/Fail.

Conclusión (teoría FO + práctica Olnatura):

> En teoría FO, el estado canónico del lote es **`BatchDispositionCode`**.  
> En la práctica Olnatura (4 lotes live), el **rechazo operativo** se refleja primero en el **almacén de inventario** (`InventLocationId` = `REM`/`RES`), y la disposición del lote **puede no actualizarse**.  
> Por tanto, para un “visor inteligente” fiable hace falta una **regla híbrida**: disposición + almacén físico (InventDim), no solo `BatchDispositionCode`.

### 3.3 Evidencia Olnatura

**Probe real** `260713-MEM0003662` (2026-07-14):

| Campo | Valor observado |
|-------|-----------------|
| QualityOrderStatus | `Open` |
| PassedBatchDispositionCode | *(vacío)* |
| WarehouseId | `MEM` |
| WarehouseLocationId | `Disponible` |
| ItemBatches select del probe | **No incluyó** `BatchDispositionCode` en ese script antiguo |

**Mock rechazo 3625** (`260619-MEM0003625`):

| Campo | Valor |
|-------|-------|
| BatchDispositionCode | `Rechazado` |
| QualityOrderStatus | `Fail` |
| PassedBatchDispositionCode | `Rechazado` |
| WarehouseLocationId | `Rechazado` |

Esto refuerza el patrón: **disposición del lote + resultado de calidad + ubicación** co-varían en rechazo.

---

## 4. Valores posibles

### 4.1 Nivel sistema (Microsoft)

| Concepto | Valores del motor FO |
|----------|----------------------|
| Batch disposition **status** | `Available`, `Unavailable` |
| Quality order status (típicos) | `Open`, `Pass`, `Fail` (y variantes según versión/configuración) |

### 4.2 Nivel compañía (Olnatura) — códigos de disposición

Los **nombres** de `BatchDispositionCode` **no son estándar globales**. Son del maestro *Batch disposition master* de la compañía.

Valores de **`BatchDispositionCode`** confirmados por filtro OData live (`ItemBatches`):

| Valor | ¿Existe en producción? | Notas |
|-------|------------------------|-------|
| `Aprobado` | **Sí** (múltiples lotes) | Incluye 3447 y 3363 de la muestra |
| `Rechazado` | **Sí** (otros lotes, p.ej. `MP-MB20042`) | **No** aparece en 3363/3109 pese a estar en REM |
| `Cuarentena` | **0 hits** en filtro exacto | No usado como código de disposición (o no con ese literal) |
| `Disponible` / `Hold` / `Available` / `Unavailable` | **0 hits** como código de disposición | `Disponible` sí existe como **ubicación** (`wMSLocationId`) |
| *(vacío)* | **Sí** | 3390 y 3109 |

Valores **confirmados como almacenes** (`Warehouses`), no como disposición:

| WarehouseId | WarehouseName (live) |
|-------------|----------------------|
| `REM` | Rechazado de medicamento |
| `RES` | Rechazado suplemento |
| `CUARENTENA` | (almacén de cuarentena; también `QuarantineWarehouseId` de REM/RES) |
| `REM-D` / `RES-D` | Variantes Driexpress |
| `MPM` / `MEM` / … | Almacenes operativos de producto |

Entidades de maestro de disposición (`PdsBatchDispositions`, `BatchDispositions`, `InventBatchDispositions`): **404** — no públicas en este entorno.

### 4.3 Importante

`Approved` / `Rejected` en inglés **no** aparecen en la muestra. Los códigos vivos son en español (`Aprobado`, `Rechazado`).  
`REM` / `RES` **no** son códigos de disposición: son **almacenes**.

---

## 5. Tabla de conversión propuesta (Dynamics → Estado QR)

> **Propuesta operativa revisada tras live** — ya no basta con `BatchDispositionCode` solo.

### 5.1 Regla híbrida recomendada (prioridad)

| Prioridad | Señal Dynamics | Estado QR | Evidencia live |
|-----------|----------------|-----------|----------------|
| **1** | Alguna dimensión del lote con `InventLocationId` ∈ {`REM`,`RES`,`REM-D`,`RES-D`} | **RECHAZADO** | 3363 y 3109 |
| **2** | Alguna dimensión con `InventLocationId` = `CUARENTENA` (o `CUA` si negocio lo confirma) | **CUARENTENA** | Almacén existe; **ninguno de los 4 lotes** lo tenía |
| **3** | `BatchDispositionCode = Rechazado` | **RECHAZADO** | Existe en otros lotes; no en la muestra de 4 |
| **4** | `BatchDispositionCode = Aprobado` **y** stock solo en almacén operativo (`MPM`/`MEM`/…) + ubicación `Disponible` | **APROBADO** | 3447 (y parcialmente 3390 con disposición vacía) |
| **5** | `BatchDispositionCode` vacío + solo `MPM`/`Disponible` + Quality `Pass` | **APROBADO** (débil) | 3390 |
| **6** | Sin señales claras | Fallback / “Sin dato Dynamics” | — |

### 5.2 Primaria teórica FO: `BatchDispositionCode` (insuficiente sola)

| Dynamics (`BatchDispositionCode`) | Estado QR propuesto | Confianza post-live |
|-----------------------------------|---------------------|---------------------|
| `Aprobado` | **APROBADO** | Media — **puede coexistir con stock en REM** (3363) |
| `Rechazado` | **RECHAZADO** | Alta cuando está poblado |
| `Cuarentena` | **CUARENTENA** | Baja — **0 hits** en OData |
| Vacío | No decidir solo | Alta — frecuente |

### 5.3 Secundaria: Quality Order

| Dynamics | Uso post-live |
|----------|---------------|
| `QualityOrderStatus = Pass` | **No implica APROBADO** (3363 está en REM y Pass) |
| `QualityOrderStatus = Open` | Candidato débil a CUARENTENA si no hay InventDim de rechazo |
| `QualityOrderStatus = Fail` | Refuerzo de RECHAZADO |
| `ValidatedDateTime` / `ValidatingPersonnelNumber` | Fecha/persona de **validación de la orden** (parcial) |

### 5.4 Terciaria / operativa fuerte: almacén InventDim (REM / RES / CUARENTENA)

| Señal | Interpretación | Estado QR |
|-------|----------------|-----------|
| `InventLocationId` ∈ {REM, RES, REM-D, RES-D} | Stock (o historial de dim) en almacén de rechazo | **RECHAZADO** |
| `InventLocationId = CUARENTENA` | Stock en almacén de cuarentena | **CUARENTENA** |
| Solo `MPM`/`MEM` + `wMSLocationId=Disponible` | Liberado en almacén operativo | Refuerzo **APROBADO** |

**Regla recomendada post-live:**  
Para Olnatura, el **estado operativo visible** debe priorizar **ubicación física del lote (InventDim)** sobre `BatchDispositionCode` cuando haya conflicto. La disposición FO sigue siendo útil, pero **no es suficiente** en el proceso actual de la planta.

---

## 6. WarehouseId / WarehouseLocationId y REM / RES

### 6.1 Qué son en FO

| Campo | Significado FO |
|-------|----------------|
| **WarehouseId** (Quality / Warehouses) | **Almacén** (warehouse) |
| **WarehouseLocationId** / **wMSLocationId** | **Ubicación** dentro del almacén |
| **InventLocationId** (InventDim) | **Almacén** en la dimensión de inventario del lote |

### 6.2 ¿Qué representan REM / RES? — **CONFIRMADO LIVE**

| Hipótesis | Resultado |
|-----------|-----------|
| REM/RES son **almacenes** | **Confirmado** vía `GET /data/Warehouses` |
| REM/RES son ubicaciones | No — la ubicación dentro de REM es `General` |
| REM/RES son códigos de disposición | **Descartado** |

Datos live:

| WarehouseId | WarehouseName | QuarantineWarehouseId |
|-------------|---------------|------------------------|
| `REM` | Rechazado de medicamento | `CUARENTENA` |
| `RES` | Rechazado suplemento | `CUARENTENA` |
| `CUARENTENA` | Almacén de cuarentena | — |
| `REM-D` / `RES-D` | Variantes Driexpress | `CUARENTENA` |

En InventDim de lotes “rechazados” esperados:

- `260206-MPM0003363` → dims en `MPM/Disponible` **y** `REM/General`
- `240923-MPM0003109` → dims en `MPM/...` **y** `REM/General`
- `260406-MPM0003390` (APROBADO) → solo `MPM`
- `260707-MPM0003447` (esperado CUARENTENA) → solo `MPM/Disponible` (**sin** `CUARENTENA`)

### 6.3 Respuesta cerrada

> **REM / RES son almacenes de material rechazado** (medicamento / suplemento).  
> Son la **mejor señal OData observada** para clasificar **RECHAZADO** en los lotes de referencia, porque `BatchDispositionCode` no se actualizó a `Rechazado` en esos casos.  
> **CUARENTENA** también es un **almacén**, no un `BatchDispositionCode` (0 hits del literal `Cuarentena` como disposición).

Nota: `QualityOrderHeaders.WarehouseId` en estos lotes sigue en `MPM` aunque el stock ya tenga dimensión en `REM`. Hay que leer **InventDim**, no solo el warehouse de la orden de calidad.

---

## 7. Información histórica (fechas)

| Pregunta | ¿Disponible vía OData estándar? | Detalle live |
|----------|---------------------------------|--------------|
| Fecha de **entrada** del lote | **Sí** | `InventTransCDSEntities.DatePhysical` (StatusReceipt Purchased/Received) |
| Fecha de **validación de calidad** | **Sí (parcial)** | `QualityOrderHeaders.ValidatedDateTime` (ej. 3363 → `2026-02-17T00:07:56Z`) |
| Fecha de **aprobación/rechazo de disposición** | **No** como historial | Solo valor **actual** de `BatchDispositionCode` |
| Fecha de movimiento a REM/RES | **Parcial** | InventDim + InventTrans por `inventDimId` de dims REM (no muestreado exhaustivamente aquí) |
| Usuario validador calidad | **Parcial** | `ValidatingPersonnelNumber` (ej. `3960`) — número de personal, no nombre de usuario FO |
| Usuario que movió a REM | **No confirmado** | `InventDimBiEntities.SysModifiedBy` existe en fila completa (ej. `mp.medicamentos`) pero es metadata de dimensión, no auditoría de negocio fiable |

### Conclusión fechas

- **Sí:** entrada (InventTrans) + validación de orden de calidad (`ValidatedDateTime`).  
- **Parcial:** movimiento a REM vía dims/transacciones.  
- **No:** historial de cambios de `BatchDispositionCode`.

---

## 8. Usuario responsable

| Campo | ¿En OData live? |
|-------|-----------------|
| `QualityOrderHeaders.ValidatingPersonnelNumber` | **Sí** (número de personal, p.ej. `3960`) |
| `QualityOrderHeaders.ValidatedDateTime` | **Sí** |
| `InventDimBiEntities.SysModifiedBy` | **Sí** en fila completa (p.ej. `mp.medicamentos`) — metadata técnica |
| Usuario “quién cambió BatchDispositionCode” | **No** como evento auditable |

**Respuesta:**

> Se puede mostrar **quién validó la orden de calidad** (personnel number) y **cuándo**.  
> **No** se obtiene de forma fiable “quién rechazó / movió a REM / cambió disposición” como timeline de negocio sin personalizar Dynamics.

---

## 9. Historial de movimientos / línea de tiempo

### 9.1 ¿Existe una entidad de timeline completa?

**No una sola.** La línea de tiempo se **reconstruye** ensamblando varias fuentes:

```
Entrada (InventTrans Received/Purchased)
    ↓
Orden de calidad abierta (QualityOrderHeaders, Status=Open)
    ↓
Resultado Pass/Fail (QualityOrderStatus)
    ↓
Cambio de disposición del lote (BatchDispositionCode)  ← estado actual, no historial
    ↓
Movimientos de inventario a ubicación/almacén REM|RES (InventTrans + InventDim)
    ↓
Liberación (disposición Disponible + ubicación Disponible)
```

### 9.2 Qué se puede reconstruir solo con OData (estimado)

| Evento | Factible sin personalización |
|--------|------------------------------|
| Entrada a inventario | **Sí** (ya) |
| Existencia de orden de calidad y estado actual | **Sí** |
| Disposición **actual** del lote | **Sí** (`BatchDispositionCode`) |
| Movimientos físicos con fechas | **Parcial** (InventTrans; validar campos publicados) |
| Historial de cambios de disposición | **No estándar** |
| Usuario de cada paso | **No estándar** |

### 9.3 Respuesta

> **No es posible una línea de tiempo completa y auditable “quién/cuándo cambió la disposición” usando únicamente OData estándar sin personalizaciones.**  
> Sí es posible una **línea de tiempo parcial** (entrada + estado actual de calidad + disposición actual + movimientos de inventario detectables).

---

## 10. Comparativa entre los 4 lotes de referencia

**Fuente:** OData producción `olnatura-produccion` — 2026-07-27  
**Artefactos:** `scripts/out/dynamics-estado-lotes-20260727-193858/`

### 10.1 Expectativa de negocio (input del usuario)

| Lote | Estado esperado QR |
|------|--------------------|
| `260406-MPM0003390` | APROBADO |
| `260707-MPM0003447` | CUARENTENA |
| `260206-MPM0003363` | RECHAZADO |
| `240923-MPM0003109` | RECHAZADO |

### 10.2 Valores OData observados

| Campo | 3390 APROBADO | 3447 CUARENTENA | 3363 RECHAZADO | 3109 RECHAZADO |
|-------|---------------|-----------------|----------------|----------------|
| ItemNumber | `106623850300` | `100623400601` | `100623401100` | `106623700100` |
| Producto | CROSPOVIDONA XL-10 (TIPO B) | SORBITOL | SABOR VAINILLA ARO | TAMSULOSINA HCL PELLETS 0.2% |
| `BatchDispositionCode` | *(vacío)* | **`Aprobado`** | **`Aprobado`** | *(vacío)* |
| `QualityOrderStatus` | `Pass` | `Pass` (2 órdenes) | `Pass` | *(sin orden)* |
| `PassedBatchDispositionCode` | *(vacío)* | *(vacío)* / `Aprobado` | `Aprobado` | — |
| `FailedBatchDispositionCode` | `Rechazado`* | `Rechazado`* (2ª) | `Rechazado` | — |
| Quality `WarehouseId` | `MPM` | `MPM` | `MPM` | — |
| Quality `WarehouseLocationId` | `Disponible` | `Disponible` | `Disponible` | — |
| InventDim `InventLocationId` | solo `MPM` | solo `MPM` | **`MPM` + `REM`** | **`MPM` + `REM`** |
| InventDim `wMSLocationId` | `Disponible` | `Disponible` | `Disponible` / `General`(REM) | `Disponible` / `General`(REM) |
| `ValidatedDateTime` | 2026-06-03 | 2026-07-17 | 2026-02-17 | — |
| `ValidatingPersonnelNumber` | 3960 | 3960 | 3960 | — |
| ¿Coincide esperado? | **Sí** (vía MPM) | **No** (parece APROBADO) | **Parcial** (REM sí; disposición no) | **Parcial** (REM sí; disposición no) |

\*En fila completa de Quality; el campo existe aunque el status sea `Pass` (es la disposición *si fallara*).

### 10.3 Campos que sí discriminan (post-live)

1. **`InventDimBiEntities.InventLocationId`** — discriminante fuerte de **RECHAZADO** (`REM`/`RES`).  
2. **`BatchDispositionCode`** — útil cuando está poblado (`Aprobado`/`Rechazado`), pero **no confiable solo** (3363 = `Aprobado` + REM).  
3. **`QualityOrderStatus`** — **no** discrimina APROBADO vs RECHAZADO en esta muestra (casi todo `Pass`).  
4. **Quality `WarehouseId`** — **no** discrimina (sigue `MPM` aunque haya REM en InventDim).  
5. Almacén **`CUARENTENA`** — existe en maestro; **no** aparece en InventDim de 3447.

### 10.4 Desalineación negocio ↔ Dynamics

| Lote | Esperado | Lectura OData más fiel | Acción sugerida con negocio |
|------|----------|------------------------|-----------------------------|
| 3390 | APROBADO | APROBADO | OK |
| 3447 | CUARENTENA | APROBADO (`BatchDisposition=Aprobado`, solo MPM) | Confirmar si el lote ya se liberó en FO o si la cuarentena vive solo en Olnatura QR |
| 3363 | RECHAZADO | RECHAZADO por **REM** (disposición aún Aprobado) | Confirmar regla: ¿REM manda sobre disposición? |
| 3109 | RECHAZADO | RECHAZADO por **REM** (sin calidad; disposición vacía) | Igual; además sin QualityOrder |

---

## 11. Preguntas finales — respuestas

### 1. ¿Puede Dynamics convertirse en la única fuente oficial del estado?

**Sí, con regla híbrida**, no con `BatchDispositionCode` solo.

Condiciones:

- Priorizar `InventLocationId` ∈ {REM, RES, …} → RECHAZADO.  
- Usar almacén `CUARENTENA` (InventDim) para CUARENTENA — **falta ejemplo live en los 4 lotes**.  
- Usar `BatchDispositionCode` como refuerzo / cuando no haya dims de rechazo.  
- Definir fallback si Dynamics no responde.  
- Alinear con negocio el caso 3447 (esperado CUARENTENA vs OData APROBADO).

### 2. ¿Es necesario mantener `qr_labels.status`?

| Opción | Recomendación |
|--------|---------------|
| Como **fuente de verdad** | **No** a largo plazo, si se adopta la regla híbrida Dynamics |
| Como **caché / fallback offline** | Opcional |
| Como **histórico de decisiones Olnatura** | Solo si se conserva workflow app como bitácora |

Hoy, ante la desalineación 3447 y la inconsistencia disposición↔REM, **no conviene apagar `qr_labels.status` sin validación de negocio**.

### 3. ¿Qué datos adicionales puede mostrar Olnatura QR?

- Disposición del lote (`BatchDispositionCode`)  
- Almacenes/ubicaciones reales del lote (InventDim: MPM, REM, …)  
- Estado de orden de calidad + `ValidatedDateTime` + personnel number  
- Cantidad / unidad / caducidad / fecha de entrada  

### 4. ¿Fechas de aprobación/rechazo/REM y usuario sin personalizar Dynamics?

| Dato | Sin personalización |
|------|---------------------|
| Fecha validación calidad | **Sí** (`ValidatedDateTime`) |
| Usuario validador | **Parcial** (`ValidatingPersonnelNumber`) |
| Fecha cambio de disposición | **No** (historial) |
| Fecha movimiento a REM | **Parcial** (InventTrans por dim REM) |
| Usuario movimiento REM | **No fiable** |

### 5. ¿Línea de tiempo completa solo con OData?

**No completa.**  
**Sí parcial:** entrada + validación calidad + disposición actual + presencia en REM/RES.

---

## 12. Conclusiones

1. **REM / RES son almacenes** (“Rechazado de medicamento/suplemento”), confirmados en `Warehouses`.  
2. En los lotes de referencia **rechazados**, la señal fiable es **`InventDim.InventLocationId = REM`**, no `BatchDispositionCode` (sigue vacío/`Aprobado`).  
3. **`BatchDispositionCode` solo no basta** como fuente de verdad para el proceso actual de Olnatura.  
4. **`QualityOrderStatus=Pass` no implica APROBADO** operativo.  
5. El lote esperado en **CUARENTENA (3447) no aparece en cuarentena en OData** (está `Aprobado` + MPM). Hay que validar con negocio/FO.  
6. El maestro OData de disposiciones **no está publicado** (404). Códigos vivos confirmados: `Aprobado`, `Rechazado`.  
7. Olnatura **puede** ser visor inteligente del **estado actual** con regla híbrida InventDim + disposición.  
8. Timeline completa / auditoría FO rica **sigue sin estar** solo con OData estándar.

---

## 13. Riesgos

| Riesgo | Impacto |
|--------|---------|
| Confiar solo en `BatchDispositionCode` | Falsos APROBADO (caso 3363) |
| Dimensión REM histórica sin stock actual | Posible falso RECHAZADO si no se cruza con cantidad on-hand |
| 3447 esperado CUARENTENA vs OData APROBADO | Divergencia negocio / mala muestra / estado solo en QR |
| Maestro disposición no público | No se lee Available/Unavailable automáticamente |
| Quality Warehouse sigue en MPM con stock en REM | Lectura incorrecta si se usa solo QualityOrderHeaders |
| Doble fuente `qr_labels.status` | Divergencia operativa |

---

## 14. Recomendaciones

### Inmediatas (negocio / TI FO)

1. Confirmar con almacén/calidad: ¿el estado operativo que debe ver QR es el **almacén físico** (REM/CUARENTENA) o la **disposición del lote**?  
2. Explicar 3447: ¿ya se liberó en Dynamics?  
3. Revisar proceso FO: al mover a REM, ¿debe actualizarse `BatchDispositionCode` a `Rechazado`? (hoy a menudo no).  
4. Exportar maestro *Batch disposition* desde UI FO (OData del maestro no está público).

### De diseño (solo si se aprueba implementación posterior)

1. Estado UI = **regla híbrida** §5.1 (InventDim REM/RES/CUARENTENA + BatchDisposition).  
2. Mostrar panel de contexto: disposición, calidad, almacenes del lote.  
3. No apagar mutaciones de `qr_labels.status` hasta validar 3447 y la política REM.  
4. No prometer timeline completa ni “usuario que movió a REM”.

### Consultas OData mínimas por lote (checklist actualizado)

```http
# 1) Disposición del lote
GET /data/ItemBatches?$filter=BatchNumber eq '{LOTE}'
 &$select=ItemNumber,BatchNumber,BatchExpirationDate,BatchDispositionCode

# 2) Orden de calidad (+ fecha/persona validación)
GET /data/QualityOrderHeaders?$filter=ItemBatchNumber eq '{LOTE}'
 &$select=ItemBatchNumber,ItemNumber,QualityOrderStatus,PassedBatchDispositionCode,FailedBatchDispositionCode,WarehouseId,WarehouseLocationId,ValidatedDateTime,ValidatingPersonnelNumber,QualityOrderNumber

# 3) Dimensiones = almacén/ubicación reales del lote (CRÍTICO)
GET /data/InventDimBiEntities?$filter=inventBatchId eq '{LOTE}'
 &$select=inventDimId,inventBatchId,InventLocationId,wMSLocationId,InventSiteId

# 4) Maestro almacenes (una vez / caché)
GET /data/Warehouses?$top=200

# 5) Movimientos (fecha entrada / dims REM)
GET /data/InventTransCDSEntities?$filter=inventDimId eq '{INVENT_DIM_ID}'
 &$select=inventDimId,StatusReceipt,DatePhysical
 &$top=100
```

---

## 15. Apéndice — Evidencia previa (no es uno de los 4 lotes)

**Lote probe real:** `260713-MEM0003662`

| Campo | Valor |
|-------|-------|
| ItemNumber | `400615440900` |
| ProductName | `QG5 FOLLETO` |
| QualityOrderStatus | `Open` |
| PassedBatchDispositionCode | vacío |
| WarehouseId | `MEM` |
| WarehouseLocationId | `Disponible` |
| Fuente | REAL_DYNAMICS |

**Mock rechazo:** `260619-MEM0003625` → `BatchDispositionCode=Rechazado`, `Fail`, ubicación `Rechazado`.

---

## 16. Siguiente paso (decisión de negocio)

La investigación live de los 4 lotes **está completa**. Antes de implementar cambios de código:

1. Validar con negocio la **regla híbrida** (¿REM manda?).  
2. Resolver el caso **3447** (esperado CUARENTENA vs OData APROBADO).  
3. Decidir si se corrige el proceso en FO (actualizar disposición al mover a REM) o se adopta InventDim como fuente operativa en QR.  
4. Solo entonces autorizar implementación en Backend/Web/Android.

**Seguridad:** el Client Secret usado en esta sesión quedó expuesto en el chat. **Rotarlo en Azure AD** y no guardarlo en el repositorio.
