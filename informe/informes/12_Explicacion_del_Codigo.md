# 12 — CÓMO FUNCIONA EL CÓDIGO DE VIERNES CRM V1

**Proyecto:** Viernes CRM V1 — CRM Híbrido de Gestión Comercial y Marketing
**Alcance:** Backend Spring Boot (`viernes-backend`), Frontend Angular (`viernes-frontend`) y capa Oracle (`base de datos`).
**Objetivo:** Explicar qué hace cada módulo, qué hace cada parte del código y cuáles son los patrones que más se repiten.

---

## 1. La idea central en una frase

El sistema es **un monolito modular** (Spring Modulith) donde **cada módulo de negocio es un hexágono** (arquitectura hexagonal / puertos y adaptadores): el controlador HTTP entra por un lado, la base de datos sale por el otro, y en el centro vive el dominio puro, sin conocer ni a Spring ni a Oracle.

```
   HTTP (JSON)                                        Oracle (SQL)
       │                                                    ▲
       ▼                                                    │
┌──────────────┐   ┌───────────────┐   ┌──────────────┐   ┌──────────────┐
│ adapters.in  │──▶│ application   │──▶│  port.out    │◀──│ adapters.out │
│  .web        │   │  .service     │   │  (interface) │   │ .persistence │
│ Controller   │   │  UseCase impl │   │              │   │ Adapter      │
└──────────────┘   └───────────────┘   └──────────────┘   └──────────────┘
                          │                                     │
                          ▼                                     ▼
                   ┌─────────────┐                       ┌─────────────┐
                   │   domain    │                       │   entity    │
                   │ (POJO puro) │                       │  (@Entity)  │
                   └─────────────┘                       └─────────────┘
```

**Tres reglas que el código respeta en todos los módulos:**

1. El `Controller` **nunca** toca JPA ni Oracle. Solo habla con una interfaz `*UseCase`.
2. El `Service` **nunca** toca JPA. Solo habla con interfaces `*PersistencePort`.
3. La `Entity` (`@Entity`) **nunca** sale del módulo: se traduce a un objeto de dominio y luego a un DTO `record`.

Si esas tres reglas se rompen, `ModularityTests` y los tests de arquitectura fallan.

---

## 2. Mapa general del backend

Ruta raíz: `viernes-backend/src/main/java/com/viernes/`

| Módulo | Responsabilidad | Tabla(s) Oracle | Endpoints |
|---|---|---|---|
| `clientes` | Clientes, contactos y consulta DNI/RUC | `CRM_CLIENTES`, `CRM_CONTACTOS` | `/api/v1/clientes` |
| `leads` | Prospectos y sus fuentes de origen | `CRM_LEADS`, `CRM_FUENTES` | `/api/v1/leads`, `/api/v1/fuentes` |
| `oportunidades` | Pipeline de ventas y sus relaciones N:M | `CRM_OPORTUNIDADES` + 3 tablas puente | `/api/v1/oportunidades` |
| `propuestas` | Cotizaciones con detalle y aprobación | `CRM_PROPUESTAS`, `CRM_PROPUESTA_DETALLE` | `/api/v1/propuestas` |
| `actividades` | Tareas/eventos, participantes y etiquetas | `CRM_ACTIVIDADES` + 2 tablas puente | `/api/v1/actividades` |
| `productos` | Catálogo de productos/servicios | `CRM_PRODUCTOS` | `/api/v1/productos` |
| `etiquetas` | Etiquetas reutilizables (N:M) | `CRM_ETIQUETAS` | `/api/v1/etiquetas` |
| `equipos` | Equipos comerciales y sus miembros | `CRM_EQUIPOS`, `CRM_EQUIPO_MIEMBROS` | `/api/v1/equipos` |
| `usuarios` | Usuarios locales espejo de Keycloak | `CRM_USUARIOS` | `/api/v1/usuarios` |
| `marketing` | Campañas y leads captados | `CRM_CAMPANIAS`, `CRM_CAMPANIA_LEADS` | `/api/v1/campanias` |
| `mensajeria` | Chat interno con polling incremental | `CRM_MENSAJES` | `/api/v1/mensajeria` |
| `dashboard` | KPIs agregados de solo lectura | (consulta 8 tablas) | `/api/v1/dashboard` |
| `relationshipmap` | Grafo de relaciones entre entidades | (no tiene tabla propia) | `/api/v1/relationship-map` |
| `auth` | Seguridad: Keycloak + roles | — | (filtro global) |
| `shared` | Cache, excepciones, tracing, rate limit | — | (transversal) |

**14 módulos funcionales + `shared` + la clase raíz `ViernesApplication`.** Cada módulo declara en su `package-info.java` qué otros módulos puede importar (`allowedDependencies`); si un módulo importa algo no declarado, la verificación de Modulith lo detecta.

---

## 3. Anatomía de un módulo: el patrón que se repite 14 veces

Tomemos `productos` (el más pequeño y completo: 12 archivos). Todos los demás módulos son la misma estructura, con más o menos piezas.

```
productos/
├── package-info.java                  ← declara el módulo para Modulith
├── domain/
│   ├── Producto.java                  ← POJO puro (lombok @Getter/@Builder)
│   └── EstadoProducto.java            ← enum del dominio
├── application/
│   ├── dto/
│   │   ├── ProductoRequest.java       ← record de entrada + validaciones
│   │   └── ProductoResponse.java      ← record de salida
│   ├── port/
│   │   ├── in/ManageProductoUseCase.java    ← QUÉ puede hacer el módulo
│   │   └── out/ProductoPersistencePort.java ← QUÉ necesita del exterior
│   └── service/
│       └── ProductoService.java       ← implementa el UseCase, orquesta
└── adapters/
    ├── in/web/
    │   └── ProductoController.java    ← traduce HTTP ↔ DTO
    └── out/persistence/
        ├── ProductoPersistenceAdapter.java        ← traduce Puerto ↔ JPA
        ├── entity/ProductoEntity.java             ← @Entity de Oracle
        └── repository/ProductoSpringDataRepository.java  ← interfaz JPA
```

### 3.1 `domain/` — el corazón

```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Producto {
    private Long id;
    private String codigo;
    private String nombre;
    private BigDecimal precioBase;
    private EstadoProducto estado;
    private LocalDateTime creadoEn;
}
```

Sin anotaciones de JPA, sin Spring, sin Jackson. Aquí viven las **reglas de negocio reales**. Ejemplos en otros módulos:

- `Oportunidad.getValorPonderado()` → `valor × probabilidad / 100`.
- `Propuesta.recalcularTotales()` → suma los `DetallePropuesta` y actualiza subtotal/total.
- `DetallePropuesta.subtotal()` → `cantidad × precioUnitario`.

Esto es **Information Expert** (GRASP): el dato y su cálculo viven juntos.

### 3.2 `application/dto/` — los contratos de entrada y salida

Son `record` (inmutables) y llevan las validaciones de Jakarta:

```java
public record ProductoRequest(
    @NotBlank @Size(max = 50) String codigo,
    @NotBlank @Size(max = 200) String nombre,
    @NotNull @PositiveOrZero BigDecimal precioBase,
    EstadoProducto estado
) {}
```

El DTO **no** es la entidad. Así se evita exponer el esquema de Oracle y se controla qué campos acepta el cliente.

### 3.3 `application/port/in/` — el caso de uso

```java
public interface ManageProductoUseCase {
    List<ProductoResponse> listar();
    List<ProductoResponse> buscar(String q);
    ProductoResponse obtener(Long id);
    ProductoResponse crear(ProductoRequest request);
    ProductoResponse actualizar(Long id, ProductoRequest request);
    void eliminar(Long id);
}
```

Es la **API interna del módulo**. El controlador depende de esta interfaz, no de la clase `ProductoService`.

### 3.4 `application/port/out/` — lo que el módulo necesita de afuera

```java
public interface ProductoPersistencePort {
    List<Producto> findAll();
    Optional<Producto> findById(Long id);
    boolean existsByCodigo(String codigo);
    Producto save(Producto producto);
    void deleteById(Long id);
}
```

Fíjate en el detalle clave: recibe y devuelve **`Producto` (dominio)**, no `ProductoEntity`. El módulo de aplicación no sabe que existe Oracle.

### 3.5 `application/service/` — el orquestador

```java
@Service
@RequiredArgsConstructor
public class ProductoService implements ManageProductoUseCase {

    private final ProductoPersistencePort persistencePort;

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "productos", key = "'all'")
    public List<ProductoResponse> listar() {
        return persistencePort.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public ProductoResponse crear(ProductoRequest request) {
        if (persistencePort.existsByCodigo(request.codigo())) {
            throw new IllegalArgumentException("Ya existe un producto con el código " + request.codigo());
        }
        Producto producto = Producto.builder()
                .codigo(request.codigo())
                .nombre(request.nombre())
                .precioBase(request.precioBase())
                .estado(request.estado() != null ? request.estado() : EstadoProducto.ACTIVO)
                .creadoEn(LocalDateTime.now())
                .build();
        return toResponse(persistencePort.save(producto));
    }

    private Producto findOrThrow(Long id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }

    private ProductoResponse toResponse(Producto p) { /* Producto → record */ }
}
```

Responsabilidades del service:

1. Aplicar reglas de negocio que necesitan coordinación (código único, estado por defecto).
2. Abrir transacción (`@Transactional`).
3. Gestionar caché (`@Cacheable` / `@CacheEvict`).
4. Traducir dominio → DTO de respuesta.
5. Lanzar excepciones de negocio (`ResourceNotFoundException`, `IllegalArgumentException`).

### 3.6 `adapters/in/web/` — la puerta HTTP

```java
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Catalogo de productos y servicios")
@RequiredArgsConstructor
public class ProductoController {

    private static final CacheControl CACHE_60S =
            CacheControl.maxAge(Duration.ofSeconds(60)).cachePrivate();

    private final ManageProductoUseCase useCase;

    @GetMapping
    @Operation(summary = "Lista productos, filtrando por texto o estado")
    public ResponseEntity<List<ProductoResponse>> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) EstadoProducto estado) {
        var data = /* q/estado → useCase */;
        return ResponseEntity.ok().cacheControl(CACHE_60S).body(data);
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(useCase.crear(request));
    }
}
```

El controlador solo: mapea URL → método, valida (`@Valid`), delega al UseCase y elige el código HTTP + cabeceras de caché.

### 3.7 `adapters/out/persistence/` — la puerta a Oracle

Tres piezas, siempre las mismas:

```java
// 1) La entidad JPA (espejo exacto de la tabla)
@Entity
@Table(name = "CRM_PRODUCTOS")
public class ProductoEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID") private Long id;
    @Column(name = "CODIGO", nullable = false, unique = true, length = 50)
    private String codigo;
    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", length = 20) private EstadoProducto estado;
}

// 2) El repositorio Spring Data (SQL derivado del nombre del método)
@Repository
public interface ProductoSpringDataRepository extends JpaRepository<ProductoEntity, Long> {
    Optional<ProductoEntity> findByCodigo(String codigo);
    boolean existsByCodigo(String codigo);
    List<ProductoEntity> findByNombreContainingIgnoreCaseOrCodigoContainingIgnoreCase(String n, String c);
}

// 3) El adaptador que implementa el puerto y traduce Entity ↔ Domain
@Component
@RequiredArgsConstructor
public class ProductoPersistenceAdapter implements ProductoPersistencePort {

    private final ProductoSpringDataRepository repository;

    @Override
    public List<Producto> findAll() {
        return repository.findAll(Sort.by("id")).stream().map(this::toDomain).toList();
    }

    @Override
    public Producto save(Producto p) {
        return toDomain(repository.save(toEntity(p)));
    }

    private Producto toDomain(ProductoEntity e) { /* ... */ }
    private ProductoEntity toEntity(Producto p) { /* ... */ }
}
```

**Este trío (`Entity` + `SpringDataRepository` + `PersistenceAdapter`) se repite 21 veces**, una por tabla.

---

## 4. El viaje de una petición, paso a paso

Ejemplo: el usuario guarda un producto en el frontend.

```
1. Angular: ProductoForm.onSubmit() → ProductosService.crear(dto)
2. HttpClient agrega cabeceras: Authorization: Bearer <jwt> y X-Trace-ID
3. Backend: CorrelationIdFilter          → guarda traceId en MDC (para logs)
4. Backend: Spring Security (JWT)        → valida firma, expiración y roles
5. Backend: ProductoController.crear()   → @Valid valida el record
6. Backend: ProductoService.crear()      → @Transactional + regla de negocio
7. Backend: ProductoPersistenceAdapter   → toEntity() → repository.save()
8. Hibernate                              → INSERT INTO CRM_PRODUCTOS ...
9. Oracle                                 → devuelve el ID generado
10. Adapter: toDomain()                   → Entity → Producto
11. Service: toResponse()                 → Producto → ProductoResponse
12. Controller: 201 Created + JSON
13. Angular: cierra el diálogo, recarga la lista, muestra snackbar
```

Si algo falla en el paso 5–7, `GlobalExceptionHandler` lo convierte en un JSON uniforme:

```json
{
  "timestamp": "2026-11-22T10:15:30Z",
  "status": 404,
  "error": "Not Found",
  "message": "Producto no encontrado con id: 99",
  "path": "/api/v1/productos/99",
  "traceId": "8f2c..."
}
```

---

## 5. Las cinco variantes del patrón

No todos los módulos son CRUD simple. Hay cinco formas reconocibles:

### Variante A — CRUD plano
`productos`, `etiquetas`, `usuarios`, `fuentes`, `campanias`, `mensajes`.
Un service, un port de persistencia, un adapter. Añaden caché los catálogos (`productos`, `etiquetas`, `fuentes`).

### Variante B — Maestro con hijos (relación 1:N)
`oportunidades`, `actividades`, `equipos`, `propuestas`, `clientes`.
Un service principal + **un port y un adapter por cada hijo**:

| Módulo padre | Hijos (ports propios) |
|---|---|
| `oportunidades` | `OportunidadContacto`, `OportunidadProducto`, `OportunidadColaborador` |
| `actividades` | `ActividadParticipante`, `ActividadEtiqueta` |
| `equipos` | `EquipoMiembro` |
| `propuestas` | `DetallePropuesta` (dentro del agregado) |
| `clientes` | `Contacto` |

`OportunidadService` inyecta **7 dependencias**: 4 puertos de persistencia + 3 puertos de consulta (`ContactoQueryPort`, `ProductoQueryPort`, `UsuarioQueryPort`). Es el service más grande del backend y el mejor ejemplo de orquestación.

### Variante C — Solo lectura / agregación
`dashboard` y `relationshipmap` **no tienen `domain/` ni `entity/`**.

- `dashboard`: `DashboardQueryRepository` usa `EntityManager` con JPQL directo (`SELECT COUNT(c) FROM ClienteEntity c`) y el service arma un único `DashboardResumenResponse`. No crea tablas.
- `relationshipmap`: no consulta la BD; **compone** los UseCase de otros módulos (`ManageClienteUseCase`, `ManageOportunidadUseCase`, …) y construye un grafo de nodos y aristas (`GraphNodeDto`, `GraphEdgeDto`).

### Variante D — Con integración externa
`clientes` es el único con un adaptador de salida que no es BD:

```
DocumentoLookupPort (interfaz)
        ▲
        │ implementa
ApisPeruDocumentoAdapter  →  https://dniruc.apisperu.com/api/v1  (DNI/RUC)
        │
        └── protegido por ConsultaDocumentoRateLimiter (20 consultas / 60 s por usuario)
```

La respuesta cruda del proveedor (`ApisPeruDniResponse`) se traduce al dominio `DocumentoConsultado`; el resto del sistema nunca ve el JSON externo. Esto es un **Adaptador / Capa Anticorrupción**.

### Variante E — Con reglas de estado
`propuestas` y `oportunidades` tienen máquinas de estado simples:

- Propuesta: `BORRADOR → ENVIADA → APROBADA | RECHAZADA | CANCELADA`. Los estados terminales bloquean la edición (`esTerminal()`).
- Oportunidad: `PROSPECCION → CALIFICACION → PROPUESTA → NEGOCIACION → GANADA | PERDIDA`.

---

## 6. `shared/` — la infraestructura transversal

Ruta: `com/viernes/shared/`

| Archivo | Qué hace |
|---|---|
| `cache/CacheConfig.java` | Configura Caffeine: `productos` (TTL largo), `etiquetas`, `fuentes`, `dashboard` (TTL corto). Habilita `@EnableCaching` y `@EnableScheduling`. |
| `cache/CacheRefreshJob.java` | Cada 10 min (`viernes.cache.refresh-ms`) limpia los catálogos y lo registra en el log. |
| `exception/GlobalExceptionHandler.java` | `@RestControllerAdvice`: traduce 12+ excepciones a respuestas JSON uniformes (404, 400, 401, 403, 409, 500). |
| `exception/ResourceNotFoundException.java` | Excepción de negocio → 404. |
| `exception/RateLimitExceededException.java` | → 429. |
| `filter/CorrelationIdFilter.java` | Lee o genera `X-Trace-ID`, lo pone en el MDC de logs y lo devuelve en la respuesta. |
| `ratelimit/ConsultaDocumentoRateLimiter.java` | Ventana deslizante en memoria por usuario. |
| `OpenApiConfig.java` | Título, versión y esquema `bearerAuth` para Swagger UI. |

**Por qué importa:** estos componentes son la razón por la que todos los módulos se ven iguales. No hay `try/catch` repartidos: se lanza la excepción y el handler global responde.

---

## 7. Seguridad: cómo encaja Keycloak

### Backend

```
Keycloak (8180, realm viernes-realm)
      │ emite JWT con realm_access.roles = ["ADMIN","JEFE","VENDEDOR"]
      ▼
KeycloakJwtAuthenticationConverter   → convierte roles en ROLE_ADMIN, ROLE_JEFE, ROLE_VENDEDOR
      ▼
KeycloakSecurityConfig               → decide qué ruta exige qué rol
```

Reglas declaradas (en orden, la primera que coincide gana):

| Método | Ruta | Rol |
|---|---|---|
| GET | `/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/**`, `/static/**` | público |
| POST/PUT/PATCH/DELETE | `/api/v1/usuarios/**` | `ADMIN` |
| PATCH | `/api/v1/propuestas/*/{aprobar,rechazar,cancelar}` | `ADMIN`, `JEFE` |
| DELETE | entidades de negocio | `ADMIN` |
| POST/PUT/PATCH/DELETE | gestión comercial | `ADMIN`, `JEFE` |
| * | cualquier otra | autenticado |

La sesión es **stateless** (`SessionCreationPolicy.STATELESS`) y CORS está limitado a `localhost:4200/8080/3000`.

### Frontend

```
app.config.ts
  ├─ provideKeycloak()            → inicializa keycloak-js (Authorization Code + PKCE S256)
  ├─ provideZonelessChangeDetection()
  └─ provideHttpClient(withInterceptors([traceIdInterceptor, jwtInterceptor]))
```

- `traceIdInterceptor` → agrega `X-Trace-ID` a cada request.
- `jwtInterceptor` → agrega `Authorization: Bearer`, aplica `timeout(15s)`, refresca el token si expiró y redirige a `/login` en 401.
- `authGuard` → bloquea rutas si no hay token.
- `roleGuard` → bloquea rutas si el rol no coincide.

La regla se aplica **dos veces**: en el navegador (UX: no muestro lo que no puedes usar) y en el backend (seguridad real: aunque manipules el frontend, el API rechaza).

---

## 8. Cómo se hablan los módulos entre sí

Spring Modulith permite tres mecanismos, y el proyecto usa los tres:

### 8.1 API pública explícita — `UsuariosApi`

```java
// com/viernes/usuarios/UsuariosApi.java  (paquete raíz = named interface)
public interface UsuariosApi {
    Optional<UsuarioResumen> buscarPorId(Long id);
    Optional<UsuarioResumen> buscarPorCorreo(String correo);
    boolean existePorId(Long id);
    boolean existePorCorreo(String correo);
}
```

Otros módulos **solo** pueden ver `UsuariosApi` y `UsuarioResumen`; nunca `UsuarioService` ni `UsuarioEntity`. `oportunidades`, `equipos` y `mensajeria` lo consumen.

### 8.2 Puertos de consulta + adaptadores locales

Cuando un módulo necesita un dato de otro, define su propio puerto y lo implementa con un adapter que llama a la API pública:

```java
// oportunidades/application/port/out/UsuarioQueryPort.java
public interface UsuarioQueryPort { Optional<UsuarioResumen> buscarPorId(Long id); }

// oportunidades/adapters/out/persistence/UsuarioQueryAdapter.java
@Component
public class UsuarioQueryAdapter implements UsuarioQueryPort {
    private final UsuariosApi usuariosApi;
    public Optional<UsuarioResumen> buscarPorId(Long id) { return usuariosApi.buscarPorId(id); }
}
```

Así `OportunidadService` no depende de `usuarios`; depende de su propio puerto. Si mañana los usuarios vienen de otro servicio, solo cambia el adapter.

### 8.3 Composición de casos de uso — `relationshipmap`

`RelationshipMapService` inyecta cinco `Manage*UseCase` y construye el grafo:

```
Cliente ─── Contacto
   ├────── Oportunidad ─── Producto
   ├────── Actividad ───── Participante (Usuario)
   └────── Propuesta ───── Detalle (Producto)
```

Es el único módulo que **no tiene persistencia propia**: es pura orquestación de lectura.

---

## 9. Persistencia: 21 entidades, 21 tablas, 21 tablespaces

### Convenciones

- Tabla: `CRM_<PLURAL>` (`CRM_CLIENTES`, `CRM_OPORTUNIDADES`).
- Columna: `SNAKE_CASE` (`PRECIO_BASE`, `FECHA_CIERRE`).
- PK: `ID` con `GenerationType.IDENTITY` (Oracle `GENERATED BY DEFAULT AS IDENTITY`).
- Fechas: `LocalDateTime` → `TIMESTAMP`.
- Dinero: `BigDecimal` → `NUMBER(14,2)`.
- Enums: `@Enumerated(EnumType.STRING)` → `VARCHAR2`.

### Relaciones N:M sin `@ManyToMany`

Decisión explícita del proyecto: **toda N:M se modela con una entidad intermedia propia**.

| Tabla puente | Entidad | Une |
|---|---|---|
| `CRM_OPORTUNIDAD_CONTACTOS` | `OportunidadContacto` | oportunidad ↔ contacto |
| `CRM_OPORTUNIDAD_PRODUCTOS` | `OportunidadProducto` | oportunidad ↔ producto |
| `CRM_OPORTUNIDAD_COLABORADORES` | `OportunidadColaborador` | oportunidad ↔ usuario |
| `CRM_ACTIVIDAD_PARTICIPANTES` | `ActividadParticipante` | actividad ↔ usuario |
| `CRM_ACTIVIDAD_ETIQUETAS` | `ActividadEtiqueta` | actividad ↔ etiqueta |
| `CRM_EQUIPO_MIEMBROS` | `EquipoMiembro` | equipo ↔ usuario |
| `CRM_CAMPANIA_LEADS` | `CampaniaLead` | campaña ↔ lead |

Ventaja: la tabla puente puede tener atributos propios (rol en la oportunidad, asistencia, fecha de ingreso al equipo) y el agregado controla sus invariantes.

### `ddl-auto: update` y el DDL canónico

En `application-dev.yml`:

```yaml
spring.jpa.hibernate.ddl-auto: update
```

Hibernate crea/ajusta tablas si faltan, pero **el DDL canónico y reproducible vive en** `viernes-backend/db/init/`:

| Script | Contenido |
|---|---|
| `001_tablespaces.sql` | 22 tablespaces (uno por tabla + `VIERNES_TEMP`) |
| `002_usuario.sql` | usuario `BOM_VENTAS` y cuotas |
| `003_esquema.sql` | 21 tablas, constraints, índices |
| `004_plsql.sql` | 6 funciones, 1 procedimiento, 5 vistas, 1 trigger |
| `005_seed.sql` | datos demo |
| `006_verificacion.sql` | conteos y comprobaciones |

### Capa PL/SQL: solo reportes, no CRUD

El criterio es deliberado: **las reglas transaccionales viven en Java**; Oracle contiene agregados de reporte y reglas de identidad.

| Objeto | Qué hace |
|---|---|
| `FN_VIERNES_NOMBRE_USUARIO(id)` | Nombre completo desde `CRM_USUARIOS` |
| `FN_VIERNES_NOMBRE_CLIENTE(id)` | `NVL(EMPRESA, NOMBRE)` |
| `FN_VIERNES_VALOR_PONDERADO(valor, prob)` | `valor × prob / 100` |
| `FN_VIERNES_TOTAL_PONDERADO(cliente)` | Suma ponderada del pipeline del cliente |
| `FN_VIERNES_TOTAL_PIPELINE(etapa, cliente)` | Pipeline con `PIPELINED`, excluye `GANADA`/`PERDIDA` |
| `FN_VIERNES_TASA_CONVERSION(desde, hasta)` | % de oportunidades que avanzan de etapa |
| `PR_VIERNES_RESUMEN_CLIENTE(id, OUT ...)` | Resumen por cliente (reutiliza `FN_..._NOMBRE_CLIENTE`) |
| 5 vistas `VW_VIERNES_*` | Pipeline, actividades, top clientes, rendimiento, tablespaces |

---

## 10. Frontend Angular: estructura y patrones

Ruta: `viernes-frontend/src/app/`

```
app/
├── app.config.ts        ← providers globales (router, http, keycloak, zoneless)
├── app.routes.ts        ← 36 rutas con carga diferida
├── core/
│   ├── auth/            ← guards, AuthService, Keycloak
│   ├── interceptors/    ← jwt + traceId
│   ├── layout/          ← shell con sidenav y toolbar
│   └── services/        ← ApiService, DraftService
├── features/            ← 14 carpetas, una por módulo del backend
│   ├── clientes/
│   │   ├── cliente-list/    (tabla)
│   │   ├── cliente-form/    (formulario de página completa)
│   │   ├── cliente-dialog/  (formulario en diálogo)
│   │   ├── clientes.model.ts   (interfaces = espejo de los DTO)
│   │   └── clientes.service.ts (HttpClient)
└── shared/
    └── table-sort.ts    ← ordenamiento local reutilizable
```

### 10.1 Rutas

Todas usan `loadComponent` (lazy loading) y cuelgan del `Layout`, protegido por `authGuard`:

```ts
{ path: '', loadComponent: () => import('./core/layout/layout').then(m => m.Layout),
  canActivate: [authGuard],
  children: [
    { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
    { path: 'dashboard',  loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard) },
    { path: 'productos',  loadComponent: () => import('.../producto-list').then(m => m.ProductoList) },
    { path: 'productos/nuevo', loadComponent: () => import('.../producto-form').then(m => m.ProductoForm) },
    ...
  ]
}
```

### 10.2 Tres piezas por feature

**a) `*.model.ts`** — interfaces TypeScript que replican los `record` del backend:

```ts
export interface ProductoRequest {
  codigo: string; nombre: string; descripcion?: string;
  categoria?: string; precioBase: number; estado?: EstadoProducto;
}
export interface ProductoResponse extends ProductoRequest { id: number; creadoEn: string; }
```

**b) `*.service.ts`** — un envoltorio fino sobre `HttpClient`:

```ts
@Injectable({ providedIn: 'root' })
export class ProductosService {
  private path = '/productos';
  constructor(private http: HttpClient, private apiService: ApiService) {}

  listar(q?: string, estado?: EstadoProducto): Observable<ProductoResponse[]> {
    let params = new HttpParams();
    if (q) params = params.set('q', q);
    if (estado) params = params.set('estado', estado);
    return this.http.get<ProductoResponse[]>(this.apiService.buildUrl(this.path), { params });
  }

  crear(p: ProductoRequest) { return this.http.post<ProductoResponse>(this.apiService.buildUrl(this.path), p); }
  actualizar(id: number, p: ProductoRequest) { return this.http.put<ProductoResponse>(this.apiService.buildUrl(`${this.path}/${id}`), p); }
  eliminar(id: number) { return this.http.delete<void>(this.apiService.buildUrl(`${this.path}/${id}`)); }
}
```

`ApiService.buildUrl()` centraliza `environment.apiBaseUrl` (`http://localhost:8080/api/v1`). **Se usa 82 veces**; es el punto único para cambiar el host.

**c) Componentes standalone** — 27 componentes, todos `standalone: true`, con `inject()` en vez de constructor y `signal()` para estado reactivo:

```ts
@Component({ selector: 'app-producto-list', standalone: true, imports: [/* Material */] })
export class ProductoList implements OnInit {
  private service = inject(ProductosService);
  private dialog  = inject(MatDialog);

  productos = signal<ProductoResponse[]>([]);
  filtro    = signal('');
  visibles  = computed(() => /* filtra por filtro() */);

  ngOnInit() { this.cargar(); }

  abrirFormulario(p?: ProductoResponse) {
    this.dialog.open(ProductoForm, { data: { id: p?.id } })
      .afterClosed().subscribe(r => { if (r?.saved) this.cargar(); });
  }
}
```

### 10.3 El doble uso de los formularios

Un mismo componente (`ProductoForm`) sirve como **página** (`/productos/nuevo`) y como **diálogo** (`MatDialog`). Lo resuelve inyectando `MatDialogRef` y `MAT_DIALOG_DATA` como opcionales:

```ts
private dialogRef  = inject<MatDialogRef<...> | null>(MatDialogRef, { optional: true });
private dialogData = inject<ProductoDialogData | null>(MAT_DIALOG_DATA, { optional: true });
readonly esDialogo = this.dialogRef !== null;
```

Si `esDialogo` es `true`, guarda y cierra el diálogo; si es `false`, navega a la lista. Esto evita duplicar el formulario y **se repite en clientes, productos, equipos, etiquetas**.

### 10.4 `DraftService` — autoguardado de borradores

Los formularios largos (oportunidad, propuesta) llaman a `DraftService` con `debounceTime` para guardar el estado parcial en `localStorage`. Si el usuario cierra el navegador, al volver recupera lo escrito.

### 10.5 `table-sort.ts`

Utilidad compartida que ordena en cliente respetando números, booleanos, nulos y texto en español (`localeCompare('es', { numeric: true })`). Evita repetir la misma lógica de orden en cada tabla.

---

## 11. Los patrones que más se repiten (con números reales)

### Backend — 259 archivos `.java`

| Patrón | Ocurrencias | Dónde |
|---|---|---|
| `ResponseEntity` | 182 | todos los controllers |
| `@Transactional` | 89 | todos los services |
| `@Builder` | 76 | dominio + entidades |
| `findOrThrow(...)` | 67 | services (método privado repetido) |
| `record` (DTO) | 50 | `application/dto` |
| `@RequiredArgsConstructor` | 46 | inyección por constructor |
| `implements ...PersistencePort` | 40 | adapters de salida |
| `toEntity(` | 40 | adapters de salida |
| `toDomain(` | 38 | adapters de salida |
| `JpaRepository` | 38 | repositorios Spring Data |
| `@Component` | 33 | adapters |
| `orElseThrow(` | 28 | services |
| `@Entity` | 21 | una por tabla |
| `@RestController` | 15 | controllers |
| `@Cacheable` | 15 | catálogos y dashboard |
| `@Service` | 14 | un service por módulo |
| `@CacheEvict` | 9 | mutaciones de catálogos |

### Frontend — 70 archivos `.ts`

| Patrón | Ocurrencias |
|---|---|
| `inject(...)` | 128 |
| `Observable<...>` | 86 |
| `buildUrl(...)` | 82 |
| `signal(...)` | 63 |
| `MatDialog` | 46 |
| `@Component` | 28 |
| `HttpClient` | 28 |
| `standalone: true` | 27 |
| `FormBuilder` | 24 |
| `takeUntilDestroyed` | 20 |
| `@Injectable` | 16 |
| `catchError` | 8 |

### Los 8 patrones estructurales que definen el proyecto

1. **Hexagonal por módulo** — `domain` ← `application` ← `adapters`.
2. **Puerto de entrada** (`Manage*UseCase`) — el controller depende de la interfaz.
3. **Puerto de salida** (`*PersistencePort`) — el service no conoce JPA.
4. **Adapter de persistencia** — `Entity` ↔ `Domain` con `toDomain` / `toEntity`.
5. **DTO como `record`** — inmutable, con validaciones Jakarta.
6. **Excepción + handler global** — nada de `try/catch` en controllers.
7. **Caché declarativa** — `@Cacheable` / `@CacheEvict` sobre catálogos.
8. **Feature folder en Angular** — `model` + `service` + `list` + `form` + `dialog`.

---

## 12. Guía práctica: dónde tocar cada cosa

| Quiero… | Archivo a tocar |
|---|---|
| Agregar un campo a productos | `ProductoEntity`, `ProductoRequest`, `ProductoResponse`, `Producto` y `producto.model.ts` |
| Agregar un endpoint | `*Controller` + método en `Manage*UseCase` + implementación en el service |
| Cambiar una regla de negocio | `domain/` (si es cálculo puro) o `application/service/` (si necesita coordinar) |
| Cambiar cómo se guarda | `*PersistenceAdapter` (no el service) |
| Agregar otra fuente externa | nuevo `*Port` + nuevo `*Adapter` en `adapters/out/external` |
| Cambiar roles/permisos | `KeycloakSecurityConfig` (backend) y `role.guard.ts` / rutas (frontend) |
| Cambiar la URL del API | `environments/environment.ts` (frontend) y `application-dev.yml` (backend) |
| Agregar una tabla | `db/init/003_esquema.sql` + `001_tablespaces.sql` + nueva `@Entity` |
| Agregar un reporte SQL | `db/init/004_plsql.sql` (función/vista nueva) |
| Cambiar el TTL de caché | `shared/cache/CacheConfig.java` |
| Ver qué módulos pueden importarse | `package-info.java` de cada módulo |

---

## 13. Resumen final

> **Viernes CRM V1 es un monolito modular donde cada módulo es un hexágono independiente.** El controlador traduce HTTP, el service aplica reglas y transacciones, el puerto de salida aísla la persistencia y el adaptador traduce entre el dominio y Oracle. Los módulos se comunican solo por APIs públicas (`UsuariosApi`) o por puertos de consulta locales. Keycloak autentica y el backend autoriza por rol. Oracle guarda 21 tablas, una por entidad, cada una en su propio tablespace, y solo expone PL/SQL para reportes. El frontend replica la misma división en `features/`, con un servicio HTTP por módulo, formularios reutilizables como página o diálogo, y estado reactivo con `signal()`.

**La consecuencia práctica:** para entender cualquier módulo, lee siempre en este orden:

```
domain/  →  application/port/in  →  application/service  →  adapters/in/web
                                  ↘  application/port/out  →  adapters/out/persistence
```

Si entiendes `productos`, entiendes el 80 % del backend. Si entiendes `oportunidades`, entiendes el otro 20 % (hijos, N:M y validaciones cruzadas).
