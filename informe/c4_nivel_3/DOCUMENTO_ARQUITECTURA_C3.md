# VIERNES CRM V1 — ARQUITECTURA C4 NIVEL 3 (COMPONENTES)
## Actualización Arquitectónica del Backend Monolítico Modular

**Fecha:** 11 de noviembre de 2026  
**Proyecto:** Viernes CRM V1 — CRM Híbrido de Gestión Comercial y Marketing  
**Ubicación de artefactos:** `e:\Viernes\Viernes\informe\c4_nivel_3\`  
**Estado:** Conciliado al 100% con el árbol de código Java, base de datos Oracle 23c y configuración Keycloak.

---

## 1. Introducción y Contexto

Durante la Sesión 2 del curso se elaboró en Lucidchart un diagrama C4 Nivel 3 (Componentes) preliminar. Dicho esquema reflejaba una concepción temprana del sistema con las siguientes características:
- Modelo relacional exclusivamente 1:N o desacoplado por IDs escalares.
- Cero relaciones muchos a muchos (N:M).
- Autenticación básica local sin proveedor de identidad externo.
- Ausencia de módulos comerciales y analíticos como `propuestas`, `relationshipmap`, `equipos`, `productos`, `etiquetas` y `marketing`.

Posteriormente, el sistema evolucionó hacia un **Monolito Modular gobernado por Spring Modulith 2.0.7 sobre Spring Boot 4.0.7**, implementando **Arquitectura Hexagonal estricta** en todos sus paquetes, integrando **7 relaciones muchos a muchos (N:M)** mediante entidades intermedias en Oracle, y desacoplando la seguridad hacia un **servidor de identidad Keycloak 25 (OAuth2 / OIDC)**.

El presente documento formaliza los nuevos diagramas C3 de componentes generados en PlantUML (con salidas vectoriales SVG y de mapa de bits PNG), aptos para su incorporación directa en el documento Word del Proyecto Integrador.

---

## 2. Diagrama C3 Principal: Arquitectura de Componentes del Backend

### 2.1 Vista General
El backend de Viernes CRM se ejecuta como una única unidad de despliegue (`viernes-backend-0.0.1-SNAPSHOT.jar`), pero está particionado internamente en 15 submódulos de dominio y soporte.

- **Diagrama Editable PlantUML:** [`c3_principal_backend.puml`](file:///e:/Viernes/Viernes/informe/c4_nivel_3/c3_principal_backend.puml)
- **Imagen Vectorial SVG:** [`c3_principal_backend.svg`](file:///e:/Viernes/Viernes/informe/c4_nivel_3/c3_principal_backend.svg)
- **Imagen de Alta Resolución PNG:** [`c3_principal_backend.png`](file:///e:/Viernes/Viernes/informe/c4_nivel_3/c3_principal_backend.png)

### 2.2 Inventario de Componentes y Responsabilidades

| Componente | Tipo / Estereotipo | Archivo / Paquete | Responsabilidad en la Arquitectura |
|---|---|---|---|
| `KeycloakSecurityConfig` | Spring Security 6 Configuration | `com.viernes.auth.keycloak` | Configura el Resource Server OAuth2 sin estado. Define la matriz de autorización por rol (`ADMIN`, `JEFE`, `VENDEDOR`) sobre las rutas `/api/v1/**`. |
| `KeycloakJwtAuthenticationConverter` | Converter (Capa Anticorrupción) | `com.viernes.auth.keycloak` | Traduce claims `realm_access.roles` del JWT de Keycloak hacia autoridades `ROLE_*` de Spring Security, aislando roles internos del proveedor. |
| `ClienteController` | REST Controller (Inbound Adapter) | `com.viernes.clientes.adapters.in.web` | Expone endpoints `/api/v1/clientes` para gestión de cuentas comerciales, contactos subordinados y consulta de padrones. |
| `ClienteService` | Application Service (`@Transactional`) | `com.viernes.clientes.application.service` | Orquesta la creación y modificación de empresas y contactos; gestiona la consulta oficial de documentos. |
| `ClientePersistenceAdapter` | Persistence Adapter (Outbound) | `com.viernes.clientes.adapters.out.persistence` | Implementa `ClientePersistencePort`. Mapea entre objetos de dominio y entidades `CRM_CLIENTES` y `CRM_CONTACTOS`. |
| `ApisPeruDocumentoAdapter` | External Lookup Adapter | `com.viernes.clientes.adapters.out.external` | Consume API REST externa de APIsPerú para DNI (RENIEC) y RUC (SUNAT), aplicando caché Caffeine y control de cuota. |
| `OportunidadController` | REST Controller (Inbound Adapter) | `com.viernes.oportunidades.adapters.in.web` | Expone endpoints del pipeline y subrecursos N:M (`/contactos`, `/productos`, `/colaboradores`). |
| `OportunidadService` | Application Service (`@Transactional`) | `com.viernes.oportunidades.application.service` | Controla las 7 etapas de la negociación comercial, cálculo de valor ponderado y sincronización de asociaciones N:M. |
| `PropuestaController` | REST Controller (Inbound Adapter) | `com.viernes.propuestas.adapters.in.web` | Endpoints de cotizaciones formales y transiciones de estado (`/aprobar`, `/rechazar`, `/cancelar`, `/enviar`). |
| `PropuestaService` | Application Service (`@Transactional`) | `com.viernes.propuestas.application.service` | Máquina de estados finita de cotizaciones; asegura recálculo de subtotales, totales y descuentos. |
| `ActividadController` | REST Controller (Inbound Adapter) | `com.viernes.actividades.adapters.in.web` | Gestión operativa de tareas, llamadas y reuniones con filtros avanzados y subrecursos de participantes. |
| `ActividadService` | Application Service (`@Transactional`) | `com.viernes.actividades.application.service` | Aplica invariante de máximo 8 etiquetas (`MAX_ETIQUETAS = 8`), cálculo dinámico de actividades vencidas y validación cruzada. |
| `LeadController` / `FuenteController` | REST Controllers | `com.viernes.leads.adapters.in.web` | Endpoints para prospección comercial y clasificación por canales de origen. |
| `DashboardService` | Facade / Analytics Component | `com.viernes.dashboard.application.service` | Fachada de agregación que consolida métricas comerciales desde múltiples repositorios, optimizada con caché Caffeine (30s TTL). |
| `RelationshipMapService` | Graph Processing Engine | `com.viernes.relationshipmap` | Ensambla estructuras de grafos nodales centrados en clientes u oportunidades para su representación en Cytoscape.js. |
| `UsuariosApi` | Named Public Interface | `com.viernes.usuarios` | Contrato público expuesto por el módulo Usuarios para consultas seguras entre módulos según reglas de Spring Modulith. |
| `CacheConfig` | Infrastructure Component | `com.viernes.shared.cache` | Configura el gestor de caché Caffeine con políticas de expiración multiescala diferenciadas por volatilidad. |
| `ConsultaDocumentoRateLimiter` | Rate Limiting Component | `com.viernes.shared.ratelimit` | Limita las consultas DNI/RUC a 20 peticiones por minuto por usuario autenticado para preservar cuota del servicio externo. |
| `CorrelationIdFilter` | Servlet Filter Component | `com.viernes.shared.filter` | Extrae o genera el encabezado HTTP `X-Trace-ID` y lo asocia al contexto de registro MDC de SLF4J para trazabilidad distribuida. |

---

## 3. Diagrama C3 del Módulo Oportunidades: Componentes Internos y Relaciones N:M

### 3.1 Vista de Detalle
El módulo Oportunidades constituye el núcleo transaccional del pipeline de ventas. Gestiona tres relaciones muchos a muchos (N:M) críticas mediante tablas intermedias dedicadas en Oracle.

- **Diagrama Editable PlantUML:** [`c3_modulo_oportunidades.puml`](file:///e:/Viernes/Viernes/informe/c4_nivel_3/c3_modulo_oportunidades.puml)
- **Imagen Vectorial SVG:** [`c3_modulo_oportunidades.svg`](file:///e:/Viernes/Viernes/informe/c4_nivel_3/c3_modulo_oportunidades.svg)
- **Imagen de Alta Resolución PNG:** [`c3_modulo_oportunidades.png`](file:///e:/Viernes/Viernes/informe/c4_nivel_3/c3_modulo_oportunidades.png)

### 3.2 Arquitectura Interna y Manejo de Relaciones N:M

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 OportunidadController                                  │
│                 (Endpoints REST: /api/v1/oportunidades y subrecursos N:M)              │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                               ManageOportunidadUseCase                                 │
└───────────────────────────────────────────┬────────────────────────────────────────────┘
                                            │
                                            ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                  OportunidadService                                    │
│                 (Orquestador de Negocio: valor ponderado y asociaciones)               │
└─────┬───────────────────┬───────────────────┬───────────────────┬──────────────────────┘
      │                   │                   │                   │
      ▼                   ▼                   ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Oportunidad │     │   Contacto  │     │   Producto  │     │ Colaborador │
│ Persistence │     │ Persistence │     │ Persistence │     │ Persistence │
│    Port     │     │  Port (N:M) │     │  Port (N:M) │     │  Port (N:M) │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Oportunidad │     │   Contacto  │     │   Producto  │     │ Colaborador │
│ Persistence │     │ Persistence │     │ Persistence │     │ Persistence │
│   Adapter   │     │   Adapter   │     │   Adapter   │     │   Adapter   │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│     CRM_    │     │     CRM_    │     │     CRM_    │     │     CRM_    │
│OPORTUNIDADES│     │ OPORTUNIDAD_│     │ OPORTUNIDAD_│     │ OPORTUNIDAD_│
│             │     │  CONTACTOS  │     │  PRODUCTOS  │     │COLABORADORES│
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

1. **Oportunidad ↔ Contacto:**
   - **Entidad de Dominio:** `OportunidadContacto` (atributos: `rol`, `esDecisor`).
   - **Puerto:** `OportunidadContactoPersistencePort`.
   - **Adaptador:** `OportunidadContactoPersistenceAdapter`.
   - **Persistencia Física:** Tabla Oracle `CRM_OPORTUNIDAD_CONTACTOS` con clave única `UK(id_oportunidad, id_contacto)`.
   - **Validación Cruzada:** `ContactoQueryAdapter` consume `ManageClienteUseCase` para asegurar que el contacto exista y pertenezca al cliente de la oportunidad.

2. **Oportunidad ↔ Producto:**
   - **Entidad de Dominio:** `OportunidadProducto` (atributo: `cantidad`).
   - **Puerto:** `OportunidadProductoPersistencePort`.
   - **Adaptador:** `OportunidadProductoPersistenceAdapter`.
   - **Persistencia Física:** Tabla Oracle `CRM_OPORTUNIDAD_PRODUCTOS` con `UK(id_oportunidad, id_producto)`.
   - **Validación Cruzada:** `ProductoQueryAdapter` consume `ProductoPersistencePort` para comprobar existencia y precio unitario vigente.

3. **Oportunidad ↔ Colaborador:**
   - **Entidad de Dominio:** `OportunidadColaborador` (atributos: `idUsuario`, `creadoEn`).
   - **Puerto:** `OportunidadColaboradorPersistencePort`.
   - **Adaptador:** `OportunidadColaboradorPersistenceAdapter`.
   - **Persistencia Física:** Tabla Oracle `CRM_OPORTUNIDAD_COLABORADORES` con `UK(id_oportunidad, id_usuario)`.
   - **Validación Cruzada:** `UsuarioQueryAdapter` consume la interfaz pública `UsuariosApi` para comprobar que el usuario asignado esté activo.

---

## 4. Diagrama C3 del Módulo Actividades: Componentes Internos, Participantes y Etiquetas N:M

### 4.1 Vista de Detalle
El módulo Actividades administra el registro cronológico de eventos comerciales e implementa dos relaciones N:M con reglas estrictas de consistencia.

- **Diagrama Editable PlantUML:** [`c3_modulo_actividades.puml`](file:///e:/Viernes/Viernes/informe/c4_nivel_3/c3_modulo_actividades.puml)
- **Imagen Vectorial SVG:** [`c3_modulo_actividades.svg`](file:///e:/Viernes/Viernes/informe/c4_nivel_3/c3_modulo_actividades.svg)
- **Imagen de Alta Resolución PNG:** [`c3_modulo_actividades.png`](file:///e:/Viernes/Viernes/informe/c4_nivel_3/c3_modulo_actividades.png)

### 4.2 Arquitectura Interna y Manejo de Relaciones N:M

1. **Actividad ↔ Contacto Participante:**
   - **Entidad de Dominio:** `ActividadParticipante` (atributos: `idContacto`, `creadoEn`).
   - **Puerto:** `ActividadParticipantePersistencePort`.
   - **Adaptador:** `ActividadParticipantePersistenceAdapter`.
   - **Persistencia Física:** Tabla Oracle `CRM_ACTIVIDAD_PARTICIPANTES` con `UK(id_actividad, id_contacto)`.
   - **Consulta Cruzada:** `ContactoQueryAdapter` consulta `ManageClienteUseCase` para enriquecer la respuesta JSON con nombres, correo y teléfono del contacto.

2. **Actividad ↔ Etiqueta Taxonómica:**
   - **Entidad de Dominio:** `ActividadEtiqueta` (atributos: `idEtiqueta`, `orden`, `creadoEn`).
   - **Puerto:** `ActividadEtiquetaPersistencePort`.
   - **Adaptador:** `ActividadEtiquetaPersistenceAdapter`.
   - **Persistencia Física:** Tabla Oracle `CRM_ACTIVIDAD_ETIQUETAS` con `UK(id_actividad, id_etiqueta)`.
   - **Invariante de Negocio:** La clase `ActividadService` impone la constante `MAX_ETIQUETAS = 8`. Si el usuario envía más de 8 identificadores, se rechaza la solicitud.
   - **Sincronización Atómica:** El adaptador ejecuta `reemplazarEtiquetas()`, eliminando los registros previos de la actividad e insertando los nuevos manteniendo la secuencia de visualización mediante el campo `orden`.

3. **Validación Referencial Desacoplada (`RelacionValidationPort`):**
   - Antes de persistir una actividad, `RelacionValidationAdapter` valida la existencia de las entidades vinculadas sin acoplar el servicio de aplicación:
     * `existeCliente(idCliente)`: verifica en `ClienteSpringDataRepository`.
     * `existeLead(idLead)`: verifica en `LeadSpringDataRepository`.
     * `existeOportunidad(idOportunidad)`: verifica en `OportunidadSpringDataRepository`.
     * `existeUsuario(idUsuario)`: verifica mediante `UsuariosApi` en el módulo Usuarios.

---

## 5. Tabla Comparativa de Cambios respecto al Diagrama C3 Antiguo (Sesión 2 en Lucidchart)

| Elemento Arquitectónico | Estado en C3 Antiguo (Lucidchart S2) | Estado Actual en C3 Verificado (Noviembre 2026) | Justificación del Cambio Técnico |
|---|---|---|---|
| **Estructura Arquitectónica** | Monolito tradicional en capas | **Monolito Modular con Spring Modulith** | Se formalizaron 15 módulos con límites verificados en compilación por `ModularityTests.java`. |
| **Arquitectura Interna** | Modelo anémico dependiente de JPA | **Arquitectura Hexagonal Estricta** | Separación en `domain/` (POJOs puros), `application/` (puertos y servicios) y `adapters/` (web, persistence, external). |
| **Cantidad de Módulos** | 8 módulos básicos | **15 módulos documentados** | Se incorporaron formalmente `propuestas`, `relationshipmap`, `equipos`, `productos`, `etiquetas` y `marketing`. |
| **Relaciones N:M** | Inexistentes (0 relaciones N:M; solo claves foráneas directas 1:N) | **7 relaciones N:M implementadas con tablas puente** | Cada relación N:M cuenta con entidad intermedia propia, repositorio, adaptador y tabla física en Oracle. |
| **Relación Oportunidad-Contactos** | Contacto único por oportunidad | **N:M mediante `CRM_OPORTUNIDAD_CONTACTOS`** | Soporta múltiples contactos por negociación con roles (`rol`) y condición de tomador de decisión (`esDecisor`). |
| **Relación Oportunidad-Productos** | Inexistente (no existía módulo productos) | **N:M mediante `CRM_OPORTUNIDAD_PRODUCTOS`** | Permite cotizar múltiples productos con especificación de cantidades por oportunidad. |
| **Relación Oportunidad-Colaboradores** | Responsable único como String libre | **N:M mediante `CRM_OPORTUNIDAD_COLABORADORES`** | Admite equipos comerciales multi-vendedor asignados a una misma oportunidad. |
| **Relación Actividad-Participantes** | Contacto único implícito | **N:M mediante `CRM_ACTIVIDAD_PARTICIPANTES`** | Múltiples contactos participantes por llamada, reunión o tarea de seguimiento. |
| **Relación Actividad-Etiquetas** | Campo String libre de etiquetas | **N:M mediante `CRM_ACTIVIDAD_ETIQUETAS`** | Categorización estructurada con orden de presentación y límite máximo de 8 etiquetas. |
| **Relación Equipos-Usuarios** | Inexistente (no existía módulo equipos) | **N:M mediante `CRM_EQUIPO_MIEMBROS`** | Estructura organizativa que agrupa colaboradores comerciales en fuerzas de venta. |
| **Relación Campaña-Leads** | Inexistente (no existía módulo marketing) | **N:M mediante `CRM_CAMPANIA_LEADS`** | Enlace entre prospectos y campañas de origen (implementado en backend y base de datos). |
| **Seguridad y Acceso** | Autenticación local con JWT HS256 simple | **OAuth2 Resource Server con Keycloak 25** | Delegación de identidad a Keycloak mediante Authorization Code + PKCE en frontend y validación JWT en backend. |
| **Capa Anticorrupción (ACL)** | Inexistente | **Implementada en `com.viernes.auth`** | `KeycloakJwtAuthenticationConverter` traduce roles técnicos externos a las autoridades del dominio. |
| **Integración con RENIEC / SUNAT** | Inexistente | **Implementada vía `ApisPeruDocumentoAdapter`** | Consulta en línea con soporte de caché Caffeine y limitador de tasa (20 req/min). |
| **Motor de Grafos Relacionales** | Inexistente | **Implementado vía `RelationshipMapService`** | Algoritmo de agregación que construye topologías de red en formato Cytoscape.js. |
| **Persistencia Oracle** | 10 tablas físicas en esquema preliminar | **26 tablas físicas en esquema `BOM_VENTAS`** | Modelo físico normalizado con 22 claves foráneas y 51 índices verificados. |

---

## 6. Lista de Elementos que No Pudieron Verificarse o Limitaciones del Sistema

Conforme al estándar de honestidad académica y técnica del informe:

1. **Interfaz de Usuario para Campañas de Marketing:**  
   El módulo `marketing` y la relación N:M `CampaniaLead` están completamente implementados en backend (entidad `CampaniaEntity`, puerto `CampaniaLeadPersistencePort`, adaptador y tabla `CRM_CAMPANIA_LEADS`), pero **carecen de vista en el frontend Angular** (la ruta `/campanas` redirige al dashboard).
2. **Máquina de Estados Formal en Leads:**  
   Aunque el agregado `Propuesta` posee una máquina de estados estricta (`esEstadoTerminal()`), el agregado `Lead` permite transiciones arbitrarias entre sus 5 estados mediante peticiones PATCH sin validar precondiciones de negocio en el servicio.
3. **Servidor SMTP para Envío Físico de Propuestas:**  
   La transición de propuesta a estado `ENVIADA` actualiza el registro en la base de datos Oracle, pero no dispara el despacho de un correo electrónico mediante un servidor de mensajería SMTP configurado.
4. **Endpoint de Eliminación Física de Usuarios:**  
   El puerto `UsuarioPersistencePort` declara el método `deleteById(Long id)`, pero `UsuarioController` no expone ningún endpoint HTTP DELETE por decisión de integridad referencial histórica.
5. **Conversión Automatizada Lead → Cliente/Oportunidad:**  
   Marcar un lead como `CONVERTIDO` es una actualización de estado del prospecto; el sistema no ejecuta una transacción compuesta que cree automáticamente la entidad `Cliente` ni la `Oportunidad`.

