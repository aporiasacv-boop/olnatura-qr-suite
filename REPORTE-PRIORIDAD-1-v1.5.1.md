# Reporte — Prioridad 1 (mejoras de presentación)

**Versión:** 1.5.1  
**Fecha:** 27 de julio de 2026  
**Alcance:** Solo las 3 mejoras autorizadas. Sin cambios de negocio, permisos, auditoría ni arquitectura.

---

## Artefactos

| Artefacto | Versión | Ruta |
|-----------|---------|------|
| JAR | 1.5.1 | `olnatura-qr/api/olnaturaqr/build/libs/olnaturaqr-1.5.1.jar` |
| APK | 1.5.1 (versionCode 8) | `OlnaturaQR/app/build/outputs/apk/release/app-release.apk` |

> Android no tiene pantallas de Usuarios / Aprobar usuarios / etiquetas Dynamics en inglés; el APK se regeneró con el bump de versión para el entregable.

---

## Pantallas modificadas

| Pantalla | Archivo | Cambio |
|----------|---------|--------|
| Usuarios | `AdminUsersPage.tsx` | Redistribución de columnas + truncate |
| Aprobar usuarios | `AdminApprovalPage.tsx` | Eliminación de UUID; usuario legible |
| Consulta por lote | `displayLabels.ts` (usado por `BatchLookupPage.tsx`) | Etiquetas Dynamics en español |

---

## Columnas ajustadas

### Usuarios (`/admin/users`)

| Columna | Ajuste |
|---------|--------|
| Usuario | Ancho 18%; ellipsis + `title` con valor completo |
| Correo | Ancho 26%; ellipsis + `title` (ya no invade Usuario) |
| Rol | Ancho 18%; Dropdown con ancho acotado al 100% de la celda |
| Estado | Ancho 12%; ellipsis |
| Habilitado | Ancho 10% (se conserva; no se pierde información) |
| Acciones | Ancho 16% |

Técnica: `table-layout: fixed` + `overflow: hidden` / `text-overflow: ellipsis` / `white-space: nowrap`.

### Aprobar usuarios (`/admin/approval`)

| Antes | Después |
|-------|---------|
| **ID** (UUID completo) | **Eliminada** |
| Usuario (username crudo) | Nombre legible (`Virginia.Amaro` → `Virginia Amaro`); si el formateo difiere, username debajo en muted |
| Correo | Truncate + title |
| Rol | Presentación en español (Administrador, Calidad, …); valor interno intacto |
| Creado / Acciones | Conservadas; anchos fijos |

---

## Etiquetas traducidas (Consulta por lote — Dynamics)

Solo presentación. Campos JSON / nombres internos sin cambio.

| Clave interna (sin cambio) | Antes (UI) | Después (UI) |
|----------------------------|------------|--------------|
| `qualityOrderStatus` | QualityOrderStatus | Estado de orden de calidad |
| `passedBatchDispositionCode` | PassedBatchDispositionCode | Código de disposición (aprobado) |
| `batchDispositionCode` | BatchDispositionCode | Código de disposición de lote |

Ya estaban en español (sin cambio en esta entrega):

- `statusDynamics` → Estado de Dynamics  
- `ubicacion`, `almacen`, `cantidad`, `fuente`

---

## Validación

| Criterio | Resultado |
|----------|-----------|
| Textos sobrepuestos / columnas invadidas en Usuarios | Corregido con anchos fijos + ellipsis |
| UUID visibles en Aprobar usuarios | Columna ID eliminada |
| Usuario legible (no UUID) cuando hay username | Sí |
| Etiquetas Dynamics en inglés en Consulta | Traducidas a español |
| Reglas de negocio / permisos / auditoría | Sin modificar |

---

## Fuera de alcance (no implementado)

- Prioridad 2+ (Auditoría, Métricas, Escaneos, Lotes, botones `→ APROBADO`, etc.)
- Cambios en Backend de DTOs o persistencia
- Cambios de menú o permisos
