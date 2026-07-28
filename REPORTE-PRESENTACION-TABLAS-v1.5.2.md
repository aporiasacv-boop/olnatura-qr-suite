# Reporte — Presentación e identidad visual de usuarios (v1.5.2)

**Fecha:** 27 de julio de 2026  
**Alcance:** Solo presentación Web embebida en JAR. Sin cambios de negocio, BD ni auditoría.

---

## Artefactos

| Artefacto | Versión | Ruta |
|-----------|---------|------|
| JAR | 1.5.2 | `olnatura-qr/api/olnaturaqr/build/libs/olnaturaqr-1.5.2.jar` |
| APK | 1.5.2 (versionCode 9) | `OlnaturaQR/app/build/outputs/apk/release/app-release.apk` |

---

## Utilidades nuevas

| Archivo | Propósito |
|---------|-----------|
| `utils/tablePresentation.ts` | `TABLE_FIXED_STYLE`, `TRUNCATE_CELL`, `TABLE_SCROLL_WRAP`, `cellTitle()` |
| `utils/auditActionTranslator.ts` | `displayUserIdentity()` — nunca UUID ni correo como identificador principal |

---

## Pantallas modificadas

| Pantalla | Archivo | Mejoras |
|----------|---------|---------|
| **Usuarios** | `AdminUsersPage.tsx` | Tabla fija, anchos, ellipsis; columna Usuario con nombre legible |
| **Aprobar usuarios** | `AdminApprovalPage.tsx` | Sin UUID; usuario legible; truncate en correo/rol |
| **Historial de escaneos** | `ScanHistoryTable.tsx` | Migrado a Fluent Table; layout fijo; sin dispositivo |
| **Consulta por lote** | `BatchLookupPage.tsx` | Comentarios con `displayUserIdentity` |
| **Historial de escaneos (página)** | `ScanHistoryPage.tsx` | Usa `ScanHistoryTable` actualizado |
| **Auditoría** | `AdminAuditPage.tsx` | Tabla fija, truncate, usuario sin email/UUID |
| **Métricas — Actividad reciente** | `AdminMetricsPage.tsx` | Tabla fija, truncate, identidad legible |
| **Lotes (admin)** | `AdminLotsPage.tsx` | Tabla fija, truncate en lote/nombre/código |

---

## Reglas de identidad de usuario

- **Nunca** mostrar UUID en columnas de usuario.
- **Nunca** usar correo como identificador principal si existe username.
- Formato legible: `Virginia.Amaro` → `Virginia Amaro`; `supervisor.inspeccion` → `Supervisor Inspeccion`.
- Correo permanece solo en columna **Correo** (Usuarios / Aprobaciones).

---

## Dispositivos

- Columna **Dispositivo** no presente en ninguna tabla Web.
- Clave `deviceId` excluida de metadatos visibles en auditoría.

---

## Técnicas aplicadas en tablas

- `table-layout: fixed`
- Anchos porcentuales por columna
- `overflow: hidden` + `text-overflow: ellipsis` + `white-space: nowrap`
- Atributo `title` con valor completo al pasar el cursor
- Scroll horizontal solo en contenedor (`TABLE_SCROLL_WRAP`) cuando el viewport es estrecho

---

## Validación

| Criterio | Estado |
|----------|--------|
| UUID visible en tablas | Eliminado |
| Correo como nombre de usuario | Corregido |
| Columna Dispositivo | Ausente |
| Texto montado / columnas invadidas | Mitigado con layout fijo + ellipsis |
| Compilación JAR | OK |
