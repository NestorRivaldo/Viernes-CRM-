# VIERNES CRM V1 — CIERRE DOCUMENTAL DE LAS SECCIONES 08, 09 Y 10
## Guía de Incorporación al Word del Proyecto Integrador

**Fecha:** 11 de noviembre de 2026  
**Proyecto:** Viernes CRM V1 — CRM Híbrido de Gestión Comercial y Marketing  
**Ubicación de archivos:** `e:\Viernes\Viernes\informe\informes\`

---

## Archivos Generados

| Capítulo | Archivo | Tamaño / Contenido |
|---|---|---|
| **08** | [08_Analisis_Dominio_DDD.md](file:///e:/Viernes/Viernes/informe/informes/08_Analisis_Dominio_DDD.md) | Análisis del dominio DDD: 14 subdominios (5 core, 6 soporte, 3 genéricos), entidades y Value Objects, agregados e invariantes, relaciones N:M con entidades intermedias, puertos y use cases, capa anticorrupción con Keycloak, reglas de negocio verificadas. |
| **09** | [09_Arquitectura_Software.md](file:///e:/Viernes/Viernes/informe/informes/09_Arquitectura_Software.md) | Arquitectura de software: Monolito modular con Spring Modulith 2.0.7, arquitectura hexagonal verificada por módulo, stack tecnológico completo (Java 21, Spring Boot 4.0.7, Angular 22, Oracle, Keycloak 25), infraestructura Docker y observabilidad (Prometheus + Grafana + Loki), decisiones arquitectónicas (6 ADRs), validez de diagramas C4. |
| **11** | [11_Base_de_Datos_Oracle.md](file:///e:/Viernes/Viernes/informe/informes/11_Base_de_Datos_Oracle.md) | Base de datos Oracle 23c Free: diseño de tablespaces (uno por tabla, 22 total), 21 tablas, capa PL/SQL (6 funciones, procedimiento, 5 vistas, trigger), datos demo, portabilidad/reproducibilidad y consultas clave para la sustentación. |
| **12** | [12_Explicacion_del_Codigo.md](file:///e:/Viernes/Viernes/informe/informes/12_Explicacion_del_Codigo.md) | Explicación del código: mapa de los 14 módulos, anatomía hexagonal de cada uno, recorrido completo de una petición, las 5 variantes del patrón, infraestructura compartida, seguridad con Keycloak, comunicación entre módulos, persistencia Oracle y patrones repetidos con conteos reales (backend y frontend). |
| **13** | [13_Auditoria_Codigo_Ajeno.md](file:///e:/Viernes/Viernes/informe/informes/13_Auditoria_Codigo_Ajeno.md) | Auditoría de código ajeno y residuos: 20 archivos del módulo Odoo `club_automation` y del asistente Ollama, README/CLAUDE.md de LP2-BomERP, nombres `bomerp` en logs y observabilidad, frontend vanilla embebido, SQL triplicado, informes duplicados (~56 MB en ZIP) y estado de git. Incluye plan de limpieza en 7 fases. |
| **14** | [14_Actividades_Explicacion_Simple.html](file:///e:/Viernes/Viernes/informe/informes/14_Actividades_Explicacion_Simple.html) | Guía visual del módulo `actividades` en lenguaje de oficina: cada carpeta es un departamento y cada archivo un empleado. Incluye mapa del edificio, glosario de oficios (controller, service, repository, entity, DTO, port, adapter), los 48 archivos explicados en una frase, el viaje de una actividad paso a paso, reglas, permisos y deudas pendientes. Con buscador e impresión a PDF. |
| **15** | [15_Examen_Actividades.html](file:///e:/Viernes/Viernes/informe/informes/15_Examen_Actividades.html) | Examen interactivo de 30 preguntas sobre el módulo `actividades`, en 5 bloques (conceptos, arquitectura, base de datos, reglas y flujo, casos prácticos). Calificación automática sobre 30 puntos, retroalimentación por pregunta, puntaje por bloque, barra de progreso, clave del docente plegable, reinicio y versión imprimible. |

---

## Puntos Clave para la Incorporación al Word

### Sección 08 — DDD
1. **Relaciones N:M**: No usar `@ManyToMany` de JPA. Todas las relaciones N:M se implementaron con **tablas intermedias y entidades propias** (`OportunidadContacto`, `OportunidadProducto`, `OportunidadColaborador`, `ActividadParticipante`, `ActividadEtiqueta`, `EquipoMiembro`, `CampaniaLead`). Si el borrador Word o los diagramas de Miro muestran relaciones directas N:M, deben actualizarse.
2. **Capa anticorrupción**: El componente `KeycloakJwtAuthenticationConverter` y la configuración `KeycloakSecurityConfig` constituyen la ACL que traduce los roles de Keycloak (`realm_access.roles`) a autoridades del dominio (`ROLE_ADMIN`, `ROLE_JEFE`, `ROLE_VENDEDOR`).
3. **API pública inter-módulo**: El módulo `usuarios` expone la interfaz `UsuariosApi` como contrato público consumido por otros módulos, siguiendo las convenciones de Spring Modulith.

### Sección 09 — Arquitectura
1. **Spring Modulith**: Se verifican 16 módulos mediante `ModularityTests.java`. Los módulos están tipados como `OPEN` o `CLOSED` con dependencias declaradas en `package-info.java`.
2. **Frontend Angular 22**: Componentes standalone, detección de cambios zoneless, carga diferida con `loadComponent`, y autenticación gestionada por `keycloak-js` con Authorization Code + PKCE S256.
3. **Diagramas C4**: C1 (Contexto) y C2 (Contenedores) siguen siendo válidos. C3 (Componentes) requiere actualizarse para incluir los módulos nuevos (`relationshipmap`, `propuestas`, `equipos`, `marketing`) y reflejar las entidades intermedias N:M.

### Sección 10 — GRASP y GoF
1. **GRASP**: Los 9 principios están plenamente implementados en el código. Destacan **Information Expert** en las entidades de dominio con métodos de cálculo (`Oportunidad.getValorPonderado()`, `Propuesta.recalcularTotales()`) y **Protected Variations** en las interfaces de puertos.
2. **GoF**: El patrón más extensamente implementado es **Adapter** (todos los `*PersistenceAdapter` y `ApisPeruDocumentoAdapter`). Se distinguió claramente qué patrones son propios y cuáles los provee Spring (Singleton, Proxy, Template Method, Chain of Responsibility).
3. **Patrones no forzados**: Se documentaron explícitamente 12 patrones GoF como "No identificados" (Abstract Factory, Prototype, Bridge, Composite, Decorator, Flyweight, Command, Iterator, Mediator, Memento, State, Visitor) para mantener rigor académico.

---

## Afirmaciones que Requieren Validación con el Equipo

1. **[08.10.2]**: Verificar si existe intención de migrar más lógica de los servicios a las entidades de dominio en futuras versiones (Anemic vs. Rich Domain Model).
2. **[09.12.3]**: Confirmar si los diagramas de Miro y Lucidchart citados en el Word serán reemplazados o si se incluirá una nota de versión indicando que corresponden al diseño preliminar previo a la implementación de relaciones N:M.

