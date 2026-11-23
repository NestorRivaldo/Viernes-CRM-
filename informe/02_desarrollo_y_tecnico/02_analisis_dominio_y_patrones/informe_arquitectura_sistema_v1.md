# Informe técnico de arquitectura — Sistema completo Viernes CRM V1

> Documento en texto (Markdown). Representa **la arquitectura de todo el sistema**: estilo arquitectónico, stack, los 14 módulos Spring Modulith y su clasificación OPEN/CLOSED, la anatomía hexagonal común, los patrones de comunicación entre módulos, las preocupaciones transversales (`shared`), la seguridad Keycloak, la única integración externa (APIsPERU), las transacciones, el frontend Angular, el flujo end-to-end de una petición, la infraestructura de activación y las brechas sistémicas. Cada afirmación lleva ancla `archivo:línea` verificada contra el código real.

## Metadatos

| Campo | Valor |
|---|---|
| Sistema | Viernes CRM V1 |
| Estilo arquitectónico | **Monolito modular** (Spring Modulith) con **módulos hexagonales** (puertos y adapters) |
| Despliegue | **Un solo** backend Spring Boot + **una** SPA Angular + **una** BD Oracle + Keycloak externo |
| Backend | Spring Boot **4.0.7**, Java **21**, Spring Modulith **2.0.7** (`pom.xml:9`, `pom.xml:31`, `pom.xml:32`) |
| Frontend | Angular **22** (`@angular/core ^22.1.0`), TypeScript **~6.0.2**, keycloak-js **^26.2.4** (`package.json:17`, `package.json:34`, `package.json:23`) |
| Persistencia | Oracle Database 23c (`gvenzl/oracle-free:23-slim`), usuario `BOM_VENTAS`, servicio `FREEPDB1` (`compose-dev.yml:5`, `application-dev.yml:6`) |
| Identidad | Keycloak **25.0.6**, realm `viernes-realm`, OAuth2 Resource Server (JWT) (`compose-keycloak.yml:23`, `application-keycloak.yml:11`) |
| Módulos de aplicación | **14** declarados con `@ApplicationModule` + contenedor transversal `shared` (2 submódulos OPEN anidados) |
| Archivos backend (main) | **259** `.java` (incl. `ViernesApplication.java`) |
| Controllers REST | **14** en **13** módulos (auth no expone REST) |
| Services (application) | **14**; **13** con `@Transactional` |
| Casos de uso (port/in) | **14** interfaces `*UseCase` |
| Pruebas | **32** clases, **291** `@Test`; `ModularityTests.verify()` valida límites |
| Integración externa | **1** adapter `out/external`: APIsPERU DNI/RUC (módulo `clientes`) |
| Perfiles activos | `dev,keycloak` (`application.yml:5`) |
| Fecha de generación | 2026-11-08 |
| Árbol canónico | `E:\Viernes\Viernes` |

### Convención de anclas

Las rutas son relativas a estas bases (se indica una vez, luego se abrevia):

- **Backend main:** `E:\Viernes\Viernes\viernes-backend\src\main\java\com\viernes\`
- **Backend resources:** `E:\Viernes\Viernes\viernes-backend\src\main\resources\`
- **Backend test:** `E:\Viernes\Viernes\viernes-backend\src\test\java\com\viernes\`
- **Backend raíz:** `E:\Viernes\Viernes\viernes-backend\`
- **Frontend src:** `E:\Viernes\Viernes\viernes-frontend\src\`
- **Frontend raíz:** `E:\Viernes\Viernes\viernes-frontend\`
- **Repo raíz:** `E:\Viernes\Viernes\`

Ejemplo: `auth/keycloak/KeycloakSecurityConfig.java:84` = `<base backend main>/auth/keycloak/KeycloakSecurityConfig.java`, línea 84. `pom.xml:9` = `<backend raíz>/pom.xml`, línea 9. `compose-keycloak.yml:23` = `<repo raíz>/compose-keycloak.yml`, línea 23.

---

## Índice

1. [Resumen ejecutivo: qué arquitectura es](#1-resumen-ejecutivo-qué-arquitectura-es)
2. [Stack tecnológico y build](#2-stack-tecnológico-y-build)
3. [Topología de despliegue](#3-topología-de-despliegue)
4. [Los módulos: inventario y clasificación Modulith](#4-los-módulos-inventario-y-clasificación-modulith)
5. [Anatomía hexagonal de un módulo](#5-anatomía-hexagonal-de-un-módulo)
6. [Variantes estructurales por módulo](#6-variantes-estructurales-por-módulo)
7. [Comunicación entre módulos](#7-comunicación-entre-módulos)
8. [El contenedor `shared`: preocupaciones transversales](#8-el-contenedor-shared-preocupaciones-transversales)
9. [Seguridad: Keycloak OAuth2 Resource Server](#9-seguridad-keycloak-oauth2-resource-server)
10. [Integración externa: APIsPERU](#10-integración-externa-apisperu)
11. [Transacciones](#11-transacciones)
12. [Frontend Angular](#12-frontend-angular)
13. [Flujo end-to-end de una petición](#13-flujo-end-to-end-de-una-petición)
14. [Arranque ≠ petición](#14-arranque--petición)
15. [Infraestructura y activación](#15-infraestructura-y-activación)
16. [Pruebas y verificación de modularidad](#16-pruebas-y-verificación-de-modularidad)
17. [Análisis de brechas sistémicas](#17-análisis-de-brechas-sistémicas)
18. [Inventario cuantitativo](#18-inventario-cuantitativo)
19. [Cómo leer el sistema](#19-cómo-leer-el-sistema)

---

## 1. Resumen ejecutivo: qué arquitectura es

Viernes CRM V1 **no es un sistema de microservicios**. Es un **monolito modular**: un único artefacto desplegable (`viernes-backend`, `pom.xml:13`) que contiene **14 módulos de aplicación** delimitados por paquete y gobernados por **Spring Modulith 2.0.7** (`pom.xml:32`). Cada módulo de negocio sigue, además, una **arquitectura hexagonal** (puertos y adapters): `adapters → application → domain`. Son dos niveles de diseño complementarios:

- **Modulith** gobierna la **macro-estructura**: qué paquetes son módulos, cuáles son **OPEN** (internos accesibles) o **CLOSED** (solo API pública), y qué dependencias entre módulos están permitidas (`allowedDependencies`). Lo verifica en tiempo de prueba con `modules.verify()` (`ModularityTests.java:12`).
- **Hexagonal** gobierna la **micro-estructura** de cada módulo: el `domain` es POJO puro; `application` define **puertos** (`port/in` = casos de uso, `port/out` = persistencia/consulta) y los **Services** que los implementan; `adapters/in/web` (Controller) y `adapters/out/persistence` (Adapter + JPA) son los detalles externos. La **inversión de dependencia** (el Service depende de interfaces `port/out`, no de JPA) hace la aplicación testeable con mocks.

**Por qué importa esta distinción para la sustentación:** todo el sistema comparte **un solo proceso JVM, un solo contexto de Spring y una sola conexión/transacción JDBC por petición**. No hay llamadas de red entre módulos: la comunicación inter-módulo es **invocación de métodos Java in-process** a través de interfaces públicas o puertos. La única salida de red del backend es hacia **Oracle** (persistencia), **Keycloak** (validación JWT/JWKS) y **APIsPERU** (consulta DNI/RUC, un solo adapter). El frontend Angular es un proceso aparte (SPA en `:4200`) que habla con el backend por HTTP/REST.

**Regla de oro de dependencia:** dentro de un módulo, `adapters → application → domain` (el dominio no conoce Spring ni JPA). Entre módulos, solo lo que `allowedDependencies` autoriza, y a través de la superficie pública del módulo destino (ver §7).

---

## 2. Stack tecnológico y build

El build es **Maven** (wrapper `mvnw.cmd`, sin Maven global). El `pom.xml` hereda de `spring-boot-starter-parent` **4.0.7** (`pom.xml:7-9`) y fija Java **21** (`pom.xml:31`) y Spring Modulith **2.0.7** (`pom.xml:32`, importado como BOM en `pom.xml:144-145`).

### Dependencias declaradas (con ancla al `artifactId`)

| Dependencia | Línea | Rol en la arquitectura |
|---|---|---|
| `spring-boot-starter-actuator` | `pom.xml:37` | Salud/métricas; endpoint Modulith expuesto |
| `spring-boot-starter-cache` | `pom.xml:41` | Abstracción de caché (`@Cacheable`) |
| `caffeine` | `pom.xml:45` | Proveedor de caché en memoria (TTL) |
| `spring-boot-starter-data-jpa` | `pom.xml:49` | Persistencia ORM (Hibernate) sobre Oracle |
| `spring-boot-starter-validation` | `pom.xml:53` | Bean Validation (`@NotNull`, `@Size`…) en DTOs |
| `spring-boot-starter-webmvc` | `pom.xml:57` | Controllers REST / Tomcat embebido |
| `springdoc-openapi-starter-webmvc-ui` **3.0.2** | `pom.xml:61-62` | Swagger UI / OpenAPI |
| `spring-modulith-starter-core` | `pom.xml:66` | Núcleo Modulith (detección de módulos) |
| `spring-boot-devtools` | `pom.xml:72` | Recarga en desarrollo |
| `ojdbc11` | `pom.xml:78` | Driver JDBC Oracle |
| `spring-modulith-actuator` | `pom.xml:83` | Expone el grafo de módulos vía Actuator |
| `lombok` | `pom.xml:89` | Generación de boilerplate (getters/constructores) |
| `spring-boot-starter-test` | `pom.xml:94` | JUnit/Mockito/AssertJ |
| `spring-boot-starter-webmvc-test` | `pom.xml:99` | MockMvc para tests de Controller |
| `spring-modulith-starter-test` | `pom.xml:104` | `ApplicationModules.verify()` en tests |
| `spring-modulith-docs` | `pom.xml:109` | `Documenter` (genera docs de módulos) |
| `mapstruct` **1.6.3** | `pom.xml:114-115` | **Declarado pero SIN uso** (0 `@Mapper`, ver §17) |
| `micrometer-registry-prometheus` | `pom.xml:119` | Métricas en formato Prometheus |
| `spring-boot-starter-security` | `pom.xml:126` | Spring Security (filter chain, autorización) |
| `spring-security-test` | `pom.xml:130` | Soporte de seguridad en tests |
| `spring-boot-starter-oauth2-resource-server` | `pom.xml:137` | Validación de JWT (Keycloak) |

### Procesamiento de anotaciones y empaquetado

- El `spring-boot-maven-plugin` **excluye Lombok** del artefacto final (`pom.xml:167`): Lombok es solo compile-time.
- El `maven-compiler-plugin` configura `annotationProcessorPaths` con **Lombok** (`pom.xml:186`) y **MapStruct processor** (`pom.xml:190-191`), y repite Lombok para `testCompile` (`pom.xml:206`). MapStruct está listo para usarse, pero **ningún mapper lo usa** todavía.
- `dependencyManagement` importa el BOM de Modulith (`pom.xml:144-145`) y fija `mapstruct-processor` 1.6.3 (`pom.xml:151-152`).

**Lectura arquitectónica:** el stack es el de un monolito Spring Boot clásico (webmvc + data-jpa + security) **reforzado con Modulith** para imponer límites de módulo, y con **Caffeine + Micrometer + Actuator** para observabilidad y caché. La presencia de MapStruct sin uso es una **intención de diseño no consumada** (brecha, §17).

---

## 3. Topología de despliegue

Cuatro procesos/sistemas, todos en `localhost` en desarrollo:

```
┌─────────────────────────┐        HTTP/REST + JWT         ┌──────────────────────────┐
│  SPA Angular (:4200)    │ ─────────────────────────────▶ │  Backend Spring Boot     │
│  viernes-frontend       │   X-Trace-ID, Bearer token     │  viernes-backend (:8080) │
│  Node / ng serve        │ ◀───────────────────────────── │  UN solo JVM / contexto  │
└─────────────────────────┘        JSON DTOs               └────────────┬─────────────┘
                                                                        │
                       ┌────────────────────────────────────────────────┼───────────────────────────┐
                       │ JDBC (ojdbc11)                                  │ HTTPS (RestClient)         │ JWKS / issuer
                       ▼                                                 ▼                            ▼
        ┌──────────────────────────┐                  ┌──────────────────────────┐   ┌──────────────────────────┐
        │ Oracle 23c (:1521)       │                  │ APIsPERU (externo)       │   │ Keycloak 25 (:8180)      │
        │ bomerp-oracle            │                  │ dniruc.apisperu.com      │   │ viernes-keycloak         │
        │ BOM_VENTAS / FREEPDB1    │                  │ token en query param     │   │ realm viernes-realm      │
        └──────────────────────────┘                  └──────────────────────────┘   │ + PostgreSQL (keycloak-db)│
                                                                                      └──────────────────────────┘
```

- **Frontend:** SPA Angular servida por `ng serve` en `:4200` (`activar-sistema.bat:85`). Su `apiBaseUrl` apunta a `http://localhost:8080/api/v1` (`environment.ts:2`).
- **Backend:** un único Spring Boot en `:8080` (`activar-sistema.bat:74`). Dentro conviven los 14 módulos; **no hay fronteras de red entre ellos**.
- **Oracle:** contenedor `bomerp-oracle`, imagen `gvenzl/oracle-free:23-slim`, puerto `1521`, usuario de aplicación `BOM_VENTAS` (`compose-dev.yml:5-12`). El datasource JDBC es `jdbc:oracle:thin:@localhost:1521/FREEPDB1` (`application-dev.yml:6`), driver `oracle.jdbc.OracleDriver` (`application-dev.yml:9`), `ddl-auto: update` (`application-dev.yml:13`).
- **Keycloak:** contenedor `viernes-keycloak`, imagen `quay.io/keycloak/keycloak:25.0.6`, mapeo `8180:8080` (`compose-keycloak.yml:23-41`), respaldado por `postgres:16-alpine` (`viernes-keycloak-db`, `compose-keycloak.yml:5-6`). El backend lo usa como **issuer/JWKS** (`application-keycloak.yml:11-12`), no como base de datos de negocio.

**Consecuencia transaccional:** al ser un solo proceso con un solo `DataSource`, una petición puede abrir **una transacción JDBC** que abarca escrituras de varios módulos (si se invocan in-process dentro del mismo Service). No hay transacciones distribuidas ni XA (ver §11).

---

## 4. Los módulos: inventario y clasificación Modulith

Bajo `com.viernes` hay **15 directorios de primer nivel**: 14 módulos de aplicación + `shared`. Cada módulo de aplicación declara `@ApplicationModule` en su `package-info.java`. **Spring Modulith por defecto es CLOSED**: si el `package-info` no especifica `type = OPEN`, el módulo es **CLOSED** (solo su API pública es accesible desde fuera).

### Tabla maestra de módulos

| Módulo | Tipo | `allowedDependencies` | Ancla `package-info` | Archivos | Rol |
|---|---|---|---|---|---|
| `actividades` | **OPEN** | clientes, oportunidades, leads, etiquetas, usuarios, shared, shared.exception | `actividades/package-info.java:2-3` | 29 | Agenda de actividades (llamadas/reuniones/tareas) |
| `auth` | **CLOSED** | usuarios | `auth/package-info.java:2` | 4 | Seguridad Keycloak (config + converter); **sin REST ni domain** |
| `clientes` | **OPEN** | *(sin restricción)* | `clientes/package-info.java:1` | 24 | Clientes/contactos + **única integración externa** (APIsPERU) |
| `dashboard` | **OPEN** | *(sin restricción)* | `dashboard/package-info.java:1` | 11 | Resumen/agregación de solo lectura; **sin `domain`** |
| `equipos` | **CLOSED** | usuarios, shared, shared.exception | `equipos/package-info.java:2` | 21 | Equipos de venta |
| `etiquetas` | **OPEN** | *(sin restricción)* | `etiquetas/package-info.java:1` | 11 | Etiquetas reutilizables |
| `leads` | **OPEN** | *(sin restricción)* | `leads/package-info.java:1` | 22 | Leads + fuentes (2 controllers, 2 services) |
| `marketing` | **OPEN** | *(sin restricción)* | `marketing/package-info.java:1` | 19 | Campañas de marketing |
| `mensajeria` | **CLOSED** | usuarios, shared, shared.exception | `mensajeria/package-info.java:2` | 12 | Mensajería interna |
| `oportunidades` | **OPEN** | *(sin restricción)* | `oportunidades/package-info.java:1` | 40 | Oportunidades de venta (módulo más grande) |
| `productos` | **OPEN** | *(sin restricción)* | `productos/package-info.java:1` | 12 | Productos/catálogo |
| `propuestas` | **OPEN** | productos, clientes, oportunidades, shared, shared.exception | `propuestas/package-info.java:2-3` | 18 | Propuestas comerciales (aprobación ADMIN/JEFE) |
| `relationshipmap` | **OPEN** | clientes, oportunidades, actividades, propuestas, productos, usuarios, shared, shared.exception | `relationshipmap/package-info.java:2-3` | 9 | Grafo de relaciones; **sin `adapters/out`** |
| `usuarios` | **CLOSED** | shared, shared.exception | `usuarios/package-info.java:2` | 16 | Usuarios; expone **`UsuariosApi`** (named interface) |
| `shared` | *(contenedor)* | — | **sin `package-info` raíz** | 10 | Transversal: exception, filter, cache, ratelimit, OpenApi |

**Recuento:** **10 OPEN** (actividades, clientes, dashboard, etiquetas, leads, marketing, oportunidades, productos, propuestas, relationshipmap) + **4 CLOSED** (auth, equipos, mensajeria, usuarios) = **14 módulos de aplicación**. `shared` no es un módulo de aplicación en su raíz (no tiene `package-info`), pero contiene **2 submódulos OPEN anidados**: `shared.exception` (`shared/exception/package-info.java:1`) y `shared.ratelimit` (`shared/ratelimit/package-info.java:1`). Por eso muchos `allowedDependencies` listan `shared` **y** `shared.exception` por separado.

### Qué significan OPEN y CLOSED en la práctica

- **OPEN:** los paquetes internos del módulo (`domain`, `application.service`, `adapters.out.persistence.repository`…) son accesibles desde otros módulos.Más libertad, **más acoplamiento**. Ejemplo: `actividades` consulta repositorios/puertos de `clientes`, `etiquetas`, `usuarios` directamente.
- **CLOSED:** desde fuera **solo** se puede tocar la **API pública** del módulo (tipos en el paquete raíz del módulo o interfaces nombradas). Ejemplo: `usuarios` es CLOSED y por eso expone `UsuariosApi` en su raíz (`usuarios/UsuariosApi.java:10`); nadie puede inyectar su repositorio interno. `auth`, `equipos` y `mensajeria` también son CLOSED, pero **ningún otro módulo los lista como dependencia**, así que son módulos hoja sin necesidad de API pública adicional.

---

## 5. Anatomía hexagonal de un módulo

Todos los módulos de negocio comparten el mismo esqueleto de paquetes (verificado en clientes, leads, oportunidades, productos, propuestas, etiquetas, equipos, usuarios, mensajeria, marketing):

```
<módulo>/
├── package-info.java                 ← declaración Modulith (OPEN/CLOSED + allowedDependencies)
├── domain/                           ← POJOs puros: entidades de dominio, enums, value objects
│                                        (sin Spring, sin JPA, sin Jackson)
├── application/
│   ├── dto/                          ← Request/Response (transporte HTTP, con Bean Validation)
│   ├── port/
│   │   ├── in/   *UseCase.java       ← PUERTO DE ENTRADA: interfaz de casos de uso
│   │   └── out/  *PersistencePort.java / *QueryPort.java / *LookupPort.java  ← PUERTO DE SALIDA
│   └── service/  *Service.java       ← implementa port/in; orquesta; @Transactional
└── adapters/
    ├── in/web/   *Controller.java    ← ADAPTER DE ENTRADA: REST, mapea HTTP → port/in
    └── out/
        ├── persistence/              ← ADAPTER DE SALIDA: implementa port/out
        │   ├── *PersistenceAdapter.java
        │   ├── entity/   *Entity.java     ← JPA (@Entity, @Table) — detalle de persistencia
        │   └── repository/ *SpringDataRepository.java
        └── external/                 ← SOLO en clientes: APIsPERU (RestClient)
```

### Las dos direcciones de la inversión de dependencia

| Puerto | Quién lo define | Quién lo implementa | Quién lo llama | Dirección |
|---|---|---|---|---|
| **`port/in`** (`*UseCase`) | `application` | el `Service` (`application/service`) | el `Controller` (`adapters/in/web`) | adapter → application |
| **`port/out`** (`*PersistencePort`, `*QueryPort`, `*LookupPort`) | `application` | el `Adapter` (`adapters/out/...`) | el `Service` (`application/service`) | application → adapter (vía interfaz) |

- El **Controller** no conoce JPA ni el Service concreto: depende de la interfaz `port/in` (`*UseCase`).
- El **Service** no conoce JPA ni Oracle: depende de las interfaces `port/out`. El adapter de persistencia las implementa y traduce `domain ↔ entity`.
- El **domain** no conoce nada de infraestructura. Por eso el Service se prueba con **mocks de los puertos** (sin BD), y el Controller con **MockMvc standalone** (sin contexto completo).

**Ejemplo canónico (módulo actividades, dossier completo en `informe_modulo_actividades_v1.md`):** `ActividadController` → `ManageActividadUseCase` (port/in) → `ActividadService` → `ActividadPersistencePort` (port/out) → `ActividadPersistenceAdapter` → `ActividadSpringDataRepository` → `ActividadEntity` → Oracle `CRM_ACTIVIDADES`.

---

## 6. Variantes estructurales por módulo

No todos los módulos son hexagonales "de libro". Las diferencias son **deliberadas** y revelan el rol de cada módulo:

| Módulo | Variantes | Evidencia | Interpretación |
|---|---|---|---|
| `clientes` | **Único** con `adapters/out/external` | `clientes/adapters/out/external/ApisPeruDocumentoAdapter.java:28` | Es el punto de integración con el mundo exterior (APIsPERU). Implementa `DocumentoLookupPort` (`clientes/application/port/out/DocumentoLookupPort.java`) |
| `dashboard` | **Sin `domain/`**; tiene `DashboardQueryRepository` en persistence | `find dashboard` → solo dto/port/service/adapters | Es un módulo de **solo lectura/agregación**: no tiene entidad de dominio propia, consulta datos de otros módulos y los resume en DTOs |
| `relationshipmap` | **Sin `adapters/out/`**; `domain` solo enums (`NodeType`, `EdgeType`) | `relationshipmap/domain/NodeType.java`, `EdgeType.java` | Es un **agregador de grafo**: no persiste nada propio, lee de 8 módulos (su `allowedDependencies` es el más ancho, `relationshipmap/package-info.java:3`) |
| `auth` | **Sin `domain`, sin `adapters`, sin Controller** | `find auth` → 4 archivos: config + keycloak + package-info | Es un módulo de **infraestructura de seguridad**, no de negocio. Solo configura Spring Security y el converter JWT |
| `leads` | **2 controllers + 2 services** (Lead y Fuente) | `leads/adapters/in/web/LeadController.java`, `FuenteController.java` | Un módulo con **dos sub-recursos** REST (`/leads` y `/fuentes`) |
| `clientes`, `leads`, `oportunidades` | Directorio `mapper/` **vacío** | `find -type d -name mapper` | Espacio reservado para MapStruct **nunca usado** (brecha, §17) |
| `dashboard` | Directorio `service/` raíz **vacío** (extra) | `find -maxdepth 2 -type d` muestra `/service` sin archivos | Residuo estructural; el Service real vive en `application/service/` |

**Lectura arquitectónica:** el sistema tiene **módulos transaccionales completos** (clientes, leads, oportunidades, propuestas, actividades, productos, equipos, etiquetas, marketing, mensajeria, usuarios), **módulos de lectura/agregación** (dashboard, relationshipmap) y **un módulo de infraestructura** (auth). La hexagonalidad se aplica con pragmatismo: donde no hay persistencia propia, no hay `adapters/out`; donde no hay entidad propia, no hay `domain`.

---

## 7. Comunicación entre módulos

Al ser un monolito, **toda comunicación inter-módulo es invocación de métodos Java in-process** (no HTTP, no colas, no eventos de red). Hay **tres patrones**, de menor a mayor acoplamiento:

### Patrón A — Interfaz pública nombrada (obligatorio en CLOSED)

El módulo destino expone una interfaz en su **paquete raíz**; el consumidor la inyecta. Es el **único** acceso permitido a un módulo CLOSED.

- **Ejemplo:** `usuarios` (CLOSED) expone `UsuariosApi` (`usuarios/UsuariosApi.java:10`) con `buscarPorId` (`:12`), `buscarPorCorreo` (`:14`), `existePorId` (`:16`), `existePorCorreo` (`:18`). `actividades` la usa para validar participantes (`RelacionValidationAdapter`), y `auth`/`equipos`/`mensajeria`/`relationshipmap` dependen de `usuarios` según sus `allowedDependencies`.
- **Ventaja:** encapsulamiento fuerte; el consumidor no ve el repositorio ni la entidad JPA del proveedor.

### Patrón B — Puerto de entrada público (`port/in`) de un módulo OPEN

El consumidor inyecta el `*UseCase` (interfaz `port/in`) del módulo destino y llama a sus casos de uso.

- **Ejemplo:** `actividades` consume `ManageClienteUseCase` de `clientes` (`clientes/application/port/in/ManageClienteUseCase.java:12`, métodos `listar` `:13`, `listarContactos` `:27`) a través de su `ContactoQueryAdapter`. `relationshipmap` y `propuestas` orquestan varios `*UseCase` de módulos OPEN.
- **Ventaja:** contrato estable (la interfaz), aunque el módulo sea OPEN.

### Patrón C — Internos / repositorios / DTOs de un módulo OPEN

El consumidor accede a tipos internos (`domain`, `application.dto`, `adapters.out.persistence.repository`) del módulo destino. **Solo posible porque el destino es OPEN.**

- **Ejemplo:** `actividades` usa `EtiquetaPersistencePort` y `EtiquetaResponse` de `etiquetas`, y `ContactoResponse` de `clientes`. `relationshipmap` lee repositorios/puertos de 8 módulos.
- **Costo:** **mayor acoplamiento**; un cambio interno en el proveedor puede romper al consumidor. Es la razón por la que CLOSED existe como alternativa más estricta.

### Reglas que impone Modulith

1. **`allowedDependencies` es una lista blanca.** Si `actividades` intenta depender de un módulo no listado (p. ej. `marketing`), `modules.verify()` **falla** (`ModularityTests.java:12`).
2. **CLOSED fuerza el Patrón A.** No se puede importar un paquete interno de un módulo CLOSED.
3. **`shared` y `shared.exception`** se listan explícitamente porque son submódulos OPEN separados (§4).

**Mapa de dependencias declarado (quién puede llamar a quién):**

```
actividades      → clientes, oportunidades, leads, etiquetas, usuarios, shared, shared.exception
propuestas       → productos, clientes, oportunidades, shared, shared.exception
relationshipmap  → clientes, oportunidades, actividades, propuestas, productos, usuarios, shared, shared.exception
auth             → usuarios
equipos          → usuarios, shared, shared.exception
mensajeria       → usuarios, shared, shared.exception
usuarios         → shared, shared.exception
clientes/dashboard/etiquetas/leads/marketing/oportunidades/productos → OPEN sin lista (sin restricción declarada)
```

> Nota: `relationshipmap` es el **nodo de mayor fan-in de dependencias** (8 módulos): es el agregador que construye el grafo comercial. `usuarios` es el **nodo de mayor fan-out** (lo consumen actividades, auth, equipos, mensajeria, relationshipmap): por eso es CLOSED con API pública `UsuariosApi`.

---

## 8. El contenedor `shared`: preocupaciones transversales

`shared` (**10 archivos**, sin `package-info` raíz) concentra lo que **no es negocio** y debe ser uniforme en todo el sistema. Sus dos submódulos OPEN (`shared.exception`, `shared.ratelimit`) son listados por separado en los `allowedDependencies`.

### Inventario de `shared`

| Archivo | Ancla | Responsabilidad |
|---|---|---|
| `OpenApiConfig.java` | `shared/OpenApiConfig.java` | Configuración Swagger/OpenAPI (esquema de seguridad JWT) |
| `cache/CacheConfig.java` | `shared/cache/CacheConfig.java:22-25` | `@EnableCaching`; `CaffeineCacheManager` con TTL (`:36-38`) |
| `cache/CacheRefreshJob.java` | `shared/cache/CacheRefreshJob.java:31` | `@Scheduled(fixedRateString = "${viernes.cache.refresh-ms:600000}")` — refresco cada **10 min** |
| `exception/GlobalExceptionHandler.java` | `shared/exception/GlobalExceptionHandler.java:24` | `@RestControllerAdvice`: traduce excepciones → HTTP |
| `exception/ResourceNotFoundException.java` | `shared/exception/ResourceNotFoundException.java:3` | Excepción de dominio → 404 |
| `exception/RateLimitExceededException.java` | `shared/exception/RateLimitExceededException.java` | Exceso de consultas → 429 |
| `filter/CorrelationIdFilter.java` | `shared/filter/CorrelationIdFilter.java:15` | `X-Trace-ID` ↔ MDC (trazabilidad de logs) |
| `ratelimit/ConsultaDocumentoRateLimiter.java` | `shared/ratelimit/ConsultaDocumentoRateLimiter.java:21` | Rate-limit por usuario para DNI/RUC |

### 8.1 Manejo de errores global (`GlobalExceptionHandler`)

`@RestControllerAdvice` (`shared/exception/GlobalExceptionHandler.java:24`) intercepta excepciones de **cualquier** Controller y produce una respuesta JSON uniforme (`baseBody` con `timestamp/status/error/message`, `:112`). Tabla de mapeo:

| Excepción | HTTP | Ancla |
|---|---|---|
| `ResourceNotFoundException` | 404 | `:27` |
| `MethodArgumentNotValidException` (Bean Validation) | 400 + `fieldErrors` | `:32` |
| `HttpMessageNotReadableException` | 400 | `:44` |
| `MethodArgumentTypeMismatchException` | 400 | `:49` |
| `IllegalArgumentException` | 400 | `:55` |
| `IllegalStateException` | 409 | `:60` |
| `DataIntegrityViolationException` | 409 | `:65` |
| `BadCredentialsException` | 401 | `:71` |
| `DisabledException` | 403 | `:76` |
| `AccessDeniedException` | 403 | `:81` |
| `RateLimitExceededException` | 429 + `Retry-After` | `:86` |
| `NoResourceFoundException` | 404 | `:93` |
| `HttpRequestMethodNotSupportedException` | 405 | `:98` |
| `Exception` (genérica) | 500 | `:103` |

**Valor arquitectónico:** los módulos **no** escriben `try/catch` HTTP; lanzan excepciones de dominio/negocio y el advice central las traduce. Esto mantiene los Controllers limpios y el contrato de error consistente.

### 8.2 Trazabilidad (`CorrelationIdFilter`)

`OncePerRequestFilter` (`shared/filter/CorrelationIdFilter.java:15`) que, por cada petición: lee `X-Trace-ID` (`:24`), genera un UUID si viene vacío (`:26`), lo pone en **MDC** (`:29`) y lo devuelve en la cabecera de respuesta (`:30`), limpiando el MDC en `finally` (`:33`). El frontend **genera** ese UUID por petición (`trace-id-interceptor.ts:8`), de modo que un log del backend y una petición del frontend quedan **correlacionados** por el mismo `traceId`.

### 8.3 Caché (Caffeine) y rate-limit

- **Caché:** `@EnableCaching` + Caffeine (`shared/cache/CacheConfig.java:23`, `:36-38`). El job de refresco corre cada `viernes.cache.refresh-ms = 600000` ms = **10 min** (`application.yml:10`, `CacheRefreshJob.java:31`). Se usa sobre la consulta externa de documentos (`@Cacheable` en `ApisPeruDocumentoAdapter.java:51`) para **no golpear APIsPERU en cada tecla**.
- **Rate-limit:** `ConsultaDocumentoRateLimiter` (`shared/ratelimit/ConsultaDocumentoRateLimiter.java:21`) limita a **20 consultas / 60 s** por usuario (`application.yml:14-15`, `@Value` en `:28-29`), con ventana deslizante Caffeine `expireAfterWrite` (`:34`) y contador atómico (`incrementAndGet() > maxConsultas`, `:40`). Al exceder, lanza `RateLimitExceededException` → **429** con `Retry-After`. Protege la **cuota gratuita de APIsPERU (1.000 consultas/mes)**.

---

## 9. Seguridad: Keycloak OAuth2 Resource Server

El backend es un **OAuth2 Resource Server**: **no** autentica usuarios (eso lo hace Keycloak); **valida** el JWT que Keycloak emitió y aplica autorización por rol. Todo el módulo `auth` está bajo `@Profile("keycloak")` (`auth/keycloak/KeycloakSecurityConfig.java:36`, `KeycloakJwtAuthenticationConverter.java:29`), activo porque `application.yml:5` fija `dev,keycloak`.

### 9.1 Configuración de identidad

| Parámetro | Valor | Ancla |
|---|---|---|
| `issuer-uri` | `http://localhost:8180/realms/viernes-realm` | `application-keycloak.yml:11` |
| `jwk-set-uri` | `…/protocol/openid-connect/certs` | `application-keycloak.yml:12` |
| `provider` | `keycloak` | `application-keycloak.yml:16` |
| `realm` | `viernes-realm` | `application-keycloak.yml:18` |
| `resource` (client) | `viernes-backend` | `application-keycloak.yml:20` |
| `principal-attribute` | `preferred_username` | `application-keycloak.yml:21` |

### 9.2 De JWT a `Authentication` (el converter)

`KeycloakJwtAuthenticationConverter` (`auth/keycloak/KeycloakJwtAuthenticationConverter.java:30`) implementa `Converter<Jwt, AbstractAuthenticationToken>`:

1. Extrae `realm_access.roles` (`:48`), filtra roles internos de Keycloak (`default-roles*`, `offline_access`, `uma_authorization`, `:62`).
2. Los pasa a MAYÚSCULAS y les antepone `ROLE_` (`:64-66`) → `ROLE_ADMIN`, `ROLE_JEFE`, `ROLE_VENDEDOR`…
3. Toma el principal de `preferred_username` (`:39`), con fallback al `subject` (`:41`).
4. Devuelve un `JwtAuthenticationToken` (`:43`).

**Clave:** los roles de negocio se **provisionan en Keycloak**, no en el código. La config solo menciona explícitamente `ADMIN` y `JEFE` en las reglas; cualquier usuario autenticado (incluido `VENDEDOR`) puede leer y crear/editar entidades de negocio salvo donde se restringe.

### 9.3 Filter chain y autorización por rol + verbo

`keycloakSecurityFilterChain` (`auth/keycloak/KeycloakSecurityConfig.java:84`): CORS (`:86`), **CSRF deshabilitado** (`:87`, correcto para API stateless con Bearer), sesión **STATELESS** (`:88`), entry point 401 JSON (`:90`) y access denied 403 JSON (`:102`), `oauth2ResourceServer.jwt` con el converter (`:144-145`).

Reglas `authorizeHttpRequests` (`:115`), **en orden** (la primera que coincide gana):

| Regla | Verbos / rutas | Rol requerido | Ancla |
|---|---|---|---|
| Público | swagger-ui, v3/api-docs, actuator, static, `/`, index.html | *(permitAll)* | `:117-119` |
| Gestión de usuarios | POST/PUT/PATCH/DELETE `/api/v1/usuarios/**` | **ADMIN** | `:122-125` |
| Aprobación de propuestas | PATCH `/propuestas/*/aprobar`, `/rechazar`, `/cancelar` | **ADMIN, JEFE** | `:128-130` |
| Borrado físico de entidades | DELETE clientes, leads, oportunidades, propuestas, actividades | **ADMIN** | `:133` |
| Gestión comercial | POST/PUT/PATCH/DELETE productos, campanias, fuentes, equipos, etiquetas | **ADMIN, JEFE** | `:136-139` |
| Resto | `anyRequest()` | **authenticated** | `:142` |

- `GESTION_COMERCIAL` = productos, campanias, fuentes, equipos, etiquetas (`:43-49`).
- `ENTIDADES_NEGOCIO` = clientes, leads, oportunidades, propuestas, actividades (`:55-61`), cuyo **DELETE** queda reservado a ADMIN.

**Modelo resultante:** lectura y creación/edición de entidades de negocio → cualquier autenticado; **borrado** de entidades → solo ADMIN; **gestión comercial** (catálogo/equipos/etiquetas/campañas/fuentes) → ADMIN o JEFE; **aprobación de propuestas** y **gestión de usuarios** → ADMIN/JEFE y ADMIN respectivamente.

### 9.4 CORS

`corsConfigurationSource` (`:71`) lee `viernes.cors.allowed-origins` con default `http://localhost:4200,http://localhost:8080` (`:72`), permite los verbos GET/POST/PUT/PATCH/DELETE/OPTIONS (`:75`) y todas las cabeceras (`:76`). Solo los orígenes del frontend y del backend están autorizados.

---

## 10. Integración externa: APIsPERU

**Única** salida HTTP del backend hacia un tercero. Vive **exclusivamente** en `clientes/adapters/out/external/` (ningún otro módulo tiene `external`). Cumple la regla de seguridad del proyecto: **la llamada se hace desde el backend, el token nunca llega a Angular**.

### Componentes

| Archivo | Ancla | Rol |
|---|---|---|
| `ApisPeruDocumentoAdapter.java` | `clientes/adapters/out/external/ApisPeruDocumentoAdapter.java:28` | Implementa `DocumentoLookupPort`; usa `RestClient` (`:18`, `:42`) |
| `ApisPeruProperties.java` | `clientes/adapters/out/external/ApisPeruProperties.java:11` | `@ConfigurationProperties(prefix = "viernes.documentos.apisperu")` |
| `dto/ApisPeruDniResponse.java` | `clientes/adapters/out/external/dto/ApisPeruDniResponse.java` | DTO de respuesta DNI |
| `dto/ApisPeruRucResponse.java` | `clientes/adapters/out/external/dto/ApisPeruRucResponse.java` | DTO de respuesta RUC |
| `DocumentoLookupPort.java` | `clientes/application/port/out/DocumentoLookupPort.java` | **Puerto de salida** que el adapter implementa (inversión de dependencia) |

### Configuración y seguridades

- `base-url = https://dniruc.apisperu.com/api/v1` (`application.yml:17`, `ApisPeruProperties.java:16`).
- **Token como variable de entorno:** `token: ${APISPERU_TOKEN:}` (`application.yml:19`); **nunca versionado**. Se carga desde `viernes-backend/.env.local` por el lanzador (`activar-sistema.bat:63-67`) sin imprimir su valor.
- **Token como query param** (exigencia de APIsPERU): `.queryParam("token", properties.getToken())` (`ApisPeruDocumentoAdapter.java:89`, `:128`). Se cuida que **no aparezca en logs**.
- **Timeout 5 s** (`application.yml:20`, `ApisPeruProperties.java:18`), aplicado a connect y read (`ApisPeruDocumentoAdapter.java:38-40`).
- **Caché:** `@Cacheable` (`:51`) para no consultar en cada tecla y preservar la cuota.
- **Rate-limit:** `ConsultaDocumentoRateLimiter` (20/60 s, §8.3) antes de llamar.
- **Errores:** `RestClientResponseException` se traduce a mensaje amigable (`mensajeError`, `:172`); 401 de APIsPERU → "Verifique el token configurado" (`:175`).

**Lectura arquitectónica:** es el ejemplo perfecto del **puerto de salida hexagonal**: `application` define `DocumentoLookupPort` (qué necesita el negocio), `adapters/out/external` lo implementa con `RestClient` (cómo se consigue). El Service de clientes no sabe que existe APIsPERU ni HTTP; solo conoce el puerto. Cambiar de proveedor = escribir otro adapter, sin tocar el negocio.

---

## 11. Transacciones

### Dónde viven

La transaccionalidad se declara en la **capa de aplicación (Service)**, no en el Controller ni en el adapter. **13 de los 14 Services** llevan `@Transactional` a nivel de clase (p. ej. `actividades/application/service/ActividadService.java:36`). El Service restante (agregación de solo lectura) no la necesita o usa `readOnly`.

### Qué compone una transacción (5 piezas)

1. **`@Transactional` en el Service** — Spring crea un **proxy AOP** que envuelve cada método público: abre la transacción **antes**, hace **commit** al salir normalmente, **rollback** ante `RuntimeException`.
2. **`JpaTransactionManager`** (autoconfigurado por `spring-boot-starter-data-jpa`, `pom.xml:49`) — gestor que coordina el `EntityManager` y la `Connection` JDBC.
3. **Una `Connection` JDBC por transacción** — tomada del pool (HikariCP) hacia Oracle; toda la tx usa la misma conexión.
4. **`EntityManager` / contexto de persistencia** — Hibernate acumula los cambios (dirty checking) y los envía a Oracle en el `flush`/commit.
5. **Propagación `REQUIRED` (default)** — si un Service llama a otro adapter/Service dentro de la misma tx, **se unen a la transacción existente** (no abren una nueva). Por eso una operación que cruza módulos (p. ej. crear actividad + sincronizar etiquetas + validar participantes) es **atómica**.

### Matices verificados

- **`readOnly = true`** en consultas: optimiza (sin dirty checking ni flush) y documenta intención. En `ActividadService`, 10 métodos son `readOnly` y 6 de escritura.
- **`flush()` explícito** en delete+insert: `ActividadEtiquetaPersistenceAdapter.reemplazarEtiquetas` hace `delete` (`:44`) → `flush()` (`:45`) → `saveAll` (`:62`) para evitar el choque de **restricción única** (Oracle ve el DELETE antes que el INSERT dentro de la misma tx).
- **Rollback automático** ante `RuntimeException` (incluidas las de dominio como `ResourceNotFoundException`); las checked exceptions no revierten por defecto.
- **Sin transacciones distribuidas:** al ser un solo `DataSource`/proceso, no hay XA ni saga. La atomicidad multi-módulo se logra por **invocación in-process dentro de la misma tx** (§7).

---

## 12. Frontend Angular

SPA **Angular 22** con **componentes standalone** y **signals**. Estructura de `src/app/`:

```
app/
├── core/                    ← singleton transversal
│   ├── auth/                ← auth.service, guards, keycloak/ (initializer, config, service)
│   ├── interceptors/        ← trace-id-interceptor, jwt.interceptor
│   ├── layout/              ← shell (menú, contenedor de rutas hijas)
│   └── services/            ← api-service (buildUrl)
├── features/                ← 14 features (una por módulo de negocio + auth/configuracion)
│   ├── actividades/  auth/  clientes/  configuracion/  dashboard/
│   ├── equipos/  etiquetas/  leads/  mensajeria/  oportunidades/
│   ├── productos/  propuestas/  relationship-map/  usuarios/
└── shared/                  ← table-sort.ts, styles/entity-dialog.css
```

### 12.1 Arranque

`main.ts:5` → `bootstrapApplication(App, appConfig)`. `app.config.ts` provee:

- `provideKeycloak()` (`app.config.ts:17`) — inicializa keycloak-js antes de renderizar.
- `provideRouter(routes)` (`:18`).
- `provideHttpClient(withInterceptors([traceIdInterceptor, jwtInterceptor]))` (`:19`) — **cadena de interceptores en este orden**.

### 12.2 Cadena de interceptores (cada petición HTTP)

1. **`traceIdInterceptor`** (`core/interceptors/trace-id-interceptor.ts:3`): genera `crypto.randomUUID()` (`:8`) y añade `X-Trace-ID` (`:9`), **excepto** a Keycloak OIDC / `:8180` (`:5`). Ese UUID es el que el `CorrelationIdFilter` del backend lee (§8.2).
2. **`jwtInterceptor`** (`core/interceptors/jwt.interceptor.ts:24`): añade `Authorization: Bearer <token>` (`:31`) salvo a OIDC/auth (`:29`); aplica **timeout de 15 s** (`REQUEST_TIMEOUT_MS = 15000`, `:22`, `:36`); ante **401** intenta **refresh + retry una sola vez** (`refreshAndRetry`, `:43`, `:59`); si el retry vuelve a 401 o no hay token nuevo, **cierra sesión** contra Keycloak (`closeSession`, `:97`) y navega a `/login`. Ante **403** muestra alerta (`:45`). Construye `friendlyMessage` (`buildFriendlyMessage`, `:110`) para la UI.

### 12.3 Guards y rutas

- `authGuard` (`core/auth/auth.guard.ts`) protege el shell: la ruta raíz `''` carga `Layout` con `canActivate: [authGuard]` (`app.routes.ts:11-13`) y redirige a `dashboard` (`:15`). `login` es pública (`:7`).
- `roleGuard` (`core/auth/role.guard.ts`) restringe por rol (p. ej. edición de productos requiere `ADMIN/JEFE`, `app.routes.ts:88`).
- Rutas lazy (`loadComponent`) por feature; `actividades` (`:91`), `actividades/nuevo` con `data.abrirNuevaActividad` (`:97`), etc.

### 12.4 De la feature al backend

Cada feature tiene un `.service.ts` que usa `HttpClient` y construye la URL con `ApiService.buildUrl` (`core/services/api-service.ts:8`), que antepone `environment.apiBaseUrl = http://localhost:8080/api/v1` (`environment.ts:2`, `api-service.ts:6`). **No hay imports de métodos del backend**: el acoplamiento es **por URL/HTTP** (`@RequestMapping` del Controller), no por firma Java. Ejemplo: `actividades.service.ts` → `POST /api/v1/actividades` → `ActividadController.crear`.

---

## 13. Flujo end-to-end de una petición

Recorrido completo **ida y vuelta** de una operación representativa (crear una actividad), uniendo frontend, seguridad, hexagonalidad, transacción y persistencia:

```
[1] UI: usuario pulsa "Guardar" en actividad-form (feature Angular)
      │  signal → servicio
[2] actividades.service.ts → HttpClient.post(buildUrl('/actividades'), body)
      │
[3] traceIdInterceptor: añade X-Trace-ID = UUID            (trace-id-interceptor.ts:8-9)
[4] jwtInterceptor: añade Authorization: Bearer <JWT>      (jwt.interceptor.ts:31)
      │  HTTP POST → http://localhost:8080/api/v1/actividades
      ▼
[5] Tomcat / Spring Security filter chain                  (KeycloakSecurityConfig.java:84)
      ├─ CorrelationIdFilter: X-Trace-ID → MDC             (CorrelationIdFilter.java:29)
      ├─ Valida JWT contra JWKS de Keycloak                (application-keycloak.yml:12)
      ├─ KeycloakJwtAuthenticationConverter: roles → ROLE_ (KeycloakJwtAuthenticationConverter.java:64)
      └─ authorizeHttpRequests: authenticated (POST no restringido) (:142)
      ▼
[6] ActividadController.crear (@PostMapping, @Valid)       (ActividadController.java:27)
      │  DTO Request validado (Bean Validation)
[7] ManageActividadUseCase.crear (port/in)                 (ManageActividadUseCase.java:9)
      ▼
[8] ActividadService.crear  ── @Transactional AOP abre tx  (ActividadService.java:63, :36)
      ├─ ActividadPersistencePort.save (port/out)
      │     └─ ActividadPersistenceAdapter → Repository → ActividadEntity → Oracle CRM_ACTIVIDADES
      ├─ sincronizarEtiquetas → EtiquetaPersistencePort (módulo etiquetas, OPEN)
      │     └─ delete + flush + saveAll (CRM_ACTIVIDAD_ETIQUETAS)
      ├─ validarRelaciones → ContactoQueryPort / UsuariosApi (clientes, usuarios)
      └─ toResponse (DTO)
      ▼
[9] commit de la tx (JpaTransactionManager) → una sola Connection JDBC
      ▼
[10] Controller devuelve 201 + ActividadResponse (JSON)
      │  (si algo lanza RuntimeException → rollback + GlobalExceptionHandler → HTTP error)
      ▼
[11] jwtInterceptor: timeout/errores; traceId ya correlacionado
[12] Feature actualiza signals → UI re-renderiza
```

**Puntos clave:** la **transacción** se abre en [8] y cierra en [9]; los cruces a otros módulos (etiquetas, clientes, usuarios) ocurren **dentro** de la misma tx (in-process, §7/§11); el **traceId** viaja de [3] a [5] y correlaciona logs; la **seguridad** se resuelve antes de tocar el Controller ([5]); el **manejo de errores** es centralizado (§8.1).

---

## 14. Arranque ≠ petición

Una confusión común en la sustentación: **`main()` no es la cadena de llamadas de negocio**.

- **Arranque (una vez):** `ViernesApplication.main` (`ViernesApplication.java:9`) → `SpringApplication.run` (`:10`) construye el **contexto de Spring**, escanea los módulos, cablea beans (Controllers, Services, Adapters, puertos), configura el `DataSource`, la seguridad y el caché, y arranca **Tomcat en :8080**. Luego **espera**. El frontend arranca aparte con `bootstrapApplication` (`main.ts:5`).
- **Petición (cada vez):** cada HTTP entrante dispara **su propia** cadena ida-y-vuelta (§13). No hay una única cadena que empiece en `main()` y recorra todo el sistema; hay **N cadenas**, una por petición, que entran por el Controller del módulo correspondiente.

**Analogía docente:** `main()` es "abrir el edificio y encender las luces"; cada petición es "un visitante que entra por una puerta concreta (Controller) y recorre un pasillo concreto (caso de uso) hasta volver a salir". El edificio (contexto Spring) es compartido; los recorridos (peticiones/transacciones) son independientes.

---

## 15. Infraestructura y activación

### 15.1 Contenedores (Docker Compose)

| Servicio | Archivo | Imagen / contenedor | Puerto | Ancla |
|---|---|---|---|---|
| Oracle 23c | `viernes-backend/compose-dev.yml` | `gvenzl/oracle-free:23-slim` / `bomerp-oracle` | 1521 | `compose-dev.yml:5-9` |
| Keycloak 25 | `compose-keycloak.yml` | `quay.io/keycloak/keycloak:25.0.6` / `viernes-keycloak` | 8180→8080 | `compose-keycloak.yml:23-41` |
| PostgreSQL (BD de Keycloak) | `compose-keycloak.yml` | `postgres:16-alpine` / `viernes-keycloak-db` | interno | `compose-keycloak.yml:5-6` |

> El Oracle de negocio es `BOM_VENTAS` sobre `FREEPDB1` (`application-dev.yml:6-7`); el PostgreSQL es **solo** para Keycloak, no para el CRM. Los compose de desarrollo traen credenciales hardcodeadas (brecha, §17).

### 15.2 Lanzador `activar-sistema.bat` (5 pasos)

| Paso | Acción | Ancla |
|---|---|---|
| [0/5] | Localiza Java 21 (portable `.tools/jdk21` o del sistema) | `activar-sistema.bat:18` |
| [1/5] | Verifica Docker Desktop instalado y arrancado | `:26` |
| [2/5] | `docker compose -f viernes-backend\compose-dev.yml up -d` (Oracle) | `:46-47` |
| [3/5] | `docker compose -f compose-keycloak.yml up -d` (Keycloak + PostgreSQL) | `:49-50` |
| [4/5] | Carga `.env.local` (secretos, sin imprimir) y arranca backend: `mvnw.cmd spring-boot:run` | `:63-67`, `:73-74` |
| [5/5] | `npm.cmd install` (si hace falta) y `npm.cmd start` (Angular :4200) | `:79-85` |

Al final abre `http://localhost:4200` (`:104`) y lista las URLs de frontend, Swagger (`:8080/swagger-ui.html`) y Keycloak (`:8180`) (`:93-95`).

**Notas de tooling (restricciones del proyecto):** no hay `mvn` global → usar `.\mvnw.cmd` desde `viernes-backend`; en PowerShell `npm` falla por política de scripts → usar `npm.cmd`. El lanzador ya usa `mvnw.cmd` (`:74`) y `npm.cmd` (`:82`, `:85`).

### 15.3 Perfiles

`application.yml:5` activa `dev,keycloak`. `application-dev.yml` aporta el datasource Oracle y JPA; `application-keycloak.yml` aporta issuer/JWKS y el provider. El código de seguridad está bajo `@Profile("keycloak")` (§9), así que sin ese perfil no hay filter chain Keycloak.

---

## 16. Pruebas y verificación de modularidad

- **`ModularityTests`** (`ModularityTests.java:6`): `ApplicationModules.of(ViernesApplication.class)` (`:8`) detecta los módulos; `modules.verify()` (`:12`) **falla el build si se viola un límite** (dependencia no permitida, acceso a interno de un CLOSED, ciclo). Un segundo test usa `Documenter.writeDocumentation()` (`:17-18`) para generar documentación del grafo.
- **291 `@Test`** en **32 clases** de prueba.Patrón por módulo (ejemplo actividades, 34 tests): Service con **mocks de puertos** (sin BD), Controller con **MockMvc standalone** + `GlobalExceptionHandler`, Adapter con `ArgumentCaptor`. La inversión de dependencia hexagonal (§5) es lo que permite testear application sin infraestructura.
- **Actuator + Modulith:** `spring-modulith-actuator` (`pom.xml:83`) expone el grafo de módulos en runtime; Micrometer/Prometheus (`pom.xml:119`) las métricas.

**Valor para la sustentación:** `verify()` es la **prueba ejecutable de la arquitectura**: no es un diagrama en un documento, es un test que rompe si alguien acopla de más. Es el argumento más fuerte de que el monolito modular está **gobernado**, no solo dibujado.

---

## 17. Análisis de brechas sistémicas

Brechas **verificadas** (no son opiniones; cada una tiene ancla). Ordenadas por impacto:

| # | Brecha | Evidencia | Impacto | Severidad |
|---|---|---|---|---|
| B1 | **MapStruct declarado pero sin uso** (0 `@Mapper`); directorios `mapper/` vacíos en clientes/leads/oportunidades | `pom.xml:114-115`, `pom.xml:190-191`; `grep @Mapper` = 0; `find -name mapper` | Boilerplate de mapeo manual domain↔dto↔entity; intención de diseño no consumada | Media |
| B2 | **Módulos OPEN con acoplamiento interno** (Patrón C): consumidores tocan repos/DTOs internos de otros módulos | `actividades/package-info.java:2` (OPEN) + dependencias amplias; `relationshipmap/package-info.java:3` (8 módulos) | Un cambio interno en el proveedor puede romper consumidores; `verify()` no lo impide en OPEN | Media |
| B3 | **`shared` sin `package-info` raíz** | `ls shared/package-info.java` → no existe | `shared` no es un módulo Modulith formal; solo sus hijos `exception`/`ratelimit` lo son. Ambigüedad de frontera | Baja |
| B4 | **`dashboard` sin `domain` y con `service/` raíz vacío** | `find dashboard` (solo dto/port/service/adapters); dir `/service` sin archivos | Residuo estructural; no rompe, pero confunde la anatomía hexagonal | Baja |
| B5 | **Credenciales de desarrollo hardcodeadas en compose** | `compose-dev.yml:11-13`, `compose-keycloak.yml:10-11`, `:32-33` | Aceptable en dev local, **nunca** en producción; riesgo si se reutiliza el compose | Media |
| B6 | **README raíz divergente** describe el dominio LP2 (Producto/Venta/Compra), no los 14 módulos del CRM | `README.md:1-15` ("LP2", "Venta–DetalleVenta", "Compra–DetalleCompra") | Documentación engañosa para quien entra al repo | Media |
| B7 | **`ddl-auto: update`** en dev | `application-dev.yml:13` | Hibernate altera el esquema automáticamente; peligroso si se lleva a prod (migraciones no controladas) | Media |
| B8 | **Sin paginación / filtros combinables** en listados (lectura client-side) | `actividad-list.ts` (filtra en cliente); patrón repetido en features | Escala mal con volumen; no es brecha de arquitectura sino de diseño de consulta | Baja |
| B9 | **`ContactoQueryAdapter` consulta ineficiente** (flatMap sobre `listar()`); patrón **duplicado** en actividades y oportunidades | `actividades/adapters/out/persistence/ContactoQueryAdapter.java:30-34`; `oportunidades/adapters/out/persistence/ContactoQueryAdapter.java` | Trae todos los clientes+contactos para resolver un solo id (`listar().stream().flatMap…filter.findFirst`) | Baja |
| B10 | **Cuota APIsPERU sin tope diario global** (solo rate-limit 20/60 s + caché) | `shared/ratelimit/ConsultaDocumentoRateLimiter.java:28-29`; free = 1.000/mes | Un mes de uso intenso podría agotar la cuota gratuita antes de fin de mes | Baja |

> Las brechas B1–B10 son **sistémicas** (afectan a varios módulos o a la infraestructura). El dossier de actividades (`informe_modulo_actividades_v1.md`, §18) lista las 18 brechas **específicas de ese módulo**. No se revierte ni se corrige nada sin autorización expresa (hay ~221 cambios preexistentes sin commit).

---

## 18. Inventario cuantitativo

### Backend (main) por módulo

| Módulo | Archivos `.java` | Controller | Service | Tipo Modulith |
|---|---|---|---|---|
| actividades | 29 | 1 | 1 | OPEN |
| auth | 4 | 0 | 0 | CLOSED |
| clientes | 24 | 1 | 1 | OPEN |
| dashboard | 11 | 1 | 1 | OPEN |
| equipos | 21 | 1 | 1 | CLOSED |
| etiquetas | 11 | 1 | 1 | OPEN |
| leads | 22 | 2 | 2 | OPEN |
| marketing | 19 | 1 | 1 | OPEN |
| mensajeria | 12 | 1 | 1 | CLOSED |
| oportunidades | 40 | 1 | 1 | OPEN |
| productos | 12 | 1 | 1 | OPEN |
| propuestas | 18 | 1 | 1 | OPEN |
| relationshipmap | 9 | 1 | 1 | OPEN |
| usuarios | 16 | 1 | 1 | CLOSED |
| shared | 10 | 0 | 0 | contenedor |
| **Raíz** (`ViernesApplication`) | 1 | — | — | — |
| **TOTAL** | **259** | **14** | **14** | 14 módulos + shared |

### Recuentos globales verificados

- **259** archivos `.java` en `src/main/java` (258 en módulos + 1 raíz).
- **14** Controllers REST en **13** módulos (auth no expone REST; leads tiene 2).
- **14** Services en `application/service`; **13** con `@Transactional`.
- **14** interfaces `*UseCase` en `application/port/in`.
- **1** named interface de módulo CLOSED: `UsuariosApi` (`usuarios/UsuariosApi.java:10`).
- **1** adapter `out/external` (APIsPERU, en clientes).
- **32** clases de test, **291** `@Test`.
- **10 OPEN + 4 CLOSED** = 14 módulos; **2** submódulos OPEN en `shared` (exception, ratelimit).
- **0** `@Mapper` (MapStruct sin uso).

### Frontend

- **14** features en `src/app/features/`.
- **2** interceptores (`traceId`, `jwt`), **2** guards (`auth`, `role`).
- **1** `ApiService.buildUrl` + `environment.apiBaseUrl`.
- Angular **22**, TypeScript **~6.0.2**, rxjs **~7.8.0**, keycloak-js **^26.2.4** (`package.json:17`, `:34`, `:24`, `:23`).

---

## 19. Cómo leer el sistema

**Ruta de navegación recomendada para la sustentación:**

1. **Empezar por el estilo:** §1 (monolito modular + hexagonal) y §3 (topología). Dejar claro que **no** son microservicios.
2. **Mostrar la gobernanza:** §4 (tabla OPEN/CLOSED + `allowedDependencies`) y §16 (`ModularityTests.verify()`). La arquitectura es **ejecutable**, no solo un diagrama.
3. **Bajar a un módulo:** §5 (anatomía hexagonal) y el dossier completo de **actividades** (`informe_modulo_actividades_v1.md`) como ejemplo canónico de las 4 capas.
4. **Explicar la comunicación:** §7 (3 patrones) y §11 (transacción in-process multi-módulo).
5. **Cruces transversales:** §8 (`shared`: errores, traceId, caché, rate-limit), §9 (Keycloak), §10 (APIsPERU como puerto de salida).
6. **Cerrar con el flujo:** §13 (ida y vuelta completa) y §14 (arranque ≠ petición).
7. **Ser honesto con las brechas:** §17. Reconocer MapStruct sin uso, acoplamiento OPEN y README divergente demuestra **comprensión crítica**, no debilidad.

**Mapa de archivos ancla (por dónde entrar al código):**

| Quiero entender… | Archivo ancla |
|---|---|
| El arranque del backend | `ViernesApplication.java:9` |
| La gobernanza de módulos | `ModularityTests.java:12` + cada `package-info.java` |
| La seguridad | `auth/keycloak/KeycloakSecurityConfig.java:84` |
| Los errores globales | `shared/exception/GlobalExceptionHandler.java:24` |
| La trazabilidad | `shared/filter/CorrelationIdFilter.java:21` |
| La integración externa | `clientes/adapters/out/external/ApisPeruDocumentoAdapter.java:28` |
| El flujo de una petición | §13 + `actividades/adapters/in/web/ActividadController.java:27` |
| El frontend | `main.ts:5`, `app.config.ts:19`, `core/interceptors/jwt.interceptor.ts:24` |
| La activación | `activar-sistema.bat:46-85` |

---

> **Nota de validación:** este informe se verificó de forma **estructural y textual** (anclas `archivo:línea` contrastadas con `grep`/`find` sobre el código real; recuentos de archivos, controllers, services, tests y `@Test` medidos; clasificación OPEN/CLOSED leída de cada `package-info.java`). **No** se validó por captura de imagen. Las afirmaciones reflejan el estado del árbol `E:\Viernes\Viernes` a fecha 2026-11-08, con ~221 cambios preexistentes sin commit que **no** se revirtieron ni commitearon.