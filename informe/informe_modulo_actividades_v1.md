# Informe técnico detallado — Módulo `actividades` (Viernes CRM V1)

> Documento en texto (Markdown). Representa **el módulo completo** de actividades: dominio, aplicación, adapters, persistencia Oracle, cruces entre módulos, transacciones, errores, frontend consumidor, pruebas y brechas. Cada afirmación lleva ancla `archivo:línea` verificada contra el código real.

## Metadatos

| Campo | Valor |
|---|---|
| Módulo | `com.viernes.actividades` |
| Capacidad de negocio | Agenda de actividades (llamadas, reuniones, tareas, seguimientos) vinculables a cliente/lead/oportunidad, con etiquetas y participantes |
| Tipo Spring Modulith | **OPEN** (`package-info.java:2`) |
| Dependencias permitidas | `clientes, oportunidades, leads, etiquetas, usuarios, shared, shared.exception` (`package-info.java:3`) |
| Archivos backend (main) | **29** `.java` |
| Archivos de prueba | **3** clases, **34** `@Test` (18 + 14 + 2) |
| Archivos frontend (feature Angular) | **8** |
| Tablas Oracle | **3**: `CRM_ACTIVIDADES`, `CRM_ACTIVIDAD_ETIQUETAS`, `CRM_ACTIVIDAD_PARTICIPANTES` |
| Endpoints REST | **9** (`/api/v1/actividades`) |
| Casos de uso (port/in) | **16** métodos |
| Fecha de generación | 2026-11-08 |
| Árbol canónico | `E:\Viernes\Viernes` |

### Convención de anclas

Las rutas son relativas a estas bases (se indica una vez, luego se abrevia):

- **Backend main:** `E:\Viernes\Viernes\viernes-backend\src\main\java\com\viernes\`
- **Backend test:** `E:\Viernes\Viernes\viernes-backend\src\test\java\com\viernes\`
- **Frontend:** `E:\Viernes\Viernes\viernes-frontend\src\`

Ejemplo: `actividades/application/service/ActividadService.java:63` = `<base backend>/actividades/application/service/ActividadService.java`, línea 63.

---

## Índice

1. [Resumen ejecutivo](#1-resumen-ejecutivo)
2. [Esqueleto hexagonal: árbol de paquetes](#2-esqueleto-hexagonal-árbol-de-paquetes)
3. [Ficha Modulith](#3-ficha-modulith)
4. [Capa `domain`](#4-capa-domain)
5. [Capa `application` — DTOs](#5-capa-application--dtos)
6. [Capa `application` — Puertos](#6-capa-application--puertos)
7. [Capa `application` — Service (casos de uso)](#7-capa-application--service-casos-de-uso)
8. [Capa `adapters/in/web` — Controller](#8-capa-adaptersinweb--controller)
9. [Capa `adapters/out/persistence` — Adapters](#9-capa-adaptersoutpersistence--adapters)
10. [Entidades JPA y tablas Oracle](#10-entidades-jpa-y-tablas-oracle)
11. [Repositorios Spring Data](#11-repositorios-spring-data)
12. [Transacciones](#12-transacciones)
13. [Cruces entre módulos](#13-cruces-entre-módulos)
14. [Cadena ida-y-vuelta completa](#14-cadena-ida-y-vuelta-completa)
15. [Manejo de errores](#15-manejo-de-errores)
16. [Frontend consumidor (Angular)](#16-frontend-consumidor-angular)
17. [Pruebas](#17-pruebas)
18. [Análisis de brechas](#18-análisis-de-brechas)
19. [Inventario cuantitativo](#19-inventario-cuantitativo)
20. [Cómo leer el módulo](#20-cómo-leer-el-módulo)

---

## 1. Resumen ejecutivo

`actividades` es un módulo **OPEN** de Spring Modulith con esqueleto hexagonal completo (`adapters → application → domain`). Gestiona una **agenda de actividades** de CRM. Una `Actividad` puede vincularse opcionalmente a un cliente, un lead o una oportunidad; puede llevar hasta **8 etiquetas** (con orden preservado) y **participantes** (contactos del módulo clientes). Expone **9 endpoints REST** bajo `/api/v1/actividades`, implementa **16 casos de uso** y persiste en **3 tablas Oracle**. No tiene adapter de salida HTTP externo (`adapters/out/external` no existe): toda su "salida" es persistencia propia o consulta a otros módulos. La regla de negocio "vencida" es **derivada en lectura**, nunca persistida.

**Regla de dependencia (hexagonal):** `adapters → application → domain`. El dominio no conoce Spring ni JPA; la aplicación define puertos; los adapters los implementan. La inversión de dependencia hace el Service testeable con mocks (ver §17).

---

## 2. Esqueleto hexagonal: árbol de paquetes

Los 29 archivos backend, con su rol:

```
actividades/
├── package-info.java                         ← declaración Modulith (OPEN + allowedDependencies)
├── domain/                                   ← NÚCLEO PURO (sin framework)
│   ├── Actividad.java                        ← agregado raíz (POJO)
│   ├── ActividadEtiqueta.java                ← vínculo actividad↔etiqueta (POJO)
│   ├── ActividadParticipante.java            ← vínculo actividad↔contacto (POJO)
│   ├── EstadoActividad.java                  ← enum (4 estados)
│   └── TipoActividad.java                    ← enum (4 tipos)
├── application/
│   ├── dto/                                  ← CONTRATOS (records)
│   │   ├── ActividadRequest.java             ← entrada + Bean Validation
│   │   ├── ActividadResponse.java            ← salida (+ vencida, etiquetas)
│   │   ├── ParticipanteRequest.java          ← entrada participante
│   │   └── ParticipanteResponse.java         ← salida participante enriquecido
│   ├── port/
│   │   ├── in/ManageActividadUseCase.java    ← PUERTO ENTRADA (16 métodos)
│   │   └── out/                              ← PUERTOS SALIDA (5 propios)
│   │       ├── ActividadPersistencePort.java          (12 métodos)
│   │       ├── ActividadEtiquetaPersistencePort.java  (4 métodos)
│   │       ├── ActividadParticipantePersistencePort.java (6 métodos)
│   │       ├── ContactoQueryPort.java                 (1 método, → clientes)
│   │       └── RelacionValidationPort.java            (4 métodos, → 4 módulos)
│   └── service/ActividadService.java         ← REGLAS + @Transactional (implementa port/in)
└── adapters/
    ├── in/web/ActividadController.java       ← ENTRADA REST (9 endpoints)
    └── out/persistence/                      ← SALIDA
        ├── ActividadPersistenceAdapter.java          ← implementa port/out propio
        ├── ActividadEtiquetaPersistenceAdapter.java  ← implementa port/out propio
        ├── ActividadParticipantePersistenceAdapter.java ← implementa port/out propio
        ├── ContactoQueryAdapter.java                 ← implementa ContactoQueryPort (→ clientes API pública)
        ├── RelacionValidationAdapter.java            ← implementa RelacionValidationPort (→ repos OPEN + UsuariosApi)
        ├── entity/                           ← MAPEO JPA (3 tablas)
        │   ├── ActividadEntity.java
        │   ├── ActividadEtiquetaEntity.java
        │   └── ActividadParticipanteEntity.java
        └── repository/                       ← Spring Data JPA (3 interfaces)
            ├── ActividadSpringDataRepository.java
            ├── ActividadEtiquetaSpringDataRepository.java
            └── ActividadParticipanteSpringDataRepository.java
```

**Ausencias deliberadas:** no hay paquete `mapper/` (el mapeo dominio↔entidad↔DTO es manual dentro de adapters y service) ni `adapters/out/external/` (el módulo no consume APIs HTTP externas). Ver §18.

---

## 3. Ficha Modulith

`actividades/package-info.java:1-5`:

```java
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN,
    allowedDependencies = {"clientes", "oportunidades", "leads", "etiquetas", "usuarios", "shared", "shared.exception"}
)
package com.viernes.actividades;
```

- **OPEN** (`:2`): permite que otros módulos accedan a sus internals, y a la vez actividades puede consumir internals de otros módulos OPEN. Por eso `RelacionValidationAdapter` puede inyectar repositorios de clientes/leads/oportunidades (§13).
- **`allowedDependencies`** (`:3`): lista blanca explícita. Todo módulo que actividades toca está en la lista. `ModularityTests.verifiesModularStructure()` (`ModularityTests.java:12`, `modules.verify()`) hace cumplir esta frontera en tiempo de prueba; `writesModuleDocumentation()` (`:16-18`) genera el documento de módulos con `Documenter`.
- Contraste: `usuarios` es **CLOSED** (`usuarios/package-info.java:1-4`, tipo por defecto, `allowedDependencies = {shared, shared.exception}`), así que actividades solo puede usar su **API pública** `UsuariosApi` (`usuarios/UsuariosApi.java:10`, named interface en el paquete raíz). Ver §13.

---

## 4. Capa `domain`

Núcleo puro: **POJOs sin anotaciones de framework** (ni `@Entity`, ni `@Component`). Solo `java.time.LocalDateTime`. Esto es lo que hace el dominio independiente de la infraestructura.

### 4.1 `Actividad` — agregado raíz

`actividades/domain/Actividad.java:5`. Campos (`:6-16`):

| Campo | Tipo | Nota |
|---|---|---|
| `id` | `Long` | identidad |
| `idUsuario` | `String` | **String**, no Long: puede ser ID numérico o correo (ver `RelacionValidationAdapter:52-57`) |
| `idCliente` | `Long` | vínculo opcional a clientes |
| `idLead` | `Long` | vínculo opcional a leads |
| `idOportunidad` | `Long` | vínculo opcional a oportunidades |
| `tipo` | `TipoActividad` | enum |
| `titulo` | `String` | obligatorio |
| `descripcion` | `String` | opcional (`@Lob` en entidad) |
| `fecha` | `LocalDateTime` | fecha/hora programada |
| `estado` | `EstadoActividad` | enum |
| `creadoEn` | `LocalDateTime` | auditoría de creación |

Constructores: vacío `:18` y completo `:21`. Getters/setters `:37-123`. **No hay lógica de negocio en el agregado** (es un modelo de datos anémico); las reglas viven en el Service (§7). Observación de diseño: estilo "domain model" rico vs anémico — aquí es anémico, válido en hexagonal siempre que las reglas estén en application.

### 4.2 `ActividadEtiqueta` — vínculo N:M con orden

`actividades/domain/ActividadEtiqueta.java:5`. Campos (`:6-10`): `id`, `idActividad`, `idEtiqueta`, `creadoEn`, **`orden` (`Integer`)**. Dos constructores: 4 args `:15` (orden=null) y 5 args `:19`. El campo `orden` preserve la secuencia elegida por el usuario (ver `reemplazarEtiquetas` §9.2 y test §17.3).

### 4.3 `ActividadParticipante` — vínculo N:M a contacto

`actividades/domain/ActividadParticipante.java:5`. Campos (`:6-9`): `id`, `idActividad`, `idContacto`, `creadoEn`. Constructor `:14`. `idContacto` es referencia **lógica** a `CRM_CONTACTOS` (módulo clientes), sin foreign key física (`ActividadParticipanteEntity.java:20`).

### 4.4 Enums

- `EstadoActividad.java:3-8`: `PENDIENTE`, `EN_PROCESO`, `COMPLETADA`, `CANCELADA`.
- `TipoActividad.java:3-8`: `LLAMADA`, `REUNION`, `TAREA`, `SEGUIMIENTO`.

Ambos se persisten como **STRING** (`@Enumerated(EnumType.STRING)`, `ActividadEntity.java:29` y `:43`), no como ordinal: robusto ante reordenamientos del enum.

---

## 5. Capa `application` — DTOs

Contratos de entrada/salida como **`record`** (inmutables). Separan el dominio de la API REST.

### 5.1 `ActividadRequest` (entrada + validación)

`actividades/application/dto/ActividadRequest.java:13`. Bean Validation declarativa:

| Campo | Restricción | Ancla |
|---|---|---|
| `tipo` | `@NotNull("El tipo de actividad es obligatorio")` | `:19` |
| `titulo` | `@NotBlank("El título es obligatorio")` | `:22` |
| `fecha` | `@NotNull("La fecha es obligatoria")` | `:27` |
| `etiquetaIds` | `@Size(max = 8, "...máximo 8 etiquetas")` | `:32` |
| elementos de `etiquetaIds` | `@Positive("El ID de etiqueta es inválido")` | `:33` |

`idUsuario`, `idCliente`, `idLead`, `idOportunidad`, `descripcion`, `estado` son opcionales. **Constructor de compatibilidad** `:38-42` (9 args, sin `etiquetaIds`) delega al canónico con `List.of()` — para clientes/tests previos a etiquetas. La validación se dispara en el controller con `@Valid` (`ActividadController.java:27`); si falla, `MethodArgumentNotValidException → 400` (§15).

> Nota: el límite de 8 etiquetas está **duplicado** en dos sitios: `@Size(max=8)` en el DTO (`:32`) y `MAX_ETIQUETAS = 8` en el Service (`ActividadService.java:39`, chequeo en `:260`). El DTO valida la forma; el Service valida tras limpiar nulos/duplicados (`etiquetaIdsValidos:250`). Defensa en profundidad, pero dos fuentes de verdad para el mismo número (ver §18).

### 5.2 `ActividadResponse` (salida)

`actividades/application/dto/ActividadResponse.java:10`. Incluye los campos del dominio **más dos derivados**:
- `vencida` (`boolean`, `:22`): calculado en lectura, no persistido.
- `etiquetas` (`List<EtiquetaResponse>`, `:23`): DTO del módulo **etiquetas** (`etiquetas/application/dto/EtiquetaResponse.java:5`), incrustado en la respuesta.

Constructor de compatibilidad `:28-33` (12 args, sin etiquetas) → `List.of()`.

### 5.3 `ParticipanteRequest` / `ParticipanteResponse`

- `ParticipanteRequest.java:5`: un solo campo `contactoId` con `@NotNull("El contacto es obligatorio")` (`:6`).
- `ParticipanteResponse.java:5`: `id`, `idActividad`, `contactoId`, **`nombreContacto`**, **`correoContacto`**, `creadoEn` (`:6-11`). Los datos del contacto se **enriquecen** desde clientes (ver `toParticipanteResponse` §7.4).

---

## 6. Capa `application` — Puertos

### 6.1 Puerto de entrada: `ManageActividadUseCase`

`actividades/application/port/in/ManageActividadUseCase.java:9`. **16 métodos** (`:10-26`):

`crear`, `actualizar`, `obtener`, `listarTodos`, `listarVencidas`, `filtrarPorEstado`, `filtrarPorCliente`, `filtrarPorLead`, `filtrarPorOportunidad`, `filtrarPorEtiqueta`, `filtrarPorUsuario`, `cambiarEstado`, `eliminar`, `agregarParticipante`, `quitarParticipante`, `listarParticipantes`.

Lo **implementa** `ActividadService` (`:37`) y lo **llama** `ActividadController` (`:20`, inyectado por constructor `:22`). Es la costura de entrada: el controller no conoce el Service concreto, solo la interfaz.

### 6.2 Puertos de salida (5 propios)

| Puerto | Ancla | Métodos | Implementado por |
|---|---|---|---|
| `ActividadPersistencePort` | `port/out/ActividadPersistencePort.java:10` | **12** (`:11-22`): save, findById, findAll, findByEstado, findByIdCliente, findByIdLead, findByIdOportunidad, findByEtiquetaId, findByIdUsuario, findByTipo, findVencidas, deleteById | `ActividadPersistenceAdapter:17` |
| `ActividadEtiquetaPersistencePort` | `port/out/ActividadEtiquetaPersistencePort.java:8` | **4** (`:9-15`): findByIdActividad, findByIdActividadIn, reemplazarEtiquetas, deleteByIdActividad | `ActividadEtiquetaPersistenceAdapter:17` |
| `ActividadParticipantePersistencePort` | `port/out/ActividadParticipantePersistencePort.java:7` | **6** (`:8-13`): save, findByIdActividad, findByIdActividadAndIdContacto, existsByIdActividadAndIdContacto, delete, deleteByIdActividad | `ActividadParticipantePersistenceAdapter:14` |
| `ContactoQueryPort` | `port/out/ContactoQueryPort.java:12` | **1** (`:14`): `buscarPorId(Long) → Optional<ContactoResponse>` | `ContactoQueryAdapter:21` |
| `RelacionValidationPort` | `port/out/RelacionValidationPort.java:11` | **4** (`:13-23`): existeCliente, existeLead, existeOportunidad, existeUsuario | `RelacionValidationAdapter:18` |

**Inversión de dependencia:** el Service depende de estas **abstracciones** (puertos), no de los adapters. Los adapters `implements` el puerto y Spring los inyecta. Por eso en los tests se mockean los puertos (`ActividadServiceTest.java:38-54`) sin tocar BD.

### 6.3 Puerto prestado de `etiquetas`

`ActividadService` además inyecta **`EtiquetaPersistencePort`** (`ActividadService.java:46`), que pertenece a **otro módulo**: `etiquetas/application/port/out/EtiquetaPersistencePort.java:9` (import en `ActividadService.java:18`). Usa `findById` (`:14`) y `findAllById` (`:16`). Como `etiquetas` es **OPEN** (`etiquetas/package-info.java:1`), Modulith permite consumir su puerto de salida y su dominio (`Etiqueta`, import `:19`). Es un acoplamiento más estrecho que el de `ContactoQueryPort` (que usa la API pública de clientes). Ver §13 y §18.

---

## 7. Capa `application` — Service (casos de uso)

`actividades/application/service/ActividadService.java`. `@Service` (`:35`), `@Transactional` a nivel de clase (`:36`), `implements ManageActividadUseCase` (`:37`). Constante `MAX_ETIQUETAS = 8` (`:39`). **6 puertos inyectados** por constructor (`:41-60`): persistencePort, validationPort, participantePort, contactoQueryPort, actividadEtiquetaPort, etiquetaPort.

### 7.1 Escritura (heredan `@Transactional` read-write de la clase)

**`crear(request)` — `:63`**
1. `validarRelaciones(request)` (`:64`) — valida existencia de cliente/lead/oportunidad/usuario.
2. `etiquetaIdsValidos(request.etiquetaIds())` (`:65`) — limpia, deduplica, valida ≤8 y existencia.
3. Construye `Actividad` (`:67-78`); estado por defecto `PENDIENTE` si viene null (`:77`); `creadoEn = now()` (`:78`).
4. `persistencePort.save(actividad)` (`:80`) → INSERT.
5. `sincronizarEtiquetas(saved.getId(), etiquetaIds)` (`:81`) → reemplaza vínculos de etiquetas.
6. `toResponse(saved, cargarEtiquetas(...))` (`:82`) → DTO con etiquetas y `vencida`.

**`actualizar(id, request)` — `:86`**: valida relaciones (`:87`), busca por id o 404 (`:89-90`), `etiquetaIds` null = "no tocar etiquetas" (`:92-94`), setea campos (`:96-107`), `save` (`:109`), sincroniza etiquetas solo si vinieron (`:110-112`), `toResponse` (`:113`).

**`cambiarEstado(id, estado)` — `:176`**: busca o 404 (`:177-178`), `setEstado` (`:180`), `save` (`:181`), `toResponse` (`:182`). Transición de estado simple, sin máquina de estados explícita (cualquier estado → cualquier estado).

**`eliminar(id)` — `:186`**: verifica existencia o 404 (`:187-189`), luego **borrado en cascada manual** dentro de la misma transacción: etiquetas (`:190`), participantes (`:191`), actividad (`:192`). No hay FK física en cascada; la integridad la mantiene la aplicación (ver §10 y §18).

**`agregarParticipante(actividadId, contactoId)` — `:196`**:
1. Verifica actividad o 404 (`:197-199`).
2. `contactoQueryPort.buscarPorId(contactoId)` o 404 (`:200-201`) — consulta a clientes.
3. `existsByIdActividadAndIdContacto` → si ya existe, `IllegalArgumentException("El contacto ya es participante...")` (`:202-204`).
4. Construye y `save` (`:206-211`), `toParticipanteResponse(saved, contacto)` (`:212`).

**`quitarParticipante(actividadId, contactoId)` — `:216`**: busca la asociación o 404 (`:217-220`), `delete` (`:221`).

### 7.2 Lectura (`@Transactional(readOnly = true)` explícito)

10 métodos readOnly: `obtener:118` (`@:117`), `listarTodos:126` (`@:125`), `listarVencidas:132` (`@:131`), `filtrarPorEstado:138`, `filtrarPorCliente:144`, `filtrarPorLead:150`, `filtrarPorOportunidad:156`, `filtrarPorEtiqueta:162`, `filtrarPorUsuario:171`, `listarParticipantes:226` (`@:225`).

- **`listarVencidas:132`**: llama `persistencePort.findVencidas(EstadoActividad.PENDIENTE, LocalDateTime.now())` (`:133`). "Vencida" = PENDIENTE + fecha pasada (JPQL en `ActividadSpringDataRepository:32`).
- **`filtrarPorEtiqueta:162`**: primero valida que la etiqueta exista (`etiquetaPort.findById(idEtiqueta).isEmpty() → 404`, `:163-165`), luego `findByEtiquetaId` (`:166`).
- **`listarParticipantes:226`**: verifica actividad (`:227-229`), lista vínculos (`:230`) y enriquece cada uno con `contactoQueryPort.buscarPorId` (`:231`) — **una consulta por participante** (posible N+1 cross-module, ver §18).

### 7.3 Reglas de negocio en helpers privados

**`validarRelaciones(request)` — `:235`**: por cada vínculo no-null, consulta `validationPort` y lanza `ResourceNotFoundException` si no existe: cliente (`:236-238`), lead (`:239-241`), oportunidad (`:242-244`), usuario (`:245-247`). Es la guarda de integridad referencial **lógica** (no hay FK física).

**`etiquetaIdsValidos(etiquetaIds)` — `:250`**:
- null/vacío → `List.of()` (`:251-253`).
- Limpia nulos y deduplica preservando orden: `.filter(Objects::nonNull).distinct().toList()` (`:255-258`).
- Si `> MAX_ETIQUETAS` → `IllegalArgumentException` (`:260-262`).
- `etiquetaPort.findAllById(limpios)` (`:268`), arma set de existentes (`:269-271`), calcula faltantes (`:273-275`); si hay faltantes → `ResourceNotFoundException("Etiquetas no encontradas: ...")` (`:277-279`).

**`sincronizarEtiquetas(idActividad, etiquetaIds)` — `:284`**: si null, no hace nada (`:285-287`); si no, `actividadEtiquetaPort.reemplazarEtiquetas` (`:288`). Estrategia **reemplazo total** (delete + insert), no incremental.

**`cargarEtiquetas` / `cargarEtiquetasPorIds` — `:291` / `:298`**: para una actividad, obtiene ids de vínculo (`:292-294`) y luego `etiquetaPort.findAllById` en **lote** (`:302`), mapea a `EtiquetaResponse` y reordena según los ids (`:306-309`).

**`toResponses(list)` — `:312`**: **carga por lotes para evitar N+1** en listados. Obtiene todos los ids de actividad (`:317-320`), trae todos los vínculos de una vez con `findByIdActividadIn` (`:323`), agrupa por actividad (`:322-330`), reúne todos los ids de etiqueta (`:332-333`), los carga en un solo `findAllById` (`:335`), y mapea cada actividad con sus etiquetas (`:339-349`). Es el punto más "complejo/técnico" del módulo: batch fetching manual.

### 7.4 Mapeo a DTO (manual, sin MapStruct)

- **`toResponse(actividad, etiquetas)` — `:382`**: calcula `esVencida = estado == PENDIENTE && fecha.isBefore(now())` (`:383-384`) y construye el record (`:386-400`). **La regla "vencida" vive aquí**, derivada en cada lectura.
- **`toParticipanteResponse(participante, contacto)` — `:352`**: concatena `nombre + apellido` si apellido no está vacío (`:356-358`), toma `correo` (`:359`), construye el DTO (`:361-368`). Si el contacto es null, nombre/correo quedan null.
- **`toEtiquetaResponse(etiqueta)` — `:371`**: mapea el dominio `Etiqueta` (de otro módulo) al DTO `EtiquetaResponse` (`:372-379`).

---

## 8. Capa `adapters/in/web` — Controller

`actividades/adapters/in/web/ActividadController.java`. `@RestController` (`:16`), `@RequestMapping("/api/v1/actividades")` (`:17`). Inyecta `ManageActividadUseCase` (`:20`, ctor `:22`). **No tiene lógica de negocio**: solo recibe, delega al caso de uso y construye la respuesta HTTP. Está **fuera de la transacción** (§12).

### 8.1 Tabla de endpoints (9)

| # | Verbo + ruta | Método | Ancla | Éxito | Delega en |
|---|---|---|---|---|---|
| 1 | `POST /api/v1/actividades` | `crear` | `:26-30` | **201** (`:29`) | `useCase.crear` (`:28`) |
| 2 | `GET /api/v1/actividades/{id}` | `obtener` | `:32-35` | 200 (`:34`) | `useCase.obtener` |
| 3 | `PUT /api/v1/actividades/{id}` | `actualizar` | `:37-42` | 200 (`:41`) | `useCase.actualizar` |
| 4 | `GET /api/v1/actividades` | `listar` | `:44-71` | 200 | dispatch (ver 8.2) |
| 5 | `PATCH /api/v1/actividades/{id}/estado` | `cambiarEstado` | `:73-79` | 200 (`:78`) | `useCase.cambiarEstado` |
| 6 | `DELETE /api/v1/actividades/{id}` | `eliminar` | `:81-85` | **204** (`:84`) | `useCase.eliminar` |
| 7 | `GET /api/v1/actividades/{id}/participantes` | `listarParticipantes` | `:89-92` | 200 (`:91`) | `useCase.listarParticipantes` |
| 8 | `POST /api/v1/actividades/{id}/participantes` | `agregarParticipante` | `:94-100` | **201** (`:99`) | `useCase.agregarParticipante` |
| 9 | `DELETE /api/v1/actividades/{id}/participantes/{contactoId}` | `quitarParticipante` | `:102-106` | **204** (`:105`) | `useCase.quitarParticipante` |

`@Valid` en `crear` (`:27`), `actualizar` (`:40`) y `agregarParticipante` (`:97`) activa Bean Validation del DTO.

### 8.2 Dispatch de filtros en `listar` (`:44-71`)

Acepta **7 `@RequestParam` opcionales** (`:46-52`): `estado`, `idCliente`, `idLead`, `idOportunidad`, `etiquetaId`, `idUsuario`, `vencidas`. Pero aplica **solo uno**, por prioridad if-else:

```
vencidas==true  → listarVencidas        (:54-55)
etiquetaId      → filtrarPorEtiqueta    (:56-57)
estado          → filtrarPorEstado      (:58-59)
idCliente       → filtrarPorCliente     (:60-61)
idLead          → filtrarPorLead        (:62-63)
idOportunidad   → filtrarPorOportunidad (:64-65)
idUsuario       → filtrarPorUsuario     (:66-67)
(ninguno)       → listarTodos           (:70)
```

**Consecuencia:** los filtros **no son combinables**. Si el cliente envía `estado=PENDIENTE&idCliente=5`, solo se aplica `etiquetaId/estado` por prioridad e ignora el resto. El frontend Angular sí soporta enviar varios params (`actividades.service.ts:14-22`), pero el componente de lista no los usa: carga todo y filtra en memoria (§16.3). Brecha documentada en §18.

---

## 9. Capa `adapters/out/persistence` — Adapters

5 adapters `@Component`. Tres implementan puertos propios de persistencia; dos implementan puertos de consulta cross-module.

### 9.1 `ActividadPersistenceAdapter`

`actividades/adapters/out/persistence/ActividadPersistenceAdapter.java:17` (`implements ActividadPersistencePort`). Inyecta `ActividadSpringDataRepository` (`:19`, ctor `:21`).

- **`save:26`**: `toEntity(actividad)` (`:27`) → `repository.save(entity)` (`:28`) → `toDomain(saved)` (`:29`). Triple transformación dominio→entidad→BD→entidad→dominio.
- **`findAll:38`**: orden **explícito** `Sort.by(DESC, "id")` (`:43`). El comentario `:39-42` explica por qué: sin `ORDER BY`, Oracle devuelve filas en orden de almacenamiento y cualquier `UPDATE` puede alterarlo; se ordena por id descendente para que lo recién creado aparezca primero.
- Demás métodos (`:47-90`): delegan al repositorio y mapean con `toDomain`.
- **Mapeo manual:** `toDomain(entity):92` (constructor completo de `Actividad`) y `toEntity(domain):108` (new + setters). Sin MapStruct.

### 9.2 `ActividadEtiquetaPersistenceAdapter`

`.../ActividadEtiquetaPersistenceAdapter.java:17`. Inyecta `ActividadEtiquetaSpringDataRepository` (`:19`).

- **`findByIdActividad:26`**: usa `findByIdActividadOrderByOrdenAsc` (`:27`) — respeta el orden persistido.
- **`findByIdActividadIn:33`**: batch; si vacío/null → `List.of()` (`:34-36`); si no, `findByIdActividadInOrderByIdActividadAscOrdenAsc` (`:37`).
- **`reemplazarEtiquetas:43`** — el método técnicamente más delicado:
  1. `repository.deleteByIdActividad(idActividad)` (`:44`) — borra todos los vínculos.
  2. **`repository.flush()`** (`:45`) — fuerza el DELETE a la BD **antes** de insertar. Sin este flush, Hibernate podría reordenar y el INSERT chocaría con la unique constraint `uk_actividad_etiqueta` si se re-agrega la misma etiqueta. Punto sutil de JPA.
  3. Si la lista es vacía/null, retorna (`:47-49`).
  4. Limpia nulos + deduplica preservando orden (`:51-54`).
  5. Construye entidades con **`orden = i`** (índice de posición, `:57-59`).
  6. `repository.saveAll(nuevas)` (`:62`).
- `deleteByIdActividad:66`, `toDomain:70`, `toEntity:80`.

### 9.3 `ActividadParticipantePersistenceAdapter`

`.../ActividadParticipantePersistenceAdapter.java:14`. Inyecta `ActividadParticipanteSpringDataRepository` (`:16`). `save:23`, `findByIdActividad:29`, `findByIdActividadAndIdContacto:34`, `existsByIdActividadAndIdContacto:39`, `delete:44` (borra por id, `:45`), `deleteByIdActividad:49`, `toDomain:53`, `toEntity:62`.

### 9.4 `ContactoQueryAdapter` (cross-module → clientes)

`.../ContactoQueryAdapter.java:21` (`implements ContactoQueryPort`). Inyecta **`ManageClienteUseCase`** (`:23`, ctor `:25`) — el **puerto de entrada público** de clientes, no sus repositorios.

- **`buscarPorId:30`**: `clienteUseCase.listar().stream().flatMap(cliente -> cliente.contactos().stream()).filter(c -> c.id().equals(idContacto)).findFirst()` (`:31-34`).
- El comentario `:10-19` documenta la limitación: el API público de clientes **no expone** búsqueda directa por id de contacto, así que recorre **todos los clientes y todos sus contactos**. Cuando clientes exponga `obtenerContacto(contactoId)`, este adapter es el único punto a cambiar.
- **Limpio en Modularith** (solo API pública) pero **ineficiente** (escaneo O(n) por búsqueda). Brecha §18.

### 9.5 `RelacionValidationAdapter` (cross-module → 4 módulos)

`.../RelacionValidationAdapter.java:18` (`implements RelacionValidationPort`). Inyecta (`:20-23`):
- `ClienteSpringDataRepository` (clientes, OPEN)
- `LeadSpringDataRepository` (leads, OPEN)
- `OportunidadSpringDataRepository` (oportunidades, OPEN)
- `UsuariosApi` (usuarios, **CLOSED** → solo API pública)

Métodos:
- `existeCliente:37` → `clienteRepository.existsById` (`:38`).
- `existeLead:42` → `leadRepository.existsById` (`:43`).
- `existeOportunidad:47` → `oportunidadRepository.existsById` (`:48`).
- **`existeUsuario:52`**: intenta `Long.parseLong(idUsuario)` y `usuariosApi.existePorId` (`:54`); si `NumberFormatException`, cae a `usuariosApi.existePorCorreo(idUsuario)` (`:55-56`). Soporta id numérico **o** correo.

**Acoplamiento:** usa repositorios **internos** de clientes/leads/oportunidades. Permitido porque son OPEN, pero más estrecho que consumir su API pública. Contraste deliberado con `ContactoQueryAdapter` (limpio) y con usuarios (CLOSED obliga a API pública). Brecha/observación §18.

---

## 10. Entidades JPA y tablas Oracle

3 entidades `@Entity` en `adapters/out/persistence/entity/`. Mapean el dominio a tablas; son **infraestructura**, no dominio.

### 10.1 `ActividadEntity` → `CRM_ACTIVIDADES`

`.../entity/ActividadEntity.java:8-9` (`@Entity`, `@Table(name="CRM_ACTIVIDADES")`).

| Columna | Campo | Ancla | Notas |
|---|---|---|---|
| `id_actividad` | `id` | `:12-15` | `@Id @GeneratedValue(IDENTITY)` |
| `id_usuario` | `idUsuario` (String) | `:17-18` | |
| `id_cliente` | `idCliente` | `:20-21` | ref. lógica |
| `id_lead` | `idLead` | `:23-24` | ref. lógica |
| `id_oportunidad` | `idOportunidad` | `:26-27` | ref. lógica |
| `tipo` | `tipo` | `:29-31` | `@Enumerated(STRING)`, `nullable=false` |
| `titulo` | `titulo` | `:33-34` | `nullable=false` |
| `descripcion` | `descripcion` | `:36-38` | **`@Lob`** (CLOB) |
| `fecha` | `fecha` | `:40-41` | `nullable=false` |
| `estado` | `estado` | `:43-45` | `@Enumerated(STRING)`, `nullable=false` |
| `creado_en` | `creadoEn` | `:47-48` | `nullable=false`, **`updatable=false`** (inmutable tras insert) |

### 10.2 `ActividadEtiquetaEntity` → `CRM_ACTIVIDAD_ETIQUETAS`

`.../entity/ActividadEtiquetaEntity.java:7-10`. **Unique constraint** `uk_actividad_etiqueta` sobre `(id_actividad, id_etiqueta)` (`:8-9`): una etiqueta no se repite por actividad.

| Columna | Ancla | Notas |
|---|---|---|
| `id_actividad_etiqueta` | `:13-16` | `@Id @GeneratedValue(IDENTITY)` |
| `id_actividad` | `:18-19` | `nullable=false` |
| `id_etiqueta` | `:21-22` | `nullable=false`, ref. lógica a etiquetas |
| `creado_en` | `:24-25` | `nullable=false`, `updatable=false` |
| `orden` | `:27-28` | `Integer`, posición de la etiqueta |

### 10.3 `ActividadParticipanteEntity` → `CRM_ACTIVIDAD_PARTICIPANTES`

`.../entity/ActividadParticipanteEntity.java:6-9`. **Unique constraint** `uk_actividad_contacto` sobre `(id_actividad, id_contacto)` (`:7-8`): un contacto no se repite como participante.

| Columna | Ancla | Notas |
|---|---|---|
| `id_participante` | `:12-15` | `@Id @GeneratedValue(IDENTITY)` |
| `id_actividad` | `:17-18` | `nullable=false` |
| `id_contacto` | `:20-22` | `nullable=false`; comentario `:20`: **ref. lógica a `CRM_CONTACTOS` (clientes), sin FK física** |
| `creado_en` | `:24-25` | `nullable=false`, `updatable=false` |

**Patrón común:** todas las relaciones cross-module son **referencias lógicas** (columnas `id_*` sin `@ManyToOne` ni FK física). La integridad la garantiza la aplicación (`validarRelaciones`, `buscarPorId`) y, para duplicados, las unique constraints. Es desacoplamiento entre módulos a nivel de BD (cada módulo es dueño de su tabla), a costa de no tener integridad referencial declarativa.

---

## 11. Repositorios Spring Data

3 interfaces `@Repository` que `extends JpaRepository<Entity, Long>`. Spring genera la implementación en runtime; los métodos derivan SQL del nombre, salvo 2 `@Query` JPQL.

### 11.1 `ActividadSpringDataRepository`

`.../repository/ActividadSpringDataRepository.java:15`.
- Derivadas: `findByEstado:16`, `findByIdCliente:17`, `findByIdLead:18`, `findByIdOportunidad:19`, `findByIdUsuario:25`, `findByTipo:26`.
- **`@Query` JPQL `findByEtiquetaId:21-23`**: `SELECT DISTINCT a FROM ActividadEntity a WHERE a.id IN (SELECT ae.idActividad FROM ActividadEtiquetaEntity ae WHERE ae.idEtiqueta = :idEtiqueta)`. Subconsulta sobre la tabla de vínculos.
- **`@Query` JPQL `findVencidas:32-34`**: `SELECT a FROM ActividadEntity a WHERE a.estado = :estado AND a.fecha < :ahora`. Comentario `:28-31`: "VENCIDA" es derivada, no un estado persistido.

### 11.2 `ActividadEtiquetaSpringDataRepository`

`.../repository/ActividadEtiquetaSpringDataRepository.java:11`.
- `findByIdActividadOrderByOrdenAsc:12`
- `findByIdActividadInOrderByIdActividadAscOrdenAsc:14` (batch, el `In`)
- `deleteByIdActividad:16`

### 11.3 `ActividadParticipanteSpringDataRepository`

`.../repository/ActividadParticipanteSpringDataRepository.java:11`.
- `findByIdActividad:12`
- `findByIdActividadAndIdContacto:13`
- `existsByIdActividadAndIdContacto:14`
- `deleteByIdActividad:15`

---

## 12. Transacciones

### 12.1 Dónde está el límite

**En el Service, no en el controller ni en el adapter.** `ActividadService:36` lleva `@Transactional` a nivel de clase → todo método público corre en transacción. Las consultas la sobrescriben con `@Transactional(readOnly=true)` (10 métodos, §7.2); las 6 escrituras heredan read-write.

### 12.2 Qué conforma una transacción (5 piezas)

1. **`@Transactional`** (`ActividadService:36`) — declara el límite (el método).
2. **Proxy AOP de Spring** — abre la transacción antes del método, commit al retornar, rollback ante `RuntimeException`.
3. **`JpaTransactionManager`** — auto-configurado por Spring Boot; ejecuta begin/commit/rollback.
4. **Una `Connection` JDBC a Oracle** — atada al hilo durante toda la transacción; todas las operaciones usan la misma conexión.
5. **`EntityManager` / persistence context** — acumula cambios y hace `flush` a SQL.

### 12.3 Atomicidad: el caso `crear`

Una sola transacción envuelve (`ActividadService:63-83`):
- `validarRelaciones` (lecturas cross-module, `:64`)
- `persistencePort.save` → INSERT actividad (`:80`)
- `sincronizarEtiquetas` → DELETE+INSERT vínculos (`:81`)
- `cargarEtiquetas` → SELECT (`:82`)

Si el INSERT de actividad funciona pero `reemplazarEtiquetas` falla → **rollback total**: no queda actividad huérfana. Propagación por defecto **`REQUIRED`**: los adapters/repositorios llamados dentro se **suman a la misma transacción** (no abren una nueva).

### 12.4 Rollback y excepciones

`ResourceNotFoundException extends RuntimeException` (`shared/exception/ResourceNotFoundException.java:3`). Por defecto Spring hace rollback ante `RuntimeException`. En `crear`, `validarRelaciones` lanza **antes** de cualquier save, así que no hay nada que revertir; en `eliminar` (`:186`), el borrado en cascada (etiquetas→participantes→actividad, `:190-192`) es atómico: si falla el delete de actividad, se revierten los deletes de hijos.

### 12.5 El `flush()` explícito

`ActividadEtiquetaPersistenceAdapter.reemplazarEtiquetas:45` hace `repository.flush()` entre el DELETE (`:44`) y el INSERT (`:62`). No es un commit; fuerza a Hibernate a ejecutar el DELETE en la BD **dentro de la misma transacción** antes de insertar, para no violar `uk_actividad_etiqueta`. El test `reemplazarEtiquetasAsignaElOrdenRecibido` verifica el orden delete→flush→saveAll (`ActividadEtiquetaPersistenceAdapterTest.java:33-38`).

### 12.6 Qué NO aplica a este módulo

La brecha "I/O externo dentro de transacción" (P2.4, presente en `clientes.consultarDocumento` con APIsPERU) **no aplica a actividades**: el módulo no tiene adapter `out/external` ni llamadas HTTP salientes. Toda su E/S es BD (propia o cross-module), que sí debe estar transaccional.

---

## 13. Cruces entre módulos

Todo módulo consumido está en `allowedDependencies` (`package-info.java:3`). Tipos Modulith verificados: clientes/leads/oportunidades/etiquetas = **OPEN**; usuarios = **CLOSED**.

| Módulo | Qué consume actividades | Vía / contrato | Tipo | Calidad del acoplamiento |
|---|---|---|---|---|
| **clientes** | `ContactoResponse` (nombre, correo) para participantes | `ContactoQueryAdapter:30` → `ManageClienteUseCase.listar()` (port/in **público**, `clientes/.../ManageClienteUseCase.java:13`) | OPEN | **Limpio** (solo API pública) pero **ineficiente** (flatMap sobre todos los clientes) |
| **clientes** | `existeCliente` | `RelacionValidationAdapter:38` → `ClienteSpringDataRepository.existsById` (repo **interno**) | OPEN | Acoplado a persistencia interna |
| **leads** | `existeLead` | `RelacionValidationAdapter:43` → `LeadSpringDataRepository.existsById` | OPEN | Acoplado a persistencia interna |
| **oportunidades** | `existeOportunidad` | `RelacionValidationAdapter:48` → `OportunidadSpringDataRepository.existsById` | OPEN | Acoplado a persistencia interna |
| **usuarios** | `existeUsuario` (id o correo) | `RelacionValidationAdapter:54-56` → `UsuariosApi.existePorId/existePorCorreo` (`usuarios/UsuariosApi.java:16,18`) | **CLOSED** | **Limpio forzado** (CLOSED solo expone API pública) |
| **etiquetas** | `Etiqueta`, `EtiquetaResponse`, `findById`, `findAllById` | `ActividadService:46` inyecta `EtiquetaPersistencePort` (port/**out** de etiquetas, `etiquetas/.../EtiquetaPersistencePort.java:9`); imports de dominio `:18-19` | OPEN | Acoplado a port/out + dominio interno |
| **shared.exception** | `ResourceNotFoundException` | import directo (`ActividadService:20`) | shared | Limpio |

**Dos filosofías de cruce conviven:**
- **Vía API pública** (correcta hexagonalmente): `ContactoQueryAdapter` usa `ManageClienteUseCase`; `usuarios` obliga a `UsuariosApi` por ser CLOSED.
- **Vía internals** (permitido por OPEN, más acoplado): `RelacionValidationAdapter` usa repos de clientes/leads/oportunidades; `ActividadService` usa el port/out y dominio de etiquetas.

`ManageClienteUseCase` (`clientes/.../ManageClienteUseCase.java:12-28`) **no** tiene `obtenerContacto(contactoId)`; sí tiene `listarContactos(clienteId)` (`:27`), pero exige saber el cliente dueño. De ahí el workaround `listar().flatMap` de `ContactoQueryAdapter`.

---

## 14. Cadena ida-y-vuelta completa

### 14.1 `POST /api/v1/actividades` (crear) — extremo a extremo

```
ANGULAR (:4200)                                    SPRING (:8080)
ActividadForm.guardar (actividad-form.ts:345)
  → ActividadesService.crear (actividades.service.ts:30)
    → http.post(buildUrl('/actividades'))  (:31)
      → jwtInterceptor (token)  ──HTTP──►  DispatcherServlet
                                           → filtros seguridad (JWT/Keycloak)
                                           → ActividadController.crear:27  [@Valid]
                                             → useCase.crear:28 (ManageActividadUseCase:10)
╔══ @Transactional ABRE (ActividadService:36) ═════════════════════════╗
║  ActividadService.crear:63                                           ║
║   → validarRelaciones:64 → RelacionValidationPort                    ║
║       → RelacionValidationAdapter:37 → repos OPEN / UsuariosApi      ║
║   → etiquetaIdsValidos:65 → etiquetaPort.findAllById:268             ║
║   → persistencePort.save:80                                          ║
║       → ActividadPersistenceAdapter.save:26                          ║
║         → toEntity:27 → repository.save:28 → Oracle CRM_ACTIVIDADES  ║
║         → toDomain:29                                                ║
║   → sincronizarEtiquetas:81                                          ║
║       → ActividadEtiquetaPersistenceAdapter.reemplazarEtiquetas:43   ║
║         → delete:44 → flush:45 → saveAll:62 → CRM_ACTIVIDAD_ETIQUETAS║
║   → cargarEtiquetas:82 → toResponse:382 (vencida derivada:383)       ║
╚══ COMMIT (o ROLLBACK si RuntimeException) ═══════════════════════════╝
                                             → ResponseEntity 201 CREATED:29
  ◄──HTTP── JSON ActividadResponse
ActividadForm: draft.clear, cerrar(true) (:376-377)
  → ActividadList.cargarActividades (:274) recarga la tabla
```

### 14.2 `GET /api/v1/actividades` (listar todos)

`ActividadController.listar:45` (sin params) → `useCase.listarTodos:70` → `ActividadService.listarTodos:126` (readOnly) → `persistencePort.findAll` → `ActividadPersistenceAdapter.findAll:38` (Sort DESC id, `:43`) → Oracle → `toResponses:312` (**batch** de etiquetas: `findByIdActividadIn:323` + `findAllById:335`, evita N+1) → `List<ActividadResponse>` → 200.

### 14.3 `POST /api/v1/actividades/{id}/participantes`

`ActividadController.agregarParticipante:95` → `useCase.agregarParticipante:98` → `ActividadService.agregarParticipante:196`: verifica actividad (`:197`), `contactoQueryPort.buscarPorId:200` → `ContactoQueryAdapter:30` → `ManageClienteUseCase.listar().flatMap` → clientes; chequea duplicado (`:202`), `participantePort.save:211` → `ActividadParticipantePersistenceAdapter.save:23` → Oracle `CRM_ACTIVIDAD_PARTICIPANTES`; `toParticipanteResponse:212` (enriquece nombre/correo) → **201** (`:99`).

---

## 15. Manejo de errores

El controller **no** catchea excepciones; delega en `shared/exception/GlobalExceptionHandler.java:24` (`@RestControllerAdvice`). Cuerpo de error estándar `baseBody:112`: `{timestamp, status, error, message}` (`:113-118`).

| Expción lanzada en actividades | Dónde | HTTP | Handler |
|---|---|---|---|
| `ResourceNotFoundException` | actividad/cliente/lead/oportunidad/usuario/etiqueta/contacto inexistente (ej. `ActividadService:90,120,164,198,201,237-246,278`) | **404** | `GlobalExceptionHandler:27-30` |
| `IllegalArgumentException` | >8 etiquetas (`ActividadService:261`), participante duplicado (`:203`) | **400** | `:55-58` |
| `MethodArgumentNotValidException` | `@Valid` falla en `ActividadRequest`/`ParticipanteRequest` | **400** + `fieldErrors` | `:32-42` |
| `HttpMessageNotReadableException` | JSON malformado | 400 | `:44-47` |
| `MethodArgumentTypeMismatchException` | param tipo inválido (ej. id no numérico) | 400 | `:49-53` |
| `DataIntegrityViolationException` | choque de unique constraint en carrera (`uk_actividad_etiqueta`, `uk_actividad_contacto`) | **409** | `:65-69` |
| `Exception` (genérica) | cualquier otro fallo | 500 | `:103-106` |

**Matiz de duplicado de participante:** el chequeo explícito `existsByIdActividadAndIdContacto` (`ActividadService:202`) lanza `IllegalArgumentException → 400`. Pero si dos peticiones concurrentes pasan el chequeo a la vez, la unique constraint `uk_actividad_contacto` (`ActividadParticipanteEntity:8`) atrapa la segunda → `DataIntegrityViolationException → 409`. Defensa en profundidad (aplicación + BD).

---

## 16. Frontend consumidor (Angular)

Feature `app/features/actividades/` (8 archivos). Es el **consumidor**, no parte del módulo backend. Se conecta por HTTP, no por import de clases Java.

### 16.1 Conexión y URL

- `environments/environment.ts:2`: `apiBaseUrl = 'http://localhost:8080/api/v1'`.
- `app/core/services/api-service.ts:8`: `buildUrl(path)` = base + path.
- `app/features/actividades/actividades.service.ts:9`: `path = '/actividades'`. Métodos: `listar:13` (get, params opcionales `:14-22`), `obtener:26`, `crear:30` (post `:31`), `actualizar:34` (put `:35`), `cambiarEstado:38` (patch `:40`), `eliminar:43` (delete `:44`).

### 16.2 Modelo y rutas

- `actividades.model.ts`: enums `EstadoActividad:3` y `TipoActividad:10` (espejo de los del backend), interfaces `ActividadResponse:17` (incluye `vencida:boolean:29`, `etiquetas:EtiquetaResponse[]:30`) y `ActividadRequest:33`.
- `app/app.routes.ts`: `actividades:91` → `ActividadList`; `actividades/nuevo:95` con `data.abrirNuevaActividad:97`; `actividades/:id/editar:100` con `data.abrirEdicion:102`. Las tres rutas cargan el mismo `ActividadList`, que abre el diálogo según el flag.

### 16.3 `ActividadList` (lista)

`actividad-list/actividad-list.ts:36`. Standalone, Angular Material table.
- **Signals** (`:44-49`): `actividades`, `etiquetas`, `filtroTexto`, `filtroEtiquetaId`, `loading`, `error`.
- **`cargarActividades:103`**: llama `actividadesService.listar()` **sin filtros** (`:106`) → carga todo.
- **Filtrado client-side**: `actividadesFiltradas` computed (`:55-70`) filtra por texto (título/tipo/estado/descripción/nombre de etiqueta) y por etiqueta. `actividadesOrdenadas` computed (`:72`) ordena con `sortRows`; `sortState` por defecto `{active:'id', direction:'desc'}` (`:53`).
- `ngOnInit:85`: carga etiquetas y actividades; si la ruta trae `abrirNuevaActividad`/`abrirEdicion`, abre el diálogo (`:89-100`).
- Acciones: `completar:182` (PATCH estado COMPLETADA), `eliminar:239` (confirm + DELETE), `editar:248`/`nuevaActividad:252` → `abrirDialogo:256` (MatDialog con `ActividadForm`).
- Etiquetas visibles: máx 2 + "+N" (`getEtiquetasVisibles:170`, `getEtiquetasOcultas:174`). Gestión de etiquetas gateada por rol `ADMIN/JEFE` (`puedeGestionarEtiquetas:150`).
- Vista `actividad-list.html`: tabla Material (`:105`), columnas id/titulo/tipo/estado/fecha/acciones; chip "Vencida" si `act.vencida` (`:178-182`).

> **Observación:** el listado filtra/ordena **en memoria del cliente**, no usa los filtros del backend. Funcional para volúmenes pequeños; no escala. El backend `listar` (con sus 7 params) queda subutilizado por esta vista. Ver §18.

### 16.4 `ActividadForm` (formulario reactivo)

`actividad-form/actividad-form.ts:45`. Standalone, `ReactiveFormsModule`, puede ser diálogo o ruta.
- `maxEtiquetas = 8` (`:51`, espejo del backend). FormGroup `:96-106` (titulo required, tipo/estado/fechaHora required, vinculacionTipo, idCliente/idLead/idOportunidad).
- **`cargarCatalogos:164`**: `forkJoin` (`:169`) carga en paralelo clientes, leads, oportunidades, etiquetas, cada uno con `catchError → of([])` (degradación elegante).
- **Borradores**: `DraftService`; `valueChanges` con `debounceTime(600)` (`:136-142`) guarda borrador; `backdropClick` también guarda (`:128-134`); `restaurarBorrador:145` pregunta con confirm.
- **`guardar:345`**: valida form (`:346`), convierte `fechaHora` "YYYY-MM-DDTHH:mm" → "...:00" porque Spring espera `LocalDateTime` con segundos (`:355-356`), arma `ActividadRequest` (`:358-368`, etiquetaIds desde selección), elige `actualizar`/`crear` (`:370-372`), subscribe → `draft.clear` + `cerrar(true)` (`:376-377`) o muestra error HTTP (`:379-386`).
- `mensajeErrorHttp:390`: lee `err.error.message` (el cuerpo del `GlobalExceptionHandler`).

---

## 17. Pruebas

**34 `@Test`** en 3 clases (conteo verificado con `grep -c "@Test"`). Todas con Mockito; el controller además con MockMvc standalone.

### 17.1 `ActividadServiceTest` — 18 tests (unitario, mocks de puertos)

`actividades/application/service/ActividadServiceTest.java:36` (`@ExtendWith(MockitoExtension.class)`). **6 `@Mock`** de puertos (`:38-54`), `@InjectMocks ActividadService` (`:56`). Cubre:
- Crear exitoso (`:64`), con etiquetas (`:86`), **conserva orden de etiquetas** (`:114`, verifica `reemplazarEtiquetas(1L, List.of(4L,3L))` `:136`), rechaza >8 etiquetas (`:140`, verifica `never().save` `:148`).
- Filtrar por etiqueta (`:152`), falla por cliente inexistente (`:173`).
- **Vencida derivada**: vencida si PENDIENTE+pasado (`:184`), no vencida si COMPLETADA+pasado (`:198`).
- Actualizar falla si no existe (`:212`).
- Participantes: agregar exitoso con nombre/correo enriquecido (`:223`, asserts "Ana Perez"/"ana@mail.com" `:244-245`), falla por actividad inexistente (`:249`), contacto inexistente (`:257`), duplicado (`:271`); quitar exitoso (`:289`) / falla si no existe (`:300`); listar enriquecido (`:308`) / falla actividad inexistente (`:331`).
- Eliminar borra participantes y actividad (`:338`, verifica `deleteByIdActividad` + `deleteById` `:348-349`).

### 17.2 `ActividadControllerTest` — 14 tests (MockMvc + GlobalExceptionHandler)

`actividades/adapters/in/web/ActividadControllerTest.java:34`. `standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler())` (`:47-48`) — prueba el mapeo HTTP real. `ObjectMapper` con `JavaTimeModule` (`:50-51`). Cubre: crear válida→201 (`:55`), crear inválida→400 (`:73`), obtener existente→200 (`:85`) / inexistente→404 (`:98`), listar→200 array (`:106`), actualizar→200 (`:115`), listar vencidas→200 (`:133`), listar participantes→200 (`:142`), agregar participante válido→201 (`:151`) / sin contactoId→400 (`:168`) / actividad inexistente→404 (`:176`) / duplicado→400 (`:189`), quitar participante→204 (`:202`) / inexistente→404 (`:208`).

### 17.3 `ActividadEtiquetaPersistenceAdapterTest` — 2 tests (ArgumentCaptor)

`actividades/adapters/out/persistence/ActividadEtiquetaPersistenceAdapterTest.java:21`.
- `reemplazarEtiquetasAsignaElOrdenRecibido:30`: pasa `List.of(5L,3L,5L)` (con duplicado), verifica `deleteByIdActividad` (`:33`) + `flush` (`:34`) + `saveAll` capturado (`:37-38`); asserts: 2 entidades (dedup), orden 0 y 1 (`:41-45`).
- `findByIdActividadDevuelveElOrdenPersistido:49`: verifica que respeta `orden` ascendente (`:57-59`).

### 17.4 Modulith (transversal)

`ModularityTests.java:8` (`ApplicationModules.of(ViernesApplication.class)`), `:12` `modules.verify()` (valida fronteras de todos los módulos, incluido actividades), `:16-18` `Documenter.writeDocumentation()`.

---

## 18. Análisis de brechas

Criterio: **implementado** (funciona y está probado) / **brecha** (falta o es mejorable, con evidencia) / **no aplica hoy**.

| # | Tema | Estado | Evidencia | Impacto / mejora |
|---|---|---|---|---|
| 1 | Hexagonal estricto (domain puro, puertos, inversión) | **Implementado** | `domain/*` sin framework; `port/in`+`port/out`; adapters `implements` | Núcleo testeable con mocks (18 tests) |
| 2 | Batch fetching de etiquetas en listados | **Implementado** | `ActividadService.toResponses:312-349` | Evita N+1 en `listarTodos`/filtros |
| 3 | Orden explícito en `findAll` | **Implementado** | `ActividadPersistenceAdapter:43` + comentario `:39-42` | Estabilidad ante orden de almacenamiento Oracle |
| 4 | `flush()` antes de re-insertar etiquetas | **Implementado** | `ActividadEtiquetaPersistenceAdapter:45`; test `:34` | Evita choque con `uk_actividad_etiqueta` |
| 5 | "Vencida" derivada, no persistida | **Implementado** | `toResponse:383`; JPQL `findVencidas:32` | Sin estado obsoleto en BD |
| 6 | `ContactoQueryAdapter.buscarPorId` escanea todos los clientes | **Brecha** | `ContactoQueryAdapter:31-34`; falta `obtenerContacto` en `ManageClienteUseCase:12-28` | O(n) por búsqueda; exponer `obtenerContacto(id)` en clientes y actualizar solo este adapter |
| 7 | `listarParticipantes` consulta contacto por cada participante | **Brecha (N+1 cross-module)** | `ActividadService:230-231` (map con `buscarPorId` por elemento) | Con k participantes, k escaneos de clientes; batch o caché |
| 8 | `RelacionValidationAdapter` acoplado a repos internos OPEN | **Brecha (acoplamiento)** | `RelacionValidationAdapter:20-22,38,43,48` | Depende de persistencia de otros módulos; ideal API pública de existencia |
| 9 | `ActividadService` usa port/out + dominio de etiquetas | **Observación (OPEN lo permite)** | `ActividadService:18-19,46`; `etiquetas/package-info.java:1` | Acoplamiento estrecho; aceptable en OPEN, pero no "API pública" |
| 10 | Filtros del backend no combinables (prioridad if-else) | **Brecha** | `ActividadController:54-70` | `estado+idCliente` ignora uno; especificación/combinación de filtros |
| 11 | Listado Angular filtra en memoria, no usa filtros backend | **Brecha (escala)** | `actividad-list.ts:106` (listar sin params), `:55-70` (computed) | No escala con muchas actividades; paginación + filtros server-side |
| 12 | Sin paginación en listados | **Brecha** | `ActividadPersistenceAdapter.findAll:38`; `toResponses:312` | Carga todo; añadir `Pageable` |
| 13 | Mapeo manual (sin MapStruct), sin paquete `mapper/` | **Observación** | `ActividadPersistenceAdapter:92,108`; `ActividadService:371,382` | Verboso pero explícito; MapStruct reduciría boilerplate |
| 14 | Límite 8 etiquetas en dos fuentes de verdad | **Observación** | `ActividadRequest:32` (`@Size`) y `ActividadService:39` (`MAX_ETIQUETAS`) | Riesgo de divergencia; centralizar constante |
| 15 | Integridad referencial lógica (sin FK física) + cascada manual | **Observación / trade-off** | `ActividadParticipanteEntity:20`; `ActividadService.eliminar:190-192` | Desacopla módulos en BD; integridad depende de la app |
| 16 | Domain model anémico (reglas en Service) | **Observación** | `Actividad.java` solo getters/setters | Válido en hexagonal; no es breach |
| 17 | I/O externo dentro de transacción (P2.4) | **No aplica hoy** | actividades no tiene `adapters/out/external` | La brecha es de `clientes` (APIsPERU), no de actividades |
| 18 | Duplicado participante: chequeo + unique constraint | **Implementado (defensa en profundidad)** | `ActividadService:202`; `ActividadParticipanteEntity:8`; `GlobalExceptionHandler:65` | 400 explícito / 409 en carrera |

---

## 19. Inventario cuantitativo

Regla de conteo explícita (para que cuadre en la sustentación):

- **Archivos backend main = 29**: 1 `package-info` + 5 domain + 4 dto + 1 port/in + 5 port/out + 1 service + 1 controller + 5 adapters + 3 entities + 3 repositories.
- **Capas:** domain 5, application 11 (4 dto + 6 port + 1 service), adapters 12 (1 in/web + 5 out/persistence adapters + 3 entity + 3 repository), package-info 1. Total 29.
- **Puertos:** 1 entrada (16 métodos) + 5 salida propios (12+4+6+1+4 = **27 métodos**) + 1 prestado de etiquetas (`EtiquetaPersistencePort`).
- **Service:** 16 métodos públicos (`@Override`) + **9 helpers privados** (`validarRelaciones:235`, `etiquetaIdsValidos:250`, `sincronizarEtiquetas:284`, `cargarEtiquetas:291`, `cargarEtiquetasPorIds:298`, `toResponses:312`, `toParticipanteResponse:352`, `toEtiquetaResponse:371`, `toResponse:382`).
- **Transacciones:** 10 métodos `readOnly=true` + 6 read-write (heredan clase) = 16.
- **Controller:** 9 endpoints — 3 GET (`obtener`, `listar`, `listarParticipantes`), 2 POST (`crear`, `agregarParticipante`), 1 PUT (`actualizar`), 1 PATCH (`cambiarEstado`), 2 DELETE (`eliminar`, `quitarParticipante`).
- **Persistencia:** 3 entidades → 3 tablas Oracle; 2 unique constraints (`uk_actividad_etiqueta`, `uk_actividad_contacto`); 2 `@Query` JPQL (`findByEtiquetaId`, `findVencidas`); resto derivadas por nombre.
- **Pruebas:** 3 clases, 34 `@Test` (18 service + 14 controller + 2 adapter) + ModularityTests transversal.
- **Frontend:** 8 archivos (model, service, list .ts/.html/.css, form .ts/.html/.css).
- **Cruces:** 6 módulos consumidos (clientes, leads, oportunidades, usuarios, etiquetas, shared.exception); 2 adapters cross-module (`ContactoQueryAdapter`, `RelacionValidationAdapter`) + 1 puerto prestado (etiquetas).

---

## 20. Cómo leer el módulo

Ruta de lectura recomendada para entender cualquier módulo hexagonal (y actividades en particular):

1. **`package-info.java`** — tipo Modulith y dependencias permitidas (la frontera).
2. **`port/in/ManageActividadUseCase`** — el contrato de casos de uso (qué puede hacer el módulo).
3. **`service/ActividadService`** — las reglas y la transacción (el corazón).
4. **`port/out/*`** — qué necesita del exterior (persistencia, validación, contactos).
5. **`adapters/out/*`** — cómo se satisface esa necesidad (Oracle, otros módulos).
6. **`domain/*`** — el modelo puro (datos + enums).
7. **`adapters/in/web/ActividadController`** — cómo entra por HTTP (9 endpoints).
8. **`entity/*` + `repository/*`** — el mapeo a tablas y las queries.
9. **Tests** — el comportamiento esperado y probado.
10. **Frontend `features/actividades/`** — quién lo consume y cómo.

**Cadena mental resumida:** `HTTP → Controller (in/web) → port/in → Service (@Transactional) → port/out → Adapter (out) → Oracle / otro módulo → DTO → HTTP`. Ida por las capas, vuelta transformando (entidad→dominio→DTO). El arranque (`ViernesApplication.main`) solo deja el grafo inyectado y Tomcat escuchando; cada petición dispara su propia cadena (§14).

---

*Informe generado sobre el código real de `E:\Viernes\Viernes`. Anclajes `archivo:línea` verificados por lectura directa de los 29 archivos backend, 3 tests, contratos cross-module y 8 archivos frontend. Validación estructural/textual, no por imagen.*