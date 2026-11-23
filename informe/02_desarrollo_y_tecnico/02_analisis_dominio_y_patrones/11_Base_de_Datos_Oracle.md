# VIERNES CRM V1 — INFORME DE BASE DE DATOS (Oracle 23c Free)

**Fecha:** 20 de noviembre de 2026  
**Proyecto:** Viernes CRM V1 — CRM Híbrido de Gestión Comercial y Marketing  
**Motor:** Oracle 23c Free (Docker, contenedor `viernes-oracle`)  
**Esquema:** `BOM_VENTAS` sobre el PDB `FREEPDB1`  
**Ubicación de los scripts:** `base de datos/` (y copia idéntica en `viernes-backend/db/`)

---

## 1. Visión general

La base de datos implementa el modelo de dominio del CRM (21 entidades JPA del backend Spring Boot) sobre Oracle 23c Free, en un contenedor Docker dedicado. El diseño prioriza:

- **Aislamiento físico por tabla:** un tablespace y un datafile por tabla (21 tablespaces de datos + 1 temporal compartido).
- **Autoextend** en todos los datafiles (capacidad máxima 1 GB por archivo).
- **Integridad referencial** en las relaciones más críticas (contactos, propuestas, equipos, campañas).
- **Capa PL/SQL** de agregados de reporting y reglas de identidad reutilizadas por vistas, procedimientos y consultas del dashboard.
- **Reproducibilidad:** la BD se recrea desde cero en cualquier PC a partir de los scripts versionados en Git.

---

## 2. Tablespaces y datafiles (diseño "uno por tabla")

### 2.1 Estructura

| Tipo | Cantidad | Tamaño inicial | Autoextend | Maxsize | Ubicación física (en el contenedor) |
|---|---|---|---|---|---|
| Tablespace de datos (1 por tabla CRM) | 21 | 32 MB | +16 MB | 1 GB | `/opt/oracle/oradata/FREE/FREEPDB1/viernes_<abbrev>01.dbf` |
| Tablespace temporal compartido | 1 | 64 MB | +32 MB | 1 GB | `/opt/oracle/oradata/FREE/FREEPDB1/viernes_temp01.dbf` |
| **Total** | **22** | — | — | — | **22 archivos físicos** |

### 2.2 Mapeo tabla → tablespace

| # | Tabla | Tablespace | Datafile |
|---|---|---|---|
| 1 | CRM_USUARIOS | VIERNES_USUARIOS | viernes_usuarios01.dbf |
| 2 | CRM_CLIENTES | VIERNES_CLIENTES | viernes_clientes01.dbf |
| 3 | CRM_CONTACTOS | VIERNES_CONTACTOS | viernes_contactos01.dbf |
| 4 | CRM_FUENTES | VIERNES_FUENTES | viernes_fuentes01.dbf |
| 5 | CRM_LEADS | VIERNES_LEADS | viernes_leads01.dbf |
| 6 | CRM_PRODUCTOS | VIERNES_PRODUCTOS | viernes_productos01.dbf |
| 7 | CRM_OPORTUNIDADES | VIERNES_OPORTUNIDADES | viernes_oportunidades01.dbf |
| 8 | CRM_OPORTUNIDAD_CONTACTOS | VIERNES_OPOR_CONTACTOS | viernes_opor_contactos01.dbf |
| 9 | CRM_OPORTUNIDAD_COLABORADORES | VIERNES_OPOR_COLAB | viernes_opor_colab01.dbf |
| 10 | CRM_OPORTUNIDAD_PRODUCTOS | VIERNES_OPOR_PROD | viernes_opor_prod01.dbf |
| 11 | CRM_PROPUESTAS | VIERNES_PROPUESTAS | viernes_propuestas01.dbf |
| 12 | CRM_PROPUESTA_DETALLES | VIERNES_PROP_DET | viernes_prop_det01.dbf |
| 13 | CRM_ACTIVIDADES | VIERNES_ACTIVIDADES | viernes_actividades01.dbf |
| 14 | CRM_ACTIVIDAD_ETIQUETAS | VIERNES_ACT_ETIQ | viernes_act_etiq01.dbf |
| 15 | CRM_ACTIVIDAD_PARTICIPANTES | VIERNES_ACT_PART | viernes_act_part01.dbf |
| 16 | CRM_ETIQUETAS | VIERNES_ETIQUETAS | viernes_etiquetas01.dbf |
| 17 | CRM_EQUIPOS | VIERNES_EQUIPOS | viernes_equipos01.dbf |
| 18 | CRM_EQUIPO_MIEMBROS | VIERNES_EQ_MIEMBROS | viernes_eq_miembros01.dbf |
| 19 | CRM_CAMPANIAS | VIERNES_CAMPANIAS | viernes_campanias01.dbf |
| 20 | CRM_CAMPANIA_LEADS | VIERNES_CAMP_LEADS | viernes_camp_leads01.dbf |
| 21 | CRM_MENSAJES | VIERNES_MENSAJES | viernes_mensajes01.dbf |
| (temp) | (compartido) | VIERNES_TEMP | viernes_temp01.dbf |

> Cada tablespace aloja **su tabla y todos sus índices** (PK, UNIQUE, secundarios) en el mismo datafile, lo que simplifica el monitoreo por módulo funcional.

### 2.3 Por qué este diseño (y no uno compartido)

- **Aislamiento de crecimiento:** si un módulo crece (ej. mensajes, actividades), su datafile se autoextiende sin afectar a los demás.
- **Monitoreo por módulo:** `VW_VIERNES_TABLESPACES` muestra el espacio libre de cada tabla por separado, útil para el dashboard de administración.
- **Cumplimiento académico:** demuestra gestión explícita de tablespaces y datafiles (un tablespace por tabla, como solicitó el usuario).

### 2.4 Verificación en Oracle

```sql
-- 22 tablespaces
SELECT COUNT(*) FROM DBA_TABLESPACES WHERE TABLESPACE_NAME LIKE 'VIERNES_%';
-- 21 tablas
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME LIKE 'CRM_%';
-- 22 datafiles (21 + 1 temp)
SELECT COUNT(*) FROM DBA_DATA_FILES WHERE TABLESPACE_NAME LIKE 'VIERNES_%'
UNION ALL
SELECT COUNT(*) FROM DBA_TEMP_FILES WHERE TABLESPACE_NAME LIKE 'VIERNES_%';
```

Resultado verificado: **22 tablespaces, 21 tablas, 22 datafiles**.

---

## 3. Modelo de datos (21 tablas)

| Módulo | Tablas |
|---|---|
| Seguridad | `CRM_USUARIOS` |
| Clientes/Contactos | `CRM_CLIENTES`, `CRM_CONTACTOS` |
| Prospección | `CRM_FUENTES`, `CRM_LEADS` |
| Catálogo | `CRM_PRODUCTOS` |
| Comercial | `CRM_OPORTUNIDADES`, `CRM_OPORTUNIDAD_CONTACTOS`, `CRM_OPORTUNIDAD_COLABORADORES`, `CRM_OPORTUNIDAD_PRODUCTOS` |
| Propuestas | `CRM_PROPUESTAS`, `CRM_PROPUESTA_DETALLES` |
| Actividades | `CRM_ACTIVIDADES`, `CRM_ACTIVIDAD_ETIQUETAS`, `CRM_ACTIVIDAD_PARTICIPANTES` |
| Etiquetas | `CRM_ETIQUETAS` |
| Equipos | `CRM_EQUIPOS`, `CRM_EQUIPO_MIEMBROS` |
| Marketing | `CRM_CAMPANIAS`, `CRM_CAMPANIA_LEADS` |
| Mensajería | `CRM_MENSAJES` |

**Restricciones de integridad aplicadas:**
- PK por identidad (`GENERATED BY DEFAULT AS IDENTITY`) en todas las tablas.
- UNIQUE en documentos, nombres, etapas y claves compuestas.
- FK físicas en `CRM_CONTACTOS.ID_CLIENTE`, `CRM_PROPUESTA_DETALLES.ID_PROPUESTA`, `CRM_EQUIPO_MIEMBROS`, `CRM_CAMPANIA_LEADS`, `CRM_ACTIVIDAD_*` (con `ON DELETE CASCADE` donde corresponde).
- Índices secundarios en columnas de búsqueda frecuente (etapa, fecha de cierre, cliente, usuario, etc.).

---

## 4. Capa PL/SQL

### 4.1 Funciones (6) — todas de solo lectura

| # | Función | Propósito | Ejemplo de uso |
|---|---|---|---|
| 1 | `FN_VIERNES_NOMBRE_USUARIO(p_id)` | Devuelve `"Nombre Apellido"` tolerando NULL | `FN_VIERNES_NOMBRE_USUARIO(1)` → `Admin Viernes` |
| 2 | `FN_VIERNES_NOMBRE_CLIENTE(p_id)` | Devuelve `EMPRESA` si es corporativo, si no `NOMBRE` | `FN_VIERNES_NOMBRE_CLIENTE(1)` → `Acme S.A.` |
| 3 | `FN_VIERNES_VALOR_PONDERADO(p_id_op)` | Valor esperado de una oportunidad (`VALOR * PROB/100`) | `FN_VIERNES_VALOR_PONDERADO(1)` → `8400` |
| 4 | `FN_VIERNES_TOTAL_PIPELINE(p_etapa, p_solo_abiertas)` | Monto del pipeline (por defecto solo abierto) | `FN_VIERNES_TOTAL_PIPELINE('negociacion')` → `12000` |
| 5 | `FN_VIERNES_TOTAL_PONDERADO(p_etapa)` | Pronóstico: suma del valor esperado del pipeline | `FN_VIERNES_TOTAL_PONDERADO()` → `12150` |
| 6 | `FN_VIERNES_TASA_CONVERSION(p_responsable)` | % de ganadas sobre las cerradas | `FN_VIERNES_TASA_CONVERSION()` → `0` (sin cierres) |

**Mejoras aplicadas:**
- `FN_VIERNES_TOTAL_PIPELINE` reescrita en 3 ramas (sin `OR`) para usar `IX_CRM_OPORTUNIDADES_ETAPA`; normaliza la etapa a mayúsculas.
- `FN_VIERNES_VALOR_PONDERADO` acota la probabilidad a 0–100 con `LEAST/GREATEST`.
- `FN_VIERNES_NOMBRE_CLIENTE` centraliza la regla `NVL(EMPRESA, NOMBRE)` que antes se duplicaba en el procedimiento.

### 4.2 Procedimiento (1)

`PR_VIERNES_RESUMEN_CLIENTE(p_id_cliente, OUT p_oportunidades, OUT p_propuestas, OUT p_total_propuestas)`  
Devuelve el resumen comercial de un cliente: oportunidades asociadas, propuestas emitidas y monto cotizado. Reutiliza `FN_VIERNES_NOMBRE_CLIENTE`.

### 4.3 Vistas (5)

| Vista | Propósito |
|---|---|
| `VW_VIERNES_PIPELINE` | Pipeline con orden de etapas y valor ponderado |
| `VW_VIERNES_AGENDA` | Agenda consolidada de actividades |
| `VW_VIERNES_TABLESPACES` | Monitoreo de tablespaces (usa `DBA_TABLESPACES`) |
| `VW_VIERNES_DATAFILES` | Monitoreo de datafiles y autoextend |
| `VW_VIERNES_OBJETOS` | Inventario general de objetos del esquema |

### 4.4 Trigger (1)

`TRG_CRM_USUARIOS_ACTUALIZADO` — actualiza `ACTUALIZADO_EN` automáticamente en cada `UPDATE` de `CRM_USUARIOS`.

---

## 5. Datos demo (seed)

El seed (`005_seed.sql`) inserta datos idempotentes:
- 3 usuarios (admin, jefe comercial, vendedor)
- 3 clientes (Acme S.A., TechSoft, Innova SAC)
- 5 productos
- 3 fuentes, 3 leads
- 3 oportunidades, 1 propuesta con 2 detalles
- Actividades, campañas, equipos, etiquetas, mensajes

Las credenciales demo son `Admin12345` para los 3 usuarios.

---

## 6. Portabilidad y reproducibilidad

- Los datafiles **no se versionan** en Git; se regeneran automáticamente en cada PC.
- El volumen Docker se llama `viernes-oracle-data` (nombre fijo, independiente del nombre de la carpeta del proyecto).
- En una PC nueva basta con clonar el repo y ejecutar:
  ```powershell
  cd viernes-backend
  docker compose -f compose-dev.yml up -d
  ```
  Docker ejecuta los 6 scripts de `init/` sobre un volumen vacío y crea la BD idéntica en 2–5 minutos.
- Para llevar datos reales entre PCs se usan `export.ps1` / `import.ps1` (Oracle Data Pump).

---

## 7. Conexión en SQL Developer

| Parámetro | Valor |
|---|---|
| Tipo | Oracle (JDBC Thin) |
| Hostname | `localhost` |
| Puerto | `1521` |
| Service Name | `FREEPDB1` |
| Usuario | `BOM_VENTAS` |
| Contraseña | `123456` |

**Consultas clave para la presentación:**
```sql
SELECT * FROM VW_VIERNES_TABLESPACES;          -- 22 tablespaces
SELECT * FROM VW_VIERNES_DATAFILES;             -- 22 datafiles
SELECT * FROM VW_VIERNES_OBJETOS;               -- inventario
SELECT FN_VIERNES_TOTAL_PIPELINE() FROM dual;   -- 20900
SELECT FN_VIERNES_TOTAL_PONDERADO() FROM dual;  -- 12150
SELECT FN_VIERNES_TASA_CONVERSION() FROM dual;  -- 0
SELECT * FROM VW_VIERNES_PIPELINE;              -- pipeline con ponderado
```

---

## 8. Archivos clave del proyecto

| Archivo | Propósito |
|---|---|
| `01_tablespaces/01_tablespaces_tablas.sql` | 21 tablespaces + datafiles |
| `01_tablespaces/03_tablespace_temp.sql` | Tablespace temporal |
| `02_usuarios_y_permisos/01_usuario_bom_ventas.sql` | Usuario, cuotas y permisos |
| `03_tablas/*.sql` (11 archivos) | 21 tablas, índices y constraints |
| `04_funciones/*.sql` (6 archivos) | Funciones PL/SQL |
| `05_procedimientos/pr_viernes_resumen_cliente.sql` | Procedimiento de resumen |
| `06_vistas/*.sql` (5 archivos) | Vistas de negocio y administración |
| `07_triggers/trg_crm_usuarios_actualizado.sql` | Trigger de auditoría |
| `08_datos_semilla/seed_datos_demo.sql` | Datos demo |
| `init/001..006*.sql` | Scripts de auto-inicialización (Docker) |
| `ejecutar_todo.sql` | Script maestro (SQL Developer) |
| `PRUEBAS_BD.sql` | Script de verificación completo |

---

*Informe generado a partir de la revisión y verificación de la base de datos en Oracle 23c Free (contenedor `viernes-oracle`, estado `healthy`).*