# Olnatura QR Suite – MVP

Monorepo para gestión de códigos QR de Olnatura: backend, web y app Android.

## Estructura

```
olnatura-qr-suite/
├── olnatura-qr/api/olnaturaqr    # Backend Spring Boot + Postgres + Flyway
├── olnatura-qr/web/qr-enterprise-frontend  # Vite + React + Fluent UI
├── OlnaturaQR/                   # Android (Kotlin + Jetpack Compose)
├── scripts/                      # Helper scripts (demo-up, dev-api, dev-web)
└── README.md
```

## Requisitos

- **Demo (Docker)**: Docker + Docker Compose
- **Local dev**: Java 21, Gradle, Postgres, Node 18+
- **Android**: Android Studio, SDK 35

---

## Quickstart

### Demo (one-command run)

From the **repo root**:

**Windows (PowerShell):**

```powershell
.\scripts\demo-up.ps1
```

**Linux / macOS / Git Bash:**

```bash
./scripts/demo-up.sh
```

- **API:** http://localhost:3001
- **Demo users:** `admin` / `Admin123!` · `inp` / `Inp123!` · `alm` / `Alm123!` (ADMIN, INSPECCION, ALMACEN)

To use the web UI with the demo API, in another terminal run `.\scripts\dev-web.ps1` or `./scripts/dev-web.sh` (first time: `cd olnatura-qr/web/qr-enterprise-frontend && npm install && cp .env.example .env`).

To stop and remove containers and DB volume:

```powershell
.\scripts\demo-down.ps1
```

```bash
./scripts/demo-down.sh
```

### Dev (local backend + frontend)

1. **Postgres** on port 5432 (e.g. Docker Desktop or `docker run -d --name olnatura-pg -e POSTGRES_USER=olnatura -e POSTGRES_PASSWORD=olnatura123 -e POSTGRES_DB=olnatura_qr -p 5432:5432 postgres:16`).
2. **API** (from repo root):
   - Windows: `.\scripts\dev-api.ps1`
   - Bash: `./scripts/dev-api.sh`
   - Or: `cd olnatura-qr/api/olnaturaqr && ./gradlew bootRun` (Windows: `.\gradlew.bat bootRun`)
3. **Web** (first time: `cd olnatura-qr/web/qr-enterprise-frontend && npm install && cp .env.example .env`):
   - Windows: `.\scripts\dev-web.ps1`
   - Bash: `./scripts/dev-web.sh`

- **API:** http://localhost:3001 · **Web:** http://localhost:5173

---

## Troubleshooting

- **Ports in use (5432, 3001, 5173)**  
  Stop whatever is using the port, or change the port in config:
  - Postgres: 5432 (docker compose or `application.yml` datasource)
  - API: 3001 (`server.port` in `application.yml`)
  - Web: 5173 (Vite default in `qr-enterprise-frontend`)

- **Reset demo DB (clean volume)**  
  Tear down demo stack and remove the Postgres volume:

  ```powershell
  .\scripts\demo-down.ps1
  ```

  ```bash
  ./scripts/demo-down.sh
  ```

  (`-v` removes the named volume `olnatura_pgdata`; next `demo-up` will run Flyway from scratch.)

- **Secrets**  
  No real secrets are committed. `JWT_SECRET` in `docker-compose.demo.yml` is a dev-only value; override via env in production.

---

## Cómo correr todo (referencia)

### 1. Base de datos (Postgres)

```bash
# Con Docker
docker run -d --name olnatura-pg -e POSTGRES_USER=olnatura -e POSTGRES_PASSWORD=olnatura123 -e POSTGRES_DB=olnatura_qr -p 5432:5432 postgres:16
```

O usa tu Postgres local con usuario `olnatura`, contraseña `olnatura123` y base de datos `olnatura_qr`.

### 2. Backend

```bash
cd olnatura-qr/api/olnaturaqr
./gradlew bootRun --args='--spring.profiles.active=dev'
```

En Windows: `.\gradlew.bat bootRun`

- Puerto: **3001**
- Demo users (tras migración V6): `admin`/`Admin123!`, `inp`/`Inp123!`, `alm`/`Alm123!`
- Demo lote: `260112-MES003456`

### 3. Web

```bash
cd olnatura-qr/web/qr-enterprise-frontend
cp .env.example .env
npm install
npm run dev
```

- URL: http://localhost:5173
- API: `VITE_API_BASE_URL` en `.env` (default `http://localhost:3001`)

**Logo QR**: Coloca `logo-olnatura.png` en `public/` para el overlay en el QR. Si no existe, el QR se genera sin logo.

### 4. Android

```bash
cd OlnaturaQR
./gradlew assembleDebug
```

- **Emulador**: `BASE_URL` por defecto `http://192.168.41.177:3001/`
- **Dispositivo físico**: usa la IP de tu máquina:
  ```bash
  ./gradlew assembleDebug -PAPI_BASE_URL=http://192.168.41.177:3001/
  ```

O edita `app/build.gradle.kts` y cambia el valor por defecto de `API_BASE_URL`.

## Endpoints

| Ruta                      | Auth | Descripción                      |
| ------------------------- | ---- | -------------------------------- |
| `GET /qr/{lote}`          | No   | Landing público (HTML)           |
| `GET /api/v1/qr/{lote}`   | Sí   | Datos del lote + estado dinámico |
| `POST /api/v1/auth/login` | No   | Login (cookie HttpOnly)          |
| `GET /api/v1/auth/me`     | Sí   | Usuario actual                   |

Sin sesión, `GET /api/v1/qr/{lote}` responde `401` con body `{ "message": "No perteneces a Olnatura", "code": "UNAUTHORIZED" }`.

## Flujo MVP

1. **Web**: Generar QR → lote puro con logo Olnatura (nivel H).
2. **APK**: Escanear QR → extrae lote (JSON, URL o texto) → si hay sesión, muestra datos y estado dinámico.
3. **Sin sesión**: Mensaje "Pide autorización para ver el contenido" y botón a login.
4. **Cámara genérica**: Abrir URL del QR muestra landing "Este código pertenece al sistema interno de Olnatura".

## Configuración

- **Backend**: `application.yml`, `application-dev.yml`, `application-docker.yml` (perfil Docker)
- **Web**: `VITE_API_BASE_URL` en `.env` (copiar desde `olnatura-qr/web/qr-enterprise-frontend/.env.example`; `.env` está en `.gitignore`, sin secretos en el ejemplo)
- **Android**: `API_BASE_URL` en `build.gradle.kts` o `Constants.kt`
