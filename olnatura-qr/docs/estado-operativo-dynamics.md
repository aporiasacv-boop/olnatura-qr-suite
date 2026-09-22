# Estado operativo — Dynamics como fuente de verdad

## Regla de negocio

> **El estado operativo pertenece exclusivamente a Dynamics 365 Finance & Operations.
> Olnatura QR únicamente almacena una copia sincronizada para fines operativos y de rendimiento.
> Ante cualquier diferencia, siempre prevalece el valor obtenido desde Dynamics.**

Esta regla aplica solo al **Estado Operativo** (`qr_labels.status`).
El **estado administrativo** del lote (`admin_status`: ACTIVE / BAJA / etc.) no se modifica por Dynamics.

## Copia local

| Campo | Significado |
|--------|-------------|
| `qr_labels.status` | Copia sincronizada del Estado Operativo (APROBADO, CUARENTENA, RECHAZADO, PARCIAL) |
| `qr_labels.admin_status` | Ciclo de vida administrativo en la plataforma (no lo escribe Dynamics) |

La UI de consulta muestra el Estado Operativo **calculado en vivo** desde Dynamics.
La copia en BD se mantiene alineada para listados, métricas, exportes y gates de workflow.

## Cuándo se actualiza la copia

Cada vez que la plataforma vuelve a consultar Dynamics para un lote:

1. **Consulta de lote** (`GET /api/v1/qr/{lote}`) — si hay diferencia, Dynamics gana y se persiste de inmediato.
2. **Sincronizar con Dynamics** (`POST /api/v1/qr/{lote}/sync-dynamics`).
3. **Alinear con Dynamics** (admin, masivo) (`POST /api/v1/admin/lots/align-from-dynamics`).
4. **Generación de etiqueta** (`POST /api/v1/label`) — tras crear, se consulta Dynamics y se guarda el estado.
5. **Backfill al arranque** (`app.dynamics.sync-operational-status-on-startup`, por defecto `true`) — revisa todos los lotes y corrige diferencias.

Si Dynamics no aporta un estado definitivo (`DESCONOCIDO` / sin datos), **no se sobrescribe** la copia local.

## Auditoría

Cada cambio de copia genera un evento `SYNC_OPERATIONAL_STATUS_DYNAMICS` con:

- Lote
- Estado anterior (`from` / `estadoAnterior`)
- Estado nuevo (`to` / `estadoNuevo`)
- Fecha (`fecha`)
- Motivo (`motivo` / `motivoActualizacion`): `CONSULTA_LOTE`, `SYNC_DYNAMICS_MANUAL`, `ALIGN_FROM_DYNAMICS`, `GENERACION_ETIQUETA`, `BACKFILL_STARTUP`

También se registra en log de aplicación con prefijo `[EstadoOperativoSync]`.

## Configuración

```yaml
app:
  dynamics:
    sync-operational-status-on-startup: true   # false para omitir el backfill al arrancar
```
