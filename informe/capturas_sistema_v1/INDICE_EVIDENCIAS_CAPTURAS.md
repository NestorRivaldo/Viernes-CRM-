# VIERNES CRM V1 — ÍNDICE DE EVIDENCIAS Y CAPTURAS FUNCIONALES
## Proyecto Integrador de Análisis y Diseño de Software

**Fecha de extracción:** 11 de noviembre de 2026  
**Entorno de ejecución:** Producción local (Angular 22.1 en :4200, Spring Boot 4.0.7 en :8080, Keycloak 25 en :8180, Oracle Free 23c en :1521)  
**Usuario de demostración:** `admin@viernes.com` (Roles: `ADMIN`, `JEFE`, `VENDEDOR`)  
**Ubicación de archivos:** `e:\Viernes\Viernes\informe\capturas_sistema_v1\`

---

## 1. Tabla de Trazabilidad: Imágenes, Módulos y Requisitos

| # | Archivo PNG | Módulo | Pantalla / Componente | Requisito Funcional | Rol Autorizado | Estado de Implementación |
|---|---|---|---|---|---|---|
| 01 | `01_login_portal.png` | Auth | Portal de Bienvenida y Acceso | RF-D01 / HU-D01 | Público | **Implementado** (SPA Angular) |
| 02 | `02_login_keycloak.png` | Auth / Keycloak | Inicio de Sesión OAuth2 / OIDC | RF-D01 / HU-D01 | Público | **Implementado** (Keycloak 25) |
| 03 | `03_dashboard_kpis.png` | Dashboard | Tablero Principal de Métricas | RF-D02 / HU-D02 | Autenticado | **Implementado** (Reactivo / Caché) |
| 04 | `04_clientes_listado.png` | Clientes | Directorio y Gestión de Clientes | RF-D03 / HU-D03 | Autenticado | **Implementado** (CRUD) |
| 05 | `05_cliente_nuevo_form_reniec.png` | Clientes | Registro con Consulta DNI / RUC | RF-D05 / HU-D05 | Autenticado | **Implementado** (APIsPerú Integrado) |
| 06 | `06_leads_prospectos.png` | Leads | Embudo y Prospección de Leads | RF-D06 / HU-D06 | Autenticado | **Implementado** (CRUD / Filtros) |
| 07 | `07_oportunidades_pipeline.png` | Oportunidades | Pipeline y Valor Ponderado | RF-D08 / HU-D08 | Autenticado | **Implementado** (Cálculo Ponderado) |
| 08 | `08_oportunidad_detalle_relaciones_nm.png` | Oportunidades | Detalle de Oportunidad con N:M | RF-D09 / HU-D09 | Autenticado | **Implementado** (Contactos, Prod., Colab.) |
| 09 | `09_actividades_seguimiento_nm.png` | Actividades | Seguimiento Operativo con N:M | RF-D11 / HU-D11 | Autenticado | **Implementado** (Etiquetas, Participantes) |
| 10 | `10_propuestas_comerciales.png` | Propuestas | Gestión y Cotización Comercial | RF-D10 / HU-D10 | Autenticado | **Implementado** (Transición de Estados) |
| 11 | `11_productos_catalogo.png` | Productos | Catálogo Maestro de Productos | RF-D13 / HU-D13 | ADMIN, JEFE | **Implementado** (CRUD Catálogo) |
| 12 | `12_usuarios_roles_admin.png` | Usuarios | Control de Usuarios y Roles | RF-D16 / HU-D16 | ADMIN | **Implementado** (BCrypt / RBAC) |
| 13 | `13_equipos_comerciales_nm.png` | Equipos | Equipos Comerciales con N:M | RF-D14 / HU-D14 | ADMIN, JEFE | **Implementado** (Miembros N:M) |
| 14 | `14_mapa_relaciones_general.png` | Relationship Map | Grafo Global de Relaciones | RF-D19 / HU-D19 | Autenticado | **Implementado** (Cytoscape.js) |
| 15 | `15_mapa_relaciones_cliente.png` | Relationship Map | Grafo Contextual por Cliente | RF-D19 / HU-D19 | Autenticado | **Implementado** (Subgrafo Cliente) |
| 16 | `16_mapa_relaciones_oportunidad.png` | Relationship Map | Grafo Contextual por Oportunidad | RF-D19 / HU-D19 | Autenticado | **Implementado** (Subgrafo Oportunidad) |
| 17 | `17_mensajeria_chat_interno.png` | Mensajería | Panel de Chat Interno Colaborativo | RF-D18 / HU-D18 | Autenticado | **Implementado** (Panel Desplegable) |
| 18 | `18_swagger_api_backend.png` | Shared / API | Documentación OpenAPI / Swagger | No Funcional (Doc) | Público | **Implementado** (SpringDoc OpenAPI 3) |

---

## 2. Descripción Breve para el Pie de Cada Imagen en el Word

### Figura 01 — Portal de Acceso a Viernes CRM (`01_login_portal.png`)
> *Figura 01: Pantalla inicial de bienvenida del cliente web (SPA Angular), ofreciendo el acceso mediante el botón de autenticación corporativa y validación de sesiones activas.*

### Figura 02 — Pasarela de Autenticación Keycloak (`02_login_keycloak.png`)
> *Figura 02: Interfaz oficial del servidor de identidad Keycloak 25 (`viernes-realm`), implementando el flujo OAuth2 Authorization Code con protección PKCE S256.*

### Figura 03 — Tablero Principal de Control y Métricas (`03_dashboard_kpis.png`)
> *Figura 03: Dashboard ejecutivo de Viernes CRM que presenta KPIs de clientes, ingresos proyectados, desglose del pipeline por etapas y volumen de actividades pendientes en tiempo real.*

### Figura 04 — Directorio Maestro de Clientes (`04_clientes_listado.png`)
> *Figura 04: Catálogo de clientes empresariales con búsqueda instantánea, filtrado por estado, identificación tributaria (RUC/DNI) y accesos directos al grafo de relaciones.*

### Figura 05 — Formulario de Registro de Cliente con Consulta RENIEC/SUNAT (`05_cliente_nuevo_form_reniec.png`)
> *Figura 05: Formulario de alta de clientes que integra el botón de consulta oficial en línea mediante `ApisPeruDocumentoAdapter`, auto-completando razón social y domicilio fiscal.*

### Figura 06 — Embudo de Prospectos Comerciales (`06_leads_prospectos.png`)
> *Figura 06: Módulo de Leads que muestra la calificación de prospectos clasificados por estados (Nuevo, Contactado, Calificado, etc.) y su respectiva fuente de procedencia.*

### Figura 07 — Pipeline y Embudo de Oportunidades (`07_oportunidades_pipeline.png`)
> *Figura 07: Vista general de negociaciones activas en el CRM, reflejando el monto base, probabilidad de éxito comercial y cálculo automatizado del valor ponderado.*

### Figura 08 — Detalle de Oportunidad y Gestión de Relaciones N:M (`08_oportunidad_detalle_relaciones_nm.png`)
> *Figura 08: Vista detallada de una oportunidad comercial que comprueba la interfaz para relaciones N:M: asignación de múltiples contactos clave con rol/decisor, catálogo de productos cotizados con cantidades y equipo de colaboradores asignados.*

### Figura 09 — Bitácora Operativa de Actividades (`09_actividades_seguimiento_nm.png`)
> *Figura 09: Cronograma de seguimiento operativo con soporte de relaciones N:M: asociación de múltiples etiquetas taxonómicas (máximo 8) y lista de contactos participantes por interacción.*

### Figura 10 — Portafolio de Propuestas y Cotizaciones (`10_propuestas_comerciales.png`)
> *Figura 10: Listado de cotizaciones comerciales asociadas a clientes, visualizando códigos correlativos únicos, totales calculados y estado de aprobación (Borrador, Enviada, Aprobada).*

### Figura 11 — Catálogo de Productos y Servicios (`11_productos_catalogo.png`)
> *Figura 11: Mantenimiento maestro de artículos y servicios comerciales con especificación de código SKU, categoría, precio unitario de referencia y estado de vigencia.*

### Figura 12 — Panel de Control de Usuarios y Permisos (`12_usuarios_roles_admin.png`)
> *Figura 12: Módulo administrativo de colaboradores exclusivo para el rol ADMIN, mostrando asignación de perfiles RBAC (`ADMIN`, `JEFE`, `VENDEDOR`) y opciones de activación/bloqueo.*

### Figura 13 — Estructura de Equipos Comerciales N:M (`13_equipos_comerciales_nm.png`)
> *Figura 13: Gestión de equipos de ventas con demostración de la relación N:M (`CRM_EQUIPO_MIEMBROS`), permitiendo vincular múltiples usuarios colaboradores a cada fuerza comercial.*

### Figura 14 — Mapa Global de Relaciones Comerciales (`14_mapa_relaciones_general.png`)
> *Figura 14: Lienzo interactivo basado en Cytoscape.js que representa la topología completa de vínculos entre clientes, oportunidades, cotizaciones y colaboradores.*

### Figura 15 — Grafo Contextual Centrado en Cliente (`15_mapa_relaciones_cliente.png`)
> *Figura 15: Subgrafo radial centrado en una cuenta corporativa específica, desplegando sus contactos subordinados, historial de actividades y cotizaciones vigentes.*

### Figura 16 — Grafo Contextual Centrado en Oportunidad (`16_mapa_relaciones_oportunidad.png`)
> *Figura 16: Vista contextual que ilustra el ecosistema de una oportunidad comercial: cliente principal, contactos decisores, ítems cotizados y vendedores asignados.*

### Figura 17 — Canal de Mensajería Interna del Equipo (`17_mensajeria_chat_interno.png`)
> *Figura 17: Panel lateral desplegable de comunicación colaborativa (`app-chat-panel`), permitiendo el intercambio de notas e interacciones entre usuarios en tiempo real.*

### Figura 18 — Especificación y Catálogo de Endpoints OpenAPI (`18_swagger_api_backend.png`)
> *Figura 18: Interfaz interactiva de Swagger UI provista por SpringDoc OpenAPI 3 en el puerto 8080, documentando los contratos REST protegidos por Bearer JWT.*

---

## 3. Comprobación de Funcionalidades Especiales

### 3.1 Relaciones N:M (Muchos a Muchos)
Se verificó el soporte de las 7 relaciones N:M en el sistema:

1. **Oportunidad ↔ Contacto:** Disponible en la interfaz (`08_oportunidad_detalle_relaciones_nm.png`). Permite agregar contactos indicando el rol (`Técnico`, `Comercial`) y si es decisor de compra. Persistido en `CRM_OPORTUNIDAD_CONTACTOS`.
2. **Oportunidad ↔ Producto:** Disponible en la interfaz (`08_oportunidad_detalle_relaciones_nm.png`). Permite incorporar productos del catálogo y definir la cantidad estimada. Persistido en `CRM_OPORTUNIDAD_PRODUCTOS`.
3. **Oportunidad ↔ Colaborador:** Disponible en la interfaz (`08_oportunidad_detalle_relaciones_nm.png`). Permite asignar co-responsables de ventas a la negociación. Persistido en `CRM_OPORTUNIDAD_COLABORADORES`.
4. **Actividad ↔ Participante:** Disponible en la interfaz (`09_actividades_seguimiento_nm.png`). Permite asociar múltiples contactos a una misma llamada o reunión. Persistido en `CRM_ACTIVIDAD_PARTICIPANTES`.
5. **Actividad ↔ Etiqueta:** Disponible en la interfaz (`09_actividades_seguimiento_nm.png`). Clasificación múltiple por etiquetas de color con orden secuencial. Persistido en `CRM_ACTIVIDAD_ETIQUETAS`.
6. **Equipo ↔ Usuario:** Disponible en la interfaz (`13_equipos_comerciales_nm.png`). Vinculación de colaboradores a fuerzas de ventas. Persistido en `CRM_EQUIPO_MIEMBROS`.
7. **Campaña ↔ Lead:** **Solo Backend / Base de Datos.** Implementado en Java (`CampaniaLead.java` y `CampaniaLeadPersistenceAdapter`) y en Oracle (`CRM_CAMPANIA_LEADS`), pero sin pantalla dedicada en el frontend Angular (la ruta `/campanas` redirige al dashboard).

### 3.2 Mapa de Relaciones Contextual (Relationship Map)
Se comprobó su funcionamiento interactivo mediante Cytoscape.js:
- Carga de grafos por nodo central (Cliente u Oportunidad).
- Representación visual diferenciada por colores y formas para 7 tipos de nodos (`CLIENTE`, `CONTACTO`, `OPORTUNIDAD`, `ACTIVIDAD`, `PROPUESTA`, `PRODUCTO`, `USUARIO`).
- Expansión de nodos al hacer clic para descubrir conexiones secundarias.

---

## 4. Funcionalidades que No Pudieron Demostrarse en Frontend

Para mantener el rigor académico del proyecto, se detallan con precisión las limitaciones identificadas en la interfaz de usuario:

| Funcionalidad | Estado Real | Motivo por el cual no se muestra captura |
|---|---|---|
| **Módulo de Campañas de Marketing** | Backend funcional, sin pantalla UI | La ruta `/campanas` en `app.routes.ts` está configurada como `redirectTo: 'dashboard'`. La entidad existe en Oracle (`CRM_CAMPANIAS`, `CRM_CAMPANIA_LEADS`), pero el frontend no implementó su vista. |
| **Conversión Automática Lead → Oportunidad** | Operación manual asistida | En el frontend, cambiar el estado del lead a `CONVERTIDO` solo actualiza el registro del prospecto. No existe un asistente interactivo que cree automáticamente la empresa cliente y la oportunidad en un solo paso. |
| **Aprobación Formal de Propuestas por Vendedor** | Restricción por Rol RBAC | El rol `VENDEDOR` tiene bloqueados los botones de aprobación/rechazo de propuestas comerciales (exclusivos para roles `ADMIN` y `JEFE` según `KeycloakSecurityConfig`). |
| **Envío Real de Correo Electrónico** | Simulación de estado | La acción "Enviar" en propuestas actualiza el estado a `ENVIADA` en la base de datos, pero el backend no tiene configurado un servidor SMTP saliente real para despachar el documento físico por correo. |

