# Revisión de la Base de Datos VIERNES CRM

Fecha de revisión: 2026-11-20
Alcance: carpeta `base de datos/` (y su copia idéntica en `viernes-backend/db/`), scripts `init/`, modulares `01_*`..`08_*`, vistas, funciones, procedimiento, trigger y seed.

## Resumen ejecutivo

La base de datos está bien estructurada para un CRM académico: 21 tablas, tablespaces separados para datos/índices/temp, identidades, índices secundarios, vistas de negocio y administración, funciones/procedimiento/trigger y seed idempotente. Se detectaron **inconsistencias entre las dos rutas de despliegue** (Docker `init/` vs. ejecución manual modular) y entre las **dos copias del proyecto** (top-level y `viernes-backend/db/`), además de varios puntos de diseño a mejorar.

## Cambios aplicados durante la revisión

1. **Regeneración de `init/003_esquema.sql`** a partir de los modulares `03_tablas/*.sql`, de modo que la ruta Docker use exactamente el mismo DDL que la ruta manual (incluyendo `USING INDEX TABLESPACE VIERNES_INDICES` y `ON DELETE CASCADE`).
2. **Regeneración de `init/004_plsql.sql`** a partir de `04_funciones/`, `05_procedimientos/`, `06_vistas/` y `07_triggers/`, garantizando identidad entre ambas rutas.
3. **Alineación del seed modular** (`08_datos_semilla/seed_datos_demo.sql`) con `init/005_seed.sql`, que es el más completo (evita divergencia en datos demo, p. ej. nombre de producto `Implementación` vs. `Implementación CRM`).
4. **Sincronización de la copia** `viernes-backend/db/init/` con la top-level, para que ambas rutas produzcan el mismo esquema.

## Hallazgos

### Críticos / Altos

- **H1. Dos copias idénticas de la BD** (`base de datos/` y `viernes-backend/db/`). Cualquier edición en una sola rompe la paridad. Se sincronizaron en esta revisión; se recomienda mantener un único origen (p. ej. `viernes-backend/db/`) y generar el otro, o eliminar la duplicación.
- **H2. Divergencia DDL entre `init/` y modulares** (resuelta): `init/003` no usaba `USING INDEX TABLESPACE VIERNES_INDICES` ni `ON DELETE CASCADE`, mientras los modulares sí. Causaba que el tablespace de índices estuviera infrautilizado y que el borrado en cascada difiriera entre rutas.
- **H3. Seed modular incompleto/desactualizado** (resuelto): faltaban fuentes, etiquetas, productos, clientes, oportunidades, propuestas, actividades, campañas, equipos y mensajes; además el producto `SRV-IMPL` se llamaba `Implementación CRM` en el modular vs. `Implementación` en `init`.
- **H4. Credenciales demo inconsistentes**: el `README` de `viernes-backend/db` indica que todos los usuarios usan `Admin12345`, pero el seed tiene un hash bcrypt distinto para `admin@viernes.com`. Verificar si la clave del admin es la misma o documentar la clave real.
- **H5. `CRM_ACTIVIDADES.ID_USUARIO` es `VARCHAR2(255)` que almacena un correo, no un ID, y no tiene FK a `CRM_USUARIOS`. El nombre de columna es engañoso y el diseño es frágil ante cambios de correo.**

### Medios

- **M1. Pocas FK físicas**: solo `CRM_CONTACTOS.ID_CLIENTE` y `CRM_PROPUESTA_DETALLES.ID_PROPUESTA` tienen FK. El resto son referencias lógicas (intencional según comentarios), pero reduce la integridad referencial. Especialmente `CRM_OPORTUNIDADES.CLIENTE_NOMBRE` (string) y `PR_VIERNES_RESUMEN_CLIENTE`, que une oportunidades por nombre de cliente → se desfasa si se renombra un cliente.
- **M2. Índices faltantes en FK secundarias no cubiertas por constraints únicos**: `CRM_PROPUESTA_DETALLES.ID_PRODUCTO` y `CRM_ACTIVIDADES.ID_LEAD`. (Las demás FK compuestas tienen índice por el leading de la UNIQUE.)
- **M3. `CRM_CLIENTES.UK_CRM_CLIENTES_DOCUMENTO` permite duplicados cuando `TIPO_DOCUMENTO`/`NUMERO_DOCUMENTO` son NULL** (semántica de NULL en Oracle). Puede generar clientes duplicados sin documento.
- **M4. Redundancia en `CRM_LEADS`**: columnas `FUENTE` (string) y `FUENTE_ID` (FK lógica) duplican información.
- **M5. `CRM_PROPUESTA_DETALLES` carece de `CREADO_EN`** (el resto de tablas lo tiene) y de FK a producto.
- **M6. `VW_VIERNES_PIPELINE`**: el `CASE` de `ORDEN_ETAPA` no tiene `ELSE`; etapas desconocidas quedan con `NULL`. Conviene agregar `ELSE 99` (o similar) para ordenamiento estable.
- **M7. `VW_VIERNES_DATAFILES`** depende de `DBA_DATA_FILES`/`DBA_TEMP_FILES`; los permisos se otorgan en `002_usuario.sql` (OK), pero si se conecta con otro usuario falla.
- **M8. `FN_VIERNES_TOTAL_PIPELINE`** usaba `OR` en el `WHERE` (`p_etapa IS NULL OR ETAPA = p_etapa`), lo que impedía usar el índice `IX_CRM_OPORTUNIDADES_ETAPA` y forzaba full scan. **(resuelto)**
- **M9. `CRM_ETIQUETAS.NOMBRE_NORMALIZADO VARCHAR2(80)`** está sobredimensionado respecto a `NOMBRE VARCHAR2(40)`.
- **M10. `CRM_PROPUESTAS.VERSION NUMBER(10,0)` y `PROBABILIDAD NUMBER(10,0)`** son adecuados pero sin CHECK; la validación es solo en la app.

### Bajos

- **B1. Tipos sobredimensionados**: `CRM_ACTIVIDADES.TIPO/ESTADO VARCHAR2(255)` y `ID_USUARIO VARCHAR2(255)` para valores cortos.
- **B2. `CRM_ACTIVIDAD_ETIQUETAS.ORDEN` y `ES_DECISOR`** son nullable; podrían tener default.
- **B3. `CRM_MENSAJES` sin FK a usuario** (intencional para sala de chat desacoplada).
- **B4. Sin CHECK constraints** para enums (rol, estado, etapa, tipo de actividad) → integridad delegada a la app.
- **B5. Scripts de tablespaces no idempotentes** (`CREATE TABLESPACE` falla si ya existe) → esperado para volumen vacío; reiniciar requiere `down -v`.
- **B6. `reiniciar-bd-limpia.bat`** no espera el healthcheck, solo sigue logs.
- **B7. `ddl-auto: update`** en `application-dev.yml` puede desfasar el esquema real respecto al DDL canónico; conviene validar con `validate` periódicamente.
- **B8. Duplicación de `db/`** en dos rutas (ver H1).

## Recomendaciones prioritarias

1. Mantener una única fuente de verdad del DDL (y generar la otra) para evitar H1/H2/H3.
2. Agregar FK físicas al menos en `CRM_OPORTUNIDAD_PRODUCTOS.ID_OPORTUNIDAD`, `CRM_OPORTUNIDAD_COLABORADORES.ID_OPORTUNIDAD`, `CRM_EQUIPO_MIEMBROS.EQUIPO_ID`, `CRM_CAMPANIA_LEADS.CAMPANIA_ID` y `CRM_ACTIVIDAD_ETIQUETAS.ID_ACTIVIDAD`, con `ON DELETE CASCADE` donde corresponda.
3. Reemplazar `CLIENTE_NOMBRE` en oportunidades por `ID_CLIENTE` (FK) para robustez, o al menos indexar y documentar la dependencia por nombre.
4. Renombrar `CRM_ACTIVIDADES.ID_USUARIO` a `USUARIO_CORREO` (o cambiar a `ID_USUARIO NUMBER` con FK).
5. Agregar índices en `CRM_PROPUESTA_DETALLES.ID_PRODUCTO` y `CRM_ACTIVIDADES.ID_LEAD`.
6. Verificar y documentar la clave del usuario admin (H4).
7. Agregar `ELSE` al `CASE` de `VW_VIERNES_PIPELINE` y mejorar `FN_VIERNES_TOTAL_PIPELINE` para usar índice (dos ramas: una para `p_etapa IS NULL` y otra filtrada).

## Cambios adicionales aplicados durante la verificación en Oracle

1. **`VW_VIERNES_TABLESPACES` ahora usa `DBA_TABLESPACES`** en lugar de `USER_TABLESPACES`, porque las tablespaces temporales (`VIERNES_TEMP`) no eran visibles para `BOM_VENTAS` con `USER_TABLESPACES`. Ahora las 3 tablespaces (DATOS, INDICES, TEMP) se muestran correctamente. Aplicado en modular, `init/004` (ambas copias) y BD en ejecución.
2. **`PR_VIERNES_RESUMEN_CLIENTE` corregido**: contaba oportunidades por `CLIENTE_NOMBRE = NOMBRE` del cliente (persona), pero las oportunidades guardan el nombre de empresa (`Acme S.A.`). Ahora usa `NVL(EMPRESA, NOMBRE)`, de modo que el resumen del cliente Acme devuelve 1 oportunidad (antes devolvía 0). Aplicado en modular, `init/004` (ambas copias) y BD en ejecución.

## Rediseño: un tablespace por tabla

A petición del usuario, se reemplazó el esquema de 3 tablespaces (DATOS/INDICES/TEMP) por **un tablespace (y un datafile) por tabla**: 21 tablespaces `VIERNES_<TABLA>` (cada uno aloja su tabla y sus índices) más `VIERNES_TEMP` compartido. Verificado en Oracle: 22 tablespaces, 21 tablas, 22 datafiles, todas las funciones/vistas/procedimiento/trigger operativos. Archivos actualizados: `01_tablespaces/`, `03_tablas/`, `init/001`, `init/002`, `ejecutar_todo.sql` y copia en `viernes-backend/db/`.

## Capa de funciones PL/SQL (revisada y ampliada)

Antes había 3 funciones: una de negocio (`TOTAL_PIPELINE`), un cálculo puntual (`VALOR_PONDERADO`) y un simple formateo (`NOMBRE_USUARIO`). La mezcla no era coherente: no había KPIs de cierre, `TOTAL_PIPELINE` contaba también oportunidades cerradas y duplicaba el `NVL(EMPRESA, NOMBRE)` que ya usaba el procedimiento. Se reorganizó en **6 funciones**, todas de solo lectura:

| Tipo | Función | Justificación |
|---|---|---|
| Identidad | `FN_VIERNES_NOMBRE_USUARIO` | Formato único `Nombre Apellido`; tolera `NULL` y espacios dobles. |
| Identidad | `FN_VIERNES_NOMBRE_CLIENTE` | **Nueva.** Única fuente de la regla `NVL(EMPRESA, NOMBRE)`; la reutiliza `PR_VIERNES_RESUMEN_CLIENTE`. |
| Valor | `FN_VIERNES_VALOR_PONDERADO` | Valor esperado de una oportunidad; probabilidad acotada a 0–100. |
| Valor | `FN_VIERNES_TOTAL_PONDERADO` | **Nueva.** Pronóstico agregado del pipeline abierto. |
| Pipeline | `FN_VIERNES_TOTAL_PIPELINE` | Reescrita en 3 ramas (sin `OR`), normaliza la etapa a mayúsculas y por defecto excluye `GANADA`/`PERDIDA`. |
| Conversión | `FN_VIERNES_TASA_CONVERSION` | **Nueva.** % de ganadas sobre cerradas; el denominador excluye las abiertas. |

Criterio para no inflar la capa: el CRUD y las reglas transaccionales siguen en el backend Java. En Oracle solo quedan **agregados de reporting** y **reglas de identidad** que varias consultas necesitan, evitando duplicar la misma lógica en SQL.

Verificado en la BD en ejecución: `TOTAL_PIPELINE()` = 20 900, `TOTAL_PONDERADO()` = 12 150, `TOTAL_PIPELINE('negociacion')` = 12 000, `TASA_CONVERSION()` = 0 (sin cierres), y con 1 ganada + 1 perdida de prueba (transacción revertida) = 50 %.

## Conclusión

La base de datos es funcional y coherente con el backend (21 entidades JPA mapean a las 21 tablas). Los principales problemas eran de **paridad entre rutas de despliegue** y de **completitud del seed modular**, ya corregidos. Los hallazgos restantes son mejoras de diseño/integridad que conviene abordar en una siguiente iteración, priorizando las FK físicas y la relación oportunidad-cliente.
