# INFORME FINAL BACKEND — VIERNES V1

**Fecha:** 2026-10-03
**Fuente de verdad:** inspección directa del código, configuración y base de datos reales (los informes HTML/MD anteriores NO se usaron como referencia).
**Verificación:** `mvn clean test` → BUILD SUCCESS + smoke test E2E contra la aplicación corriendo con Oracle real.

---

## 1. Estado general

```text
BACKEND: LISTO CON OBSERVACIONES
```

- 8 módulos funcionales + módulo transversal `shared`, arquitectura hexagonal coherente en todos.
- Spring Modulith verificado: `ApplicationModules.verify()` en verde (antes fallaba con ~30 violaciones).
- Oracle configurado y arrancando en el esquema correcto (`BOM_VENTAS`).
- Seguridad JWT + roles verificada de extremo a extremo con login real.
- 132/132 pruebas en verde; smoke test E2E con 11 verificaciones en vivo superadas.
- Las observaciones son deuda técnica no bloqueante (sección 9). No existen errores críticos conocidos.

---

## 2. Módulos

| Módulo | Estado | Implementación | Pendientes |
|---|---|---|---|
| clientes | 🟢 COMPLETA | CRUD, búsqueda (q), filtro por estado, desactivar, responsable, 30 tests | `responsable` String libre (V2) |
| contactos | 🟢 COMPLETA | Submódulo real de `clientes` (entidad `CRM_CONTACTOS`, DTOs, 4 endpoints anidados, principal, tests) | Regla "un solo contacto principal" (V2) |
| leads | 🟢 COMPLETA | CRUD, 5 estados, fuente, búsqueda, PATCH estado, 16 tests | Transiciones libres sin máquina de estados (V2) |
| oportunidades | 🟢 COMPLETA | CRUD, 7 etapas de pipeline, valor/probabilidad, valor ponderado (bug de redondeo corregido), fecha cierre, 20 tests | `clienteNombre`/`responsable` Strings libres (V2) |
| actividades | 🟢 COMPLETA | 4 tipos, 4 estados, PUT + PATCH, filtros, validación de relaciones contra 4 módulos, **vencidas calculadas (no persistidas)**, 12 tests | Sin máquina de estados (V2) |
| propuestas | 🟢 COMPLETA | Detalles, totales, estados con máquina de transiciones (BORRADOR→ENVIADA→APROBADA/RECHAZADA, CANCELADA), edición solo en BORRADOR, numeración corregida (correlativo global anual), 16 tests | Correlativo con race residual documentado (9.IMPORTANTE) |
| dashboard | 🟢 COMPLETA | Agregados SQL reales (COUNT/SUM/GROUP BY, BigDecimal): clientes, leads por estado, pipeline (abiertas/ganadas/perdidas/valores/por etapa), actividades por estado/tipo/pendientes/vencidas, **nueva sección propuestas** (total, por estado, valor aprobadas), 8 tests | 17-20 queries por `/resumen` sin caché (V2) |
| usuarios | 🟢 COMPLETA | CRUD, roles (ADMIN/JEFE/VENDEDOR), activar/desactivar, BCrypt, correo único, **API pública Modulith (`UsuariosApi`)**, 19 tests | Sin borrado de usuarios (deleteById sin endpoint) |
| auth | 🟢 COMPLETA | Login, JWT HS256 (8h), filtro robusto, SecurityConfig stateless, autorización por rol verificada, 8 tests | Sin refresh tokens (V2) |
| shared | 🟢 COMPLETA | GlobalExceptionHandler ampliado (404/400/401/403/405/409/500 + fieldErrors), CORS, OpenAPI, CorrelationIdFilter | — |
| campañas | 🔴 NO EXISTE | **No pertenece al alcance vigente**: no hay código, ni tablas, ni referencias en el proyecto. No se implementó (regla del brief) | — |

---

## 3. Cambios realizados

### Archivos creados (16)

| Archivo | Motivo |
|---|---|
| `usuarios/UsuariosApi.java` | API pública del módulo (única vía legal de acceso inter-módulo) — corrige violación Modulith |
| `usuarios/UsuarioResumen.java` | Record público con rol/estado como String (no expone enums internos) |
| `auth/application/dto/UsuarioLoginResponse.java` | DTO propio de auth; desacopla `LoginResponse` del DTO interno de usuarios (JSON de respuesta idéntico) |
| `auth/config/PasswordEncoderConfig.java` | Rompe ciclo de beans: usuarioService → PasswordEncoder → SecurityConfig → JwtAuthFilter → UsuariosApi |
| `auth/package-info.java` | Declara módulo auth con `allowedDependencies = {"usuarios"}` |
| `src/test/.../clientes/ClienteServiceTest.java` (18 tests) | Módulo sin cobertura |
| `src/test/.../clientes/ClienteControllerTest.java` (12 tests) | Módulo sin cobertura |
| `src/test/.../leads/LeadServiceTest.java` (6) + `LeadControllerTest.java` (10) | Módulo sin cobertura |
| `src/test/.../oportunidades/OportunidadServiceTest.java` (5) + `OportunidadControllerTest.java` (10) + `domain/OportunidadTest.java` (5) | Módulo sin cobertura + bug de redondeo |
| `src/test/.../usuarios/UsuarioServiceTest.java` (10) + `UsuarioControllerTest.java` (9) | Módulo sin cobertura |
| `src/test/.../auth/AuthServiceTest.java` (4) + `JwtServiceTest.java` (4) | Módulo sin cobertura |

### Archivos modificados (24)

| Archivo | Cambio | Motivo |
|---|---|---|
| `auth/application/service/AuthService.java` | Usa `UsuariosApi` en vez de puerto/dominio interno de usuarios | Violación Modulith (build rojo) |
| `auth/config/JwtAuthFilter.java` | Usa `UsuariosApi`; try/catch de `JwtException`; exige usuario ACTIVO | Token inválido causaba 500; usuario desactivado conservaba acceso 8h |
| `auth/application/dto/LoginResponse.java` | Campo `usuario` pasa a `UsuarioLoginResponse` | Desacoplamiento (mismo JSON) |
| `auth/config/SecurityConfig.java` | Bean `PasswordEncoder` extraído | Ciclo de beans |
| `auth/application/service/JwtService.java` | `isTokenValid` devuelve `false` ante `JwtException` | ExpiredJwtException propagada como 500 |
| `usuarios/application/service/UsuarioService.java` | Implementa `UsuariosApi`; password obligatoria en `crear` | NPE/constraint al crear sin password |
| `usuarios/package-info.java` | `allowedDependencies = {"shared", "shared.exception"}` | Violación Modulith residual |
| `actividades/.../RelacionValidationAdapter.java` | Usa `UsuariosApi`; javadoc corregido | Usaba repositorio interno de usuarios; javadoc afirmaba falsamente que usuarios no existía |
| `actividades/.../RelacionValidationPort.java` | Javadoc corregido | Misma desactualización |
| `shared/exception/GlobalExceptionHandler.java` | Handlers: 400 (validación con fieldErrors, JSON malformado, type mismatch, IllegalArgument), 401 (BadCredentials), 403 (Disabled/AccessDenied), 404, 405, 409 (IllegalState, DataIntegrity), 500 fallback | Errores de negocio devolvían 500; validaciones sin detalle por campo |
| `oportunidades/domain/Oportunidad.java` | `getValorPonderado()` con escala 2 + `RoundingMode.HALF_UP` | `ArithmeticException` potencial en cualquier GET |
| `propuestas/.../PropuestaService.java` | Numeración: correlativo global por año (`countByNumeroPropuestaStartsWith`) | Antes: count por oportunidad con número global → colisión garantizada entre oportunidades + race condition |
| `propuestas/.../PropuestaPersistencePort/Adapter/SpringDataRepository` | Método `countByNumeroPropuestaStartsWith` | Soporte de la corrección anterior |
| `clientes/.../entity/ContactoEntity.java` | `ID_CLIENTE` con `insertable=false, updatable=false` | Columna duplicada con el `@JoinColumn` del padre |
| `clientes/.../ClienteService.java` | `@Transactional(readOnly=true)` en 5 métodos de lectura | **LazyInitializationException → 500 real** en GET /api/v1/clientes (detectado en smoke test) |
| `dashboard` (7 archivos: DTO, UseCase, Service, Port, Adapter, QueryRepository, Controller) | Sección `propuestas` + endpoint `GET /api/v1/dashboard/propuestas` | El brief exige métricas de propuestas en el dashboard |
| `compose-dev.yml` | `APP_USER: BOM_VENTAS` | Mismatch: compose creaba `BOMERP_APP` pero la app conecta como `BOM_VENTAS` |
| `src/test/.../PropuestaServiceTest.java` | Stub actualizado a la nueva numeración | Mantener suite en verde |
| `src/test/.../DashboardServiceTest.java` | Stubs + assertions de la sección propuestas | Cobertura del cambio |
| `src/test/.../JwtServiceTest.java` | Token expirado → `false` (no excepción) | Acorde al fix de `isTokenValid` |

### Archivos eliminados

Ninguno.

---

## 4. API

Autenticación: `Authorization: Bearer <token>` en todo excepto login/swagger/actuator. Base: `/api/v1`.

| Módulo | Endpoints |
|---|---|
| auth | `POST /auth/login` (público) → 200 / 400 / 401 / 403 |
| clientes | `GET /clientes?q=&estado=` · `GET /clientes/{id}` · `POST /clientes` (201) · `PUT /clientes/{id}` · `PATCH /clientes/{id}/desactivar` (204) · `DELETE /clientes/{id}` (204) |
| contactos | `GET/POST /clientes/{id}/contactos` (201) · `PUT/DELETE /clientes/{id}/contactos/{cid}` |
| leads | `GET /leads?q=&estado=` · `GET /leads/{id}` · `POST /leads` (201) · `PUT /leads/{id}` · `PATCH /leads/{id}/estado?estado=` · `DELETE /leads/{id}` (204) |
| oportunidades | `GET /oportunidades?q=&etapa=` · `GET /oportunidades/{id}` · `POST /oportunidades` (201) · `PUT /oportunidades/{id}` · `PATCH /oportunidades/{id}/etapa?etapa=` · `DELETE /oportunidades/{id}` (204) |
| actividades | `POST /actividades` (201) · `GET /actividades/{id}` · `PUT /actividades/{id}` · `GET /actividades?vencidas=&estado=&idCliente=&idLead=&idOportunidad=&idUsuario=` · `PATCH /actividades/{id}/estado?estado=` · `DELETE /actividades/{id}` (204) |
| propuestas | `POST /propuestas` (201) · `GET /propuestas?idOportunidad=&idCliente=&estado=` · `GET /propuestas/{id}` · `PUT /propuestas/{id}` · `PATCH /propuestas/{id}/enviar|aprobar|rechazar|cancelar` · `DELETE /propuestas/{id}` (204) |
| usuarios | `GET /usuarios` · `GET /usuarios/{id}` · `POST /usuarios` (201, **ADMIN**) · `PUT /usuarios/{id}` (**ADMIN**) · `PATCH /usuarios/{id}/activar|desactivar` (204, **ADMIN**) |
| dashboard | `GET /dashboard/resumen` · `/kpis` · `/pipeline` · `/leads` · `/actividades` · `/propuestas` (nuevo) |

Verificado: sin endpoints duplicados; ningún endpoint expone entidades JPA (todo pasa por DTOs); todos los protegidos exigen JWT (403 sin token, verificado en vivo).

---

## 5. Base de datos

- **Oracle AI Database 26ai Free 23.26** (`gvenzl/oracle-free:23-slim`, contenedor `bomerp-oracle`), servicio `FREEPDB1`, esquema **`BOM_VENTAS`** (verificado en arranque: `Default schema: BOM_VENTAS`).
- `compose-dev.yml` corregido: ahora crea el usuario `BOM_VENTAS` que la app realmente usa. **Nota:** el volumen Docker existente conserva el usuario antiguo `BOMERP_APP`; para instalaciones nuevas basta `docker compose -f compose-dev.yml up -d`; para la BD actual no se requirió cambio (el usuario `BOM_VENTAS` ya existía).
- **Tablas (8):** `CRM_CLIENTES`, `CRM_CONTACTOS`, `CRM_LEADS`, `CRM_OPORTUNIDADES`, `CRM_ACTIVIDADES`, `CRM_PROPUESTAS`, `CRM_PROPUESTA_DETALLES`, `CRM_USUARIOS`. Generadas con `ddl-auto: update`.
- **IDs:** `GenerationType.IDENTITY` coherente en todas las entidades (soportado por Oracle 12c+; correcto para 23c).
- **Relaciones:** `@OneToMany` Cliente→Contactos y Propuesta→Detalles (cascade ALL, orphanRemoval). Referencias entre módulos (idCliente, idLead, idOportunidad, idUsuario, responsable) como columnas planas **sin FK física**: diseño modular desacoplado intencional; la integridad se valida en aplicación (puertos de validación verificados).
- **Constraints reales:** `CRM_USUARIOS.CORREO` UNIQUE, `CRM_PROPUESTAS.NUMERO_PROPUESTA` UNIQUE (garantía final de la numeración), NOT NULL en campos obligatorios.
- **Cambios de esquema derivados del código:** ninguno destructivo; `ContactoEntity.ID_CLIENTE` quedó de solo lectura.
- **Datos:** intactos (cliente preexistente verificado). Se crearon 2 usuarios de desarrollo (sección 6).
- **Posibles problemas:** sin índices adicionales a los por defecto (aceptable en V1); correlativo de propuestas ante concurrencia extrema (sección 9).

---

## 6. Seguridad

| Aspecto | Estado real |
|---|---|
| Login | ✅ Verificado E2E real: `admin@viernes.com` y `vendedor@viernes.com` devuelven JWT + datos de usuario |
| JWT | HS256, secreto Base64 por configuración, expiración 8h, subject = correo |
| Password hashing | BCrypt(12) — verificado en creación de usuarios y login |
| Protección de endpoints | Todo protegido salvo `/auth/**`, swagger, `/actuator/**`, estáticos (403 sin token, verificado) |
| Autorización por roles | **Funciona, no es decorativa**: POST/PUT/PATCH `/api/v1/usuarios` solo ADMIN — verificado en vivo: vendedor → 403, admin → 201 |
| Token inválido/expirado | Filtro continúa como anónimo → 403/401 limpio (antes: 500). Verificado con token malformado |
| Usuario desactivado | Bloqueado en login (403 "El usuario está desactivado") **y** en el filtro en cada request (nuevo) |
| Errores de auth | 401 "Credenciales inválidas" y 403 con cuerpo JSON consistente (nuevo handler) |
| Credenciales dev creadas | `admin@viernes.com / Admin12345` (ADMIN) · `vendedor@viernes.com / Vendedor123` (VENDEDOR) |
| Observaciones | `/actuator/**` público con `show-details: always` (solo perfil dev); GET `/usuarios` accesible a cualquier autenticado; sin refresh tokens; secreto JWT de dev en texto plano en `application-dev.yml` |

---

## 7. Reglas de negocio verificadas

- **Propuestas:** edición/eliminación solo en BORRADOR (409); enviar solo desde BORRADOR; aprobar/rechazar solo desde ENVIADA; cancelar prohibida en estados terminales; `total = subtotal − descuento` con subtotal de detalles (`cantidad × precioUnitario`); fecha de vencimiento no anterior a emisión (400); oportunidad y cliente deben existir (404); numeración única `PROP-<año>-NNNN` global por año.
- **Actividades:** vencida = `PENDIENTE && fecha < ahora`, **calculada en DTO y en query SQL, nunca persistida** (correcto, sin estado artificial); estado por defecto PENDIENTE; validación de existencia de cliente/lead/oportunidad/usuario al crear/actualizar (404).
- **Oportunidades:** etapa por defecto PROSPECCION; `valorPonderado = valor × probabilidad / 100` con redondeo seguro; pipeline SQL excluye GANADA/PERDIDA.
- **Leads:** estado por defecto NUEVO.
- **Clientes:** estado por defecto ACTIVO; desactivar → INACTIVO; contacto `principal=false` por defecto; borrado de cliente cascada a contactos (orphanRemoval).
- **Usuarios:** correo único (400 en alta y edición); password obligatoria en alta y re-hash solo si se envía en edición; alta siempre en estado ACTIVO; login rechaza INACTIVO.
- **Dashboard:** todos los KPIs se calculan en Oracle (COUNT/SUM/GROUP BY), nunca en memoria; valores monetarios `BigDecimal` con SUM null-safe.

---

## 8. Pruebas

```text
Tests ejecutados: 132
Tests exitosos:   132
Failures:         0
Errors:           0
Skipped:          0
Resultado Maven:  BUILD SUCCESS (./mvnw.cmd clean test) — 2 ejecuciones finales consecutivas
./mvnw.cmd compile: BUILD SUCCESS
```

Cobertura por módulo (antes → ahora): actividades 12→12, propuestas 16→16, dashboard 7→8, **clientes 0→30, leads 0→16, oportunidades 0→20, usuarios 0→19, auth 0→8**, ModularityTests 1→2 en verde, contexto Spring 1→1.

**Smoke test E2E (app real + Oracle real), 11 verificaciones:**
1. `GET /actuator/health` → 200, Oracle UP ✅
2. Sin token → 403 ✅ · 3. Token malformado → 403 (antes 500) ✅
4. Login credenciales inválidas → 401 JSON ✅ · 5. Login body inválido → 400 con `fieldErrors` por campo ✅
6. **Login admin real → 200 + JWT** ✅ · 7. `GET /usuarios` con token → 200 ✅
8. `GET /dashboard/resumen` → 200 con sección `propuestas` ✅
9. ADMIN crea vendedor → 201; **vendedor intenta crear usuario → 403** (roles reales) ✅
10. Correo duplicado → 400 con mensaje claro ✅
11. `GET /clientes` con token → 200 (se detectó y corrigió un **LazyInitializationException 500 real** durante esta prueba) ✅

---

## 9. Deuda técnica

**CRÍTICA**
- Ninguna.

**IMPORTANTE**
- Numeración de propuestas: correlativo `count+1` no es atómico; ante dos altas concurrentes del mismo año, la UNIQUE de BD convierte la segunda en 409 reintentable (integridad garantizada, sin duplicados posibles). Para alta concurrencia: secuencia Oracle en V2.
- Módulos `clientes`, `leads`, `oportunidades`, `dashboard` declarados OPEN: `actividades`/`propuestas` consultan sus repositorios internos y `dashboard` consulta sus entidades por JPQL (invisible a Modulith). `verify()` pasa y funciona, pero un rename de entidad rompería dashboard en runtime. V2: cerrar módulos y exponer contratos estilo `UsuariosApi`.
- No existe seed automatizado de usuarios: en una BD vacía no hay login posible (en dev se creó el admin manualmente; ver credenciales en sección 6).
- GET `/api/v1/usuarios` y `/{id}` accesibles a cualquier usuario autenticado (expone correos y roles a VENDEDOR); solo la escritura es ADMIN.

**MENOR**
- Listados sin paginación en todos los módulos; filtros de listado mutuamente excluyentes (`q` ignora `estado`/`etapa` silenciosamente); PATCH de estado con query param en vez de body; enums como literales String en JPQL de dashboard; `ClienteService.agregarContacto` asume que el nuevo contacto es el último de la lista; warning de "generated security password" en arranque (UserDetailsService por defecto no usado).

**FUTURA / V2** (mejoras, no errores)
- Cerrar módulos OPEN con APIs públicas; secuencia Oracle para numeración; refresh tokens y revocación; caché en dashboard y consolidación de las ~20 queries de `/resumen`; máquina de estados formal para leads/oportunidades/actividades; integrar `responsable`/`idUsuario` (String) como relación formal con usuarios (requiere migración de datos — se priorizó compatibilidad); `@Version` optimistic locking; `@PreAuthorize` granular por rol en módulos de negocio; regla "un solo contacto principal"; unicidad de correo en clientes/leads; tests de integración con perfil H2/Testcontainers (hoy `ViernesApplicationTests` requiere Oracle disponible).

---

## 10. Problemas conocidos

- Ninguno bloqueante ni error funcional abierto.
- En BD vacía es necesario crear el primer usuario ADMIN por SQL (hecho en dev; documentado arriba).
- `ViernesApplicationTests` y cualquier arranque requieren Oracle disponible en `localhost:1521` (no hay perfil de test alternativo).
- El volumen Docker previo conserva el usuario obsoleto `BOMERP_APP` (inofensivo; eliminar con `docker volume rm` solo si se quiere una BD limpia — operación destructiva, no ejecutada).

---

## 11. Veredicto final

```text
¿El backend está listo para comenzar el frontend?  SÍ
```

Los 8 módulos del alcance están completos y verificados en código real; la arquitectura hexagonal y los límites Modulith pasan verificación automática; Oracle arranca en el esquema correcto; la seguridad (login, JWT, roles) está probada de extremo a extremo con credenciales reales; el dashboard calcula en base de datos e incluye propuestas; contactos está integrado; la API no expone entidades JPA y devuelve códigos HTTP correctos; campañas quedó correctamente fuera del alcance. La suite completa (132 tests) termina en **BUILD SUCCESS** y el smoke test en vivo validó los flujos críticos. La deuda restante es mejora de V2, no bloqueo.

**Siguiente paso: FRONTEND DE VIERNES V1** (Angular en `localhost:4200`, CORS ya configurado; Swagger en `http://localhost:8080/swagger-ui.html`; credenciales dev en sección 6).
 Credenciales dev creadas: admin@viernes.com / Admin12345 (ADMIN) · vendedor@viernes.com /
   Vendedor123 (VENDEDOR) — la tabla estaba vacía y sin esto no había login posible.