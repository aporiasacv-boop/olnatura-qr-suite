# Olnatura QR Suite – MVP

Monorepo para gestión de códigos QR de Olnatura: backend, web y app Android.

## Estructura

```
olnatura-qr-suite/
├── olnatura-qr/api/olnaturaqr    # Backend Spring Boot + Postgres + Flyway
├── olnatura-qr/web/qr-enterprise-frontend  # Vite + React + Fluent UI
├── OlnaturaQR/                   # Android (Kotlin + Jetpack Compose)
└── README.md
```

## Requisitos

- **Backend**: Java 21, Gradle, Postgres
- **Web**: Node 18+
- **Android**: Android Studio, SDK 35

## Cómo correr todo

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

- Puerto: **3001**
- Admin bootstrap (dev): `admin` / `Admin123!`
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

- **Emulador**: `BASE_URL` por defecto `http://192.168.41.172:3001/`
- **Dispositivo físico**: usa la IP de tu máquina:
  ```bash
  ./gradlew assembleDebug -PAPI_BASE_URL=http://192.168.41.172:3001/
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

- **Backend**: `application.yml`, `application-dev.yml`
- **Web**: `VITE_API_BASE_URL` en `.env`
- **Android**: `API_BASE_URL` en `build.gradle.kts` o `Constants.kt`
