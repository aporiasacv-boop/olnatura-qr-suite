# Reporte — Centro de información del lote v1.6.1

**Fecha:** 27 de julio de 2026  
**Versión:** 1.6.1  
**Alcance:** Rediseño UI de Consulta por lote → Centro de información del lote (solo presentación).

---

## 1. Objetivo cumplido

La pantalla concentra en un solo lugar:

- Información del lote  
- Comentarios del lote  
- Estado Operativo Dynamics  
- Resumen operativo  
- Información técnica (acordeón)  
- Historial de escaneos (ancho completo)

Sin espacios vacíos grandes: los comentarios ocupan la columna izquierda bajo los datos del lote.

---

## 2. Layout

### Desktop / tablet
| Columna | ~% | Contenido |
|---------|----|-----------|
| Izquierda | 70% (`7fr`) | Información del lote + Comentarios |
| Derecha | 30% (`3fr`) | Estado Operativo → Resumen operativo → Info técnica |

### Móvil (≤960px)
Una columna, orden:

1. Estado Operativo / Resumen / Técnico  
2. Información del lote  
3. Comentarios  

---

## 3. Confirmaciones requeridas

| Ítem | Confirmación |
|------|--------------|
| Endpoints de comentarios | **Reutilizados** — `GET/POST /api/v1/comments/{lote}` sin cambios |
| Endpoints de consulta lote / escaneos | **Sin cambios** — `GET /qr/{lote}`, `GET /scan/{lote}` |
| Lógica de negocio Estado Operativo | **No modificada** — mismas reglas v1.6.0 (`OperationalStatusResolver`) |
| Integración Dynamics | **No modificada** |
| Auditoría | **No modificada** |
| Seguridad / roles / permisos | **No modificados** (mismo `canUseComments`, mismos permisos de aprobación) |
| Estado Operativo desde Dynamics | **Sí** — `dynamic.status` sigue viniendo del resolver Dynamics; el banner no usa `qr_labels.status` |
| Información técnica | Colapsada por defecto (`<details>` sin `open`) |

---

## 4. Archivos modificados

| Archivo | Cambio |
|---------|--------|
| `web/.../pages/BatchLookupPage.tsx` | Layout centro de información; sin tabs; compositor de comentarios |
| `web/.../utils/displayLabels.ts` | Títulos/labels del centro |
| `web/.../components/ui/StatusTag.tsx` | Texto “No determinado” |
| `web/.../components/layout/BreadcrumbsBar.tsx` | Nav label `lookupNav` |
| `api/.../build.gradle` | Versión `1.6.1` |
| `OlnaturaQR/app/build.gradle.kts` | `1.6.1` / versionCode 11 |

**No se tocaron:** backend Dynamics, resolver operativo, auditoría, controllers de comentarios, BD, OAuth.

---

## 5. Comentarios (UX)

- Orden: **más reciente → más antiguo**  
- Tarjeta: fecha/hora, usuario, rol, comentario entre comillas  
- Botón **Agregar comentario** abre caja + **Guardar** / Cancelar  
- Sin permiso: solo lectura / mensaje de acceso  

---

## 6. Estado Operativo (derecha)

Muestra únicamente:

- 🟢 Aprobado / 🟡 Cuarentena / 🔴 Rechazado / ⚪ No determinado  
- **Origen:** Dynamics 365 Finance & Operations  

La **regla aplicada** vive solo dentro del acordeón técnico.

### Resumen operativo
Almacén · Ubicación · Inventario · Fecha de entrada · Caducidad · Fuente Dynamics  

### Información técnica Dynamics (colapsada)
BatchDispositionCode · PassedBatchDispositionCode · QualityOrderStatus · BatchDisposition (resumen) · Estado determinado mediante: {regla}

---

## 7. Capturas

Ubicación: `docs/capturas-v1.6.1/`

| Archivo | Vista |
|---------|-------|
| `captura-centro-info-lote-desktop.png` | Dos columnas |
| `captura-centro-info-lote-mobile.png` | Una columna (estado primero) |

> Mockups de la nueva interfaz; validar en runtime con Web desplegada.

---

## 8. Artefactos

| Artefacto | Ruta |
|-----------|------|
| Web build | `olnatura-qr/web/qr-enterprise-frontend/dist/` (`tsc` + `vite` OK) |
| JAR | `olnatura-qr/api/olnaturaqr/build/libs/olnaturaqr-1.6.1.jar` |
| APK | `OlnaturaQR/app/build/outputs/apk/release/` (versionName 1.6.1) |

---

## 9. Validación manual sugerida

1. Buscar un lote → no hay tabs ni hueco vacío grande.  
2. Comentarios cargan y se ordenan del más nuevo al más viejo.  
3. Agregar comentario (rol permitido) → POST existente → aparece arriba.  
4. Rol sin permiso → solo lectura.  
5. Estado Operativo coincide con Dynamics (v1.6.0).  
6. Acordeón técnico cerrado al entrar; al expandir muestra regla y campos técnicos.
