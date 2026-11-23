# Base de Datos VIERNES CRM (Oracle 23c Free, Docker)

Esta carpeta independiza la base de datos de cualquier computadora física y contiene la arquitectura modular completa del sistema CRM Viernes V1.

---

## 📁 Estructura Modular del Proyecto

```
base de datos/
│
├── 01_tablespaces/                      # Almacenamiento y Datafiles
│   ├── 01_tablespaces_tablas.sql        # 21 tablespaces (uno por tabla) + su datafile
│   └── 03_tablespace_temp.sql           # Tablespace temporal VIERNES_TEMP + viernes_temp01.dbf
│
├── 02_usuarios_y_permisos/              # Seguridad y Cuotas
│   └── 01_usuario_bom_ventas.sql        # Usuario BOM_VENTAS, cuotas ilimitadas y permisos DBA
│
├── 03_tablas/                           # DDL de Tablas e Índices (21 tablas)
│   ├── 01_usuarios.sql
│   ├── 02_clientes_y_contactos.sql
│   ├── 03_fuentes_y_leads.sql
│   ├── 04_productos.sql
│   ├── 05_oportunidades.sql
│   ├── 06_propuestas.sql
│   ├── 07_actividades.sql
│   ├── 08_etiquetas.sql
│   ├── 09_equipos.sql
│   ├── 10_campanias.sql
│   └── 11_mensajeria.sql
│
├── 04_funciones/                          # Funciones PL/SQL de Negocio (6)
│   ├── fn_viernes_nombre_cliente.sql      # Empresa o nombre del cliente
│   ├── fn_viernes_nombre_usuario.sql      # Nombre completo del usuario
│   ├── fn_viernes_tasa_conversion.sql     # % de oportunidades ganadas
│   ├── fn_viernes_total_pipeline.sql      # Monto del pipeline abierto
│   ├── fn_viernes_total_ponderado.sql     # Pronóstico (valor esperado)
│   └── fn_viernes_valor_ponderado.sql     # Valor esperado de 1 oportunidad
│
├── 05_procedimientos/                   # Procedimientos Almacenados
│   └── pr_viernes_resumen_cliente.sql   # Reporte comercial de un cliente
│
├── 06_vistas/                           # Vistas de Negocio y Administración
│   ├── vw_viernes_pipeline.sql          # Vista con orden de etapas y montos ponderados
│   ├── vw_viernes_agenda.sql            # Agenda consolidada de actividades
│   ├── vw_viernes_tablespaces.sql       # Monitoreo de Tablespaces desde SQL Developer
│   ├── vw_viernes_datafiles.sql         # Monitoreo de Datafiles y Autoextend
│   └── vw_viernes_objetos.sql           # Inventario general de objetos del esquema
│
├── 07_triggers/                         # Automatización y Auditoría
│   └── trg_crm_usuarios_actualizado.sql # Actualización automática de timestamps
│
├── 08_datos_semilla/                    # Datos Demo Iniciales
│   └── seed_datos_demo.sql              # Inserción idempotente de usuarios, clientes, etc.
│
├── init/                                # Directorio de auto-inicialización para Docker
│   ├── 001_tablespaces.sql
│   ├── 002_usuario.sql
│   ├── 003_esquema.sql
│   ├── 004_plsql.sql
│   ├── 005_seed.sql
│   └── 006_verificacion.sql
│
├── backups/                             # Directorio para dumps (.dmp) de Oracle Data Pump
├── compose-dev.yml                      # Definición Docker Compose para Oracle 23c Slim
├── ejecutar_todo.sql                    # Script maestro para correr todo en SQL Developer
├── iniciar-bd.bat                       # Doble-clic para levantar la base de datos
├── reiniciar-bd-limpia.bat              # Doble-clic para recrear la base desde cero
├── export.ps1                           # Exportar backup completo (.dmp)
└── import.ps1                           # Importar backup (.dmp) en otra máquina
```

---

## 🚀 Cómo levantar la Base de Datos

### Opción A (Recomendada con doble-clic):
Ejecuta el archivo:
`iniciar-bd.bat`

### Opción B (Desde terminal):
```powershell
cd "E:\Viernes\Viernes\base de datos"
docker compose -f compose-dev.yml up -d
```
Para ver el progreso de arranque:
```powershell
docker logs -f viernes-oracle
```
*(Espera hasta ver el mensaje `DATABASE IS READY TO USE!`)*.

---

## 💻 Conexión en Oracle SQL Developer

| Parámetro | Valor |
|---|---|
| **Tipo de conexión** | Oracle (JDBC Thin) |
| **Hostname** | `localhost` |
| **Puerto** | `1521` |
| **Service Name** | `FREEPDB1` |
| **Usuario** | `BOM_VENTAS` |
| **Contraseña** | `123456` |

---

## 📊 Consultas clave para la presentación

```sql
-- 1. Demostrar Tablespaces configurados
SELECT * FROM VW_VIERNES_TABLESPACES;

-- 2. Demostrar Datafiles con Autoextend activo
SELECT * FROM VW_VIERNES_DATAFILES;

-- 3. Probar Función PL/SQL: Total del pipeline (solo oportunidades abiertas)
SELECT FN_VIERNES_TOTAL_PIPELINE() AS PIPELINE_ABIERTO FROM dual;

-- 4. Probar Función PL/SQL: Pipeline filtrado por etapa
SELECT FN_VIERNES_TOTAL_PIPELINE('NEGOCIACION') AS PIPELINE_NEGOCIACION FROM dual;

-- 5. Probar Función PL/SQL: Pronóstico ponderado por probabilidad
SELECT FN_VIERNES_TOTAL_PONDERADO() AS PRONOSTICO FROM dual;

-- 6. Probar Función PL/SQL: Tasa de conversión
SELECT FN_VIERNES_TASA_CONVERSION() AS TASA_CONVERSION FROM dual;

-- 7. Probar Función PL/SQL: Nombre formateado
SELECT FN_VIERNES_NOMBRE_USUARIO(1) AS USUARIO_ADMIN FROM dual;

-- 8. Demostrar Vista de Negocio con cálculo ponderado
SELECT * FROM VW_VIERNES_PIPELINE;

-- 9. Inventario completo de objetos creados
SELECT * FROM VW_VIERNES_OBJETOS;
```

---

## 🧮 Funciones PL/SQL (6)

Todas son de solo lectura: calculan, no modifican datos. Se dividen en **utilidades de identidad** (2) y **KPIs comerciales** (4).

| # | Función | Qué hace | Ejemplo |
|---|---|---|---|
| 1 | `FN_VIERNES_NOMBRE_USUARIO(p_id)` | Devuelve `"Nombre Apellido"` de `CRM_USUARIOS`. | `FN_VIERNES_NOMBRE_USUARIO(1)` → `Admin Viernes` |
| 2 | `FN_VIERNES_NOMBRE_CLIENTE(p_id)` | Devuelve `EMPRESA` si el cliente es corporativo, si no `NOMBRE`. Es la regla única para comparar con `CRM_OPORTUNIDADES.CLIENTE_NOMBRE`. | `FN_VIERNES_NOMBRE_CLIENTE(1)` → `Acme S.A.` |
| 3 | `FN_VIERNES_VALOR_PONDERADO(p_id_oportunidad)` | Valor esperado de **una** oportunidad: `VALOR * PROBABILIDAD / 100` (probabilidad acotada a 0–100). | `FN_VIERNES_VALOR_PONDERADO(1)` → `8400` |
| 4 | `FN_VIERNES_TOTAL_PIPELINE(p_etapa, p_solo_abiertas)` | Monto del pipeline. Por defecto **excluye GANADA/PERDIDA**; con `'N'` suma el histórico completo. Acepta la etapa en minúsculas. | `FN_VIERNES_TOTAL_PIPELINE('negociacion')` → `12000` |
| 5 | `FN_VIERNES_TOTAL_PONDERADO(p_etapa)` | Pronóstico: suma del valor esperado del pipeline abierto. | `FN_VIERNES_TOTAL_PONDERADO()` → `12150` |
| 6 | `FN_VIERNES_TASA_CONVERSION(p_responsable)` | % de oportunidades ganadas sobre las cerradas (ganadas + perdidas). | `FN_VIERNES_TASA_CONVERSION()` → `0` (aún no hay cierres) |

**Por qué solo estas y no más:** el CRUD y las reglas transaccionales viven en el backend Java (Spring Boot); duplicarlas en PL/SQL crearía dos fuentes de verdad. Las funciones de Oracle cubren lo que el backend no hace hoy: **agregados de reporting** y **reglas de identidad** reutilizadas por vistas, procedimientos y consultas del dashboard.

```sql
-- Ejemplos de uso
SELECT FN_VIERNES_TOTAL_PIPELINE()                    AS ABIERTO      FROM dual;
SELECT FN_VIERNES_TOTAL_PIPELINE(NULL, 'N')           AS HISTORICO    FROM dual;
SELECT FN_VIERNES_TOTAL_PIPELINE('negociacion')       AS NEGOCIACION  FROM dual;
SELECT FN_VIERNES_TOTAL_PONDERADO()                   AS PRONOSTICO   FROM dual;
SELECT FN_VIERNES_TASA_CONVERSION()                   AS TASA         FROM dual;
SELECT FN_VIERNES_NOMBRE_CLIENTE(1)                   AS CLIENTE      FROM dual;
SELECT FN_VIERNES_NOMBRE_USUARIO(1)                   AS USUARIO      FROM dual;
SELECT ID, FN_VIERNES_VALOR_PONDERADO(ID) AS ESPERADO FROM CRM_OPORTUNIDADES;
```
