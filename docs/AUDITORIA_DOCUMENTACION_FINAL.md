# Auditoría de documentación — Olnatura QR (post Fase 2)

**Fecha de la auditoría:** 6 de agosto de 2026
**Alcance revisado:** backend (Spring Boot/Java), frontend web (React/TS), app Android (OlnaturaQR), carpeta `docs/`, manuales en `Documentos/`, migraciones de base de datos, configuración, endpoints, roles y permisos, y control de versiones (git).
**Modificaciones realizadas:** ninguna. Este documento es exclusivamente diagnóstico.

---

## 0. Nota metodológica y limitación importante

Esta auditoría se realizó leyendo directamente el código fuente, las migraciones SQL, los archivos de configuración, el documento de arquitectura en Markdown y el historial de referencias de git. Para los manuales en `Documentos/` se encontró una limitación técnica del entorno de trabajo que **impide dar por completo el diagnóstico de tres archivos**:

- `Manual de procesos 11.05.2026.docx` — **no se pudo leer el contenido.**
- `Analista de procesos.docx` — **no se pudo leer el contenido.**
- `Medidas QR.xlsx` — **no se pudo leer el contenido.**

El entorno de esta sesión no tiene acceso a una terminal (shell) para convertir estos archivos binarios (.docx/.xlsx son en realidad archivos ZIP con XML interno), y la herramienta de lectura de archivos no soporta binarios de Office directamente. Sí fue posible extraer el 100% del contenido de `Manual de administrador 11.05.2026.pdf` (12 páginas, con soporte nativo de lectura de PDF), y se asume —sin poder confirmarlo— que `Manual de administrador 11.05.2026.docx` tiene el mismo contenido que su PDF homónimo.

**Recomendación para desbloquear el diagnóstico completo:** exportar `Manual de procesos 11.05.2026.docx` y `Analista de procesos.docx` a PDF, y `Medidas QR.xlsx` a CSV, desde una computadora con Word/Excel, y volver a correr esta auditoría sobre esos tres archivos. El resto de este informe es completo y no depende de ese paso.

---

## 1. Resumen ejecutivo

El proyecto Olnatura QR tiene tres componentes de software activos (backend Spring Boot, frontend web React, app Android "OlnaturaQR" v1.6.1) y una carpeta Android adicional (`QrScanner`) que resultó ser un **scaffold vacío sin código**, no un producto real. El sistema implementó durante la Fase 2 un conjunto grande de funcionalidad que hoy **no tiene ninguna documentación oficial**, o solo tiene documentación parcial y desalineada:

- La app móvil Android (login, escaneo, consulta de lote, comentarios, compartir, reportar problema, solicitud de acceso) **no tiene manual alguno**. Todos los manuales encontrados son exclusivamente de la plataforma web/administrador.
- El único documento de arquitectura (`estado-operativo-dynamics.md`) es correcto en lo que se pudo verificar contra el código, pero está incompleto: no menciona el flag experimental de estado `PARCIAL`, no enlaza las capturas de pantalla existentes, y no tiene ningún diagrama.
- El `Manual de administrador 11.05.2026.pdf` (única versión 100% legible) tiene información que **ya no corresponde al sistema actual**: lista un rol "Usuario de consulta" que no existe en el modelo de roles actual, omite el rol "Producción" que sí existe, afirma una regla de "solo 1 administrador a la vez" que no se encontró implementada en el código, y no documenta pantallas que ya existen en producción (Administración de BD, Alinear con Dynamics).
- Existen dos endpoints/pantallas con nombres de archivo y de ruta que dicen "temp" (`api/temp/...`, `pages/temp/...`) pero que en realidad son **funcionalidad central y de uso diario** (la pantalla "Consulta de lote"), lo cual es un riesgo de confusión y de borrado accidental, y debe resolverse (renombrar) antes de documentar formalmente.
- Existe un endpoint administrativo de **borrado destructivo total de datos operativos** (`/api/v1/temp/admin/purge-operational-data`, accesible desde "Administración de BD" en el frontend) protegido únicamente por una frase de confirmación fija en el código. No está documentado en ningún manual y representa un riesgo operativo que debe resolverse con el equipo de desarrollo antes de redactar cualquier documentación sobre él.
- No existe README, RELEASE NOTES/CHANGELOG ni presentación final en ningún punto del proyecto (fuera de `node_modules`).
- Los manuales existentes tienen fecha 11 de mayo de 2026 (confirmado en el propio documento); el sistema actual, a 6 de agosto de 2026, tiene funcionalidad añadida después de esa fecha que no está reflejada.

El resto de este documento detalla archivo por archivo el estado encontrado, qué falta documentar, y en qué orden conviene trabajar.

---

## 2. Inventario completo de documentos

| # | Documento | Ubicación | Tipo | Última fecha conocida | ¿Se pudo leer? |
|---|---|---|---|---|---|
| 1 | Manual de procesos 11.05.2026.docx | `Documentos/` | Manual de proceso | 11/05/2026 (nombre de archivo) | No (binario, sin shell disponible) |
| 2 | Manual de administrador 11.05.2026.docx | `Documentos/` | Manual de administrador | 11/05/2026 | No (se asume igual al PDF, sin confirmar) |
| 3 | Manual de administrador 11.05.2026.pdf | `Documentos/` | Manual de administrador | 11/05/2026 (confirmado en portada) | **Sí, completo (12 páginas)** |
| 4 | Analista de procesos.docx | `Documentos/` | Manual de rol/proceso | Sin fecha en el nombre | No (binario) |
| 5 | Medidas QR.xlsx | `Documentos/` | Hoja de datos (medidas de etiqueta) | Sin fecha en el nombre | No (binario) |
| 6 | `olnatura-qr/docs/estado-operativo-dynamics.md` | Carpeta docs del repo | Documento de arquitectura | Sin fecha explícita; consistente con V14–V16 | **Sí, completo** |
| 7 | `docs/capturas-v1.6.0/` (4 imágenes) | Carpeta docs del repo | Capturas de pantalla sin contexto | Implica versión 1.6.0 | Sí (solo imágenes, sin texto) |
| 8 | `docs/capturas-v1.6.1/` (2 imágenes) | Carpeta docs del repo | Capturas de pantalla sin contexto | Implica versión 1.6.1 | Sí (solo imágenes, sin texto) |
| 9 | `AUTHORS` | Raíz del repo | Atribución de autoría | Sin fecha | Sí |
| 10 | `NOTICE` | Raíz del repo | Aviso de propiedad/licencia | Sin fecha | Sí |
| — | README.md | — | — | — | **No existe en ningún punto del proyecto** |
| — | RELEASE_NOTES / CHANGELOG | — | — | — | **No existe en ningún punto del proyecto** |
| — | Presentación final (.pptx/.potx) | — | — | — | **No existe** (confirmado, ver sección 10) |
| — | "Manual de usuario" | — | — | — | **Referenciado dentro del Manual de administrador §1, pero no existe en `Documentos/`** |

Adicionalmente se inventariaron (no son "documentos" pero son insumo directo de esta auditoría): 110+ archivos Java del backend, 16 migraciones SQL (V1–V16) + 1 seed de demo, 3 archivos de configuración YAML, ~40 archivos TypeScript/React del frontend, ~35 archivos Kotlin de la app Android `OlnaturaQR`, y la carpeta `QrScanner` (scaffold Android vacío, ver sección 9).

---

## 3. Documentos vigentes (correctos, sin cambios necesarios)

| Documento | Justificación |
|---|---|
| `AUTHORS` | Contenido específico del proyecto ("Olnatura QR / QR empresarial", autor Angel Alexis Sanchez Calero), consistente con la atribución embebida en el código (`core/integrity/DragonSeal.java`, ver hallazgo en sección 11). No requiere cambios. |
| `NOTICE` | Aviso de propiedad/reserva de derechos vigente y consistente con la naturaleza propietaria del software. Único punto menor: los "Provenance markers" (`AASC, 161101, 270625, JUSC, SCF, LOC, HASU`) no tienen leyenda explicativa en ningún lugar del proyecto — no es un error, es una mejora opcional de bajo impacto (ver sección 11). |

No se encontraron manuales, guías de proceso ni documentos de arquitectura que puedan calificarse como 100% vigentes sin ningún cambio — todos los que se pudieron leer tienen al menos una brecha frente al sistema actual.

---

## 4. Documentos parcialmente desactualizados

### 4.1 `olnatura-qr/docs/estado-operativo-dynamics.md`

- **Secciones que ya no corresponden del todo:** ninguna sección es incorrecta, pero el documento **no está completo**.
- **Por qué:** al comparar contra `OperationalStatusResolver` en el backend, el documento no menciona que existe un flag experimental (`ENABLE_PARTIAL_STATE_EXPERIMENT`, actualmente `true`) que introduce el estado `PARCIAL` con una advertencia explícita en el propio código de que puede producir falsos positivos porque Dynamics OData no expone cantidades por almacén. El documento sí menciona `PARCIAL` como uno de los estados posibles, pero no advierte que es experimental/no definitivo.
- **Qué habría que actualizar:** agregar una nota explícita sobre el carácter experimental del estado `PARCIAL`; enlazar o incorporar las capturas de `docs/capturas-v1.6.0/` (que documentan visualmente el estado operativo aprobado/rechazado, el mismo tema de este documento) y `docs/capturas-v1.6.1/` (centro de información de lote); agregar al menos un diagrama de secuencia del flujo Dynamics ↔ Olnatura QR, ya que hoy el documento es solo prosa + 2 tablas + 1 fragmento YAML.

### 4.2 `Manual de administrador 11.05.2026.pdf` (y, presumiblemente, su .docx)

| Sección del manual | Qué ya no corresponde | Por qué | Qué actualizar |
|---|---|---|---|
| §4 Aprobar usuarios / §5 Usuarios | Lista de roles "Inspección, Almacén, Usuario de consulta y Control de calidad" | El modelo de roles actual (`Role.java`, migraciones V1/V14) es **ADMIN, ALMACEN, PRODUCCION, CALIDAD, INSPECCION**. El rol "Usuario de consulta" (visto también como `USUARIO_CONSULTA` en una captura de pantalla del propio manual) no existe en el código actual; el rol "Producción" (agregado en la migración V14) no aparece mencionado en el manual. | Reescribir la lista de roles disponibles para solicitud de acceso y gestión de usuarios, alineada a los 5 roles reales; confirmar con el equipo si "Usuario de consulta" fue renombrado a "Producción" o si es un rol descontinuado. |
| §2 Objetivo | Afirma "Pudiendo existir solo 1 usuario con privilegio administrador al mismo tiempo" | No se encontró ninguna restricción en el código (`AdminUsersController`, migraciones) que limite el número de usuarios con rol ADMIN; un administrador puede promover a varios usuarios a ADMIN sin bloqueo. | Verificar con el equipo de desarrollo si esta regla debe implementarse en el sistema, o corregir el texto del manual si nunca fue una regla real. |
| §3 Métricas operativas (navegación) | El menú mostrado en las capturas no incluye "Administración de BD" | Actualmente el frontend tiene una pantalla adicional `/admin/db` ("Administración de BD") con una herramienta de purga de datos operativos, que no existía o no se capturó al momento del manual. | Agregar la sección "Administración de BD" con capturas actuales, y documentar con advertencias explícitas su naturaleza destructiva (ver hallazgo de riesgo en sección 11). |
| §6 Lotes | No menciona la acción "Alinear con Dynamics" | El frontend actual (`AdminLotsPage`) tiene una acción global de realineación masiva contra Dynamics no descrita en el manual. | Agregar la descripción de "Alinear con Dynamics" (qué campos actualiza, qué preserva) a la sección de Lotes. |
| §7 Historial de auditorías | Formato de fecha inconsistente dentro del propio documento (placeholder `dd/mm/aaaa` vs. texto de ayuda "yyyy-MM-dd") | Posible desactualización de capturas de pantalla frente al comportamiento actual del filtro de fechas. | Volver a capturar la pantalla de filtros de auditoría y confirmar el formato de fecha real que usa el sistema hoy. |
| §1 Introducción | Referencia un "Manual de usuario Sistema de trazabilidad mediante códigos QR" como documento complementario | Ese documento no existe en `Documentos/`. | Localizar el manual de usuario si existe en otro repositorio/carpeta, o autorizar su redacción desde cero (ver sección 6). |

### 4.3 Manual de procesos 11.05.2026.docx (estado inferido, no confirmado — ver limitación en sección 0)

No se pudo leer el contenido, por lo que no se puede clasificar con precisión como "parcial" u "obsoleto" sin inventar información. Sin embargo, por inferencia razonable a partir de la navegación mostrada en el Manual de administrador (que incluye "Consulta por lote", "Consultar etiqueta", "Registrar etiqueta" como pantallas operativas que probablemente documenta el Manual de procesos) y por el hecho de que el sistema actual agregó después del 11 de mayo de 2026 funcionalidad como comentarios de lote, sincronización manual con Dynamics, reimpresión por rango de envases y corrección administrativa de datos, es altamente probable que este manual **requiera actualización sustancial** una vez legible. Se marca como **estado indeterminado, pendiente de extracción**, no como "vigente" ni "obsoleto" para evitar conclusiones sin evidencia directa.

### 4.4 Analista de procesos.docx (estado indeterminado)

Mismo caso que el anterior: no se pudo leer. El nombre sugiere que documenta un rol/perfil "Analista de procesos", que no coincide con ninguno de los 5 roles reales del sistema (`ADMIN, ALMACEN, PRODUCCION, CALIDAD, INSPECCION`). Debe verificarse si "Analista de procesos" es un nombre alterno de alguno de estos roles o un perfil operativo que ya no aplica.

---

## 5. Documentos completamente obsoletos

No se identificó ningún documento (manual, arquitectura, README) que deba calificarse como **completamente** obsoleto — es decir, ninguno está tan desalineado que deba eliminarse o archivarse en su totalidad; todos los documentos legibles conservan partes válidas. Los casos más cercanos a "obsoleto" son artefactos de código/configuración, no documentos, y se listan aparte por transparencia ya que el alcance de esta auditoría explícitamente incluye scripts, migraciones y Android:

| Artefacto | Motivo | ¿Eliminar o archivar? |
|---|---|---|
| Carpeta `QrScanner/` (proyecto Android) | Scaffold vacío: no contiene ningún archivo `.kt`/`.java`, solo iconos por defecto y el wrapper de Gradle. No es una segunda app real. | **Eliminar** del repositorio; no debe documentarse como componente del producto. |
| `db/demo/R__seed_demo_data.sql` | Usa vocabulario de estado (`DESCONOCIDO`, `LIBERADO`, `PENDING`) que la migración V14 reemplazó por `CUARENTENA/APROBADO/RECHAZADO`. Solo afecta entornos de desarrollo/demo. | Actualizar el vocabulario o archivar como referencia histórica; no afecta producción. |

---

## 6. Documentación faltante

Funcionalidad implementada en el sistema actual que **no tiene documentación oficial en ningún manual**:

- **App móvil OlnaturaQR completa** (v1.6.1): login con guardado de credenciales, escaneo QR, pantalla de resultado/consulta de lote, comentarios desde el teléfono, compartir resultado como imagen (WhatsApp/Gmail/galería), reportar problema, solicitud de acceso. No existe ningún manual de usuario para la app móvil.
- **Consulta de lote unificada** (pantalla web "Consulta de lote", implementada en `pages/temp/OperationalStatusValidationTempPage.tsx`): combina en una sola vista el estado operativo calculado desde Dynamics, el registro de la etiqueta, el historial de escaneos, los comentarios y el detalle técnico de validación. No está documentada con su nombre y comportamiento actuales.
- **Nuevo flujo de comentarios de lote** (bitácora por lote, máximo 200 caracteres, inmutable, disponible en web y móvil, roles ADMIN/ALMACEN/CALIDAD/INSPECCION — nótese que PRODUCCIÓN no puede comentar). Sin documentación.
- **Nueva arquitectura de estados**: separación entre `admin_status` (ciclo de vida administrativo: ACTIVE/INACTIVE/BAJA) y `status` (estado operativo/workflow: CUARENTENA/APROBADO/RECHAZADO/PARCIAL), y el flujo de doble aprobación (Calidad + Inspección) para material tipo Empaque Primario. Documentado parcialmente solo en `estado-operativo-dynamics.md`, ausente de los manuales de usuario/administrador.
- **Dynamics como fuente de verdad y sincronización** (automática al consultar, manual vía botón, masiva vía "Alinear con Dynamics", y al iniciar el servidor). Documentado técnicamente en `estado-operativo-dynamics.md`, pero sin equivalente en lenguaje de usuario final dentro de los manuales.
- **Roles y permisos actuales** (5 roles reales y su matriz de permisos exacta). El manual de administrador tiene una lista de roles desactualizada (ver sección 4.2).
- **Vista previa de etiqueta** (mockup de la etiqueta física con QR, usado al registrar, generar/reimprimir y en la consulta de lote) — sin documentación visual actualizada.
- **Historial de escaneos** (pantalla dedicada, búsqueda por lote) — mencionada solo como nombre de menú en el manual de administrador, sin descripción de su funcionamiento.
- **Reimpresión por rango de envases** (seleccionar "desde/hasta" sin exceder el total registrado) — sin documentación.
- **Corrección administrativa de datos y de estado** (edición de campos de etiqueta y sobre-escritura manual de estado de flujo, ambas con motivo obligatorio y auditoría) — sin documentación en ningún manual.
- **Administración de BD / purga de datos operativos** — funcionalidad existente y accesible, sin documentación y con riesgo operativo relevante (ver sección 11).
- **Nuevos endpoints internos**: `/api/v1/dynamics/lookup/{lote}`, `/api/v1/diagnostics/liberacion/{lote}`, `/api/v1/admin/lots/align-from-dynamics`, `/api/v1/comments/{lote}`, `/api/v1/admin/metrics/export/powerbi`, y el paquete completo `/api/v1/temp/*` — ninguno documentado.
- **"Manual de usuario"** — referenciado por el propio Manual de administrador como documento complementario, pero no existe en `Documentos/`. Debe localizarse o autorizarse su redacción.
- **Presentación final** — no existe (ver sección 10); no se generó en esta auditoría por instrucción explícita.

---

## 7. Manual de usuario

**No se localizó ningún archivo que sea, por nombre o contenido, un "Manual de usuario" independiente.** El único candidato es el Manual de administrador, que en su introducción aclara que los perfiles operativos (no administradores) se describen en un manual de usuario separado que no está presente en `Documentos/`.

Dado que no existe el documento, no es posible listar con precisión qué pantallas fueron eliminadas o renombradas dentro de él. Lo que sí se puede afirmar, comparando el árbol de navegación visible en las capturas del Manual de administrador contra el código actual del frontend:

- El ítem de menú "Consulta por lote" (visto en las capturas del manual) hoy se llama **"Consulta de lote"** en el frontend actual — cambio de nombre menor pero real.
- El manual muestra "Consultar etiqueta" y "Registrar etiqueta" como dos ítems de menú separados bajo "Etiquetas"; el frontend actual tiene "Generar etiqueta" (que en realidad funciona como pantalla de **reimpresión** de etiquetas existentes, un nombre potencialmente confuso) y "Registrar etiqueta" (alta de una etiqueta nueva). Esto debe aclararse en cualquier manual de usuario nuevo, ya que "Generar etiqueta" no genera una etiqueta desde cero.
- Botones/flujos nuevos que un manual de usuario debería cubrir y que no existían documentados: "Sincronizar con Dynamics", comentarios de lote, vista previa de etiqueta con descarga de historial en PDF, reimpresión por rango de envases.
- Permisos nuevos: rol PRODUCCIÓN (puede generar/registrar etiquetas y escanear, pero no puede comentar ni aprobar/rechazar material).
- No hay imágenes que "deban reemplazarse" porque no hay manual de usuario vigente que contenga imágenes — se trata de una creación desde cero, no de una actualización.

---

## 8. Manual de procesos

No se pudo verificar el contenido actual del archivo `Manual de procesos 11.05.2026.docx` (ver limitación en sección 0). Como referencia para cuando se pueda leer, estos son los puntos del sistema actual contra los que deberá compararse cada proceso:

| Proceso | Comportamiento actual confirmado en código |
|---|---|
| Generación de etiquetas | "Registrar etiqueta" crea una etiqueta nueva (estado inicial CUARENTENA, admin_status ACTIVE), con autocompletado desde Dynamics y generación de token/QR únicos. |
| Consulta de lote | Pantalla unificada que combina estado operativo (Dynamics), datos de la etiqueta local, escaneos y comentarios en una sola vista, con botón de sincronización manual. |
| Sincronización | Ocurre automáticamente al consultar un lote, manualmente vía botón, de forma masiva vía "Alinear con Dynamics", y al iniciar el servidor (backfill configurable). Dynamics siempre prevalece salvo cuando no aporta un estado definitivo. |
| Reimpresión | Pantalla "Generar etiqueta" permite reimprimir por rango de envases (desde/hasta) sin exceder el total original registrado. |
| Corrección de información | Exclusiva de ADMIN, requiere motivo obligatorio, queda auditada con el detalle de cada campo modificado (antes/después). |
| Comentarios | Bitácora por lote, inmutable, máximo 200 caracteres, disponible para ADMIN/ALMACEN/CALIDAD/INSPECCION (no PRODUCCIÓN), en web y en la app móvil. |
| Auditoría | Registro central de toda acción relevante (aprobar/rechazar, corregir, sincronizar, imprimir, exportar, gestionar usuarios), consultable con filtros y exportable a CSV/PDF. |
| Roles | ADMIN, ALMACEN, PRODUCCION, CALIDAD, INSPECCION, con matriz de permisos específica por tipo de material (Materia Prima → solo Calidad; Empaque Secundario → solo Inspección; Empaque Primario → requiere ambos). |
| Dynamics | Única fuente de verdad del estado operativo; el estado administrativo del lote (activo/inactivo/baja) nunca es tocado por Dynamics. |

Se recomienda, una vez desbloqueada la lectura del .docx, repetir esta auditoría específicamente sobre este archivo usando la tabla anterior como checklist de comparación.

---

## 9. Arquitectura

El único documento formal de arquitectura, `estado-operativo-dynamics.md`, documenta correctamente y de forma verificable contra el código actual:

- Dynamics 365 F&O como única fuente de verdad del **Estado Operativo** (`qr_labels.status`), separado explícitamente del **estado administrativo** (`admin_status`), que nunca es modificado por Dynamics — **confirmado en el código** (`LotOperationalGate`, `AdminLotStatus`, `OperationalStatusSyncService`).
- Los 5 puntos donde se refresca la copia local (consulta, sync manual, alineación masiva, generación de etiqueta, backfill al iniciar) — **confirmados uno a uno** contra los controladores/servicios correspondientes.
- La cláusula de seguridad de que un estado `DESCONOCIDO`/sin datos de Dynamics no sobreescribe la copia local — consistente con el diseño de `OperationalStatusSyncService`.
- El flag de configuración `app.dynamics.sync-operational-status-on-startup` — **el nombre y el valor por defecto (`true`) coinciden exactamente** entre el documento y `application.yml`.

Decisiones de arquitectura ya consolidadas en el código que **no están reflejadas en ningún documento**:

- Separación de comentarios de lote (`lote_comments`) de la tabla `qr_labels` — la migración V16 eliminó explícitamente la llave foránea para permitir comentar lotes que aún no existen localmente (solo en Dynamics). No documentado.
- Consulta de lote unificada — implementada como un único servicio (`QrQueryService` + `DynamicsLookupService`); no se encontró ninguna clase o referencia a un concepto anterior de "BatchLookup" en el código actual (búsqueda exhaustiva sin resultados), lo que sugiere que si existió, ya fue completamente reemplazado. No hay documento que narre esta evolución.
- Flujo de doble aprobación (Calidad + Inspección) para material Empaque Primario, y aprobación de una sola vía para Materia Prima (Calidad) y Empaque Secundario (Inspección) — no documentado en ningún lugar fuera del propio código.
- El flag experimental de estado `PARCIAL` (`ENABLE_PARTIAL_STATE_EXPERIMENT`) — no documentado como experimental en `estado-operativo-dynamics.md` (ver sección 4.1).
- Existencia de un mecanismo de arranque que verifica la integridad de un conjunto de constantes internas y detiene el arranque de la aplicación si se alteran (paquete `core/integrity`) — no documentado en ningún lugar; ver hallazgo de riesgo en sección 11.

---

## 10. Presentación final

**No existe ninguna presentación** (.pptx/.potx) en ningún punto del proyecto. Se realizó una búsqueda exhaustiva desde la raíz de la carpeta seleccionada y no se encontró ningún archivo con esas extensiones. Conforme a la instrucción explícita de esta tarea, **no se generó ninguna presentación** en esta auditoría; solo se deja constancia de su ausencia para la planeación de la entrega final.

---

## 11. Hallazgos técnicos relevantes para la documentación (no son documentos, pero condicionan qué y cómo documentar)

Estos hallazgos no son "documentos" en sí, pero deben resolverse o al menos discutirse con el equipo de desarrollo antes de redactar los manuales definitivos, porque afectan directamente qué se puede documentar con seguridad:

1. **Nomenclatura "temp" en funcionalidad central.** La pantalla web más usada por todos los roles ("Consulta de lote") vive en el código como `pages/temp/OperationalStatusValidationTempPage.tsx` y llama a un endpoint `/api/v1/temp/operational-status-validation/{lote}`. Recomendación: pedir al equipo de desarrollo que renombre estos artefactos a nombres definitivos antes de documentarlos, para no tener que reescribir el manual cuando eso ocurra.
2. **Endpoint de borrado total de datos operativos.** `POST /api/v1/temp/admin/purge-operational-data` (expuesto en el frontend como "Administración de BD") borra permanentemente comentarios, escaneos, auditoría (con lote) y etiquetas, protegido solo por una frase de confirmación fija en el código (no un secreto real). Es solo para ADMIN, pero está activo en producción. Recomendación: decidir con desarrollo si se retira antes del lanzamiento final o si se documenta como herramienta de administración con advertencias explícitas y un procedimiento de doble confirmación reforzado.
3. **Mecanismo de bloqueo de arranque por integridad (`core/integrity`).** El sistema deja de arrancar si se altera un conjunto de constantes internas ligadas a la atribución de autoría del desarrollador. No está documentado en ningún lugar del proyecto. Recomendación: que el equipo de desarrollo decida si esto debe documentarse como una advertencia interna (para evitar que alguien "limpie código muerto" y tumbe el sistema por accidente) o se retire.
4. **Funcionalidad no conectada en la app Android.** `AdminLotRepository.kt` (corrección de etiquetas/estado desde el celular) existe en el código pero no está conectado a ninguna pantalla — no debe documentarse como funcionalidad disponible en el manual de usuario móvil porque hoy no es accesible.
5. **Flujo "Reportar problema" no funcional.** En la app Android, el botón "Enviar" del flujo de reporte de problemas no envía nada al servidor (no existe endpoint ni llamada de red); solo cierra la pantalla. No debe documentarse como una funcionalidad que efectivamente reporta algo hasta que se implemente en el backend.
6. **Manual de administrador: regla de "un solo administrador"** — no verificada en el código (ver sección 4.2); requiere decisión de negocio antes de documentar.
7. **Doble control remoto de git** (`origin` en una organización de GitHub, `empresa` en otra) sin que quede claro cuál es la fuente autoritativa — no es un tema de documentación de producto, pero puede afectar de dónde se genera la versión "oficial" del código para futuras auditorías.

---

## 12. Cambios requeridos (consolidado)

| Prioridad | Documento/artefacto | Cambio requerido |
|---|---|---|
| Alta | Manual de administrador (docx/pdf) | Corregir lista de roles, verificar/corregir regla de "1 solo admin", agregar "Administración de BD" con advertencias, agregar "Alinear con Dynamics", refrescar capturas de auditoría. |
| Alta | Administración de BD (código) | Decidir con desarrollo: ¿se documenta con advertencias o se retira antes de la entrega final? |
| Alta | Manual de usuario | Localizar si existe en otro lugar, o autorizar su redacción desde cero (web + móvil). |
| Alta | App móvil OlnaturaQR | Redactar manual de usuario completo; no existe ninguno actualmente. |
| Alta | Manual de procesos (docx) | Desbloquear lectura (exportar a PDF) y volver a auditar contra la tabla de la sección 8. |
| Media | Consulta de lote, comentarios, sincronización, reimpresión, corrección de datos | Documentar como funcionalidad nueva en el manual de procesos y/o de usuario. |
| Media | Nomenclatura "temp" | Pedir a desarrollo que renombre antes de documentar definitivamente. |
| Media | `estado-operativo-dynamics.md` | Agregar advertencia sobre el estado `PARCIAL` experimental, enlazar capturas v1.6.0/v1.6.1, agregar diagrama. |
| Media | Carpetas de capturas v1.6.0/v1.6.1 | Agregar un archivo de contexto (qué versión, qué feature, fecha) para cada carpeta. |
| Baja | README.md / RELEASE_NOTES / CHANGELOG | Crear desde cero a nivel de repositorio. |
| Baja | `NOTICE` | Agregar leyenda explicativa de los "Provenance markers". |
| Baja | `QrScanner/` (Android) | Eliminar del repositorio. |
| Baja | `AdminLotRepository.kt` / flujo "Reportar problema" (Android) | Conectar la funcionalidad o retirar del código antes de documentar. |
| Baja | Seed de demo SQL | Actualizar vocabulario de estado o archivar. |
| Pendiente (fuera de esta tarea) | Presentación final | Generar una vez autorizado explícitamente. |

---

## 13. Prioridad de actualización

**Alta:** Manual de administrador (correcciones de roles y funcionalidad faltante), decisión sobre el endpoint de purga, localización/redacción del Manual de usuario, documentación de la app móvil, desbloqueo y revisión del Manual de procesos.

**Media:** Documentación de funcionalidad nueva de Fase 2 (consulta de lote, comentarios, sincronización, reimpresión, corrección de datos), renombrado de artefactos "temp", ampliación del documento de arquitectura, contexto para las carpetas de capturas.

**Baja:** README/RELEASE NOTES/CHANGELOG a nivel de repositorio, leyenda de NOTICE, limpieza de código muerto/scaffold (QrScanner, AdminLotRepository, flujo de reporte), actualización del seed de demo.

**Pendiente por instrucción explícita:** presentación final (no generar todavía).

---

## 14. Recomendación del orden de trabajo

1. Exportar `Manual de procesos 11.05.2026.docx` y `Analista de procesos.docx` a PDF, y `Medidas QR.xlsx` a CSV, para completar el diagnóstico de esos tres archivos.
2. Reunión con el equipo de desarrollo para decidir el destino de: el endpoint de purga de datos, la nomenclatura "temp", la funcionalidad Android no conectada (`AdminLotRepository`, "Reportar problema"), y el mecanismo de bloqueo por integridad (`core/integrity`).
3. Actualizar el Manual de administrador con los hallazgos de la sección 4.2.
4. Redactar el Manual de procesos actualizado (una vez legible), cubriendo los procesos de la tabla de la sección 8.
5. Localizar o redactar el Manual de usuario, incluyendo por primera vez el flujo completo de la app móvil.
6. Ampliar `estado-operativo-dynamics.md` con el caveat del estado `PARCIAL`, un diagrama, y enlaces a las capturas existentes.
7. Crear README.md y RELEASE_NOTES/CHANGELOG a nivel de repositorio.
8. Generar la presentación final (solo cuando se autorice explícitamente esa tarea).
9. Limpieza técnica de bajo riesgo: eliminar `QrScanner/`, resolver o eliminar código Android sin conexión, actualizar el seed de demo, agregar leyenda a `NOTICE`.

---

## Anexo — Metodología de las cinco líneas de investigación

Esta auditoría se apoyó en cinco revisiones especializadas y de lectura exhaustiva del código y la documentación disponible: (1) backend Java/Spring (controladores, servicios, dominio, migraciones V1–V16, configuración), (2) frontend React/TypeScript (rutas, páginas, componentes, permisos), (3) app Android OlnaturaQR y verificación de la carpeta `QrScanner`, (4) documentos de arquitectura, capturas de pantalla, archivos de configuración y metadatos de git, y (5) los manuales en `Documentos/`. Ninguna de estas revisiones modificó ningún archivo del proyecto.
