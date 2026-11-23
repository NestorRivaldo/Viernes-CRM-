# INFORME FINAL DE IMPLEMENTACIÓN: MÓDULO 6 — DASHBOARD BACKEND

## 1. Archivos Creados
**Dominio y DTOs (`com.viernes.dashboard.application.dto`):**
- `DashboardResumenResponse.java` (DTO principal que agrupa todas las métricas)
- `EstadoCantidadResponse.java`
- `EtapaCantidadValorResponse.java`
- `TipoCantidadResponse.java`

**Puertos (`com.viernes.dashboard.application.port`):**
- `DashboardUseCase.java` (in)
- `DashboardPersistencePort.java` (out)

**Casos de Uso (`com.viernes.dashboard.application.service`):**
- `DashboardService.java` (Implementación de la lógica para agregar los KPIs)

**Adaptadores (`com.viernes.dashboard.adapters`):**
- `DashboardController.java` (in.web)
- `DashboardPersistenceAdapter.java` (out.persistence)
- `DashboardQueryRepository.java` (out.persistence - maneja consultas JPQL nativas vía `EntityManager`)

**Pruebas Unitarias:**
- `DashboardServiceTest.java` (Verifica el correcto mapeo de métricas con datos vacíos y reales)
- `DashboardControllerTest.java` (Verifica las respuestas correctas de los endpoints)

## 2. Archivos Modificados/Eliminados
- Se eliminaron rastros de un modelo anterior de dashboard (ej. `GetDashboardMetricsUseCase.java`).
- Los módulos existentes (Clientes, Leads, Oportunidades, Actividades) **NO FUERON MODIFICADOS**, tal como se solicitó, para garantizar la integridad de sus propios contextos.

## 3. Endpoints Creados
Todos bajo la ruta base: `/api/v1/dashboard`
1. `GET /resumen`: Retorna el JSON completo con todas las métricas.
2. `GET /kpis`: Retorna los totales relacionados a Clientes.
3. `GET /pipeline`: Retorna totales y valores relacionados a las oportunidades.
4. `GET /leads`: Retorna totales de Leads y agrupación por estado.
5. `GET /actividades`: Retorna totales de Actividades y su agrupación por tipo y estado.

## 4. KPIs Implementados
- **Clientes:** Total
- **Leads:** Total general y desglose por Estado.
- **Oportunidades:** Total general, Abiertas, Ganadas, Perdidas.
- **Valores Financieros:** Valor total del Pipeline (oportunidades abiertas) y Valor Ganado. Todo manejado con `BigDecimal`. Si es nulo devuelve `0`.
- **Actividades:** Pendientes, En Proceso, Completadas, Canceladas y Vencidas.

## 5. Consultas Utilizadas
Todas las consultas se construyeron utilizando **JPQL de agregación** (`COUNT`, `SUM`, `GROUP BY`) vía `EntityManager` en `DashboardQueryRepository`.
Ejemplos de las consultas:
- `SELECT COUNT(c) FROM ClienteEntity c`
- `SELECT l.estado, COUNT(l) FROM LeadEntity l GROUP BY l.estado`
- `SELECT SUM(o.valor) FROM OportunidadEntity o WHERE o.etapa NOT IN ('GANADA', 'PERDIDA')`
- `SELECT COUNT(a) FROM ActividadEntity a WHERE a.estado = 'PENDIENTE' AND a.fecha < :ahora`

Esto garantiza alta eficiencia al delegar el trabajo a la BD en lugar de extraer listas de Java.

## 6. Tablas Oracle Utilizadas
Las consultas se ejecutan sobre las entidades JPA que mapean a las tablas oficiales en **Oracle XE**:
- `CRM_CLIENTES` (vía `ClienteEntity`)
- `CRM_LEADS` (vía `LeadEntity`)
- `CRM_OPORTUNIDADES` (vía `OportunidadEntity`)
- `CRM_ACTIVIDADES` (vía `ActividadEntity`)

**NO se creó ninguna tabla adicional** (ej. no existe `CRM_DASHBOARD`).

## 7. Pruebas Realizadas y Compilación
- Pruebas unitarias de Service y Controller fueron ejecutadas satisfactoriamente mediante Mockito y MockMvc.
- El build (`mvnw clean compile`) fue **Exitoso**.
- Las pruebas (`mvnw test`) pasaron exitosamente. El Dashboard puede inicializarse y funcionar incluso si la Base de Datos Oracle está vacía (las sumas manejan nulos retornando `BigDecimal.ZERO`).

## 8. Funcionalidades Pendientes
De acuerdo con las instrucciones (sección 25, "No sobre-diseñar"):
- No se han agregado filtros por fecha.
- No se agrupan métricas por "responsable" ya que el módulo de Usuarios y su relación con el dashboard aún no han sido definidos formalmente, lo cual fue documentado para no generar riesgos de inconsistencias.

## Conclusión
El Módulo 6 ha sido entregado respetando la **Arquitectura Hexagonal**, el principio del **Modulith** y siendo 100% pasivo (sin modificar la BD).
