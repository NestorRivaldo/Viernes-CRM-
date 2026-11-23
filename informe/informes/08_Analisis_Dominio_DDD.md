# CAPÍTULO 08 — ANÁLISIS DEL DOMINIO (DDD)

## 8.1 Introducción

El presente capítulo analiza el modelo de dominio de Viernes CRM V1 aplicando los conceptos del Diseño Dirigido por el Dominio (Domain-Driven Design). Se documenta la estructura resultante del código fuente actual —no de diagramas históricos— verificando entidades, objetos de valor, agregados, puertos, servicios y reglas de negocio directamente en la implementación Java.

---

## 8.2 Dominio Principal y Subdominios

### 8.2.1 Dominio principal

**Gestión Comercial CRM**: sistema híbrido de gestión comercial y marketing orientado a la captación, calificación y cierre de oportunidades de venta para empresas peruanas del segmento B2B.

### 8.2.2 Clasificación de subdominios

| Subdominio | Tipo | Paquete Java | Justificación |
|---|---|---|---|
| **Clientes** | Core | `com.viernes.clientes` | Entidad central del CRM; gestiona empresas, contactos y consultas de documentos RENIEC/SUNAT. |
| **Oportunidades** | Core | `com.viernes.oportunidades` | Pipeline de ventas con relaciones N:M a contactos, productos y colaboradores. Contiene la lógica de valor ponderado. |
| **Propuestas** | Core | `com.viernes.propuestas` | Cotizaciones formales con detalles, cálculo de totales y máquina de estados. |
| **Actividades** | Core | `com.viernes.actividades` | Seguimiento operativo de llamadas, reuniones y tareas con participantes y etiquetas. |
| **Leads** | Core | `com.viernes.leads` | Captación y calificación de prospectos con fuentes de origen. |
| **Marketing** | Soporte | `com.viernes.marketing` | Campañas asociadas a leads. Complementa la captación sin lógica propia compleja. |
| **Productos** | Soporte | `com.viernes.productos` | Catálogo de productos/servicios referenciados desde oportunidades y propuestas. |
| **Etiquetas** | Soporte | `com.viernes.etiquetas` | Catálogo transversal de etiquetas para clasificación de actividades. |
| **Equipos** | Soporte | `com.viernes.equipos` | Agrupación de usuarios en equipos de trabajo con relación N:M. |
| **Usuarios** | Soporte | `com.viernes.usuarios` | Gestión de identidad interna, roles y estados. |
| **Mensajería** | Soporte | `com.viernes.mensajeria` | Canal de mensajes internos del equipo. |
| **Dashboard** | Genérico | `com.viernes.dashboard` | Agregación de KPIs de lectura. No posee entidades propias. |
| **Relationship Map** | Genérico | `com.viernes.relationshipmap` | Visualización de grafos de relaciones. Consume datos de múltiples módulos sin estado propio. |
| **Auth** | Genérico | `com.viernes.auth` | Configuración de seguridad y capa anticorrupción con Keycloak. |
| **Shared** | Genérico | `com.viernes.shared` | Excepciones, caché, rate limiting, OpenAPI y trazabilidad. |

---

## 8.3 Entidades y Objetos de Valor

### 8.3.1 Entidades de dominio por módulo

| Módulo | Entidad de dominio | Archivo |
|---|---|---|
| Clientes | `Cliente` | `clientes/domain/Cliente.java` |
| Clientes | `Contacto` | `clientes/domain/Contacto.java` |
| Clientes | `DocumentoConsultado` (record) | `clientes/domain/DocumentoConsultado.java` |
| Oportunidades | `Oportunidad` | `oportunidades/domain/Oportunidad.java` |
| Oportunidades | `OportunidadContacto` | `oportunidades/domain/OportunidadContacto.java` |
| Oportunidades | `OportunidadProducto` | `oportunidades/domain/OportunidadProducto.java` |
| Oportunidades | `OportunidadColaborador` | `oportunidades/domain/OportunidadColaborador.java` |
| Propuestas | `Propuesta` | `propuestas/domain/Propuesta.java` |
| Propuestas | `DetallePropuesta` | `propuestas/domain/DetallePropuesta.java` |
| Actividades | `Actividad` | `actividades/domain/Actividad.java` |
| Actividades | `ActividadParticipante` | `actividades/domain/ActividadParticipante.java` |
| Actividades | `ActividadEtiqueta` | `actividades/domain/ActividadEtiqueta.java` |
| Leads | `Lead` | `leads/domain/Lead.java` |
| Leads | `Fuente` | `leads/domain/Fuente.java` |
| Marketing | `Campania` | `marketing/domain/Campania.java` |
| Marketing | `CampaniaLead` | `marketing/domain/CampaniaLead.java` |
| Equipos | `Equipo` | `equipos/domain/Equipo.java` |
| Equipos | `EquipoMiembro` | `equipos/domain/EquipoMiembro.java` |
| Productos | `Producto` | `productos/domain/Producto.java` |
| Usuarios | `Usuario` | `usuarios/domain/Usuario.java` |
| Mensajería | `Mensaje` | `mensajeria/domain/Mensaje.java` |

### 8.3.2 Objetos de valor (Value Objects)

En la implementación actual, los objetos de valor se representan como enumeraciones (enums) que encapsulan estados, tipos y clasificaciones cerradas. No se identifican Value Objects inmutables como clases independientes con igualdad por atributos, lo cual es una decisión de diseño aceptable para la complejidad actual del sistema.

| Objeto de valor | Tipo | Módulo | Valores |
|---|---|---|---|
| `EstadoCliente` | Enum | Clientes | ACTIVO, INACTIVO |
| `TipoDocumento` | Enum | Clientes | DNI, RUC |
| `EtapaOportunidad` | Enum | Oportunidades | PROSPECCION, CONTACTO, CALIFICACION, PROPUESTA, NEGOCIACION, GANADA, PERDIDA |
| `EstadoPropuesta` | Enum | Propuestas | BORRADOR, ENVIADA, APROBADA, RECHAZADA, CANCELADA |
| `TipoActividad` | Enum | Actividades | LLAMADA, REUNION, TAREA, SEGUIMIENTO |
| `EstadoActividad` | Enum | Actividades | PENDIENTE, EN_PROCESO, COMPLETADA, CANCELADA |
| `EstadoLead` | Enum | Leads | NUEVO, CONTACTADO, CALIFICADO, CONVERTIDO, DESCARTADO |
| `EstadoCampania` | Enum | Marketing | BORRADOR, ACTIVA, FINALIZADA, CANCELADA |
| `EstadoProducto` | Enum | Productos | ACTIVO, INACTIVO, DESCONTINUADO |
| `EstadoEquipo` | Enum | Equipos | ACTIVO, INACTIVO |
| `EstadoUsuario` | Enum | Usuarios | ACTIVO, INACTIVO |
| `RolUsuario` | Enum | Usuarios | ADMIN, JEFE, VENDEDOR |
| `NodeType` | Enum | Relationship Map | CLIENTE, CONTACTO, OPORTUNIDAD, ACTIVIDAD, PROPUESTA, PRODUCTO, USUARIO |
| `EdgeType` | Enum | Relationship Map | CLIENTE_CONTACTO, CLIENTE_OPORTUNIDAD, … (11 tipos) |

El record `DocumentoConsultado` en el módulo Clientes funciona como un Value Object: es inmutable por definición del lenguaje Java (record), no posee identidad propia y representa datos provenientes de RENIEC/SUNAT.

---

## 8.4 Agregados y Raíces de Agregado

Cada módulo de dominio define un agregado cuya raíz es la entidad principal del módulo. Las entidades secundarias se gestionan exclusivamente a través de la raíz.

| Agregado | Raíz | Entidades internas | Invariantes |
|---|---|---|---|
| **Cliente** | `Cliente` | `Contacto` | Un cliente posee una lista de contactos. Los contactos se crean, modifican y eliminan vía `ManageClienteUseCase`. |
| **Oportunidad** | `Oportunidad` | `OportunidadContacto`, `OportunidadProducto`, `OportunidadColaborador` | Las relaciones N:M se gestionan vía subrecursos REST de la oportunidad. Cada relación tiene su propio `PersistencePort`. |
| **Propuesta** | `Propuesta` | `DetallePropuesta` | Los detalles se persisten con `@OneToMany(cascade=ALL, orphanRemoval=true)`. La propuesta recalcula totales (`recalcularTotales()`) e impide modificaciones en estados terminales (`esEstadoTerminal()`). |
| **Actividad** | `Actividad` | `ActividadParticipante`, `ActividadEtiqueta` | Máximo 8 etiquetas por actividad (`MAX_ETIQUETAS = 8`). Participantes validados contra módulo Clientes. |
| **Lead** | `Lead` | — | Entidad autónoma con referencia a `Fuente` por FK. |
| **Campaña** | `Campania` | `CampaniaLead` | Relación N:M con leads gestionada internamente. |
| **Equipo** | `Equipo` | `EquipoMiembro` | Miembros validados contra módulo Usuarios. |
| **Producto** | `Producto` | — | Entidad autónoma de catálogo. |
| **Etiqueta** | `Etiqueta` | — | Nombre normalizado para unicidad. |
| **Usuario** | `Usuario` | — | Correo único. Password encriptado con BCrypt(12). |
| **Mensaje** | `Mensaje` | — | Entidad autónoma para chat interno. |

---

## 8.5 Relaciones N:M — Implementación Actual

La migración a relaciones muchos a muchos (N:M) fue un cambio estructural significativo respecto al diseño original. En lugar de utilizar `@ManyToMany` de JPA, todas las relaciones N:M se implementan mediante **tablas intermedias con entidad propia** (entidad de dominio + entidad JPA independiente). Esta decisión otorga control explícito sobre atributos adicionales de la relación.

### 8.5.1 Tabla de relaciones N:M verificadas

| Relación | Entidad de dominio | Entidad JPA | Tabla Oracle | Atributos adicionales |
|---|---|---|---|---|
| **Oportunidad ↔ Contacto** | `OportunidadContacto` | `OportunidadContactoEntity` | `CRM_OPORTUNIDAD_CONTACTOS` | `rol` (String), `esDecisor` (Boolean) |
| **Oportunidad ↔ Producto** | `OportunidadProducto` | `OportunidadProductoEntity` | `CRM_OPORTUNIDAD_PRODUCTOS` | `cantidad` (Integer) |
| **Oportunidad ↔ Colaborador** | `OportunidadColaborador` | `OportunidadColaboradorEntity` | `CRM_OPORTUNIDAD_COLABORADORES` | — (solo FK + timestamp) |
| **Actividad ↔ Participante** | `ActividadParticipante` | `ActividadParticipanteEntity` | `CRM_ACTIVIDAD_PARTICIPANTES` | — (solo FK + timestamp) |
| **Actividad ↔ Etiqueta** | `ActividadEtiqueta` | `ActividadEtiquetaEntity` | `CRM_ACTIVIDAD_ETIQUETAS` | `orden` (Integer) |
| **Equipo ↔ Usuario** | `EquipoMiembro` | `EquipoMiembroEntity` | `CRM_EQUIPO_MIEMBROS` | — (solo FK + timestamp) |
| **Campaña ↔ Lead** | `CampaniaLead` | `CampaniaLeadEntity` | `CRM_CAMPANIA_LEADS` | — (solo FK + timestamp) |

### 8.5.2 Patrón de implementación

Todas las relaciones N:M siguen el mismo patrón:

1. **Entidad de dominio pura** en `modulo/domain/` — POJO sin anotaciones JPA.
2. **Entidad JPA** en `modulo/adapters/out/persistence/entity/` — con `@Entity`, `@Table`, constraint `@UniqueConstraint` sobre los dos FK.
3. **Puerto de salida** en `modulo/application/port/out/` — interfaz que declara operaciones CRUD de la relación.
4. **Adaptador de persistencia** en `modulo/adapters/out/persistence/` — implementa el puerto con conversión bidireccional dominio ↔ JPA.
5. **Gestión vía caso de uso de la raíz** — los endpoints se exponen como subrecursos REST: `POST /api/v1/oportunidades/{id}/contactos`, `DELETE /api/v1/actividades/{id}/participantes/{contactoId}`.

### 8.5.3 Impacto en el modelo de dominio

La implementación N:M modificó el diseño original en los siguientes aspectos:

- **Cada relación es una entidad de dominio independiente** con identidad propia (ID autogenerado), no un simple par de FK. Esto permite almacenar metadatos contextuales (rol del contacto, cantidad del producto, orden de la etiqueta).
- **No se utilizan `@ManyToMany` de JPA** en ningún módulo. La relación se gestiona explícitamente con entidades intermedias y repositorios dedicados.
- **Los límites del agregado se respetan**: la tabla intermedia pertenece al módulo propietario de la raíz de agregado. Por ejemplo, `OportunidadContacto` vive en el módulo `oportunidades`, no en `clientes`.
- **La validación cruzada de módulos** se realiza a través de puertos de salida dedicados (`RelacionValidationPort`, `ContactoQueryPort`) que verifican la existencia de entidades externas sin acoplar los módulos.

---

## 8.6 Puertos, Servicios y Repositorios

### 8.6.1 Puertos de entrada (Driving Ports)

Los puertos de entrada definen los casos de uso del sistema y son interfaces implementadas por los servicios de aplicación.

| Módulo | Puerto de entrada | Operaciones principales |
|---|---|---|
| Clientes | `ManageClienteUseCase` | CRUD clientes, CRUD contactos, consulta DNI/RUC |
| Oportunidades | `ManageOportunidadUseCase` | CRUD oportunidades, gestión de contactos/productos/colaboradores N:M, cambio de etapa |
| Propuestas | `ManagePropuestaUseCase` | CRUD propuestas, transiciones de estado (enviar, aprobar, rechazar, cancelar) |
| Actividades | `ManageActividadUseCase` | CRUD actividades, gestión de participantes y etiquetas N:M, filtros múltiples |
| Leads | `ManageLeadUseCase` | CRUD leads con filtros por estado y fuente |
| Leads | `ManageFuenteUseCase` | CRUD fuentes de captación |
| Marketing | `ManageCampaniaUseCase` | CRUD campañas y gestión de leads asociados |
| Productos | `ManageProductoUseCase` | CRUD productos de catálogo |
| Etiquetas | `ManageEtiquetaUseCase` | CRUD etiquetas con normalización |
| Equipos | `ManageEquipoUseCase` | CRUD equipos y gestión de miembros N:M |
| Usuarios | `ManageUsuarioUseCase` | CRUD usuarios, activar/desactivar |
| Usuarios | `UsuarioConsultaPort` | Consultas de existencia por ID y correo (puerto público inter-módulo) |
| Dashboard | `DashboardUseCase` | KPIs agregados de lectura |
| Relationship Map | `ManageRelationshipMapUseCase` | Grafos contextuales y expansión de nodos |
| Mensajería | `ManageMensajeUseCase` | CRUD mensajes internos |

### 8.6.2 Puertos de salida (Driven Ports)

| Módulo | Puerto de salida | Responsabilidad |
|---|---|---|
| Clientes | `ClientePersistencePort` | Persistencia de clientes y contactos |
| Clientes | `DocumentoLookupPort` | Consulta externa RENIEC/SUNAT |
| Oportunidades | `OportunidadPersistencePort` | Persistencia de oportunidades |
| Oportunidades | `OportunidadContactoPersistencePort` | Persistencia relación N:M oportunidad-contacto |
| Oportunidades | `OportunidadProductoPersistencePort` | Persistencia relación N:M oportunidad-producto |
| Oportunidades | `OportunidadColaboradorPersistencePort` | Persistencia relación N:M oportunidad-colaborador |
| Actividades | `ActividadPersistencePort` | Persistencia de actividades |
| Actividades | `ActividadParticipantePersistencePort` | Persistencia relación N:M actividad-participante |
| Actividades | `ActividadEtiquetaPersistencePort` | Persistencia relación N:M actividad-etiqueta |
| Actividades | `RelacionValidationPort` | Validación de existencia de entidades externas |
| Actividades | `ContactoQueryPort` | Consulta de contactos del módulo Clientes |
| Propuestas | `PropuestaPersistencePort` | Persistencia de propuestas con detalles |
| Propuestas | `PropuestaRelacionValidationPort` | Validación de oportunidad y cliente externos |
| Leads | `LeadPersistencePort` | Persistencia de leads |
| Leads | `FuentePersistencePort` | Persistencia de fuentes |
| Marketing | `CampaniaPersistencePort` | Persistencia de campañas |
| Marketing | `CampaniaLeadPersistencePort` | Persistencia relación N:M campaña-lead |
| Equipos | `EquipoPersistencePort` | Persistencia de equipos |
| Equipos | `EquipoMiembroPersistencePort` | Persistencia relación N:M equipo-usuario |
| Productos | `ProductoPersistencePort` | Persistencia de productos |
| Etiquetas | `EtiquetaPersistencePort` | Persistencia de etiquetas |
| Usuarios | `UsuarioPersistencePort` | Persistencia de usuarios |
| Mensajería | `MensajePersistencePort` | Persistencia de mensajes |

### 8.6.3 API pública inter-módulo

El módulo Usuarios expone una interfaz pública `UsuariosApi` en el paquete raíz del módulo, siguiendo la convención de named interfaces de Spring Modulith. Esta interfaz permite a otros módulos consultar usuarios sin acceder a la infraestructura interna:

```java
public interface UsuariosApi {
    Optional<UsuarioResumen> buscarPorId(Long id);
    Optional<UsuarioResumen> buscarPorCorreo(String correo);
    boolean existePorId(Long id);
    boolean existePorCorreo(String correo);
}
```

---

## 8.7 Reglas de Negocio e Invariantes

### 8.7.1 Reglas verificadas en el código

| # | Regla | Módulo | Archivo | Tipo |
|---|---|---|---|---|
| R1 | El valor ponderado de una oportunidad se calcula como `valor × probabilidad / 100` | Oportunidades | `Oportunidad.java` → `getValorPonderado()` | Dominio |
| R2 | Una propuesta en estado terminal (APROBADA, RECHAZADA, CANCELADA) no puede modificarse | Propuestas | `Propuesta.java` → `esEstadoTerminal()` | Dominio |
| R3 | El subtotal de un detalle se calcula como `cantidad × precioUnitario` | Propuestas | `DetallePropuesta.java` → `calcularSubtotal()` | Dominio |
| R4 | El total de la propuesta es `∑ subtotales − descuento` | Propuestas | `Propuesta.java` → `recalcularTotales()` | Dominio |
| R5 | Máximo 8 etiquetas por actividad | Actividades | `ActividadService.java` → `MAX_ETIQUETAS = 8` | Servicio |
| R6 | Antes de crear/actualizar una actividad, se valida existencia de cliente, lead, oportunidad y usuario | Actividades | `ActividadService.java` → `validarRelaciones()` | Servicio |
| R7 | El correo de un usuario es único | Usuarios | `UsuarioService.java` → `existsByCorreo()` | Servicio |
| R8 | La contraseña se encripta con BCrypt factor 12 | Auth/Usuarios | `PasswordEncoderConfig.java`, `UsuarioService.java` | Servicio |
| R9 | Consultas DNI/RUC limitadas a 20 por usuario por ventana de 60 segundos | Shared | `ConsultaDocumentoRateLimiter.java` | Infraestructura |
| R10 | Número de propuesta generado automáticamente con prefijo y secuencia | Propuestas | `PropuestaService.java` | Servicio |
| R11 | No pueden existir relaciones N:M duplicadas (constraint `UNIQUE` sobre par de FK) | Todos los N:M | `@UniqueConstraint` en entidades JPA | Persistencia |

---

## 8.8 Capa Anticorrupción con Keycloak

### 8.8.1 Diseño

La integración con Keycloak constituye una capa anticorrupción (ACL) que traduce los conceptos del proveedor de identidad externo al lenguaje ubícuo del dominio de Viernes CRM.

**Componente: `KeycloakJwtAuthenticationConverter`**
- **Ubicación**: `auth/keycloak/KeycloakJwtAuthenticationConverter.java`
- **Función**: Traduce el claim `realm_access.roles` del JWT de Keycloak a `GrantedAuthority` de Spring Security con prefijo `ROLE_`.
- **Filtrado**: Ignora roles internos de Keycloak (`default-roles-*`, `offline_access`, `uma_authorization`).
- **Perfil**: Solo se activa con `@Profile("keycloak")`.

**Componente: `KeycloakSecurityConfig`**
- **Ubicación**: `auth/keycloak/KeycloakSecurityConfig.java`
- **Función**: Define la cadena de seguridad del backend como OAuth2 Resource Server. El backend **no** almacena sesiones; valida tokens JWT contra el JWK Set URI de Keycloak.
- **Mapeo de roles**:
  - `ADMIN`: gestión completa de usuarios y eliminación de registros de negocio.
  - `JEFE`: gestión comercial (productos, equipos, fuentes, etiquetas), aprobación/rechazo de propuestas.
  - `VENDEDOR`: operaciones CRUD estándar sobre entidades de negocio.

**Frontend: `KeycloakAuthService`**
- **Ubicación**: `viernes-frontend/src/app/core/auth/keycloak/keycloak-auth.service.ts`
- **Función**: Adaptador que traduce la API de `keycloak-js` a la interfaz `UsuarioSesion` del dominio Angular.
- **Flujo**: Authorization Code + PKCE S256. No almacena tokens en `localStorage`.

**Frontend: `AuthService` (Fachada)**
- **Ubicación**: `viernes-frontend/src/app/core/auth/auth.service.ts`
- **Función**: Fachada que desacopla el resto de la aplicación Angular de la implementación concreta de Keycloak. Los guards, interceptores y componentes consumen esta fachada, no `KeycloakAuthService` directamente.

### 8.8.2 Justificación como ACL

La integración con Keycloak cumple con las características de una capa anticorrupción DDD:
1. **Traducción de modelos**: Los roles de Keycloak (`realm_access.roles`) se mapean a los roles del dominio (`ADMIN`, `JEFE`, `VENDEDOR`).
2. **Aislamiento**: El módulo `auth` tiene dependencia declarada únicamente hacia `usuarios` (`allowedDependencies = {"usuarios"}`).
3. **Protección del dominio**: Ningún módulo de negocio importa clases de Keycloak o Spring Security OAuth2 directamente.

---

## 8.9 Relaciones entre Módulos

Las dependencias entre módulos están declaradas explícitamente en los archivos `package-info.java` mediante anotaciones `@ApplicationModule` de Spring Modulith.

### 8.9.1 Mapa de dependencias

```
actividades  → clientes, oportunidades, leads, etiquetas, usuarios, shared
auth         → usuarios
equipos      → usuarios, shared
mensajeria   → usuarios, shared
propuestas   → productos, clientes, oportunidades, shared
relationshipmap → clientes, oportunidades, actividades, propuestas, productos, usuarios, shared
usuarios     → shared
clientes     → (OPEN — sin restricciones declaradas)
dashboard    → (OPEN — sin restricciones declaradas)
etiquetas    → (OPEN — sin restricciones declaradas)
leads        → (OPEN — sin restricciones declaradas)
marketing    → (OPEN — sin restricciones declaradas)
oportunidades → (OPEN — sin restricciones declaradas)
productos    → (OPEN — sin restricciones declaradas)
```

### 8.9.2 Observaciones

- Los módulos marcados como `Type.OPEN` permiten que cualquier otro módulo los consuma. Esto es intencional para los módulos de catálogo (`productos`, `etiquetas`, `leads`) y de consulta (`dashboard`).
- El módulo `relationshipmap` es el de mayor acoplamiento: depende de 7 módulos. Esto es aceptable dado que su función es exclusivamente de lectura y visualización cruzada.
- El módulo `shared` actúa como librería compartida (excepciones, caché, rate limiting) y está dividido en submódulos independientes (`shared.exception`, `shared.ratelimit`), ambos de tipo OPEN.

---

## 8.10 Responsabilidades del Dominio vs. Aplicación

### 8.10.1 Distribución actual

| Capa | Responsabilidad | Ejemplo |
|---|---|---|
| **Dominio** | Cálculos de negocio puros | `Oportunidad.getValorPonderado()`, `Propuesta.recalcularTotales()`, `DetallePropuesta.calcularSubtotal()`, `Propuesta.esEstadoTerminal()` |
| **Servicio de aplicación** | Orquestación, validación cruzada, transacciones | `ActividadService.validarRelaciones()`, `UsuarioService.crear()` con verificación de unicidad y encriptación |
| **Adaptador de persistencia** | Conversión dominio ↔ JPA, consultas SQL | `*PersistenceAdapter.java` con métodos `toDomain()` / `toEntity()` |
| **Adaptador externo** | Integración con servicios externos | `ApisPeruDocumentoAdapter` para consultas RENIEC/SUNAT |
| **Controlador** | Traducción HTTP ↔ caso de uso | `*Controller.java` con inyección del `UseCase` correspondiente |

### 8.10.2 Observación académica

La mayor parte de la lógica de negocio reside en los servicios de aplicación, con excepciones notables en el módulo de Propuestas donde las entidades de dominio contienen lógica de cálculo. Este es un patrón común en aplicaciones Spring Boot que priorizan la simplicidad sobre un modelo de dominio completamente rico (Anemic Domain Model vs. Rich Domain Model). Para un CRM de esta escala, la distribución actual es pragmática y mantenible.

> **[REQUIERE VALIDACIÓN]**: Verificar con el equipo si existe intención de migrar más reglas de negocio de los servicios al dominio en futuras versiones.

