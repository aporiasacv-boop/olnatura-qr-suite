# Reporte: Mejora de presentación — Historial, Auditoría y Exportaciones

**Versión:** 1.5.0  
**Fecha:** 27 de julio de 2026  
**Alcance:** Solo capa de presentación (sin cambios en BD, eventos internos, enums ni almacenamiento de dispositivos)

---

## Artefactos generados

| Artefacto | Versión | Ruta |
|-----------|---------|------|
| JAR (Spring Boot) | 1.5.0 | `olnatura-qr/api/olnaturaqr/build/libs/olnaturaqr-1.5.0.jar` |
| APK (Android) | 1.5.0 (versionCode 7) | `OlnaturaQR/app/build/outputs/apk/release/app-release.apk` |

---

## Traductor centralizado

### Backend
- `AuditActionTranslator.java` — traducción de acciones para PDF, CSV, Excel y API enriquecida
- `RoleDisplayTranslator.java` — roles legibles (Administrador, Calidad, Inspección, Almacén, Producción)
- `UserDisplayHelper.java` — resolución UUID → username legible (ej. `Virginia.Amaro` → `Virginia Amaro`)

### Web
- `auditActionTranslator.ts` — único traductor frontend; `displayLabels.ts` delega aquí

---

## Acciones traducidas

| Acción interna | Presentación en español |
|----------------|-------------------------|
| PRINT_LABEL | Impresión de etiquetas |
| GENERATE_LABEL | Generación de etiquetas |
| SCAN_QR / SCAN | Escaneo de código QR |
| LOGIN_SUCCESS | Inicio de sesión |
| LOGOUT | Cierre de sesión |
| EXPORT_AUDIT_PDF | Exportación de auditoría (PDF) |
| EXPORT_AUDIT_CSV | Exportación de auditoría (CSV) |
| EXPORT_EXECUTIVE_DASHBOARD | Exportación de dashboard ejecutivo |
| ADD_LOTE_COMMENT | Comentario agregado al lote |
| ADMIN_CORRECT_LABEL | Corrección administrativa |
| ADMIN_CORRECT_STATUS | Corrección administrativa de estado |
| CHANGE_STATUS | Cambio de estado |
| CHANGE_LOT_ADMIN_STATUS | Cambio de estado administrativo del lote |
| APPROVE_USER | Aprobación de usuario |
| REJECT_USER | Rechazo de usuario |
| ACCESS_REQUEST | Solicitud de acceso |
| DOWNLOAD_LABEL | Descarga de etiqueta |
| APPROVE_MATERIAL | Aprobación de material |
| REJECT_MATERIAL | Rechazo de material |
| UPDATE_USER | Actualización de usuario |

---

## Columnas modificadas

### Historial de escaneos (Web)
| Antes | Después |
|-------|---------|
| Usuario (UUID) | Usuario legible (`userDisplay`) |
| Dispositivo | **Eliminada** |
| — | **Rol** (`roleDisplay`) |
| Acción: "Escaneo" | Acción: "Escaneo de código QR" |

### Auditoría (Web)
| Antes | Después |
|-------|---------|
| Actor (rol + email) | **Usuario** + **Rol** (columnas separadas) |
| Dispositivo en detalle | **Eliminado** |

### Métricas — Actividad reciente (Web)
| Antes | Después |
|-------|---------|
| Usuario (email) + rol enum | Usuario legible + Rol traducido |
| Dispositivo en detalle | **Eliminado** |

### Exportación CSV de auditoría
| Antes | Después |
|-------|---------|
| `createdAt,actionType,actorEmail,actorRol,lote,deviceId,metadata` | `createdAt,accion,usuario,rol,lote,metadata` (valores traducidos) |

### PDF de trazabilidad por lote
- Usuario: email → nombre legible
- Rol: enum → español
- Acción: traducida
- Detalle: sin prefijo "Dispositivo: …"

### Excel Power BI (dashboard ejecutivo)
- **Scan_Events:** eliminada columna `DeviceId`; agregadas `Usuario`, `Rol` legibles
- **Audit_Events:** eliminadas `ActorId`, `ActorEmail`, `ActorRol`, `ActionType`, `DeviceId`; agregadas `Usuario`, `Rol`, `Accion` legibles
- **Users:** columna `Role` traducida al español
- **DataDictionary:** actualizado acorde a las nuevas columnas

---

## Pantallas modificadas

| Pantalla | Cambios |
|----------|---------|
| Historial de escaneos (`ScanHistoryPage`) | Tabla sin dispositivo; usuario y rol legibles |
| Consulta por lote (`BatchLookupPage`) | Misma tabla de historial actualizada |
| Auditoría (`AdminAuditPage`) | Columnas Usuario + Rol; filtros desde traductor central |
| Métricas operativas (`AdminMetricsPage`) | Actividad reciente con usuario/rol legibles |
| Detalle de auditoría (`AuditDetailCell`) | Sin dispositivo en metadatos visibles |

---

## API enriquecida (sin cambiar persistencia)

### `GET /api/v1/scan/{lote}`
Campos nuevos en respuesta: `userDisplay`, `roleDisplay` (mantiene `scannedBy` interno).

### `GET /api/v1/audit`
Devuelve `AuditEventView` con: `actorDisplay`, `actorRoleDisplay`, `actionTypeDisplay`.

### `GET /api/v1/admin/metrics`
Actividad reciente con campos de presentación en lugar de email/enum crudo.

---

## Android

- `ScanEventResponse` actualizado con `userDisplay`, `roleDisplay`, `scannedBy` (compatibilidad con API enriquecida)
- Versión: **1.5.0** (versionCode **7**)

---

## Validación

- [x] UUID de usuario no se muestra cuando existe username resoluble
- [x] Columna Dispositivo eliminada de UI y exportaciones
- [x] Acciones visibles en español
- [x] Rol visible en español (Administrador, Calidad, Inspección, Almacén, Producción)
- [x] Traductor único backend + traductor único web
- [x] JAR y APK compilados correctamente
