# VIERNES V1 — Guía de Implementación y Activación de Keycloak

Este documento describe la arquitectura, archivos preparados, pasos de despliegue y estrategia de activación para migrar la autenticación de **Viernes V1** a **Keycloak (OAuth2 / OpenID Connect)** de forma segura y sin romper la funcionalidad actual.

---

## 1. Arquitectura de Autenticación con Keycloak

```
                +---------------------------------------+
                |           Keycloak 25.0               |
                |       http://localhost:8180           |
                |        Realm: viernes-realm           |
                +-------------------+-------------------+
                                    ^
       1. Login OIDC + PKCE         |  4. Valida firma JWKS
       2. Retorna Access Token      |     (/certs)
                                    |
+----------------------+     3. Bearer Token      +-----------------------+
|   Viernes Frontend   | -----------------------> |    Viernes Backend    |
|   Angular (4200)     |                          |   Spring Boot (8080)  |
| Client:              |                          | Resource Server       |
| viernes-frontend     |                          | Profile: keycloak     |
+----------------------+                          +-----------------------+
```

### Puertos de la Solución
* **Keycloak:** `http://localhost:8180` *(se usa el puerto 8180 para evitar colisiones con Spring Boot en 8080)*.
* **Spring Boot Backend:** `http://localhost:8080`
* **Angular Frontend:** `http://localhost:4200`
* **Oracle DB:** `localhost:1521`
* **PostgreSQL (Keycloak DB):** `localhost:5433`

---

## 2. Inventario de Archivos Preparados

Todos los archivos necesarios ya han sido creados en el repositorio y están listos para usarse:

| Componente | Archivo | Propósito |
| :--- | :--- | :--- |
| **Docker Compose** | [compose-keycloak.yml](file:///e:/LPII/Viernes/compose-keycloak.yml) | Levanta Keycloak 25 y PostgreSQL en Docker con un solo comando. |
| **Configuración Realm** | [viernes-realm.json](file:///e:/LPII/Viernes/docker/keycloak/viernes-realm.json) | Export completo del Realm con roles (`ADMIN`, `JEFE`, `VENDEDOR`), clientes OIDC y usuarios de prueba. |
| **Backend Config** | [application-keycloak.yml](file:///e:/LPII/Viernes/viernes-backend/src/main/resources/application-keycloak.yml) | Propiedades de Spring Security OAuth2 Resource Server (emisor, JWKS, etc.). |
| **Backend Security** | [KeycloakSecurityConfig.java](file:///e:/LPII/Viernes/viernes-backend/src/main/java/com/viernes/auth/keycloak/KeycloakSecurityConfig.java) | Cadena de filtros OAuth2 Resource Server con `@Profile("keycloak")`. |
| **Backend Roles** | [KeycloakJwtAuthenticationConverter.java](file:///e:/LPII/Viernes/viernes-backend/src/main/java/com/viernes/auth/keycloak/KeycloakJwtAuthenticationConverter.java) | Mapea roles de Keycloak (`realm_access.roles`) a `ROLE_ADMIN`, `ROLE_JEFE`, etc. |
| **Frontend Config** | [keycloak.config.ts](file:///e:/LPII/Viernes/viernes-frontend/src/app/core/auth/keycloak/keycloak.config.ts) | Parámetros OIDC para Angular (URL, realm, clientId, PKCE). |
| **Frontend Service** | [keycloak-auth.service.ts](file:///e:/LPII/Viernes/viernes-frontend/src/app/core/auth/keycloak/keycloak-auth.service.ts) | Servicio adaptador que implementa la interfaz estándar de autenticación. |
| **Frontend Provider** | [keycloak.initializer.ts](file:///e:/LPII/Viernes/viernes-frontend/src/app/core/auth/keycloak/keycloak.initializer.ts) | Proveedor `APP_INITIALIZER` para iniciar sesión antes de montar las vistas. |

---

## 3. Usuarios de Prueba Preconfigurados en el Realm

Al iniciar Keycloak, el archivo `viernes-realm.json` creará automáticamente los siguientes usuarios:

| Correo / Usuario | Contraseña | Rol de Realm | Nombre |
| :--- | :--- | :--- | :--- |
| `admin@viernes.com` | `Admin12345` | `ADMIN` | Nestor Barrantes |
| `jefe@viernes.com` | `Jefe12345` | `JEFE` | Carlos Mendoza |
| `vendedor@viernes.com` | `Vendedor12345` | `VENDEDOR` | Ana Lopez |

*(Consola de administración de Keycloak: `http://localhost:8180` — Usuario maestro: `admin` / `admin`)*.

---

## 4. Pasos para la Activación Definitiva

### Paso 1: Iniciar el contenedor de Keycloak
En la raíz del proyecto (`e:\LPII\Viernes`), ejecuta:

```bash
docker compose -f compose-keycloak.yml up -d
```

Verifica que el servicio esté arriba abriendo:
* **Consola Keycloak:** `http://localhost:8180`
* **Metadatos OpenID:** `http://localhost:8180/realms/viernes-realm/.well-known/openid-configuration`

---

### Paso 2: Iniciar el Backend con el Perfil Keycloak
El backend ya cuenta con la dependencia `spring-boot-starter-oauth2-resource-server` y las clases de seguridad preparadas.

Para arrancarlo en modo Keycloak:
```powershell
cd viernes-backend
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev,keycloak
```

*Cuando el perfil `keycloak` está activo, Spring Boot:*
1. Desactiva el filtro JWT antiguo gracias a `@Profile("!keycloak")`.
2. Activa `KeycloakSecurityConfig` gracias a `@Profile("keycloak")`.
3. Valida automáticamente las firmas criptográficas de los tokens Bearer contra `http://localhost:8180/realms/viernes-realm/protocol/openid-connect/certs`.
4. Transforma los roles de Keycloak en autoridades `ROLE_ADMIN`, `ROLE_JEFE`, `ROLE_VENDEDOR`.

---

### Paso 3: Activar Keycloak en el Frontend (Angular)
1. Instala el cliente oficial en `viernes-frontend`:
   ```powershell
   cd viernes-frontend
   npm install keycloak-js
   ```

2. Registra el inicializador en [app.config.ts](file:///e:/LPII/Viernes/viernes-frontend/src/app/app.config.ts):
   ```typescript
   import { provideKeycloak } from './core/auth/keycloak/keycloak.initializer';

   export const appConfig: ApplicationConfig = {
     providers: [
       provideKeycloak(),
       // ...resto de proveedores
     ]
   };
   ```

3. Actualiza [jwt.interceptor.ts](file:///e:/LPII/Viernes/viernes-frontend/src/app/core/interceptors/jwt.interceptor.ts) para obtener el token desde `KeycloakAuthService.getTokenSync()`.

---

## 5. Estrategia de Rollback Inmediato

Si en cualquier momento deseas volver a la autenticación clásica sin Keycloak:
1. En el backend, ejecuta sin el perfil keycloak:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
   *(El perfil por defecto `dev` reactiva automáticamente el sistema JWT clásico)*.
2. En el frontend, mantén `AuthService` como el proveedor activo.
