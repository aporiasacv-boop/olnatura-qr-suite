# Fuente de verdad del estado operativo del lote → Dynamics 365

**Fecha original:** 27 de julio de 2026  
**Actualización:** 28 de julio de 2026 — arquitectura **implementada y consolidada**.  
**Estado:** El Estado Operativo ya proviene solo de Dynamics (`OperationalStatusResolver`).  
Ver también: `ARQUITECTURA-ESTADOS-LOTE.md`, `IMPLEMENTACION-ESTADO-OPERATIVO-DYNAMICS-v1.6.0.md`.

---

## 0. Situación vigente (to-be implementado)

| Concepto | Origen | ¿Mutable desde la app? |
|----------|--------|-------------------------|
| **Estado Operativo** (banner) | Dynamics → `OperationalStatusResolver` → `dynamic.status` | **No** |
| **Estado de plataforma** | `qr_labels.status` → `platformStatus` | Sí (approve/reject / corrección admin) |
| **Admin lifecycle** | `admin_status` | Sí (ACTIVE/INACTIVE/BAJA) |

Las secciones siguientes conservan el análisis histórico (as-is previo a v1.6.0).

---

## 1. Situación histórica (as-is previo)

Hoy el sistema mantiene **dos mundos de estado**:

| Concepto | Origen | Valores | Uso |
|----------|--------|---------|-----|
| **Estado operativo (UI “Estado”)** | Columna interna `qr_labels.status` | `CUARENTENA` / `APROBADO` / `RECHAZADO` | Banner Web/Android, permisos de aprobación, métricas, export Power BI |
| **Estado Dynamics (referencia)** | OData en vivo | `QualityOrderStatus`, `PassedBatchDispositionCode`, `BatchDispositionCode`, almacén, ubicación | Solo lectura / diagnóstico |

Comentario explícito en código (`QrQueryService.java`):

> *Estado del material: solo plataforma (CUARENTENA/APROBADO/RECHAZADO). Nunca Dynamics Open/Pending.*

### Cómo se actualiza hoy el estado interno

| Acción | Quién | Efecto |
|--------|-------|--------|
| Alta de etiqueta | ADMIN / ALMACÉN | `status = CUARENTENA` |
| Aprobar material | CALIDAD / INSPECCIÓN / ADMIN | `ApprovalService` → `APROBADO` |
| Rechazar material | ídem | `ApprovalService` → `RECHAZADO` |
| Corrección administrativa de estado | Solo ADMIN | `AdminStatusCorrectionService` (CUARENTENA↔APROBADO, RECHAZADO→CUARENTENA) |

**Ninguna de estas acciones escribe en Dynamics.**  
**Ningún campo Dynamics escribe hoy en `qr_labels.status`.**

### Distinción importante (no confundir)

| Campo | Significado | ¿Es el estado operativo? |
|-------|-------------|---------------------------|
| `qr_labels.status` | Calidad operativa Olnatura | **Sí (hoy)** |
| `qr_labels.admin_status` | ACTIVE / INACTIVE / BAJA | **No** — ciclo de vida administrativo del lote |
| Dynamics (varios) | Referencia | **No (hoy)** |

Este documento habla solo del **estado operativo de calidad** (CUARENTENA / APROBADO / RECHAZADO), no del admin_status.

---

## 2. Campos Dynamics ya leídos por el sistema

Fuente: `RealDynamicsClient.java` + `DynamicsLookupService.java`.

### ItemBatches

| Campo OData | Uso actual |
|-------------|------------|
| `ItemNumber` | Código de ítem |
| `BatchNumber` | Lote |
| `BatchExpirationDate` | Caducidad |
| **`BatchDispositionCode`** | Solo diagnóstico en UI (`batchDispositionCode`) |

### QualityOrderHeaders

| Campo OData | Uso actual |
|-------------|------------|
| **`QualityOrderStatus`** | Parte de `statusDynamics` (prioridad 1) |
| **`PassedBatchDispositionCode`** | Parte de `statusDynamics` (fallback); diagnóstico |
| **`WarehouseId`** | Almacén |
| **`WarehouseLocationId`** | Ubicación |

### Resumen legado `statusDynamics`

```
statusDynamics = QualityOrderStatus
                 ?? PassedBatchDispositionCode
```

**No incluye** `BatchDispositionCode`. Comentario en código: *informativo; no sincroniza estado QR*.

### Lo que NO se usa para estado

- `InventTrans` / `InventDim` → solo **fecha de entrada**
- No hay entidad/campo llamado `BatchDisposition` suelto; el campo real es **`BatchDispositionCode`**
- **REM / RES** no aparecen en el código ni en probes del repo como códigos conocidos

---

## 3. Caso de referencia: lote 3625

Referencia funcional en mock (`MockDynamicsClient.java`):

| Campo | Valor mock lote `260619-MEM0003625` |
|-------|--------------------------------------|
| `BatchDispositionCode` | `Rechazado` |
| `QualityOrderStatus` | `Fail` |
| `PassedBatchDispositionCode` | `Rechazado` |
| `WarehouseId` | `MEM` |
| `WarehouseLocationId` | `Rechazado` |

Interpretación funcional del escenario:

1. En Dynamics el lote quedó en disposición **Rechazado**.
2. La orden de calidad está en **Fail**.
3. La ubicación refleja rechazo.
4. Un movimiento administrativo hacia zona/almacén de rechazo debería verse en **disposición del lote** y/o **ubicación/almacén**, no en un estado paralelo de Olnatura.

Hoy, si en `qr_labels.status` el lote sigue en `CUARENTENA` o `APROBADO`, la UI mostraría ese valor interno y Dynamics solo como “Estado Dynamics / disposición”, generando **divergencia** — exactamente el problema a eliminar.

> **Nota REM/RES:** en el repositorio no hay evidencia de códigos `REM`/`RES` como `BatchDispositionCode`. Pueden ser **almacenes** o **ubicaciones** de rechazo en FO. Antes de implementar hay que validar en Dynamics producción el catálogo real (disposición vs warehouse).

---

## 4. Fuente oficial propuesta del estado operativo

### Decisión recomendada

| Prioridad | Campo Dynamics | Rol |
|-----------|----------------|-----|
| **1 (fuente oficial)** | **`ItemBatches.BatchDispositionCode`** | Disposición del **lote** en inventario — alineada al material/lote, no a la orden puntual |
| 2 (refuerzo / fallback) | `QualityOrderHeaders.PassedBatchDispositionCode` | Disposición cuando la orden de calidad ya resolvió |
| 3 (contexto, no status) | `QualityOrderStatus` | Ciclo de la orden (`Open` / `Pass` / `Fail`) — diagnóstico |
| 4 (contexto, no status) | `WarehouseId` / `WarehouseLocationId` | Almacén / ubicación (p. ej. rechazo REM/RES) — **no** sustituyen el status, pero pueden reforzar detección de rechazo |

### Por qué `BatchDispositionCode` y no `QualityOrderStatus`

| Campo | Problema si se usa solo |
|-------|-------------------------|
| `QualityOrderStatus = Open` | No implica necesariamente “cuarentena operativa” de forma unívoca |
| `PassedBatchDispositionCode` | Vacío mientras la orden está abierta |
| **`BatchDispositionCode`** | Representa la disposición vigente del lote (mock: Cuarentena / Disponible / Rechazado) |

### Mapeo propuesto (pendiente de validar catálogo FO)

| Valor Dynamics (ejemplos conocidos / mock) | Estado operativo Olnatura |
|--------------------------------------------|---------------------------|
| `Cuarentena`, `CUARENTENA`, (y equivalentes) | **CUARENTENA** |
| `Disponible`, `Disponible`, `Pass`* | **APROBADO** |
| `Rechazado`, `Fail`*, `REM`**, `RES`** | **RECHAZADO** |
| Vacío / desconocido | Fallback: mantener `qr_labels.status` o marcar “Sin dato Dynamics” |

\* Solo si se usa como fallback desde QualityOrder / Passed.  
\*\* **Condicional:** solo si negocio confirma que REM/RES son códigos de disposición o almacenes de rechazo a mapear a RECHAZADO.

### Regla de lectura propuesta (pseudológica)

```
si BatchDispositionCode presente → mapear a CUARENTENA|APROBADO|RECHAZADO
si no → PassedBatchDispositionCode
si no → (opcional) WarehouseId/Location ∈ {REM, RES, …} → RECHAZADO
si no → QualityOrderStatus (Open→CUARENTENA, Pass→APROBADO, Fail→RECHAZADO)  [solo si se aprueba]
si Dynamics no disponible → fallback qr_labels.status + fuente DB_ONLY
```

---

## 5. Lógica histórica: qué dejó de ser fuente de verdad

> **Implementado en v1.6.0+.** El banner ya no usa `qr_labels.status`.  
> Approve/reject y corrección admin **siguen** mutando solo el estado de plataforma.

### Dejó de ser fuente del Estado Operativo (banner)

| Lógica | Qué cambió |
|--------|------------|
| `qr_labels.status` como origen del banner | Solo `platformStatus`; banner = Dynamics |
| `ApprovalService` → `label.setStatus` | Sigue escribiendo plataforma; **no** afecta Estado Operativo |
| `AdminStatusCorrectionService` | Conservado para **plataforma**; UI/auditoría dejan claro que Dynamics no cambia |
| `QrQueryService` | `dynamic.status` = `OperationalStatusResolver` |

### Conservar

| Elemento | Motivo |
|----------|--------|
| **Auditoría** | Historial intacto |
| **Historial de escaneos / comentarios** | Sin cambios |
| **`admin_status`** | Distinto del operativo |
| Corrección admin de plataforma | Solo `qr_labels.status` |

---

## 6. Impacto por capa (histórico / plan)

### Backend

| Área | Impacto |
|------|---------|
| `DynamicsLookupService` / resolver | Estado Operativo desde Dynamics |
| `QrQueryService` | `dynamic.status` = operativo; `platformStatus` = BD |
| `ApprovalService` / `AdminStatusCorrectionService` | Solo plataforma |
| Métricas / export | Siguen contando `qr_labels.status` como workflow de plataforma |

### Web

| Pantalla | Impacto |
|----------|---------|
| Consulta por lote | “Estado” refleja Dynamics mapeado; botones Aprobar/Rechazar/Corregir estado se retiran o dejan de cambiar el banner |
| Registrar etiqueta | Alta puede seguir creando fila en BD; el estado visible en consulta posterior vendrá de Dynamics |
| Métricas | KPIs de Aprobado/Rechazado/Cuarentena pueden desalinearse hasta redefinir fuente |
| Labels Dynamics | Ya traducidos; el banner operativo se alinea con disposición |

### Android

| Pantalla | Impacto |
|----------|---------|
| Resultado | Banner de estado = mismo mapeo; dejar de ofrecer corrección administrativa de estado |
| Modelo `DynamicDto` | Ya trae `status`; el valor llegará ya mapeado desde API |
| Share / imagen | Usará el nuevo status derivado |

---

## 7. Riesgos y validaciones previas a implementar

1. **Catálogo real de `BatchDispositionCode` en FO** (textos exactos: Cuarentena, Disponible, Rechazado, códigos técnicos, etc.).
2. **Confirmación de REM / RES:** ¿son `WarehouseId`, `WarehouseLocationId` o disposición?
3. **Lotes sin Quality Order o sin disposición:** política de fallback.
4. **Latencia / caída de Dynamics:** qué muestra la planta si OData falla.
5. **Doble fuente temporal:** si se deja `ApprovalService` escribiendo `qr_labels.status` mientras se muestra Dynamics, se viola “no lógica paralela”.
6. **Empaque primario con dos piernas de aprobación:** el flujo actual depende de status interno CUARENTENA; al pasar a Dynamics, ese workflow de app deja de ser la verdad (puede quedar solo como bitácora).

---

## 8. Resumen ejecutivo

| Pregunta | Respuesta |
|----------|-----------|
| **¿Qué campo será la fuente oficial?** | **`ItemBatches.BatchDispositionCode`**, con fallback a `PassedBatchDispositionCode` y, si negocio lo confirma, detección de rechazo por almacén/ubicación (REM/RES) |
| **¿Qué lógica se elimina como verdad?** | Mutación manual/workflow de `qr_labels.status` (aprobar/rechazar/corregir estado) como origen del estado mostrado |
| **¿Qué se conserva?** | Auditoría, historial de escaneos, comentarios, admin_status del lote |
| **¿Se modificó código en esta entrega?** | **No.** Documento de decisión únicamente |
| **¿Caso 3625?** | Mock ya modela rechazo vía `BatchDispositionCode=Rechazado` + ubicación `Rechazado`; es la referencia funcional para el mapeo |

---

## 9. Aprobaciones necesarias antes de codificar

Para implementar sin ambigüedad, confirmar:

1. ¿Se aprueba **`BatchDispositionCode`** como fuente oficial?
2. ¿Tabla de mapeo exacta (lista de valores FO → CUARENTENA/APROBADO/RECHAZADO)?
3. ¿REM/RES se mapean a RECHAZADO? ¿Desde qué campo (`WarehouseId` / `WarehouseLocationId` / disposición)?
4. ¿Se **deshabilitan** por completo Aprobar/Rechazar y Corrección de estado en UI, o se conservan solo como eventos de auditoría sin cambiar status?
5. ¿Qué hacer si Dynamics no responde: fallback a `qr_labels.status` o mensaje de error?

**Ningún cambio de Backend, Web ni Android se aplicará hasta recibir esa aprobación.**
