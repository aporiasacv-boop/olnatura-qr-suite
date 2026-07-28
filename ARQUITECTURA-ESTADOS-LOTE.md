# Arquitectura de estados del lote — Dynamics como única autoridad del Estado Operativo

**Vigente desde:** v1.6.0 / consolidado v1.6.2+  
**Relacionado:** `IMPLEMENTACION-ESTADO-OPERATIVO-DYNAMICS-v1.6.0.md`, `OperationalStatusResolver`, `DISENO-TRAZABILIDAD-ESTADO-OPERATIVO.md`, `SYNC-DYNAMICS-MANUAL.md`

---

## Tres conceptos (no mezclar)

| Concepto | Campo | Quién lo escribe | Quién lo lee (UI) |
|----------|-------|------------------|-------------------|
| **Estado Operativo** | `dynamic.status` (JSON) ← `OperationalStatusResolver` | **Nadie en Olnatura QR** — solo Dynamics (interpretado) | Banner Consulta por lote (Web/Android) |
| **Estado de plataforma** | `qr_labels.status` → `dynamic.platformStatus` | Alta, approve/reject, corrección admin | Workflow interno / Corrección Administrativa |
| **Ciclo administrativo** | `qr_labels.admin_status` | Admin lotes ACTIVE/INACTIVE/BAJA | Listado «Lotes» (Estado administrativo) |

---

## Regla de oro

1. El **Estado Operativo** (Aprobado / Rechazado / Cuarentena / Desconocido) es **exclusivamente** un reflejo de Dynamics vía `OperationalStatusResolver`.
2. **Ningún** endpoint, botón o corrección puede modificar el Estado Operativo.
3. Aprobaciones y **Corrección Administrativa** mutan solo **platformStatus** (`qr_labels.status`).
4. Web, Android y Backend deben leer el banner desde `dynamic.status` y la corrección desde `dynamic.platformStatus` (nunca al revés).

---

## Endpoints que escriben `qr_labels.status` (plataforma)

- `POST /api/v1/label` → CUARENTENA inicial  
- `POST .../approve` / `.../reject` → workflow Calidad/Inspección  
- `PATCH .../correct-status` → corrección admin de **plataforma**  

Ninguno altera el resultado de `OperationalStatusResolver`.

---

## Documento histórico

`DOC-ESTADO-OPERATIVO-DESDE-DYNAMICS.md` describe el análisis previo a la implementación.  
La arquitectura **as-is** vigente es la de esta página y de `IMPLEMENTACION-ESTADO-OPERATIVO-DYNAMICS-v1.6.0.md`.

**Extensión futura (solo diseño):** trazabilidad del Estado Operativo — ensamblaje en `DynamicsLookupService.executeLookup()`; ver `DISENO-TRAZABILIDAD-ESTADO-OPERATIVO.md`.

**Sincronización manual (v1.6.3):** `POST /api/v1/qr/{lote}/sync-dynamics` fuerza una nueva lectura OData; **nunca escribe en Dynamics**. Ver `SYNC-DYNAMICS-MANUAL.md`.
