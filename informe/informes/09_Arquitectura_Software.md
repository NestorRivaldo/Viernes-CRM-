# CAPÍTULO 09 — ARQUITECTURA DE SOFTWARE

## 9.1 Introducción

Este capítulo documenta la arquitectura vigente de Viernes CRM V1, verificada directamente desde el código fuente, la configuración Maven/Angular y los archivos de infraestructura. Se contrasta con los diagramas existentes y se señalan las discrepancias cuando corresponde.

---

## 9.2 Stack Tecnológico

### 9.2.1 Backend

| Tecnología | Versión | Artefacto Maven |
|---|---|---|
| Java | 21 | `<java.version>21</java.version>` |
| Spring Boot | 4.0.7 | `spring-boot-starter-parent:4.0.7` |
| Spring Modulith | 2.0.7 | `spring-modulith-bom:2.0.7` |
| Spring Data JPA | (heredada de Boot) | `spring-boot-starter-data-jpa` |
| Spring Security | (heredada de Boot) | `spring-boot-starter-security` |
| Spring OAuth2 Resource Server | (heredada de Boot) | `spring-boot-starter-oauth2-resource-server` |
| Oracle JDBC | (heredada) | `ojdbc11` |
| Lombok | (heredada de Boot) | `lombok` |
| MapStruct | 1.6.3 | `mapstruct` + `mapstruct-processor` |
| Caffeine Cache | (heredada de Boot) | `caffeine` |
| SpringDoc OpenAPI | 3.0.2 | `springdoc-openapi-starter-webmvc-ui` |
| Micrometer Prometheus | (heredada de Boot) | `micrometer-registry-prometheus` |
| Spring Boot Actuator | (heredada de Boot) | `spring-boot-starter-actuator` |

### 9.2.2 Frontend

| Tecnología | Versión |
|---|---|
| Angular | 22.1.x |
| Angular Material + CDK | 22.2.x |
| TypeScript | 6.0.2 |
| keycloak-js | 26.2.4 |
| Cytoscape | 3.34.4 |
| RxJS | 7.8.x |
| Vitest | 4.0.8 |

### 9.2.3 Infraestructura

| Componente | Tecnología | Puerto |
|---|---|---|
| Base de datos | Oracle Database Free (Docker) | 1521 (FREEPDB1) |
| Proveedor de identidad | Keycloak 25.0.6 (Docker) | 8180 |
| BD de Keycloak | PostgreSQL 16 Alpine (Docker) | 5433 |
| Observabilidad | Grafana + Prometheus + Promtail + Loki | Definidos en `obs/compose.yml` |

---

## 9.3 Monolito Modular

### 9.3.1 Definición

Viernes CRM V1 se implementa como un **monolito modular**: una sola unidad de despliegue (`viernes-backend-0.0.1-SNAPSHOT.jar`) que organiza internamente el código en módulos con límites bien definidos y dependencias controladas.

### 9.3.2 Módulos del sistema

El punto de entrada `ViernesApplication.java` con `@SpringBootApplication` escanea el paquete raíz `com.viernes`, y Spring Modulith identifica automáticamente los módulos del primer nivel:

| # | Módulo | Tipo | Dependencias declaradas |
|---|---|---|---|
| 1 | `actividades` | OPEN | clientes, oportunidades, leads, etiquetas, usuarios, shared |
| 2 | `auth` | CLOSED | usuarios |
| 3 | `clientes` | OPEN | (sin restricciones) |
| 4 | `dashboard` | OPEN | (sin restricciones) |
| 5 | `equipos` | CLOSED | usuarios, shared |
| 6 | `etiquetas` | OPEN | (sin restricciones) |
| 7 | `leads` | OPEN | (sin restricciones) |
| 8 | `marketing` | OPEN | (sin restricciones) |
| 9 | `mensajeria` | CLOSED | usuarios, shared |
| 10 | `oportunidades` | OPEN | (sin restricciones) |
| 11 | `productos` | OPEN | (sin restricciones) |
| 12 | `propuestas` | OPEN | productos, clientes, oportunidades, shared |
| 13 | `relationshipmap` | OPEN | clientes, oportunidades, actividades, propuestas, productos, usuarios, shared |
| 14 | `shared.exception` | OPEN | (submódulo) |
| 15 | `shared.ratelimit` | OPEN | (submódulo) |
| 16 | `usuarios` | CLOSED | shared |

### 9.3.3 Verificación con Spring Modulith

La clase `ModularityTests.java` verifica en tiempo de compilación que las dependencias entre módulos respetan las declaraciones:

```java
class ModularityTests {
    ApplicationModules modules = ApplicationModules.of(ViernesApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();  // Falla si hay accesos no declarados
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(modules).writeDocumentation();  // Genera documentación
    }
}
```

Las dependencias se declaran en `package-info.java` de cada módulo utilizando `@ApplicationModule`:
- `Type.OPEN`: permite acceso desde cualquier módulo.
- `Type.CLOSED` (por defecto): solo permite acceso desde módulos listados en `allowedDependencies`.

---

## 9.4 Arquitectura Hexagonal

### 9.4.1 Estructura por módulo

Cada módulo del backend sigue la estructura de Arquitectura Hexagonal (Ports & Adapters), separando el dominio de la infraestructura:

```
com.viernes.<modulo>/
├── domain/                              ← Entidades y Value Objects puros
├── application/
│   ├── dto/                             ← Objetos de transferencia (Request/Response)
│   ├── port/
│   │   ├── in/                          ← Puertos de entrada (Use Cases)
│   │   └── out/                         ← Puertos de salida (Persistence Ports)
│   └── service/                         ← Servicios de aplicación (orquestación)
├── adapters/
│   ├── in/
│   │   └── web/                         ← Controladores REST (adaptadores primarios)
│   └── out/
│       ├── persistence/
│       │   ├── entity/                  ← Entidades JPA
│       │   └── repository/             ← Repositorios Spring Data
│       └── external/                   ← Adaptadores a servicios externos (solo en clientes)
└── package-info.java                    ← Declaración de módulo Spring Modulith
```

### 9.4.2 Evidencia por módulo

| Módulo | Controller | UseCase (Port In) | Service | PersistencePort (Port Out) | PersistenceAdapter |
|---|---|---|---|---|---|
| actividades | `ActividadController` | `ManageActividadUseCase` | `ActividadService` | `ActividadPersistencePort` + 4 más | `ActividadPersistenceAdapter` + 4 más |
| clientes | `ClienteController` | `ManageClienteUseCase` | `ClienteService` | `ClientePersistencePort`, `DocumentoLookupPort` | `ClientePersistenceAdapter`, `ApisPeruDocumentoAdapter` |
| oportunidades | `OportunidadController` | `ManageOportunidadUseCase` | `OportunidadService` | `OportunidadPersistencePort` + 3 más | Correspondientes adapters |
| propuestas | `PropuestaController` | `ManagePropuestaUseCase` | `PropuestaService` | `PropuestaPersistencePort`, `PropuestaRelacionValidationPort` | Correspondientes adapters |
| leads | `LeadController`, `FuenteController` | `ManageLeadUseCase`, `ManageFuenteUseCase` | `LeadService`, `FuenteService` | `LeadPersistencePort`, `FuentePersistencePort` | Correspondientes adapters |
| marketing | `CampaniaController` | `ManageCampaniaUseCase` | `CampaniaService` | `CampaniaPersistencePort`, `CampaniaLeadPersistencePort` | Correspondientes adapters |
| productos | `ProductoController` | `ManageProductoUseCase` | `ProductoService` | `ProductoPersistencePort` | `ProductoPersistenceAdapter` |
| etiquetas | `EtiquetaController` | `ManageEtiquetaUseCase` | `EtiquetaService` | `EtiquetaPersistencePort` | `EtiquetaPersistenceAdapter` |
| equipos | `EquipoController` | `ManageEquipoUseCase` | `EquipoService` | `EquipoPersistencePort`, `EquipoMiembroPersistencePort` | Correspondientes adapters |
| usuarios | `UsuarioController` | `ManageUsuarioUseCase` | `UsuarioService` | `UsuarioPersistencePort` | `UsuarioPersistenceAdapter` |
| mensajeria | `MensajeController` | `ManageMensajeUseCase` | `MensajeService` | `MensajePersistencePort` | `MensajePersistenceAdapter` |
| dashboard | `DashboardController` | `DashboardUseCase` | `DashboardService` | (consulta directa a repositorios de otros módulos) | — |
| relationshipmap | `RelationshipMapController` | `ManageRelationshipMapUseCase` | `RelationshipMapService` | (consulta mediante puertos de otros módulos) | — |

### 9.4.3 Flujo de una petición HTTP

```
HTTP Request
    │
    ▼
┌─────────────┐      ┌──────────────┐      ┌────────────────┐      ┌──────────────────┐
│  Controller │─────▶│ UseCase Port │─────▶│ Service (impl) │─────▶│ PersistencePort  │
│  (adapter)  │      │   (port in)  │      │ (orchestrator) │      │   (port out)     │
└─────────────┘      └──────────────┘      └────────────────┘      └──────────────────┘
                                                                            │
                                                                            ▼
                                                                   ┌──────────────────┐
                                                                   │PersistenceAdapter│
                                                                   │  (adapter out)   │
                                                                   └──────────────────┘
                                                                            │
                                                                            ▼
                                                                   ┌──────────────────┐
                                                                   │  Oracle Database │
                                                                   └──────────────────┘
```

---

## 9.5 Spring Boot y sus Responsabilidades

### 9.5.1 Perfiles de configuración

| Perfil | Archivo | Propósito |
|---|---|---|
| `dev` | `application-dev.yml` | Conexión Oracle local, ddl-auto:update, logging DEBUG |
| `keycloak` | `application-keycloak.yml` | Configuración OAuth2 Resource Server con Keycloak |
| Base | `application.yml` | Perfiles activos (`dev,keycloak`), configuración de caché y rate limiting |

### 9.5.2 Responsabilidades del framework

| Responsabilidad | Mecanismo | Configuración |
|---|---|---|
| Inyección de dependencias | Spring IoC + `@Service`, `@Component`, `@Configuration` | Automático |
| Persistencia | Spring Data JPA + Hibernate + Oracle | `application-dev.yml` |
| Seguridad | Spring Security + OAuth2 Resource Server | `KeycloakSecurityConfig.java` |
| Modularidad | Spring Modulith + `@ApplicationModule` | `package-info.java` (16 archivos) |
| Caché | Spring Cache + Caffeine | `CacheConfig.java` |
| API REST | Spring Web MVC | `@RestController` + `@RequestMapping` |
| Documentación API | SpringDoc OpenAPI 3 | `OpenApiConfig.java` |
| Métricas | Actuator + Micrometer Prometheus | `application-dev.yml` |
| Logs | SLF4J + Logback con Correlation ID | `logback-spring.xml` + `CorrelationIdFilter.java` |
| Validación | Spring Validation (Bean Validation) | `@Valid` en controllers |
| Transacciones | `@Transactional` | Servicios de aplicación |

---

## 9.6 Oracle y Persistencia

### 9.6.1 Estrategia de persistencia

- **ORM**: Hibernate vía Spring Data JPA.
- **DDL**: `ddl-auto: update` — Hibernate genera y actualiza el esquema automáticamente.
- **Esquema Oracle**: `BOM_VENTAS` en `FREEPDB1` (Oracle Free).
- **Driver**: `ojdbc11` (Oracle JDBC Thin).

### 9.6.2 Convenciones de nombrado

Todas las tablas siguen el prefijo `CRM_`:

| Tabla Oracle | Módulo | Entidad JPA |
|---|---|---|
| `CRM_CLIENTES` | Clientes | `ClienteEntity` |
| `CRM_CONTACTOS` | Clientes | `ContactoEntity` |
| `CRM_OPORTUNIDADES` | Oportunidades | `OportunidadEntity` |
| `CRM_OPORTUNIDAD_CONTACTOS` | Oportunidades | `OportunidadContactoEntity` |
| `CRM_OPORTUNIDAD_PRODUCTOS` | Oportunidades | `OportunidadProductoEntity` |
| `CRM_OPORTUNIDAD_COLABORADORES` | Oportunidades | `OportunidadColaboradorEntity` |
| `CRM_PROPUESTAS` | Propuestas | `PropuestaEntity` |
| `CRM_PROPUESTA_DETALLES` | Propuestas | `DetallePropuestaEntity` |
| `CRM_ACTIVIDADES` | Actividades | `ActividadEntity` |
| `CRM_ACTIVIDAD_PARTICIPANTES` | Actividades | `ActividadParticipanteEntity` |
| `CRM_ACTIVIDAD_ETIQUETAS` | Actividades | `ActividadEtiquetaEntity` |
| `CRM_LEADS` | Leads | `LeadEntity` |
| `CRM_FUENTES` | Leads | `FuenteEntity` |
| `CRM_CAMPANIAS` | Marketing | `CampaniaEntity` |
| `CRM_CAMPANIA_LEADS` | Marketing | `CampaniaLeadEntity` |
| `CRM_PRODUCTOS` | Productos | `ProductoEntity` |
| `CRM_ETIQUETAS` | Etiquetas | `EtiquetaEntity` |
| `CRM_EQUIPOS` | Equipos | `EquipoEntity` |
| `CRM_EQUIPO_MIEMBROS` | Equipos | `EquipoMiembroEntity` |
| `CRM_USUARIOS` | Usuarios | `UsuarioEntity` |
| `CRM_MENSAJES` | Mensajería | `MensajeEntity` |

### 9.6.3 Separación dominio vs. persistencia

Cada módulo mantiene entidades de dominio separadas de las entidades JPA:
- **Dominio**: `com.viernes.<modulo>.domain.<Entidad>` — POJOs sin anotaciones de persistencia.
- **JPA**: `com.viernes.<modulo>.adapters.out.persistence.entity.<Entidad>Entity` — con anotaciones `@Entity`, `@Table`, `@Column`, etc.
- **Conversión**: Los adaptadores de persistencia implementan métodos `toDomain()` y `toEntity()` para la conversión bidireccional.

---

## 9.7 Keycloak y Seguridad

### 9.7.1 Arquitectura de autenticación

```
┌──────────┐     Authorization Code     ┌───────────┐
│  Angular │ ─────── + PKCE S256 ──────▶│ Keycloak  │
│  (SPA)   │ ◀────── JWT tokens ────────│ 25.0.6    │
└──────────┘                             └───────────┘
     │                                        │
     │ Bearer JWT                              │ JWK Set
     ▼                                        ▼
┌──────────────┐                    ┌─────────────────────┐
│ Spring Boot  │ ──── valida ─────▶│ issuer-uri:          │
│ (Resource    │     JWT signature  │ /realms/viernes-realm│
│  Server)     │                    └─────────────────────┘
└──────────────┘
```

### 9.7.2 Componentes de seguridad

**Backend:**
- `KeycloakSecurityConfig`: Cadena de seguridad con autorizaciones por rol y endpoint.
- `KeycloakJwtAuthenticationConverter`: Traduce claims JWT → Spring Security authorities.
- `PasswordEncoderConfig`: BCrypt(12) para contraseñas almacenadas en Oracle.

**Frontend:**
- `KeycloakAuthService`: Adaptador para keycloak-js con gestión de tokens en memoria.
- `AuthService`: Fachada que abstrae Keycloak del resto de Angular.
- `authGuard`: Protege rutas que requieren sesión activa.
- `roleGuard`: Protege rutas que requieren roles específicos.
- `jwtInterceptor`: Inyecta `Authorization: Bearer <token>` en cada petición HTTP.
- `traceIdInterceptor`: Propaga el header `X-Trace-ID` para trazabilidad.

### 9.7.3 Matriz de autorización

| Recurso | GET | POST/PUT | DELETE |
|---|---|---|---|
| `/api/v1/usuarios/**` | Autenticado | ADMIN | ADMIN |
| `/api/v1/propuestas/*/aprobar,rechazar,cancelar` | — | ADMIN, JEFE | — |
| Entidades de negocio (DELETE) | — | — | ADMIN |
| Gestión comercial (productos, equipos, fuentes, etiquetas) | Autenticado | ADMIN, JEFE | ADMIN, JEFE |
| Resto de endpoints | Autenticado | Autenticado | Autenticado |

---

## 9.8 Angular y sus Módulos

### 9.8.1 Arquitectura del frontend

Angular 22 con componentes **standalone** (sin `NgModule`), **zoneless** (`provideZonelessChangeDetection`), y carga diferida (`loadComponent`).

### 9.8.2 Estructura de carpetas

```
viernes-frontend/src/app/
├── core/
│   ├── auth/
│   │   ├── keycloak/          ← Adaptador Keycloak (keycloak-auth.service, config, initializer)
│   │   ├── auth.service.ts    ← Fachada de autenticación
│   │   ├── auth.guard.ts      ← Guard de sesión
│   │   └── role.guard.ts      ← Guard de roles
│   ├── interceptors/
│   │   ├── jwt.interceptor.ts           ← Inyección de Bearer token
│   │   └── trace-id-interceptor.ts      ← Propagación de Trace ID
│   ├── layout/
│   │   └── layout.ts          ← Componente shell con sidebar y toolbar
│   └── services/
│       ├── api-service.ts     ← Base URL centralizada
│       └── draft.service.ts   ← Persistencia de borradores
├── features/
│   ├── actividades/    (actividad-form, actividad-list, model, service)
│   ├── auth/           (login)
│   ├── clientes/       (cliente-dialog, cliente-form, cliente-list, model, service)
│   ├── configuracion/  (configuracion)
│   ├── dashboard/      (dashboard, model, service)
│   ├── equipos/        (equipo-form, equipo-list, model, service)
│   ├── etiquetas/      (etiqueta-manager-dialog, etiqueta-selector, model, service)
│   ├── leads/          (lead-form, lead-list, model, service)
│   ├── mensajeria/     (chat-panel, model, service)
│   ├── oportunidades/  (oportunidad-detail, oportunidad-dialog, oportunidad-form, oportunidad-list, model, service)
│   ├── productos/      (producto-form, producto-list, model, service)
│   ├── propuestas/     (propuesta-form, propuesta-list, model, service)
│   ├── relationship-map/ (relationship-map, model, service)
│   └── usuarios/       (usuario-form, usuario-list, model, service)
└── shared/
    └── table-sort.ts   ← Utilidad de ordenamiento de tablas
```

### 9.8.3 Rutas protegidas por rol

| Ruta | Roles requeridos |
|---|---|
| `/usuarios/**` | ADMIN |
| `/productos/nuevo`, `/productos/:id/editar` | ADMIN, JEFE |
| `/equipos/**` | ADMIN, JEFE |
| `/configuracion` | ADMIN |
| Resto | Autenticado (cualquier rol) |

---

## 9.9 Dependencias entre Módulos

### 9.9.1 Diagrama de dependencias (Spring Modulith)

```
                        ┌──────────────────┐
                        │  shared          │
                        │  (exception,     │
                        │   ratelimit,     │
                        │   cache, filter) │
                        └────────┬─────────┘
                  ┌──────────────┼──────────────────────────┐
                  │              │                           │
          ┌───────▼──────┐  ┌───▼────────┐  ┌──────────────▼─────┐
          │   usuarios   │  │   equipos  │  │    mensajeria      │
          └───────┬──────┘  └────────────┘  └────────────────────┘
                  │
       ┌──────────┼──────────┐
       │          │          │
  ┌────▼───┐  ┌──▼──────────▼───────────────┐
  │  auth  │  │      actividades            │
  └────────┘  │  → clientes, oportunidades, │
              │    leads, etiquetas,         │
              │    usuarios, shared          │
              └──────────────────────────────┘

  ┌─────────────────────────────────────────┐
  │          propuestas                      │
  │  → productos, clientes, oportunidades,  │
  │    shared                                │
  └─────────────────────────────────────────┘

  ┌─────────────────────────────────────────┐
  │          relationshipmap                 │
  │  → clientes, oportunidades, actividades,│
  │    propuestas, productos, usuarios,     │
  │    shared                                │
  └─────────────────────────────────────────┘

  ┌────────────┐  ┌────────────┐  ┌────────────┐
  │  clientes  │  │   leads    │  │ productos  │  ← Módulos OPEN
  │  (OPEN)    │  │  (OPEN)    │  │  (OPEN)    │    (consumidos por otros)
  └────────────┘  └────────────┘  └────────────┘

  ┌────────────┐  ┌────────────┐  ┌─────────────────┐
  │ etiquetas  │  │ marketing  │  │ oportunidades   │  ← Módulos OPEN
  │  (OPEN)    │  │  (OPEN)    │  │  (OPEN)         │
  └────────────┘  └────────────┘  └─────────────────┘

  ┌────────────┐
  │ dashboard  │  ← OPEN, solo lectura
  │  (OPEN)    │
  └────────────┘
```

---

## 9.10 Observabilidad

### 9.10.1 Stack de observabilidad

| Componente | Función |
|---|---|
| **Spring Boot Actuator** | Expone endpoints `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` |
| **Micrometer + Prometheus** | Exporta métricas en formato Prometheus |
| **Prometheus** | Recolecta métricas del backend (configurado en `obs/prometheus/prometheus.yml`) |
| **Promtail** | Envía logs del backend a Loki (configurado en `obs/promtail/promtail-config.yml`) |
| **Grafana** | Dashboards de visualización |
| **Logback con MDC** | Logs estructurados con `traceId` para trazabilidad distribuida |

### 9.10.2 Trazabilidad

La trazabilidad se implementa con dos componentes coordinados:
- **Backend**: `CorrelationIdFilter` genera un UUID como `X-Trace-ID` si el cliente no lo envía, y lo inyecta en el MDC de SLF4J.
- **Frontend**: `traceIdInterceptor` genera un UUID y lo envía en el header `X-Trace-ID` de cada petición HTTP.

---

## 9.11 Decisiones Arquitectónicas

### ADR-01: Monolito modular sobre microservicios

**Decisión**: Implementar un monolito modular con Spring Modulith.
**Contexto**: Equipo académico pequeño, ciclo de desarrollo limitado, un solo dominio de negocio.
**Justificación**: Reduce la complejidad operativa (un solo JAR), permite evolucionar a microservicios en el futuro gracias a los límites de módulo verificados por Spring Modulith.

### ADR-02: Arquitectura hexagonal estricta

**Decisión**: Separar dominio, aplicación e infraestructura en todos los módulos.
**Justificación**: Independencia del framework de persistencia. Las entidades de dominio no tienen anotaciones JPA. Se puede cambiar de Oracle a otro motor sin modificar la lógica de negocio.

### ADR-03: Relaciones N:M con entidad intermedia explícita

**Decisión**: No utilizar `@ManyToMany` de JPA; crear entidades intermedias con su propio repositorio.
**Justificación**: Permite atributos adicionales (rol, cantidad, orden), control explícito del ciclo de vida, y endpoints REST como subrecursos.

### ADR-04: Keycloak como IdP externo

**Decisión**: Delegar autenticación a Keycloak con OAuth2/OIDC.
**Justificación**: Separa la gestión de identidad del CRM. Permite SSO, MFA y gestión centralizada de roles sin código propio de autenticación.

### ADR-05: Frontend standalone y zoneless

**Decisión**: Angular 22 con componentes standalone y `provideZonelessChangeDetection()`.
**Justificación**: Elimina Zone.js para mejor rendimiento. Carga diferida automática con `loadComponent`.

### ADR-06: Caché multicapa con Caffeine

**Decisión**: Cache en memoria para catálogos, dashboard y consultas externas.
**Justificación**: Reduce carga a Oracle y al API de RENIEC/SUNAT. TTL diferenciados por volatilidad de datos.

---

## 9.12 Validez de Diagramas C4

### 9.12.1 Diagramas que siguen siendo válidos

| Nivel C4 | Elemento | Estado | Observación |
|---|---|---|---|
| C1 - Contexto | Sistema Viernes CRM ↔ Usuario ↔ Keycloak ↔ APIsPerú | **Válido** | Los actores y sistemas externos no han cambiado. |
| C2 - Contenedores | Angular SPA ↔ Spring Boot ↔ Oracle ↔ Keycloak | **Válido** | La topología de contenedores es correcta. Agregar stack de observabilidad si no aparece. |

### 9.12.2 Diagramas que requieren corrección

| Nivel C4 | Elemento | Estado | Corrección necesaria |
|---|---|---|---|
| C3 - Componentes | Módulos internos del backend | **Requiere actualización** | Agregar módulos: `relationshipmap`, `propuestas`, `equipos`, `marketing`. Actualizar dependencias inter-módulo según `package-info.java`. |
| C3 - Componentes | Relaciones entre módulos | **Requiere actualización** | Reflejar las relaciones N:M con entidades intermedias. Eliminar relaciones `@ManyToMany` que aparezcan en diagramas antiguos. |

### 9.12.3 Diagramas de Miro/Lucidchart

> **[REQUIERE VALIDACIÓN]**: Los diagramas de Miro y Lucidchart mencionados en el borrador Word deben verificarse contra las tablas `CRM_*` actuales. En particular, cualquier diagrama que muestre relaciones directas `@ManyToMany` entre entidades (sin tabla intermedia explícita) pertenece a una versión anterior y debe actualizarse.

---

## 9.13 Integración Frontend-Backend

### 9.13.1 Comunicación

| Aspecto | Implementación |
|---|---|
| Protocolo | HTTP/REST sobre JSON |
| Base URL | `http://localhost:8080/api/v1/` (configurable en `environment.ts`) |
| Autenticación | Bearer JWT en header `Authorization` (inyectado por `jwtInterceptor`) |
| Trazabilidad | Header `X-Trace-ID` (propagado por `traceIdInterceptor`) |
| Timeout | 15 segundos por petición (configurado en `jwtInterceptor`) |
| Renovación de token | Automática ante 401 + refresh con Keycloak |

### 9.13.2 Patrón de servicios Angular

Cada feature module tiene un servicio dedicado (`*.service.ts`) que encapsula las llamadas HTTP al backend, siguiendo el mismo patrón:

```typescript
@Injectable({ providedIn: 'root' })
export class ClientesService {
  private readonly api = inject(ApiService);
  private readonly http = inject(HttpClient);

  listar(): Observable<Cliente[]> {
    return this.http.get<Cliente[]>(this.api.buildUrl('/api/v1/clientes'));
  }
  // ...
}
```

