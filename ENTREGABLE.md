##################################################
     **ENTREGA TÉCNICA · OLNATURA QR SUITE**
##################################################
Estado del proyecto

Versión estable entregada y congelada mediante tag:

#### v1.0 ####

Repositorio respaldado con commits firmados y documentación operativa.

# **Arquitectura general**

El sistema se compone de:
-Backend Java Spring Boot
-Frontend React + TypeScript
-Base de datos PostgreSQL
-Despliegue en Azure App Service
-Persistencia administrada mediante variables de entorno

# **Variables de entorno requeridas**

Backend
Seguridad y sesión

JWT_SECRET=
APP_CORS_ALLOWED_ORIGINS=

# **Base de datos PostgreSQL**

SPRING_DATASOURCE_URL=
SPRING_DATASOURCE_USERNAME=
SPRING_DATASOURCE_PASSWORD=

# **Dynamics 365 (perfil prod + mode=real)**

APP_DYNAMICS_MODE=real
APP_DYNAMICS_BASE_URL=
APP_DYNAMICS_RESOURCE=
APP_DYNAMICS_TENANT_ID=
APP_DYNAMICS_CLIENT_ID=
APP_DYNAMICS_CLIENT_SECRET=

Perfil y JPA:
-SPRING_PROFILES_ACTIVE=prod
-ddl-auto = validate (nunca update; el esquema lo controla Flyway)

# **Spring Boot / build (Azure App Service Java SE 17)**

- Versión GA: Spring Boot **3.5.16** (Maven Central; sin SNAPSHOT).
- Misma línea 3.5.x que el SNAPSHOT previo → drop-in compatible con Java 17 en Azure.
- No se migró a Spring Boot 4.x (cambio mayor / breaking).
- Artifact: `1.0.0` (ver `build.gradle`).

# **Flyway / datos demo**

- Producción: solo `classpath:db/migration` (sin usuarios ni lotes demo).
- Desarrollo: `classpath:db/migration,classpath:db/demo` (seeds en `R__seed_demo_data.sql`).
- V3/V4 quedaron como placeholder; V6 solo `CREATE EXTENSION pgcrypto`.
- V11 elimina filas demo legacy (`@demo.local` y lotes DEMO/TEST conocidos) sin alterar tablas.

**Importante tras el primer deploy con estos cambios** (BD que ya corrió V3/V4/V6 antiguos):
Flyway validará checksums distintos. Ejecutar **una vez** repair antes o tras el arranque fallido:

```sql
-- Opción A (recomendada): desde un cliente SQL actualiza checksums
-- tras flyway repair CLI, o:
```

```bash
# Opción B: CLI Flyway repair apuntando a la misma BD de Azure
flyway -url="$SPRING_DATASOURCE_URL" -user=... -password=... repair
```

Tras `repair`, el arranque aplica V11 (purge) y deja `ddl-auto=validate` OK.

Azure / Monitoring:
-APPLICATIONINSIGHTS_CONNECTION_STRING=
-ApplicationInsightsAgent_EXTENSION_VERSION=
-XDT_MicrosoftApplicationInsights_Mode=

# **Dependencias requeridas en PostgreSQL**

La base de datos requiere las siguientes extensiones:

# **UUID: CREATE EXTENSION IF NOT EXISTS "uuid-ossp";**

Uso:
-Generación de identificadores únicos
-Trazabilidad segura
-Eventos y referencias internas

# **pgcrypto: CREATE EXTENSION IF NOT EXISTS pgcrypto;**

Uso:
-Funciones criptográficas
-Generación segura de tokens
-Hashing y operaciones auxiliares

# **Despliegue rápido en Azure**

-Requisitos
-Azure App Service
-PostgreSQL Flexible Server o equivalente
-Variables de entorno configuradas
-Java 17+
-Node.js 18+

# **Flujo recomendado de despliegue**

# **Backend**

1. Compilar:
./gradlew build

2. Generar archivo:
build/libs/*.jar

3. Subir a Azure App Service


# **Frontend**

1. Instalar dependencias:
npm install

2. Compilar:
npm run build

3. Publicar carpeta:
dist/

# **Configuración recomendada Azure**

Variables críticas

Configurar desde:

Azure Portal
-App Service
-Variables de entorno

Variables mínimas:

-JWT_SECRET
-SPRING_DATASOURCE_URL
-SPRING_DATASOURCE_USERNAME
-SPRING_DATASOURCE_PASSWORD
-APP_CORS_ALLOWED_ORIGINS

# **Seguridad implementada**

El proyecto incluye:

-Commits firmados
-Versionado congelado
-Trazabilidad administrativa
-Auditoría de eventos
-Control de usuarios
-Historial de escaneos
-Roles operativos
-Control de sesiones


# **Módulos disponibles**

-Métricas operativas
-Gestión de usuarios
-Aprobación de usuarios
-Gestión de lotes
-Historial de auditoría
-Exportación CSV
-Consulta de eventos

# **Respaldo entregado**

Se recomienda conservar:

-Repositorio principal
-Repositorio congelado
-Tag v1.0
-Manual de usuario
-Manual administrador


# **Notas finales**

La versión entregada corresponde a una compilación estable y funcional utilizada para operación interna y pruebas de flujo completo.

Se recomienda mantener:
-Versionado semántico
-Commits firmados
-Respaldos periódicos
-Documentación actualizada

Entrega técnica realizada por: Angel Alexis Sánchez Calero
Fecha: 11/05/2026