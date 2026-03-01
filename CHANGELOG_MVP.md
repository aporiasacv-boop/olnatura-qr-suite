# Cambios MVP Demo – Resumen

## 1. Archivos modificados/creados

### Backend (`olnatura-qr/api/olnaturaqr/`)
- **Nuevos:** `support/qr/LoteExtractor.java`, `domain/audit/AuditEvent.java`, `repository/AuditEventRepository.java`, `support/audit/AuditService.java`, `api/AuditController.java`, `api/AdminController.java`
- **Modificados:** `support/qr/QrQueryService.java`, `infra/dynamics/MockDynamicsClient.java`, `api/AuthController.java`, `api/LabelController.java`, `OlnaturaQrApplication.java` (+@EnableMethodSecurity), `support/security/SecurityConfig.java`
- **Migraciones:** `V4__add_mock_lote_251201.sql`, `V5__audit_events.sql`

### Web (`olnatura-qr/web/qr-enterprise-frontend/`)
- **Nuevos:** `utils/labelToPng.ts`, `pages/AdminAuditPage.tsx`
- **Modificados:** `pages/GenerateQrPage.tsx`, `pages/BatchLookupPage.tsx`, `app/router.tsx`, `components/layout/Sidebar.tsx`

### Android (`OlnaturaQR/`)
- **Nuevos:** `ui/screen/requestaccess/RequestAccessScreen.kt`, `ui/screen/requestaccess/RequestAccessViewModel.kt`
- **Modificados:** `data/network/OlnaturaApi.kt`, `data/model/Models.kt`, `data/repo/AuthRepository.kt`, `ui/navigation/Routes.kt`, `ui/navigation/AppNavGraph.kt`, `ui/screen/login/LoginScreen.kt`, `MainActivity.kt`

---

## 2. Resumen ejecutivo

- **Backend:** LoteExtractor extrae lote de JSON/URL/texto; mock 251201-MEM0003454; endpoints público (401 JSON) y privado; AdminController para aprobar/rechazar; AuditService + tabla audit_events; INSPECCION puede cambiar estatus por lote.
- **Web:** Etiqueta imprimible con campos estáticos + QR con logo; descarga PNG e impresión; audit GENERATE_LABEL/DOWNLOAD_LABEL; BatchLookup con cambio de estatus para INSPECCION; AdminAuditPage; permisos por rol.
- **Android:** Solicitud de acceso con RequestAccessScreen; LoteExtractor ya existente.

---

## 3. Checklist "no rompí nada"

- [ ] Backend arranca: `./gradlew bootRun --args='--spring.profiles.active=dev'`
- [ ] Login funciona: admin / Admin123!
- [ ] Endpoints públicos/privados: `curl http://localhost:3001/api/v1/qr/251201-MEM0003454` sin cookie → 401 + `{"message":"No perteneces a Olnatura","code":"UNAUTHORIZED"}`
- [ ] Web genera etiqueta: login → Etiquetas → lote 251201-MEM0003454 → Generar → Descargar PNG
- [ ] APK escanea y muestra dinámicos si autorizado; si no, "Pide autorización…"

---

## 4. Pasos de prueba (copy/paste)

### Backend
```bash
cd olnatura-qr/api/olnaturaqr
./gradlew bootRun --args="--spring.profiles.active=dev"
```

### Web
```bash
cd olnatura-qr/web/qr-enterprise-frontend
npm install
npm run dev
```

### APK (dispositivo físico)
```bash
cd OlnaturaQR
./gradlew assembleDebug -PAPI_BASE_URL=http://192.168.100.10:3001/
# Instalar el APK desde app/build/outputs/apk/debug/
```

Credenciales demo: admin / Admin123!
