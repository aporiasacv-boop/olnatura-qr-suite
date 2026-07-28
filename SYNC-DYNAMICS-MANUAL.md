# Sincronización manual con Dynamics

**Vigente desde:** v1.6.3  
**Relacionado:** `ARQUITECTURA-ESTADOS-LOTE.md`, `DISENO-TRAZABILIDAD-ESTADO-OPERATIVO.md`

---

## Qué es

La acción **«Sincronizar con Dynamics»** permite a **cualquier usuario autenticado** (Web y Android) forzar una **nueva lectura OData** del lote en Dynamics 365 F&O y refrescar toda la información derivada del ERP en Olnatura QR.

## Qué NO es

| Esta acción **NO**… | Motivo |
|---------------------|--------|
| Modifica datos en Dynamics | Solo lectura OData |
| Cambia el Estado Operativo “a mano” | Lo calcula `OperationalStatusResolver` desde la lectura |
| Ejecuta aprobaciones | `ApprovalService` no participa |
| Realiza correcciones administrativas | Distinto de Corrección Administrativa / `platformStatus` |
| Escribe información operacional en PostgreSQL | No hay snapshot Dynamics en BD |
| Requiere rol ADMIN | Visible para todos los roles autenticados |

**Regla:** Dynamics sigue siendo la única autoridad del Estado Operativo. Olnatura QR únicamente **refleja** el ERP.

---

## Endpoint

```http
POST /api/v1/qr/{lote}/sync-dynamics
Authorization: cookie JWT (usuario autenticado)
```

- Misma respuesta que `GET /api/v1/qr/{lote}` (`QrDto.Response`).
- Internamente reutiliza el ensamblaje de `QrQueryService` → `DynamicsLookupService.executeLookup()`.
- **No** hay caché de Dynamics: cada sync (y cada GET) consulta el ERP en vivo.
- Seguridad: autenticado; sin `@PreAuthorize` de rol admin.

---

## Campo `lastSyncedAt`

En `dynamic.lastSyncedAt` (ISO-8601) se indica el momento en que se completó la lectura Dynamics **de esa respuesta**.

- No se persiste en base de datos.
- Se actualiza en cada `GET /qr/{lote}` exitoso y en cada `POST .../sync-dynamics` exitoso.
- UI (Web/Android):

```text
Última sincronización
24/07/2026 14:36:18

Fuente:
Dynamics 365 Finance & Operations
```

---

## Comportamiento ante fallo de Dynamics

| Capa | Comportamiento |
|------|----------------|
| Backend | Responde error (p. ej. 502/504 `DYNAMICS_*`); **no** inventa datos |
| Web / Android | Si ya había datos en pantalla, **los conserva** y muestra mensaje: *«No fue posible sincronizar…»* |
| | No elimina Estado Operativo, inventario ni resumen previos |

---

## Qué se refresca

Todo lo derivado de Dynamics en la respuesta QR:

- Estado Operativo (`dynamic.status`) + regla + fuente  
- Inventario / cantidad / unidad  
- Almacén y ubicación  
- Datos de calidad / disposición (campos de diagnóstico)  
- Fecha de entrada Dynamics  
- Código, nombre, caducidad Dynamics (cuando aplican)  
- Futura trazabilidad operativa (`operationalTraceability`, cuando se implemente)  

**No** forman parte del sync Dynamics (siguen siendo locales):

- Datos de etiqueta en PostgreSQL (`label.*`) salvo lo que se relee de BD  
- `platformStatus` / workflow interno  
- Escaneos y comentarios  

---

## Componentes tocados (v1.6.3)

| Capa | Archivo |
|------|---------|
| API | `QrController` — `POST /{lote}/sync-dynamics` |
| Servicio | `QrQueryService.syncWithDynamics` / `buildResponse` |
| DTO | `QrDto.Dynamic.lastSyncedAt` |
| Security | `POST /api/v1/qr/*/sync-dynamics` autenticado |
| Web | `BatchLookupPage` — botón + toast + conservar datos |
| Android | `ResultViewModel.syncWithDynamics`, `ResultScreen`, `OlnaturaApi` |

**Sin cambios:** `OperationalStatusResolver`, `ApprovalService`, reglas de workflow, corrección admin.

---

## Filosofía

> «Sincronizar con Dynamics» **únicamente fuerza una nueva lectura del ERP** y **nunca modifica información del mismo**.  
> Olnatura QR es un espejo operativo de Dynamics, no un sistema de registro paralelo del Estado Operativo.
