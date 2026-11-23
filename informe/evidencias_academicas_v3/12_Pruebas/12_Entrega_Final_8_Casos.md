# 12. Verificación final de calidad y pruebas

> **Nota metodológica.** Los identificadores `RF-Dxx` y `HU-Dxx` son la correspondencia técnica provisional que existe en el repositorio; no se presentan como los códigos oficiales del Word académico. Los estados se refieren al alcance realmente evidenciado: `Satisfactorio` significa que las pruebas backend existentes del alcance pasan, no que exista una aceptación E2E o UAT. No se modificó código, Oracle ni Keycloak. La ejecución backend se toma del registro existente; no se volvió a ejecutar la suite.

## 1. Tabla de ocho casos de prueba

| ID | Funcionalidad | RF / HU | Procedimiento resumido | Resultado esperado | Resultado obtenido | Estado | Archivo o evidencia |
|---|---|---|---|---|---|---|---|
| CP-01 | Inicio de sesión y permisos | RF-D01 / HU-D01 | Portal → Keycloak (admin) → ruta protegida; VENDEDOR contra acción ADMIN (no ejecutado). | Sesión válida; 401/403 sin token o rol insuficiente. | Capturas 01–03: portal, Keycloak y dashboard admin. Sin prueba de 401/403. | Parcial | `01_login_portal.png`, `02_login_keycloak.png`, `03_dashboard_kpis.png`; `KeycloakSecurityConfig`; sin test |
| CP-02 | Gestión de clientes | RF-D03 / HU-D03 (rel. RF-D04, RF-D05) | CRUD de cliente; consulta inexistente; validar documento; agregar contacto. | 201/200/204/400/404; documento inválido o duplicado rechazado; contacto asociado. | 42 tests PASS (17+25) y 8 documentales; capturas 04/05. Sin E2E Oracle. | Satisfactorio | `ClienteControllerTest` 17; `ClienteServiceTest` 25; `ApisPeruDocumentoAdapterTest` 8; `04`, `05` |
| CP-03 | Gestión de leads | RF-D06 / HU-D06 (rel. RF-D07) | Listar, filtrar y buscar; CRUD; cambiar estado; validar fuente. | Filtros y CRUD correctos; estado por defecto `NUEVO`; 400/404. | 19 tests PASS (11+8); captura 06. No se probó que `CONVERTIDO` no cree oportunidad. | Satisfactorio | `LeadControllerTest` 11; `LeadServiceTest` 8; `06_leads_prospectos.png` |
| CP-04 | Relaciones N:M de oportunidades | RF-D09 / HU-D09 (rel. RF-D08) | Asociar contactos (rol/decisor), productos (cantidad) y colaboradores a una oportunidad. | N:M persistidas y visibles; sin duplicados. | Captura 08 muestra la UI. Sin tests N:M; 20 tests de oportunidad cubren CRUD/etapa. Sin E2E. | Parcial | `08_oportunidad_detalle_relaciones_nm.png`; `OportunidadControllerTest` 10; `OportunidadServiceTest` 5; `OportunidadTest` 5 |
| CP-05 | Actividades y participantes | RF-D13 / HU-D13 (rel. RF-D14) | Crear actividad; ≤8 etiquetas; agregar/quitar participantes; duplicados y vencidas. | Relaciones persistidas; >8 etiquetas y duplicados rechazados; vencida calculada. | 34 tests PASS (14+18+2); captura 09. Sin E2E. | Satisfactorio | `ActividadControllerTest` 14; `ActividadServiceTest` 18; `ActividadEtiquetaPersistenceAdapterTest` 2; `09` |
| CP-06 | Propuestas y cálculo de totales | RF-D10 / HU-D10 (rel. RF-D11, RF-D12) | Crear con detalles; catálogo/override; calcular subtotal/total/descuento; transiciones. | Cálculo correcto; transiciones válidas; no persistir si falta una relación. | 22 tests PASS (7+15); captura 10. Sin Oracle, concurrencia ni E2E. | Satisfactorio | `PropuestaControllerTest` 7; `PropuestaServiceTest` 15; `10_propuestas_comerciales.png` |
| CP-07 | Dashboard | RF-D18 / HU-D18 | Consultar resumen, KPIs, pipeline, leads, actividades y propuestas; caso vacío. | Totales y desgloses correctos; caso vacío en cero. | 8 tests PASS (5+3); captura 03. Sin E2E ni caché. | Satisfactorio | `DashboardControllerTest` 5; `DashboardServiceTest` 3; `03_dashboard_kpis.png` |
| CP-08 | Mapa contextual de relaciones | RF-D19 / HU-D19 | Mapa general, cliente 7 y oportunidad 1; expandir/contraer; detalle. | Nodos y aristas correctos; expansión sin duplicados; 404 solo si el ID no existe. | 7 tests PASS. Capturas 14: 2 nodos/1 arista; 16: 1 nodo/0; 15: 0/0 y error `Cliente no encontrado: 1`. Sin E2E. | Parcial | `RelationshipMapServiceTest` 7; `14`, `15`, `16` |

**Rutas de las evidencias.** Capturas: `E:\Viernes\Viernes\informe\capturas_sistema_v1\`. Reportes: `E:\Viernes\Viernes\viernes-backend\target\surefire-reports\`. Inventario y extracto: `E:\Viernes\Viernes\informe\evidencias_academicas_v3\12_Pruebas\`.

## 2. Resumen de las pruebas backend ejecutadas

| Indicador | Resultado registrado |
|---|---|
| Clases de prueba | 32 |
| Tests ejecutados | 291 |
| Fallos / errores / omitidos | 0 / 0 / 0 |
| Duración informada por Maven | 38,405 s |
| Resultado de construcción | `BUILD SUCCESS` |
| Base de datos en `contextLoads` | Oracle `23.26.2` |
| Perfiles activos | `dev`, `keycloak` |
| Fin de la ejecución | `2026-11-11T09:46:08-05:00` |
| Fuente | Extracto Maven y reportes Surefire existentes; no es una ejecución nueva |

**Lectura correcta del resultado.** La suite es de backend y combina pruebas unitarias, de servicio y de controlador con mocks; solo `ViernesApplicationTests.contextLoads` conecta con Oracle. El PASS de 291 tests no acredita interfaz, RBAC real con Keycloak, concurrencia, integridad referencial en Oracle ni aceptación de usuarios. Tampoco existe un informe JaCoCo de cobertura. Los casos con prueba backend directa son CP-02, CP-03, CP-05, CP-06, CP-07 y CP-08; CP-01 y CP-04 no tienen prueba backend específica. La ejecución usó `ddl-auto:update`, por lo que no puede presentarse como estrictamente de solo lectura; no hay evidencia de cambios concretos.

## 3. Registro breve de incidencias

| ID | Tipo | Descripción comprobada | Evidencia | Estado |
|---|---|---|---|---|
| I-01 | Error funcional | El mapa del cliente muestra `No se pudo cargar el mapa del cliente: Cliente no encontrado: 1`, con 0 nodos y 0 conexiones. La captura 04 muestra clientes con IDs 7, 8, 9, 22 y 62; el ID 1 no aparece. No se dispone de un log en la evidencia que explique por qué la UI solicitó el ID 1; no se atribuye una causa no comprobada. | `15_mapa_relaciones_cliente.png`, `04_clientes_listado.png`; `RelationshipMapServiceTest#obtenerMapaCliente_NoEncontrado_LanzaExcepcion` (verifica un ID inexistente, no la ruta de la UI) | Abierto; causa pendiente de diagnóstico |
| I-02 | Observación | El mapa de la oportunidad 1 carga solo el nodo raíz: 1 nodo y 0 conexiones. No se puede afirmar que sea un defecto; puede corresponder a un registro sin relaciones cargadas. | `16_mapa_relaciones_oportunidad.png` | Por confirmar |
| I-03 | Limitación de cobertura | No hay prueba E2E del frontend, UAT ni ejecución de la matriz de permisos con Keycloak real. Las capturas son evidencia visual, no una prueba funcional completa. | Capturas 01–18; ausencia de reporte frontend | Abierto |
| I-04 | Limitación de cobertura | Las relaciones N:M de oportunidad (contactos, productos y colaboradores) no tienen pruebas backend específicas; solo existe evidencia visual. | `08_oportunidad_detalle_relaciones_nm.png`; inventario de 291 tests | Abierto |
| I-05 | Limitación de ejecución | La corrida backend usó los perfiles `dev` y `keycloak` y `ddl-auto:update`; no es estrictamente de solo lectura. No se observaron cambios concretos, pero no se puede certificar ausencia de DDL. | `ejecucion_maven_extracto.txt`; `application-dev.yml` | Informativo |
| I-06 | Limitación funcional | Cambiar un lead a `CONVERTIDO` solo actualiza su estado; no crea automáticamente la oportunidad ni el cliente. La conversión sigue siendo manual. | `06_leads_prospectos.png`; índice de capturas, sección 4; `LeadService` | Documentado; no es un error del alcance actual |

## 4. Matriz RF → HU → prueba → evidencia

| RF provisional | HU provisional | Caso | Prueba o evidencia | Tipo | Estado |
|---|---|---|---|---|---|
| RF-D01 | HU-D01 | CP-01 Inicio de sesión y permisos | Sin test automatizado; `KeycloakSecurityConfig.java`; capturas 01 y 02 | Configuración + visual | Parcial |
| RF-D03 (rel. D04, D05) | HU-D03 (rel. D04, D05) | CP-02 Gestión de clientes | `ClienteControllerTest` 17; `ClienteServiceTest` 25; `ApisPeruDocumentoAdapterTest` 8; capturas 04 y 05 | Backend + visual | Satisfactorio |
| RF-D06 (rel. D07) | HU-D06 (rel. D07) | CP-03 Gestión de leads | `LeadControllerTest` 11; `LeadServiceTest` 8; captura 06 | Backend + visual | Satisfactorio |
| RF-D09 (rel. D08) | HU-D09 (rel. D08) | CP-04 Relaciones N:M de oportunidades | `OportunidadControllerTest` 10; `OportunidadServiceTest` 5; `OportunidadTest` 5; captura 08; sin pruebas N:M | Backend CRUD + visual | Parcial |
| RF-D13 (rel. D14) | HU-D13 (rel. D14) | CP-05 Actividades y participantes | `ActividadControllerTest` 14; `ActividadServiceTest` 18; `ActividadEtiquetaPersistenceAdapterTest` 2; captura 09 | Backend + visual | Satisfactorio |
| RF-D10 (rel. D11, D12) | HU-D10 (rel. D11, D12) | CP-06 Propuestas y totales | `PropuestaControllerTest` 7; `PropuestaServiceTest` 15; captura 10 | Backend + visual | Satisfactorio |
| RF-D18 | HU-D18 | CP-07 Dashboard | `DashboardControllerTest` 5; `DashboardServiceTest` 3; captura 03 | Backend + visual | Satisfactorio |
| RF-D19 | HU-D19 | CP-08 Mapa contextual de relaciones | `RelationshipMapServiceTest` 7; capturas 14, 15 y 16 | Backend + visual | Parcial |

**Trazabilidad oficial pendiente.** La matriz anterior usa los IDs técnicos disponibles. La correspondencia con los RF/HU del documento académico debe validarse antes de declararla definitiva; no se inventan códigos oficiales.

## 5. Comprobaciones pendientes

| # | Prioridad | Comprobación pendiente | Evidencia que falta |
|---|---|---|---|
| 1 | P1 | Reproducir el mapa del cliente con un ID existente (por ejemplo, 7) y registrar la petición y la respuesta; aclarar por qué la captura solicitó el ID 1. No modificar datos reales. | Captura de red/consola, respuesta 200/404 y conclusión del diagnóstico |
| 2 | P1 | E2E del mapa: cargar cliente 7, expandir, contraer y reexpandir; abrir el panel de detalle; verificar que no se duplican nodos ni aristas. | Secuencia de capturas o video y log de consola |
| 3 | P1 | RBAC con Keycloak real: sin token, token expirado y VENDEDOR contra aprobar/rechazar/cancelar propuestas y escribir usuarios. | Respuestas 401/403 y matriz de roles ejecutada |
| 4 | P2 | N:M de oportunidades: pruebas backend y UI para contactos con rol/decisor, productos con cantidad y colaboradores; verificar duplicados. | Tests y evidencia de persistencia/recuperación |
| 5 | P2 | Confirmar que un lead `CONVERTIDO` no crea oportunidad ni cliente y que la creación sigue siendo manual. | Prueba de integración y captura del resultado |
| 6 | P2 | Propuestas: vencimiento anterior a emisión, descuento mayor al subtotal, detalles al límite, correlativo concurrente y transiciones de estado. | Casos de borde en base aislada y resultados |
| 7 | P2 | FK, `CASCADE`/`NO ACTION` y borrados en Oracle; ejecutar solo en base de prueba aislada. | Script y resultado; nunca contra datos operativos |
| 8 | P2 | Dashboard con datos vacíos/parciales e invalidación de caché; generar informe de cobertura JaCoCo. | Pruebas ejecutadas y reporte de cobertura |
| 9 | P3 | UAT formal con usuarios, guion, testigo, fecha y aprobación. | Acta o firma y evidencia de aceptación |
