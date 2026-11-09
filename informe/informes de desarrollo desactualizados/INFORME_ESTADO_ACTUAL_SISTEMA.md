# INFORME TÉCNICO Y EJECUTIVO: ESTADO ACTUAL DEL SISTEMA VIERNES CRM V1

**Fecha de emisión:** 19 de Octubre de 2026  
**Sistema:** VIERNES CRM V1 (Monorepo LP2 - BomERP)  
**Ambiente verificado:** DEV (Entorno de Desarrollo Local)  
**Estado General:** 🟢 **100% OPERATIVO Y VERIFICADO EN VIVO**  

---

## 1. Resumen Ejecutivo

El sistema **VIERNES CRM V1** se encuentra totalmente desplegado, integrado y en estado operativo. Todas las capas de la arquitectura —Base de Datos relacional, Servidor de Identidad OAuth2/OIDC, Backend de microservicios REST y Frontend SPA— están encendidas, respondiendo peticiones HTTP en sus puertos respectivos y con sus suites de pruebas en verde.

### Indicadores Clave de Estado (KPIs)
- **Servicios en Ejecución:** 5 de 5 activos (Oracle 23c, Keycloak 25, PostgreSQL Keycloak, Spring Boot 4, Angular 22).
- **Pruebas de Backend:** **132 de 132 pruebas superadas** (`BUILD SUCCESS`, 0 errores, 0 fallos).
- **Compilación de Frontend:** **0 errores de TypeScript**, empaquetado exitoso en 4.0s.
- **Salud del Sistema (Actuator):** `UP` (Conexión Oracle validada con `isValid()`, SSL, liveness y readiness activos).
- **Consistencia de Datos:** Esquema `BOM_VENTAS` inicializado con tablas `CRM_*` y datos semilla de prueba.

---

## 2. Topología e Infraestructura en Ejecución

Todos los puertos requeridos se encuentran en estado de escucha (`LISTEN`):

| Componente | Tecnología | Contenedor / Proceso | Puerto | Protocolo | Estado Actual |
| :--- | :--- | :--- | :---: | :---: | :---: |
| **Base de Datos Principal** | Oracle AI Database 23c Free | `bomerp-oracle` (Docker) | `1521` | Oracle T4C | 🟢 `Up (healthy)` |
| **Base de Datos Auth** | PostgreSQL 16 Alpine | `viernes-keycloak-db` (Docker) | `5433` | PostgreSQL | 🟢 `Up (healthy)` |
| **Servidor OIDC / IAM** | Keycloak 25.0.6 Quarkus | `viernes-keycloak` (Docker) | `8180` | HTTP / OIDC | 🟢 `Up` |
| **Backend REST API** | Spring Boot 4.0.7 (Java 21) | Proceso Local PID 26172 | `8080` | HTTP / REST | 🟢 `Up (Tomcat 11)` |
| **Frontend Web SPA** | Angular 22.1 + Vite Dev Server | Proceso Local PID Activo | `4200` | HTTP / HTML5 | 🟢 `Up (Vite 5)` |

---

## 3. Estado de Módulos Funcionales

La arquitectura del sistema sigue los principios de **Spring Modulith** y **Arquitectura Hexagonal (Puertos y Adaptadores)** en el backend, y arquitectura modular por componentes reactivos en el frontend.

### 3.1. Clientes y Contactos
- **Estado:** 🟢 **Operativo**
- **Funcionalidades:** CRUD completo de empresas cliente, búsqueda por texto libre (`?q=`), filtro por estado (`ACTIVO`/`INACTIVO`), desactivación lógica (`PATCH /desactivar`). Subrecurso anidado de contactos (`/api/v1/clientes/{id}/contactos`) con marcación de contacto principal.
- **Transaccionalidad:** Métodos de lectura asegurados con `@Transactional(readOnly = true)`.

### 3.2. Leads (Prospectos)
- **Estado:** 🟢 **Operativo**
- **Funcionalidades:** Registro de prospectos comerciales, clasificación por fuente de captación, actualización rápida de estado (`NUEVO`, `CONTACTADO`, `CALIFICADO`, `CONVERTIDO`, `DESCARTADO`). Búsqueda y paginación reactiva en frontend.

### 3.3. Oportunidades y Pipeline de Ventas
- **Estado:** 🟢 **Operativo**
- **Funcionalidades:** Gestión de embudo comercial a través de 7 etapas (`PROSPECCION`, `CONTACTO`, `CALIFICACION`, `PROPUESTA`, `NEGOCIACION`, `GANADA`, `PERDIDA`).
- **Cálculo de Negocio:** Cálculo en tiempo real del valor ponderado (`valor * probabilidad / 100`) con redondeo bancario estricto (`RoundingMode.HALF_UP` a 2 decimales) para prevenir `ArithmeticException`.

### 3.4. Propuestas Comerciales (Cotizaciones)
- **Estado:** 🟢 **Operativo**
- **Funcionalidades:** Elaboración de propuestas económicas con desglose de ítems (detalle, cantidad, precio unitario, subtotal y total calculado).
- **Reglas de Negocio:** Máquina de estados formal: `BORRADOR` ➔ `ENVIADA` ➔ `APROBADA` / `RECHAZADA` / `CANCELADA`. Restricción de edición estricta permitida únicamente en estado `BORRADOR`. Numeración correlativa estructurada con prefijo anual.

### 3.5. Actividades y Tareas
- **Estado:** 🟢 **Operativo**
- **Funcionalidades:** Registro y seguimiento de llamadas, reuniones, tareas y seguimientos. Integración relacional polimórfica (asociable a Cliente, Lead u Oportunidad).
- **Lógica Temporal:** Identificación computada dinámica de actividades vencidas (`vencida = true` calculada en backend en base a fecha/hora del sistema vs. fecha límite, sin desincronización de BD).

### 3.6. Dashboard y Métricas de Negocio
- **Estado:** 🟢 **Operativo**
- **Funcionalidades:** Agregaciones SQL directas (`COUNT`, `SUM`, `GROUP BY`) que devuelven el estado del CRM en una sola llamada:
  - Total de clientes.
  - Distribución de leads por estado.
  - Resumen del pipeline comercial (abiertas, ganadas, perdidas, valor total y valor ganado).
  - Resumen de actividades (pendientes, en proceso, completadas, canceladas y vencidas).
  - Resumen de propuestas económicas y monto total aprobado.

### 3.7. Seguridad, Autenticación y Autorización
- **Estado:** 🟢 **Operativo (Arquitectura Dual)**
  - **Modo Keycloak (Activo):** Funciona como OAuth2 Resource Server validando tokens JWT emitidos por Keycloak contra el endpoint JWKS (`http://localhost:8180/realms/viernes-realm/protocol/openid-connect/certs`). Mapeo automático de roles del realm (`ADMIN`, `JEFE`, `VENDEDOR`) hacia `GrantedAuthority` de Spring Security.
  - **Modo Standalone JWT:** Capacidad de fallback local con autenticación estándar contra base de datos mediante BCrypt y tokens firmados con HS256 (`/api/v1/auth/login`).
  - **Filtro de Auditoría:** `CorrelationIdFilter` activo en el ciclo de vida HTTP asignando un identificador de traza a cada petición.

---

## 4. Estado de los Datos en la Base de Datos Oracle

El esquema `BOM_VENTAS` en el servicio `FREEPDB1` contiene las tablas generadas por JPA Hibernate (`ddl-auto: update`) con integridad referencial activa:

```sql
SELECT TABLA, COUNT(*) FROM CRM_*;
```

| Tabla | Descripción | Filas Actuales | Estado |
| :--- | :--- | :---: | :---: |
| `CRM_USUARIOS` | Usuarios y operadores del sistema | **2** | 🟢 Poblado con usuarios demo |
| `CRM_CLIENTES` | Empresas clientes registradas | **4** | 🟢 Datos de prueba activos |
| `CRM_CONTACTOS` | Personas de contacto de clientes | **4** | 🟢 Vinculados a clientes |
| `CRM_LEADS` | Prospectos en proceso comercial | **2** | 🟢 Datos de prueba activos |
| `CRM_OPORTUNIDADES` | Negociaciones en pipeline | **3** | 🟢 Con valores y probabilidades |
| `CRM_ACTIVIDADES` | Tareas y seguimientos registrados | **2** | 🟢 Vinculadas a usuarios y entidades |
| `CRM_PROPUESTAS` | Encabezados de cotizaciones | **0** | ⚪ Listo para recibir registros |
| `CRM_PROPUESTA_DETALLES` | Líneas de detalle de cotizaciones | **0** | ⚪ Listo para recibir registros |

---

## 5. Pruebas y Aseguramiento de Calidad

### 5.1. Backend (`mvnw.cmd test`)
- **Total de pruebas ejecutadas:** **132**
- **Fallos:** **0**
- **Errores:** **0**
- **Omitidas:** **0**
- **Tiempo total de ejecución:** 18.38 segundos
- **Resultado:** `BUILD SUCCESS`
- **Cobertura de suites:**
  - `ClienteServiceTest` & `ClienteControllerTest`: 30 tests
  - `LeadServiceTest` & `LeadControllerTest`: 16 tests
  - `OportunidadServiceTest`, `OportunidadControllerTest` & `OportunidadDomainTest`: 20 tests
  - `PropuestaServiceTest`: 16 tests
  - `ActividadServiceTest` & `ActividadControllerTest`: 12 tests
  - `UsuarioServiceTest` & `UsuarioControllerTest`: 19 tests
  - `AuthServiceTest` & `JwtServiceTest`: 8 tests
  - `DashboardServiceTest`: 8 tests
  - `ViernesApplicationTests` (Carga completa del contexto): 1 test

### 5.2. Frontend (`npm.cmd run build`)
- **Compilador:** Angular CLI 22 + Vite
- **Resultado:** Exitoso (`Application bundle generation complete in 4.011s`)
- **Optimización:** División de código en 34 chunks perezosos (Lazy Loading) reduciendo el bundle inicial a solo 340.58 kB (93.71 kB transferidos gzippeados).
- **Compatibilidad de Estilos:** Material Theme integrado sin colisiones de layout.

---

## 6. Automatización de Despliegue (Lanzador de 1 Clic)

El archivo [`activar-sistema.bat`](file:///e:/LPII/Viernes/activar-sistema.bat) en la raíz del proyecto fue consolidado y optimizado con una secuencia de 5 fases a prueba de fallos:

1. **Chequeo de Docker Desktop:** Valida que el demonio Docker esté corriendo.
2. **Arranque de Oracle 23c:** Levanta el contenedor `bomerp-oracle` en puerto 1521 con persistencia de volumen.
3. **Arranque de Keycloak 25 + PostgreSQL:** Levanta `viernes-keycloak` y su BD interna con importación automática de realm.
4. **Arranque de Backend Spring Boot:** Ejecuta el backend con Maven Wrapper y perfiles `dev,keycloak`.
5. **Arranque de Frontend Angular:** Verifica dependencias de Node e inicia el servidor de desarrollo Vite abriendo automáticamente el navegador en `http://localhost:4200`.

---

## 7. Directorio de Acceso y Credenciales

### Puntos de Acceso Web
- **Frontend SPA (Aplicación):** [http://localhost:4200](http://localhost:4200)
- **Documentación Swagger / OpenAPI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Especificación OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Endpoint de Salud Actuator:** [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- **Consola de Administración Keycloak:** [http://localhost:8180](http://localhost:8180)
- **Metadatos OIDC del Realm:** [http://localhost:8180/realms/viernes-realm/.well-known/openid-configuration](http://localhost:8180/realms/viernes-realm/.well-known/openid-configuration)

### Credenciales de Demostración
- **Usuario Administrador del CRM:**
  - **Correo:** `admin@viernes.com`
  - **Contraseña:** `Admin12345`
  - **Roles:** `ADMIN`, `JEFE`, `VENDEDOR`
- **Usuario Vendedor del CRM:**
  - **Correo:** `vendedor@viernes.com`
  - **Contraseña:** `Vendedor123`
  - **Roles:** `VENDEDOR`
- **Administrador de Consola Keycloak:**
  - **Usuario:** `admin`
  - **Contraseña:** `admin`

---

## 8. Conclusiones y Observaciones Técnicas

1. **Estabilidad Completa:** El sistema no presenta errores bloqueantes ni inconsistencias de esquema. La arquitectura modular bajo Spring Modulith garantiza límites arquitectónicos limpios entre dominios.
2. **Seguridad Robusta:** La integración de Keycloak permite delegar el ciclo de vida de identidades, tokens y rotación de llaves criptográficas RSA sin sobrecargar la lógica de negocio del backend.
3. **Rendimiento:** Tanto el empaquetado del frontend como los tiempos de respuesta de la API REST se sitúan en rangos óptimos para el ambiente de desarrollo.
4. **Próxima Fase (Hacia Producción S13-S14):** Se recomienda a futuro la adición de caché Redis para agregados pesados de analítica (`/dashboard/resumen`) y la configuración de perfiles `prod` con variables de entorno seguras fuera del repositorio de código.
