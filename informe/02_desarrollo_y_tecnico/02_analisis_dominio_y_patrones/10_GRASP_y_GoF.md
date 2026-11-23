# CAPÍTULO 10 — PATRONES GRASP Y GoF

## 10.1 Introducción

Este capítulo identifica y documenta la aplicación de los nueve principios GRASP y los patrones GoF en Viernes CRM V1. Cada identificación se basa en evidencia concreta del código fuente, distinguiendo entre patrones implementados por código propio y mecanismos proporcionados por el framework Spring.

---

## 10.2 Principios GRASP

### 10.2.1 Information Expert (Experto en Información)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Principio** | Asignar la responsabilidad a la clase que posee la información necesaria para cumplirla. |
| **Aplicación 1** | `Oportunidad.getValorPonderado()` — La entidad posee `valor` y `probabilidad`, por lo que es experta en calcular el valor ponderado. |
| **Archivo** | `oportunidades/domain/Oportunidad.java` |
| **Aplicación 2** | `Propuesta.recalcularTotales()` — La propuesta posee `detalles` y `descuento`, por lo que es experta en calcular subtotal y total. |
| **Archivo** | `propuestas/domain/Propuesta.java` |
| **Aplicación 3** | `DetallePropuesta.calcularSubtotal()` — El detalle posee `cantidad` y `precioUnitario`, por lo que es experto en calcular su subtotal. |
| **Archivo** | `propuestas/domain/DetallePropuesta.java` |
| **Aplicación 4** | `Propuesta.esEstadoTerminal()` — La propuesta es experta en determinar si su estado actual permite modificaciones. |
| **Archivo** | `propuestas/domain/Propuesta.java` |
| **Es propio o de Spring** | Propio |

### 10.2.2 Creator (Creador)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Principio** | Asignar la responsabilidad de crear una instancia de B a la clase A que contiene, agrega, registra o usa directamente a B. |
| **Aplicación 1** | `ActividadService.crear()` — El servicio de aplicación crea instancias de `Actividad` y las persiste a través del puerto de salida. La creación incluye validación de relaciones y sincronización de etiquetas. |
| **Archivo** | `actividades/application/service/ActividadService.java` |
| **Aplicación 2** | `UsuarioService.crear()` — Crea instancias de `Usuario` con Builder, encriptando la contraseña y verificando unicidad del correo. |
| **Archivo** | `usuarios/application/service/UsuarioService.java` |
| **Aplicación 3** | `PropuestaService.crear()` — Genera número de propuesta, calcula subtotales de cada detalle y totales de la propuesta, luego persiste el agregado completo. |
| **Archivo** | `propuestas/application/service/PropuestaService.java` |
| **Es propio o de Spring** | Propio (servicios de aplicación como creadores) |

### 10.2.3 Controller (Controlador)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Principio** | Asignar la responsabilidad de recibir o manejar un evento del sistema a una clase no-UI que represente el sistema global o un escenario de caso de uso. |
| **Aplicación** | Cada módulo tiene un controlador REST dedicado que recibe las peticiones HTTP y las delega al caso de uso correspondiente. |
| **Ejemplo** | `ActividadController` recibe `POST /api/v1/actividades` y delega a `ManageActividadUseCase.crear()`. |
| **Archivo** | `actividades/adapters/in/web/ActividadController.java` |

**Tabla de controladores del sistema:**

| Controlador | Módulo | Endpoints base | Puerto de entrada |
|---|---|---|---|
| `ActividadController` | Actividades | `/api/v1/actividades` | `ManageActividadUseCase` |
| `ClienteController` | Clientes | `/api/v1/clientes` | `ManageClienteUseCase` |
| `OportunidadController` | Oportunidades | `/api/v1/oportunidades` | `ManageOportunidadUseCase` |
| `PropuestaController` | Propuestas | `/api/v1/propuestas` | `ManagePropuestaUseCase` |
| `LeadController` | Leads | `/api/v1/leads` | `ManageLeadUseCase` |
| `FuenteController` | Leads | `/api/v1/fuentes` | `ManageFuenteUseCase` |
| `CampaniaController` | Marketing | `/api/v1/campanias` | `ManageCampaniaUseCase` |
| `ProductoController` | Productos | `/api/v1/productos` | `ManageProductoUseCase` |
| `EtiquetaController` | Etiquetas | `/api/v1/etiquetas` | `ManageEtiquetaUseCase` |
| `EquipoController` | Equipos | `/api/v1/equipos` | `ManageEquipoUseCase` |
| `UsuarioController` | Usuarios | `/api/v1/usuarios` | `ManageUsuarioUseCase` |
| `MensajeController` | Mensajería | `/api/v1/mensajes` | `ManageMensajeUseCase` |
| `DashboardController` | Dashboard | `/api/v1/dashboard` | `DashboardUseCase` |
| `RelationshipMapController` | Relationship Map | `/api/v1/relationship-map` | `ManageRelationshipMapUseCase` |

| **Es propio o de Spring** | Híbrido: la clase es propia, el mapeo HTTP lo provee Spring Web MVC (`@RestController`, `@RequestMapping`). |

### 10.2.4 Low Coupling (Bajo Acoplamiento)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Principio** | Asignar responsabilidades de forma que el acoplamiento entre clases se mantenga bajo. |
| **Aplicación 1** | **Puertos de entrada/salida**: Los servicios de aplicación dependen de interfaces (`ManageActividadUseCase`, `ActividadPersistencePort`), no de implementaciones concretas. |
| **Aplicación 2** | **Spring Modulith**: Las dependencias inter-módulo están declaradas explícitamente y verificadas en compilación. Un módulo no puede acceder a los internos de otro. |
| **Aplicación 3** | **`RelacionValidationPort`**: El módulo de actividades valida la existencia de clientes, leads y oportunidades a través de una interfaz abstracta, sin importar clases de esos módulos directamente en el servicio. |
| **Archivo** | `actividades/application/port/out/RelacionValidationPort.java` |
| **Aplicación 4** | **Frontend — `AuthService` como fachada**: Los componentes Angular dependen de `AuthService`, no de `KeycloakAuthService`, lo que permite cambiar el proveedor de identidad sin modificar los componentes. |
| **Es propio o de Spring** | Diseño propio facilitado por el DI de Spring |

### 10.2.5 High Cohesion (Alta Cohesión)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Principio** | Las clases deben tener responsabilidades estrechamente relacionadas. |
| **Aplicación 1** | Cada servicio de aplicación se dedica a un solo agregado: `ClienteService` gestiona solo clientes y contactos, `OportunidadService` solo oportunidades y sus relaciones N:M. |
| **Aplicación 2** | Los adaptadores de persistencia están especializados: `ActividadPersistenceAdapter` solo convierte y persiste actividades; `ActividadParticipantePersistenceAdapter` solo maneja la relación actividad-participante. |
| **Aplicación 3** | Cada controlador atiende un único recurso REST con su respectivo conjunto de endpoints. |
| **Aplicación 4** | El módulo `shared` agrupa responsabilidades transversales (excepciones, caché, filtros) pero está subdividido en submódulos cohesivos (`shared.exception`, `shared.ratelimit`). |
| **Es propio o de Spring** | Propio |

### 10.2.6 Polymorphism (Polimorfismo)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Principio** | Cuando el comportamiento varía según el tipo, asignar la responsabilidad a los tipos usando operaciones polimórficas. |
| **Aplicación 1** | Los puertos de salida (`*PersistencePort`) son interfaces con múltiples posibles implementaciones. Actualmente cada interfaz tiene un adaptador concreto para Oracle/JPA, pero el diseño permite sustituir la implementación (ej: cambiar a MongoDB) sin modificar la capa de aplicación. |
| **Aplicación 2** | `DocumentoLookupPort` — interfaz que podría tener implementaciones para diferentes proveedores de consulta DNI/RUC. Actualmente implementada por `ApisPeruDocumentoAdapter`. |
| **Aplicación 3** | `Converter<Jwt, AbstractAuthenticationToken>` — la interfaz de Spring es implementada por `KeycloakJwtAuthenticationConverter` para la traducción específica de tokens Keycloak. |
| **Es propio o de Spring** | Híbrido: interfaces propias, inyección polimórfica por Spring DI |

### 10.2.7 Pure Fabrication (Fabricación Pura)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Principio** | Crear clases que no representan conceptos del dominio del problema cuando es necesario para mantener alta cohesión, bajo acoplamiento o reutilización. |
| **Aplicación 1** | **Adaptadores de persistencia** (`*PersistenceAdapter`): No representan conceptos del dominio, sino que son fabricaciones puras para desacoplar el dominio de JPA/Oracle. |
| **Archivo** | `*/adapters/out/persistence/*PersistenceAdapter.java` (todos los módulos) |
| **Aplicación 2** | **`CorrelationIdFilter`**: Filtro de trazabilidad que no pertenece al dominio comercial. |
| **Archivo** | `shared/filter/CorrelationIdFilter.java` |
| **Aplicación 3** | **`ConsultaDocumentoRateLimiter`**: Control de cuota que no pertenece al dominio pero es necesario por restricciones del proveedor externo. |
| **Archivo** | `shared/ratelimit/ConsultaDocumentoRateLimiter.java` |
| **Aplicación 4** | **`CacheConfig` y `CacheRefreshJob`**: Infraestructura de caché sin relación con el dominio de negocio. |
| **Archivo** | `shared/cache/CacheConfig.java`, `shared/cache/CacheRefreshJob.java` |
| **Aplicación 5** | **`ApiService`** (frontend): Clase utilitaria para construir URLs, sin correspondencia en el dominio. |
| **Archivo** | `viernes-frontend/src/app/core/services/api-service.ts` |
| **Es propio o de Spring** | Propio |

### 10.2.8 Indirection (Indirección)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Principio** | Asignar la responsabilidad a un objeto intermediario para desacoplar otros componentes. |
| **Aplicación 1** | **Puertos de salida**: Actúan como intermediarios entre los servicios de aplicación y la infraestructura de persistencia. `ActividadService` nunca accede a `ActividadSpringDataRepository` directamente; lo hace a través de `ActividadPersistencePort`. |
| **Aplicación 2** | **`RelacionValidationPort` y `ContactoQueryPort`**: Intermediarios que permiten al módulo de Actividades consultar entidades de otros módulos (Clientes, Leads, Oportunidades, Usuarios) sin acoplarse a sus implementaciones. |
| **Archivo** | `actividades/application/port/out/RelacionValidationPort.java` |
| **Aplicación 3** | **`AuthService` (frontend)**: Intermediario entre los componentes Angular y `KeycloakAuthService`. |
| **Archivo** | `viernes-frontend/src/app/core/auth/auth.service.ts` |
| **Aplicación 4** | **`jwtInterceptor` (frontend)**: Intermediario HTTP que inyecta el token JWT sin que los servicios de features lo conozcan. |
| **Archivo** | `viernes-frontend/src/app/core/interceptors/jwt.interceptor.ts` |
| **Es propio o de Spring** | Propio |

### 10.2.9 Protected Variations (Variaciones Protegidas)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Principio** | Proteger elementos de las variaciones de otros elementos envolviéndolos con una interfaz estable. |
| **Aplicación 1** | **Puertos hexagonales**: Si se cambia de Oracle a PostgreSQL, solo se modifica el adaptador de persistencia. Las interfaces `*PersistencePort` protegen al dominio de esa variación. |
| **Aplicación 2** | **`DocumentoLookupPort`**: Protege al servicio de clientes de cambios en el proveedor de consulta DNI/RUC. Si se reemplaza APIsPerú por otro servicio, solo se implementa un nuevo adapter. |
| **Archivo** | `clientes/application/port/out/DocumentoLookupPort.java` |
| **Aplicación 3** | **Spring Modulith `@ApplicationModule`**: Protege los módulos de accesos no autorizados a sus internos. Si se reorganiza la estructura interna de un módulo, los módulos dependientes no se ven afectados mientras la API pública no cambie. |
| **Aplicación 4** | **`UsuariosApi`**: Interfaz pública del módulo Usuarios que protege a los consumidores de los cambios internos en la persistencia o lógica del usuario. |
| **Archivo** | `usuarios/UsuariosApi.java` |
| **Es propio o de Spring** | Propio |

---

## 10.3 Resumen GRASP

| # | Principio | Estado | Ámbito principal |
|---|---|---|---|
| 1 | Information Expert | Implementado | Entidades de dominio (Propuesta, Oportunidad, DetallePropuesta) |
| 2 | Creator | Implementado | Servicios de aplicación (*Service) |
| 3 | Controller | Implementado | Controladores REST (*Controller) |
| 4 | Low Coupling | Implementado | Puertos hexagonales, Spring Modulith, fachadas |
| 5 | High Cohesion | Implementado | Un servicio/controlador/adapter por agregado |
| 6 | Polymorphism | Implementado | Interfaces de puertos con implementaciones intercambiables |
| 7 | Pure Fabrication | Implementado | Adapters, filtros, rate limiters, configuraciones |
| 8 | Indirection | Implementado | Puertos como intermediarios, fachadas, interceptores |
| 9 | Protected Variations | Implementado | Interfaces estables que protegen de cambios de infraestructura |

---

## 10.4 Patrones GoF

### 10.4.1 Patrones Creacionales

#### Builder

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Clase(s)** | `Usuario`, `Producto`, `Propuesta`, `DetallePropuesta`, `Oportunidad`, `OportunidadContacto`, `OportunidadProducto`, `OportunidadColaborador`, `Lead`, `Fuente`, `Campania`, `CampaniaLead`, `Equipo`, `EquipoMiembro`, `Etiqueta`, `Mensaje`, `Cliente`, `Contacto` |
| **Mecanismo** | Anotación `@Builder` de Lombok genera el patrón Builder automáticamente. |
| **Ejemplo** | `Usuario.builder().nombre("Admin").apellido("Viernes").correo("admin@viernes.com").build()` |
| **Problema que resuelve** | Construcción de objetos con múltiples parámetros opcionales sin constructores telescópicos. |
| **Es propio o de Spring** | Proporcionado por Lombok (generación en compilación) |

#### Singleton

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado (proporcionado por Spring) |
| **Clase(s)** | Todos los `@Service`, `@Component`, `@Configuration`, `@RestController` |
| **Mecanismo** | Spring IoC crea una única instancia por defecto (scope `singleton`) para cada bean. |
| **Ejemplo** | `ActividadService`, `ClientePersistenceAdapter`, `CacheConfig` — existe una sola instancia de cada uno en el ApplicationContext. |
| **Problema que resuelve** | Garantiza una instancia única con punto de acceso global, evitando creaciones redundantes. |
| **Es propio o de Spring** | Proporcionado por Spring Framework |

#### Factory Method

| Aspecto | Detalle |
|---|---|
| **Estado** | Parcial |
| **Clase(s)** | `PasswordEncoderConfig`, `CacheConfig`, `OpenApiConfig` |
| **Mecanismo** | Métodos `@Bean` en clases `@Configuration` actúan como Factory Methods que encapsulan la creación de objetos. |
| **Ejemplo** | `PasswordEncoderConfig.passwordEncoder()` retorna `new BCryptPasswordEncoder(12)`. `CacheConfig.cacheManager()` crea y configura un `CaffeineCacheManager` con múltiples caches. |
| **Problema que resuelve** | Centraliza la configuración de creación de objetos de infraestructura. |
| **Es propio o de Spring** | Híbrido: método propio con anotación Spring |

---

### 10.4.2 Patrones Estructurales

#### Adapter (Adaptador)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Tipo** | Adaptador de objeto |
| **Clase(s)** | Todos los `*PersistenceAdapter`, `ApisPeruDocumentoAdapter`, `RelacionValidationAdapter`, `ContactoQueryAdapter`, `PropuestaRelacionValidationAdapter` |
| **Problema que resuelve** | Traduce la interfaz del puerto de salida (definida por el dominio) a la interfaz de la infraestructura (Spring Data JPA, APIs externas). |

**Tabla de adaptadores del sistema:**

| Adaptador | Puerto que implementa | Infraestructura adaptada |
|---|---|---|
| `ActividadPersistenceAdapter` | `ActividadPersistencePort` | Spring Data JPA → Oracle |
| `ActividadParticipantePersistenceAdapter` | `ActividadParticipantePersistencePort` | Spring Data JPA → Oracle |
| `ActividadEtiquetaPersistenceAdapter` | `ActividadEtiquetaPersistencePort` | Spring Data JPA → Oracle |
| `RelacionValidationAdapter` | `RelacionValidationPort` | Repositorios de otros módulos |
| `ContactoQueryAdapter` | `ContactoQueryPort` | `ManageClienteUseCase` de Clientes |
| `ClientePersistenceAdapter` | `ClientePersistencePort` | Spring Data JPA → Oracle |
| `ApisPeruDocumentoAdapter` | `DocumentoLookupPort` | RestClient → API DNI/RUC |
| `OportunidadPersistenceAdapter` | `OportunidadPersistencePort` | Spring Data JPA → Oracle |
| `PropuestaPersistenceAdapter` | `PropuestaPersistencePort` | Spring Data JPA → Oracle |
| `PropuestaRelacionValidationAdapter` | `PropuestaRelacionValidationPort` | Repositorios de otros módulos |
| `UsuarioPersistenceAdapter` | `UsuarioPersistencePort` | Spring Data JPA → Oracle |
| `KeycloakJwtAuthenticationConverter` | `Converter<Jwt, AbstractAuthenticationToken>` | JWT Keycloak → Spring Security |

| **Justificación** | Es el patrón más extensivamente implementado en Viernes CRM. La arquitectura hexagonal se basa fundamentalmente en el patrón Adapter para conectar el dominio con la infraestructura. |
| **Es propio o de Spring** | Propio (las clases adapter son código del proyecto) |

#### Facade (Fachada)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Clase(s) Backend** | `DashboardService` — Fachada que agrega KPIs de múltiples módulos en una sola respuesta. |
| **Archivo** | `dashboard/application/service/DashboardService.java` |
| **Clase(s) Frontend** | `AuthService` — Fachada que simplifica el acceso a Keycloak para guards, interceptores y componentes. |
| **Archivo** | `viernes-frontend/src/app/core/auth/auth.service.ts` |
| **Problema que resuelve** | Proporciona una interfaz simplificada a un subsistema complejo (métricas de múltiples módulos, autenticación Keycloak). |
| **Es propio o de Spring** | Propio |

#### Proxy

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado (proporcionado por Spring) |
| **Mecanismo** | Spring AOP crea proxies para los beans anotados con `@Transactional`, `@Cacheable` y los interceptores de seguridad OAuth2. |
| **Ejemplo 1** | Los métodos de `ActividadService` anotados con `@Transactional` son interceptados por un proxy que gestiona la transacción JPA. |
| **Ejemplo 2** | `ApisPeruDocumentoAdapter.consultar()` anotado con `@Cacheable` es interceptado por un proxy que verifica la cache Caffeine antes de ejecutar el método real. |
| **Problema que resuelve** | Añade comportamiento transversal (transacciones, caché, seguridad) sin modificar la lógica de negocio. |
| **Es propio o de Spring** | Proporcionado por Spring Framework |

---

### 10.4.3 Patrones Comportamentales

#### Template Method (Método Plantilla)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado (proporcionado por Spring) |
| **Clase(s)** | `OncePerRequestFilter` (extendida por `CorrelationIdFilter`) |
| **Mecanismo** | `OncePerRequestFilter` define el flujo de filtrado y `CorrelationIdFilter` implementa `doFilterInternal()` como paso variable. |
| **Archivo** | `shared/filter/CorrelationIdFilter.java` |
| **Problema que resuelve** | Garantiza que el filtro se ejecute una sola vez por request, dejando al desarrollador implementar solo la lógica específica. |
| **Es propio o de Spring** | Proporcionado por Spring (la clase base); propio (la implementación) |

#### Strategy (Estrategia)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Clase(s)** | Puertos de salida como estrategias intercambiables. |
| **Ejemplo 1** | `DocumentoLookupPort` es una estrategia para la consulta de documentos. `ApisPeruDocumentoAdapter` es la estrategia concreta seleccionada en tiempo de inyección. |
| **Ejemplo 2** | `KeycloakJwtAuthenticationConverter` es la estrategia concreta de conversión de tokens, seleccionada por el perfil `@Profile("keycloak")`. |
| **Problema que resuelve** | Permite cambiar la implementación de un algoritmo sin modificar el código que lo consume. |
| **Es propio o de Spring** | Híbrido: interfaces propias, selección por Spring DI |

#### Observer (Observador)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado (frontend) |
| **Clase(s)** | `KeycloakAuthService` con `BehaviorSubject<boolean>` para `isAuthenticated()`. |
| **Archivo** | `viernes-frontend/src/app/core/auth/keycloak/keycloak-auth.service.ts` |
| **Mecanismo** | RxJS `BehaviorSubject` notifica a los suscriptores sobre cambios en el estado de autenticación. Los guards y componentes se suscriben al `Observable` retornado por `isAuthenticated()`. |
| **Problema que resuelve** | Desacopla la notificación de cambios de estado de autenticación de los componentes que reaccionan a ellos. |
| **Es propio o de Spring** | Propio (RxJS en Angular) |

#### Chain of Responsibility (Cadena de Responsabilidad)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado (proporcionado por Spring/Angular) |
| **Backend** | La cadena de filtros de Spring Security (`SecurityFilterChain`) procesa cada petición a través de filtros de CORS, CSRF, autenticación JWT y autorización por roles. `CorrelationIdFilter` se añade a esta cadena. |
| **Frontend** | La cadena de interceptores HTTP de Angular (`jwtInterceptor`, `traceIdInterceptor`) procesa cada petición de forma encadenada, donde cada interceptor decide si modifica la petición y la pasa al siguiente. |
| **Problema que resuelve** | Permite que múltiples objetos tengan la oportunidad de manejar una petición sin que el emisor conozca al receptor. |
| **Es propio o de Spring** | Proporcionado por Spring Security y Angular HttpClient |

#### Repository (Repositorio)

| Aspecto | Detalle |
|---|---|
| **Estado** | Implementado |
| **Nota** | Aunque Repository no es un patrón GoF clásico, es un patrón DDD ampliamente reconocido. Se incluye por su implementación extensiva. |
| **Clase(s)** | Todas las interfaces `*SpringDataRepository` que extienden `JpaRepository`, más los puertos `*PersistencePort`. |
| **Mecanismo** | Doble capa: Spring Data JPA genera la implementación del repositorio automáticamente; el `PersistenceAdapter` envuelve ese repositorio y expone solo la interfaz de dominio. |
| **Problema que resuelve** | Abstrae la lógica de acceso a datos, proporcionando una interfaz orientada a colecciones para acceder a los objetos de dominio. |
| **Es propio o de Spring** | Híbrido: Spring Data genera la implementación JPA; los Ports y Adapters son propios |

---

### 10.4.4 Patrones GoF no identificados en el código actual

Los siguientes patrones GoF no se identifican en la implementación actual de Viernes CRM V1. No se fuerza su existencia:

| Patrón | Categoría | Observación |
|---|---|---|
| Abstract Factory | Creacional | No hay familias de objetos relacionados que requieran creación polimórfica. |
| Prototype | Creacional | No se clona ninguna entidad. |
| Bridge | Estructural | La separación dominio-JPA podría interpretarse como Bridge, pero se ajusta mejor al patrón Adapter. |
| Composite | Estructural | No hay jerarquías recursivas. |
| Decorator | Estructural | Los proxies de Spring podrían considerarse decoradores, pero se clasifican más apropiadamente como Proxy. |
| Flyweight | Estructural | No hay optimización de objetos compartidos. |
| Command | Comportamental | No hay encapsulación de peticiones como objetos. |
| Iterator | Comportamental | Java Streams y colecciones implementan Iterator internamente, pero no es un patrón de diseño explícito del proyecto. |
| Mediator | Comportamental | No hay un objeto mediador centralizado entre módulos. |
| Memento | Comportamental | No se implementa deshacer/rehacer. |
| State | Comportamental | Los estados de Propuesta y Lead podrían beneficiarse del patrón State, pero actualmente se implementan con enums y condicionales. |
| Visitor | Comportamental | No hay operaciones polimórficas sobre jerarquías de objetos. |

---

## 10.5 Resumen GoF

| # | Patrón | Categoría | Estado | Es propio o de Spring |
|---|---|---|---|---|
| 1 | Builder | Creacional | Implementado | Lombok |
| 2 | Singleton | Creacional | Implementado | Spring IoC |
| 3 | Factory Method | Creacional | Parcial | Híbrido (@Bean) |
| 4 | Adapter | Estructural | Implementado | Propio |
| 5 | Facade | Estructural | Implementado | Propio |
| 6 | Proxy | Estructural | Implementado | Spring AOP |
| 7 | Template Method | Comportamental | Implementado | Spring (base) + Propio (impl) |
| 8 | Strategy | Comportamental | Implementado | Híbrido |
| 9 | Observer | Comportamental | Implementado | Propio (RxJS) |
| 10 | Chain of Responsibility | Comportamental | Implementado | Spring Security + Angular |
| 11 | Repository | DDD/PoEAA | Implementado | Híbrido |

---

## 10.6 Clasificación consolidada por origen

### 10.6.1 Patrones implementados por código propio

| Patrón | Evidencia principal |
|---|---|
| Adapter | `*PersistenceAdapter`, `*ValidationAdapter`, `*QueryAdapter` |
| Facade | `DashboardService`, `AuthService` (frontend) |
| Information Expert | `Oportunidad.getValorPonderado()`, `Propuesta.recalcularTotales()` |
| Creator | `*Service.crear()` |
| Pure Fabrication | Adapters, filtros, rate limiters |
| Indirection | Puertos hexagonales, fachadas |
| Protected Variations | Interfaces de puertos, `UsuariosApi` |
| Observer | `BehaviorSubject` en `KeycloakAuthService` |

### 10.6.2 Patrones proporcionados por Spring Framework

| Patrón | Mecanismo Spring |
|---|---|
| Singleton | Scope por defecto de Spring IoC |
| Proxy | `@Transactional`, `@Cacheable`, Spring Security |
| Template Method | `OncePerRequestFilter` |
| Chain of Responsibility | `SecurityFilterChain` |
| Repository | `JpaRepository` de Spring Data |

### 10.6.3 Patrones híbridos (diseño propio + framework)

| Patrón | Diseño propio | Framework |
|---|---|---|
| Controller | Clase `*Controller` | `@RestController`, `@RequestMapping` de Spring Web |
| Strategy | Interfaces `*Port` | Inyección de dependencias de Spring |
| Factory Method | Métodos de creación | `@Bean`, `@Configuration` de Spring |
| Builder | Entidades de dominio | `@Builder` de Lombok |
| Low Coupling / High Cohesion | Estructura modular hexagonal | `@ApplicationModule` de Spring Modulith |
| Polymorphism | Interfaces con múltiples impls posibles | Inyección polimórfica de Spring DI |

