# Release Notes — v1.6.1 Phase 1

**Versión:** 1.6.1  
**Tag:** `v1.6.1-phase1`  
**Fecha:** 27 de julio de 2026  
**Rama:** `main`  
**Estado:** Cierre oficial de la **Fase 1 — Operativa Integrada**

---

## Objetivos alcanzados

- Sistema operativo de trazabilidad QR para Olnatura (Backend + Web + Android).
- Integración de lectura con **Microsoft Dynamics 365 Finance & Operations**.
- Estado Operativo del lote determinado **exclusivamente desde Dynamics**.
- Centro de información del lote unificado para operación diaria.
- Auditoría, historial de escaneos, comentarios y roles funcionales.
- Exportaciones / métricas orientadas a Power BI.

---

## Funcionalidades principales

- Consulta de lote por identificador / token público.
- Registro de etiquetas y generación/descarga ZPL.
- Escaneo y historial de escaneos.
- Bitácora de comentarios por lote.
- Flujo de aprobaciones / rechazos / corrección administrativa (estado de plataforma).
- Administración de usuarios y solicitudes de acceso.
- Auditoría de acciones y exportación PDF de trazabilidad.
- Métricas operativas y exportaciones para analítica.

---

## Cambios arquitectónicos

- Separación clara entre:
  - **Estado Operativo** (Dynamics → banner UI)
  - **Estado de plataforma** (`qr_labels.status` → workflow interno / compatibilidad)
- Cliente Dynamics OData (`RealDynamicsClient` / mock) orquestado por `DynamicsLookupService`.
- `OperationalStatusResolver` como regla central de interpretación del estado.
- Frontend embebido en el JAR estático del API para despliegue unificado.

---

## Integración con Dynamics

- OAuth client credentials hacia FO producción.
- Entidades usadas: ItemBatches, QualityOrderHeaders, InventDimBiEntities, InventTransCDSEntities, InventorySitesOnHand, ReleasedProductsV2, Warehouses (investigación).
- Lectura de almacén físico vía **InventDim.InventLocationId** (REM / RES / CUARENTENA).
- Lectura de `BatchDispositionCode` como refuerzo.
- El banner **no** usa `qr_labels.status` como fuente de verdad.

---

## Estado Operativo (v1.6.0 → vigente en 1.6.1)

| Regla | Resultado |
|-------|-----------|
| Almacén REM | RECHAZADO |
| Almacén RES | RECHAZADO |
| Almacén CUARENTENA | CUARENTENA |
| Otro almacén + disposición / liberado implícito | APROBADO |
| Datos insuficientes | DESCONOCIDO (⚪ No determinado) |

Presentación: 🟢 Aprobado · 🟡 Cuarentena · 🔴 Rechazado · ⚪ No determinado  
Origen visible: Dynamics 365 Finance & Operations.

---

## Mejoras UI

- **v1.6.1:** Centro de información del lote (2 columnas; comentarios integrados; resumen operativo; técnico colapsable).
- Tablas de Usuarios / Aprobaciones / Escaneos / Auditoría / Métricas / Lotes con presentación consistente.
- Identidad de usuario legible (sin UUID como nombre).
- Traducción de acciones de auditoría y roles a español.
- StatusTag operativo con semántica visual clara.

---

## Seguridad

- Autenticación de sesión web y roles (ADMIN, ALMACÉN, PRODUCCIÓN, CALIDAD, INSPECCIÓN).
- Permisos granulares en consulta de lote (comentarios, aprobación, corrección admin, PDF).
- Secretos OAuth fuera del repositorio (variables de entorno / configuración local).
- Sin commits de credenciales en este cierre.

---

## Auditoría

- Registro de eventos operativos y administrativos.
- Traducción de acciones para lectura humana.
- PDF de trazabilidad por lote.
- Corrección de etiqueta/estado con motivo auditado.

---

## Comentarios

- Endpoints existentes `GET/POST /api/v1/comments/{lote}`.
- Integrados en el Centro de información (orden reciente → antiguo).
- Alta bajo permiso; solo lectura si el rol no aplica.

---

## Power BI

- Métricas operativas en Web.
- Exportaciones ejecutivas (Excel / dashboard) alineadas al modelo de datos de auditoría y operación.

---

## Android

- App `OlnaturaQR` versionName **1.6.1** / versionCode **11**.
- Banner de Estado Operativo + origen Dynamics.
- Modelos alineados a la respuesta enriquecida del API.

---

## Web

- React + Fluent UI — Centro de información del lote.
- Build embebido en recursos estáticos del API (`index-D37Qvh07.js`).

---

## Backend

- Spring Boot API version **1.6.1**.
- `OperationalStatusResolver` + tests.
- Enriquecimiento de `QrDto.Dynamic` (`status`, `operationalStatusRule`, `statusSource`, `platformStatus`).
- InventDim ampliado para almacenes operativos.

---

## Documentación incluida en el cierre

- `IMPLEMENTACION-ESTADO-OPERATIVO-DYNAMICS-v1.6.0.md`
- `INVESTIGACION-DYNAMICS-ESTADO-Y-TRAZABILIDAD-LOTES.md`
- `DOC-ESTADO-OPERATIVO-DESDE-DYNAMICS.md`
- `REPORTE-CENTRO-INFORMACION-LOTE-v1.6.1.md`
- `REPORTE-PRESENTACION-TABLAS-v1.5.2.md`
- `docs/capturas-v1.6.0/` · `docs/capturas-v1.6.1/`
- Este archivo: `RELEASE-v1.6.1-PHASE1.md`

---

## Fuera de alcance de este tag

- Artefactos temporales de probes OData (`scripts/out/...`) — **no versionados**.
- Cambios de código posteriores a este cierre.

---

## Nota de cierre

La **Fase 1** queda congelada en `v1.6.1-phase1` como línea base operativa integrada (Dynamics + Estado Operativo + Centro de información + Android/Web/Backend).
