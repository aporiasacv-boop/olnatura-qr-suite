# Auditoría del sistema — Estructura funcional actual

**Fecha:** 27 de julio de 2026  
**Alcance:** Solo análisis (lectura de código). **Sin modificaciones** en Backend, Web ni Android.  
**Estado:** Documento de diagnóstico. Esperar aprobación antes de implementar cambios.

---

## 1. Roles existentes

El sistema tiene **exactamente 5 roles**. Un usuario tiene **un solo rol**.

| Nombre interno (BD / JWT) | Presentación estándar | Otras etiquetas en UI |
|---------------------------|------------------------|------------------------|
| `ADMIN` | Administrador | “ADMIN” en dropdown de Usuarios |
| `CALIDAD` | Calidad | “CONTROL DE CALIDAD” en Usuarios / solicitud de acceso |
| `INSPECCION` | Inspección | “INSPECCIÓN” |
| `ALMACEN` | Almacén | “ALMACÉN” |
| `PRODUCCION` | Producción | “PRODUCCIÓN” |

**Origen:** migraciones `V1__init.sql` + `V14__approval_workflow_roles.sql`.  
**Solicitud de acceso:** solo se puede pedir `ALMACEN`, `PRODUCCION`, `CALIDAD` o `INSPECCION` (no `ADMIN`).

No existen roles adicionales.

---

## 2. Matriz de acceso

### 2.1 ADMIN (Administrador)

**Pantallas Web que puede abrir**
- Panel principal
- Consulta por lote
- Historial de escaneos
- Generar etiqueta
- Registrar etiqueta
- Métricas operativas
- Aprobar usuarios
- Usuarios
- Lotes
- Historial de auditoría

**Android**
- Login, Escáner, Resultado de lote, Reportar problema, Solicitar acceso
- Comentarios de lote
- Corrección administrativa de etiqueta y de estado

**Puede realizar**
- Todo lo operativo (consulta, escaneo, ZPL, registrar etiqueta)
- Aprobar / rechazar material (actúa como Calidad e Inspección)
- Comentarios de lote
- Auditoría (listado, PDF, CSV)
- Gestión de usuarios (aprobar solicitudes, cambiar rol, habilitar/deshabilitar)
- Métricas y exportación Power BI
- Gestión administrativa de lotes
- Corrección administrativa de datos y de estado

**No puede / restricciones**
- Quitarse a sí mismo el rol ADMIN
- Deshabilitarse a sí mismo (protección en UI)

---

### 2.2 CALIDAD

**Pantallas Web**
- Panel principal
- Consulta por lote
- Historial de escaneos
- Generar etiqueta
- Historial de auditoría

**Android**
- Escáner + Resultado + Comentarios + Reportar

**Puede realizar**
- Consulta, escaneo, ZPL
- Comentarios
- Aprobar / rechazar según tipo de material:
  - Materia Prima → sí
  - Empaque Primario → pata de Calidad
  - Empaque Secundario → no (solo Inspección)
- Ver y exportar auditoría (PDF/CSV)

**No puede**
- Registrar etiqueta
- Métricas / Usuarios / Aprobar usuarios / Lotes admin
- Corrección administrativa
- Aprobar Empaque Secundario puro

---

### 2.3 INSPECCIÓN (`INSPECCION`)

**Pantallas Web**
- Panel principal
- Consulta por lote
- Historial de escaneos
- Generar etiqueta
- Historial de auditoría

**Android**
- Escáner + Resultado + Comentarios + Reportar

**Puede realizar**
- Consulta, escaneo, ZPL
- Comentarios
- Aprobar / rechazar según tipo:
  - Empaque Secundario → sí
  - Empaque Primario → pata de Inspección
  - Materia Prima → no (solo Calidad)
- Auditoría (listado, PDF, CSV)

**No puede**
- Registrar etiqueta
- Admin (métricas, usuarios, lots)
- Corrección administrativa
- Aprobar Materia Prima pura

---

### 2.4 ALMACÉN (`ALMACEN`)

**Pantallas Web**
- Panel principal
- Consulta por lote
- Historial de escaneos
- Generar etiqueta
- Registrar etiqueta

**Android**
- Escáner + Resultado + Comentarios + Reportar

**Puede realizar**
- Consulta, escaneo, ZPL
- Alta / registro de etiquetas
- Comentarios de lote

**No puede**
- Aprobar / rechazar material
- Ver auditoría
- Admin (métricas, usuarios, lots)
- Corrección administrativa

---

### 2.5 PRODUCCIÓN (`PRODUCCION`)

**Pantallas Web**
- Panel principal
- Consulta por lote
- Historial de escaneos
- Generar etiqueta

**Android**
- Escáner + Resultado + Reportar
- **Sin comentarios**

**Puede realizar**
- Consulta, escaneo, ZPL

**No puede**
- Registrar etiqueta
- Comentarios de lote (excluido en API y UI)
- Aprobar / rechazar
- Auditoría
- Admin / correcciones

---

### 2.6 Resumen rápido de capacidades

| Capacidad | ADMIN | CALIDAD | INSPECCIÓN | ALMACÉN | PRODUCCIÓN |
|-----------|:-----:|:-------:|:----------:|:-------:|:----------:|
| Consulta / escaneo / ZPL | ✓ | ✓ | ✓ | ✓ | ✓ |
| Registrar etiqueta | ✓ | ✗ | ✗ | ✓ | ✗ |
| Comentarios | ✓ | ✓ | ✓ | ✓ | ✗ |
| Aprobar / rechazar material | ✓ | ✓* | ✓* | ✗ | ✗ |
| Auditoría | ✓ | ✓ | ✓ | ✗ | ✗ |
| Admin usuarios / métricas / lots | ✓ | ✗ | ✗ | ✗ | ✗ |
| Corrección administrativa | ✓ | ✗ | ✗ | ✗ | ✗ |
| Aprobar/rechazar en Android | ✗** | ✗** | ✗** | ✗ | ✗ |

\* Según tipo de material (ver §2.2–2.3).  
\*\* La API lo permite; la UI Android **no** expone botones de aprobación/rechazo.

---

## 3. Pantallas existentes

### 3.1 Web (`qr-enterprise-frontend`)

| Pantalla | Ruta | Para qué sirve |
|----------|------|----------------|
| Login | `/login` | Inicio de sesión |
| Solicitud de acceso | `/register-request` | Pedir cuenta (elige área/rol) |
| Panel principal | `/` | Hub con accesos rápidos |
| Consulta por lote | `/lookup` | Ficha completa: etiqueta, Dynamics, workflow, escaneos, comentarios, PDF, aprobación, corrección admin |
| Historial de escaneos | `/scan-history` | Buscar lote y ver escaneos |
| Generar etiqueta | `/generate-qr` | Preview QR, PNG, ZPL / reimpresión |
| Registrar etiqueta | `/register-label` | Alta de etiqueta (Dynamics + formulario) |
| Métricas operativas | `/admin/metrics` | KPIs, serie diaria, actividad reciente, export Power BI |
| Aprobar usuarios | `/admin/approval` | Aprobar/rechazar solicitudes |
| Usuarios | `/admin/users` | Listar, cambiar rol, habilitar/deshabilitar |
| Lotes | `/admin/lots` | Listado admin y estado administrativo |
| Historial de auditoría | `/admin/audit` | Filtros + tabla + export CSV |
| 404 | `*` | Ruta inexistente |

**Landing pública (no SPA):** `GET /qr/{lote}` — aviso al escanear QR fuera de la app autenticada.

---

### 3.2 Android (`OlnaturaQR`)

| Pantalla | Ruta Compose | Para qué sirve |
|----------|--------------|----------------|
| Bootstrap | `boot` | Restaura sesión → Scanner o Login |
| Login | `login` | Autenticación (+ Credential Manager) |
| Solicitar acceso | `request-access` | Pedir cuenta |
| Escáner | `scanner` | Cámara + ML Kit |
| Resultado | `result/{lote}` | Ficha del lote tras escanear |
| Reportar (lote) | `report/{lote}` | Problema del QR/lote |
| Reportar (acceso) | `report-access` | Problema de login |

No hay pantallas Android de: admin, auditoría global, generar/registrar etiqueta, métricas.

---

### 3.3 Backend (API — no hay pantallas; alimenta UI)

| Controller | Base | Alimenta |
|------------|------|----------|
| AuthController | `/api/v1/auth` | Login, me, logout, request-access |
| QrController | `/api/v1/qr` | Consulta lote Web/Android |
| ScanController | `/api/v1/scan` | Escaneos |
| LabelController | `/api/v1/label` | Alta, approve/reject, ZPL |
| LabelSuggestController | `/api/v1/labels` | Autocomplete |
| DynamicsLookupController | `/api/v1/dynamics` | Lookup Dynamics |
| AuditController | `/api/v1/audit` | Auditoría, PDF, CSV |
| AdminController | `/api/v1/admin` | Solicitudes de acceso |
| AdminUsersController | `/api/v1/admin/users` | Usuarios |
| AdminLotsController | `/api/v1/admin/lots` | Lotes + correcciones |
| MetricsController | `/api/v1/admin/metrics` | Métricas / Power BI |
| LoteCommentController | `/api/v1/comments` | Comentarios |
| LiberacionDiagnosticoController | `/api/v1/diagnostics/liberacion` | Diagnóstico liberación |
| PublicQrController | `/qr` | Landing pública QR |

---

## 4. Menús por rol (Web)

Fuente: `Sidebar.tsx` + `AuthContext.can()`.

| Ítem del menú | ADMIN | ALMACÉN | PRODUCCIÓN | CALIDAD | INSPECCIÓN |
|---------------|:-----:|:-------:|:----------:|:-------:|:----------:|
| **Operación →** Panel principal | ✓ | ✓ | ✓ | ✓ | ✓ |
| Consulta por lote | ✓ | ✓ | ✓ | ✓ | ✓ |
| Historial de escaneos | ✓ | ✓ | ✓ | ✓ | ✓ |
| **Etiquetas →** Generar etiqueta | ✓ | ✓ | ✓ | ✓ | ✓ |
| Registrar etiqueta | ✓ | ✓ | ✗ | ✗ | ✗ |
| **Administración →** Métricas | ✓ | ✗ | ✗ | ✗ | ✗ |
| Aprobar usuarios | ✓ | ✗ | ✗ | ✗ | ✗ |
| Usuarios | ✓ | ✗ | ✗ | ✗ | ✗ |
| Lotes | ✓ | ✗ | ✗ | ✗ | ✗ |
| Historial de auditoría | ✓ | ✗ | ✗ | ✓ | ✓ |

**Android:** no tiene menú lateral. Flujo lineal Login → Escáner → Resultado. Las diferencias por rol aparecen dentro de Resultado (comentarios / corrección).

---

## 5. Acciones auditadas

### 5.1 Acciones que SÍ se registran hoy (15)

| Acción interna | Dónde se emite |
|----------------|----------------|
| `PRINT_LABEL` | LabelController (impresión / descarga ZPL) |
| `GENERATE_LABEL` | Cliente → `POST /api/v1/audit/log` (whitelist) |
| `ACCESS_REQUEST` | AuthController (`logUnauthenticated`) |
| `APPROVE_USER` | AdminController |
| `REJECT_USER` | AdminController |
| `UPDATE_USER` | AdminUsersController |
| `APPROVE_MATERIAL` | ApprovalService |
| `REJECT_MATERIAL` | ApprovalService |
| `CHANGE_LOT_ADMIN_STATUS` | AdminLotsController |
| `ADMIN_CORRECT_LABEL` | AdminLabelCorrectionService |
| `ADMIN_CORRECT_STATUS` | AdminStatusCorrectionService |
| `ADD_LOTE_COMMENT` | LoteCommentController |
| `EXPORT_AUDIT_PDF` | AuditController |
| `EXPORT_AUDIT_CSV` | AuditController |
| `EXPORT_EXECUTIVE_DASHBOARD` | MetricsController |

### 5.2 En el traductor, pero NO se emiten en auditoría hoy

| Acción | Nota |
|--------|------|
| `SCAN_QR` / `SCAN` | Los escaneos viven en `scan_events`, no en `audit_events` |
| `LOGIN_SUCCESS` | Solo log de consola; no se escribe en auditoría |
| `LOGOUT` | Sin emisión encontrada |
| `CHANGE_STATUS` | Solo seed demo; flujo real usa APPROVE/REJECT_MATERIAL |
| `DOWNLOAD_LABEL` | Sin emisión; descarga ZPL se audita como `PRINT_LABEL` |

### 5.3 Presentación actual (traductor)

| Interna | Español |
|---------|---------|
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

## 6. Propuestas de mejora visual (solo análisis)

### 6.1 Problemas detectados

#### A) Usuarios (`/admin/users`) — caso señalado
- Columnas: Usuario | Correo | Rol | Estado | Habilitado | Acciones.
- Usuario y Correo se renderizan en celdas planas **sin truncate, sin maxWidth, sin tooltip**.
- Solo la columna Rol tiene `minWidth: 140` (por el Dropdown) → comprime Usuario/Correo.
- Usernames largos (`Virginia.Amaro`, `supervisor.inspeccion`) y correos corporativos compiten por espacio → sensación de que el nombre “invade” la columna Correo.
- Columna Estado muestra valor crudo (`u.estado`) sin traducción amigable.
- Dropdown de roles mezcla: “ADMIN” crudo vs “CONTROL DE CALIDAD” / “ALMACÉN”.

#### B) Historial de escaneos (`ScanHistoryTable`)
- Grid `1fr 0.8fr 1.2fr 1fr 1fr 1fr` **sin `minmax(0, …)`** → contenido largo no encoge bien.
- Sin ellipsis en Usuario / Detalle.
- Columna Acción siempre igual (“Escaneo de código QR”) → poco valor informativo.
- “Detalle” a veces es el lote (redundante / confuso).

#### C) Auditoría y Métricas (actividad reciente)
- Sin anchos fijos ni truncate en Acción / Usuario / Lote.
- Labels de acción muy largas (ej. “Cambio de estado administrativo del lote”).
- Filtro de auditoría incluye acciones “fantasma” (`LOGIN_SUCCESS`, `LOGOUT`, `DOWNLOAD_LABEL`, `CHANGE_STATUS`, `SCAN`) que casi nunca existen en BD.
- En Auditoría, fecha+hora en una sola línea sin `nowrap` (en Métricas ya se separan).

#### D) Consulta por lote
- Labels Dynamics en inglés: `QualityOrderStatus`, `PassedBatchDispositionCode`, `BatchDispositionCode`.
- Nombres de material largos sin `line-clamp`.
- Tres flujos admin/operativos poco diferenciados visualmente: Aprobar/Rechazar vs Editar (Admin) vs Corrección de estado.
- Botones con códigos internos: `→ APROBADO`, `→ CUARENTENA`.

#### E) Aprobar usuarios
- Columna **ID** muestra UUID completo.

#### F) Otros
- IDs técnicos en detalle de auditoría (ya detrás de “Ver IDs técnicos” — correcto).
- Inconsistencia: `canCreateLabel` en API QR puede marcar true para roles que no pueden hacer `POST /label`.

---

### 6.2 Cómo reorganizar tablas (sin perder información)

#### Usuarios — propuesta
| Columna actual | Propuesta |
|----------------|-----------|
| Usuario + Correo (2 cols) | **Una columna “Usuario”**: nombre en negrita + correo en 2.ª línea muted, truncate + tooltip |
| Rol | Mantener; ancho fijo ~180px; labels siempre en español (Administrador, etc.) |
| Estado | Traducir a español (Activo / Pendiente / …) |
| Habilitado | Mantener o fusionar visualmente con Estado |
| Acciones | Mantener |

Alternativa: mantener Usuario y Correo separados pero con `table-layout: fixed`, anchos relativos y ellipsis + `title`.

#### Escaneos — propuesta
| Columna | Propuesta |
|--------|-----------|
| Fecha / Hora | Mantener; `minmax` + nowrap |
| Usuario | Truncate + tooltip |
| Rol | Mantener |
| Acción | Valorar **ocultar** (siempre igual) o dejarla |
| Detalle | Renombrar a **Lote** (o quitar si el lote ya está en el contexto de la página) |

#### Auditoría — propuesta
| Columna | Propuesta |
|--------|-----------|
| Fecha | Dos líneas (fecha / hora) como en Métricas |
| Acción | Truncate; labels más cortas |
| Usuario / Rol / Lote | Truncate + tooltip |
| Detalle | Mantener colapsable actual |
| Filtro | Solo las 15 acciones realmente emitidas |

#### Aprobar usuarios — propuesta
| Columna | Propuesta |
|--------|-----------|
| ID (UUID) | **Eliminar de la vista** o mostrar últimos 8 caracteres + botón copiar |
| Resto | Truncate en Usuario/Correo |

---

## 7. Propuesta de mejora (documento de decisión)

> **Importante:** Nada de esto está implementado. Requiere aprobación explícita.

### Qué cambiaría

1. **Presentación de tablas** (Usuarios, Escaneos, Auditoría, Métricas, Aprobar usuarios, Lotes).
2. **Copy / traducciones de UI** (roles en dropdowns, estados, labels Dynamics, botones de estado).
3. **Filtro de auditoría** (quitar acciones no emitidas o marcarlas como histórico).
4. **Jerarquía visual en Consulta por lote** (separar aprobación operativa vs corrección excepcional).
5. Opcional (fuera de solo-visual): alinear `canCreateLabel` con el permiso real de `POST /label`.

### Por qué

- Mejor legibilidad operativa (menos solapamiento, menos UUID, menos inglés técnico).
- Menos confusión entre roles/acciones similares.
- Consistencia con el trabajo ya hecho de usuario/rol/acciones legibles en Historial y Auditoría.
- Sin tocar BD, eventos internos, enums ni lógica de auditoría.

### Qué pantallas modificaría

| Prioridad | Pantalla | Tipo de cambio |
|-----------|----------|----------------|
| 1 | Usuarios | Layout tabla + truncate + labels |
| 1 | Aprobar usuarios | Quitar/acortar UUID |
| 1 | Historial de escaneos / tabla compartida | Grid + columnas |
| 2 | Auditoría | Anchos, fecha, filtro |
| 2 | Métricas (actividad reciente) | Truncate / consistencia |
| 2 | Consulta por lote | Labels Dynamics ES + jerarquía botones |
| 3 | Lotes (admin) | Truncate / labels de estado |
| 3 | Dropdowns de roles (Usuarios, Solicitud acceso) | Unificar a español estándar |

**Android:** solo si se desea alinear textos de roles/botones; no hay tablas densas como en Web.

**Backend:** solo si se aprueba enriquecer/ajustar DTOs de presentación (p. ej. estado de usuario traducido). **No** cambiar persistencia ni auditoría interna.

### Qué tablas reorganizaría

1. **Usuarios** — fusionar Usuario+Correo visualmente o truncar ambas.
2. **Escaneos** — ajustar columnas; reconsiderar Acción y Detalle.
3. **Auditoría / Actividad reciente** — anchos fijos + ellipsis.
4. **Aprobar usuarios** — sin UUID completo.
5. **Lotes** — truncate en Nombre/Código si aplica.

### Qué traducciones haría

| De | A (propuesta) |
|----|----------------|
| ADMIN (visible) | Administrador |
| CONTROL DE CALIDAD | Calidad (o mantener “Control de calidad” de forma consistente en todo el sistema) |
| QualityOrderStatus | Estado de orden de calidad |
| PassedBatchDispositionCode | Código de disposición (aprobado) |
| BatchDispositionCode | Código de disposición de lote |
| `→ APROBADO` / `→ CUARENTENA` | “Pasar a Aprobado” / “Pasar a Cuarentena” |
| Estado crudo de usuario | Activo / Pendiente / etc. |
| Labels de acción muy largas | Versiones ≤ ~32 caracteres donde sea posible |

### Qué información eliminaría (solo de la vista)

- Columna UUID completa en Aprobar usuarios (o reducirla).
- Posiblemente columna Acción en historial de escaneos (si se confirma que siempre es “Escaneo”).
- Acciones fantasma del filtro de auditoría (del dropdown, no de la BD).
- Prefijos/códigos internos en botones visibles.

**No eliminaría** datos de base de datos ni eventos de auditoría.

### Qué información agregaría (solo presentación)

- Tooltip / `title` con valor completo al truncar.
- Segunda línea muted para correo bajo el username (Usuarios).
- Separadores visuales claros: “Aprobación operativa” vs “Corrección excepcional (Administrador)” en Consulta.
- (Opcional) Indicación “(histórico)” si se mantienen acciones no emitidas en el filtro.

### Qué NO tocaría (salvo nueva instrucción)

- Base de datos / migraciones
- Enums internos de roles o estados
- Emisión de eventos de auditoría (nombres internos)
- Catálogo o almacenamiento de dispositivos
- Lógica de negocio de aprobación / corrección

---

## Inconsistencias funcionales detectadas (para contexto)

1. `canCreateLabel` en respuesta QR puede ser true para roles que no pueden `POST /api/v1/label`.
2. PRODUCCIÓN opera (lookup/scan/ZPL) pero no comenta ni ve auditoría.
3. Android no expone UI de aprobación/rechazo aunque la API lo permita a CALIDAD/INSPECCIÓN/ADMIN.
4. Traductor incluye acciones (`LOGIN_SUCCESS`, `LOGOUT`, etc.) que hoy no se persisten en `audit_events`.

---

## Siguiente paso

Este documento es **solo diagnóstico**.

Para implementar, se requiere aprobación explícita sobre:

1. ¿Se aprueba la prioridad 1 (Usuarios + UUID en Aprobar + Escaneos)?
2. ¿Se unifican labels de rol a “Administrador / Calidad / Inspección / Almacén / Producción”?
3. ¿Se traduce Dynamics a español en Consulta?
4. ¿Se limpia el filtro de auditoría a solo las 15 acciones vivas?
5. ¿Se toca Android o solo Web en esta iteración?

**Ningún código se modificará hasta recibir esa aprobación.**
