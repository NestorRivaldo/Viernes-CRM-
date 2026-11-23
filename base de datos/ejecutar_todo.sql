-- =============================================================================
-- SCRIPT MAESTRO: EJECUTAR TODO
-- Ejecuta todos los scripts modulares en orden estricto de dependencias.
-- Puede ejecutarse en Oracle SQL Developer o SQL*Plus conectado como SYS o SYSTEM.
-- =============================================================================

WHENEVER SQLERROR EXIT SQL.SQLCODE

PROMPT =========================================================================
PROMPT [1/8] CREANDO TABLESPACES Y DATAFILES EN FREEPDB1
PROMPT =========================================================================
@@01_tablespaces/01_tablespaces_tablas.sql;
@@01_tablespaces/03_tablespace_temp.sql;

PROMPT =========================================================================
PROMPT [2/8] CONFIGURANDO USUARIO BOM_VENTAS, CUOTAS Y PERMISOS
PROMPT =========================================================================
@@02_usuarios_y_permisos/01_usuario_bom_ventas.sql;

ALTER SESSION SET CONTAINER = FREEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = BOM_VENTAS;

PROMPT =========================================================================
PROMPT [3/8] CREANDO TABLAS E ÍNDICES (21 TABLAS)
PROMPT =========================================================================
@@03_tablas/01_usuarios.sql;
@@03_tablas/02_clientes_y_contactos.sql;
@@03_tablas/03_fuentes_y_leads.sql;
@@03_tablas/04_productos.sql;
@@03_tablas/05_oportunidades.sql;
@@03_tablas/06_propuestas.sql;
@@03_tablas/07_actividades.sql;
@@03_tablas/08_etiquetas.sql;
@@03_tablas/09_equipos.sql;
@@03_tablas/10_campanias.sql;
@@03_tablas/11_mensajeria.sql;

PROMPT =========================================================================
PROMPT [4/8] CREANDO FUNCIONES PL/SQL
PROMPT =========================================================================
@@04_funciones/fn_viernes_nombre_cliente.sql;
@@04_funciones/fn_viernes_nombre_usuario.sql;
@@04_funciones/fn_viernes_tasa_conversion.sql;
@@04_funciones/fn_viernes_total_pipeline.sql;
@@04_funciones/fn_viernes_total_ponderado.sql;
@@04_funciones/fn_viernes_valor_ponderado.sql;

PROMPT =========================================================================
PROMPT [5/8] CREANDO PROCEDIMIENTOS PL/SQL
PROMPT =========================================================================
@@05_procedimientos/pr_viernes_resumen_cliente.sql;

PROMPT =========================================================================
PROMPT [6/8] CREANDO VISTAS
PROMPT =========================================================================
@@06_vistas/vw_viernes_pipeline.sql;
@@06_vistas/vw_viernes_agenda.sql;
@@06_vistas/vw_viernes_tablespaces.sql;
@@06_vistas/vw_viernes_datafiles.sql;
@@06_vistas/vw_viernes_objetos.sql;

PROMPT =========================================================================
PROMPT [7/8] CREANDO TRIGGERS
PROMPT =========================================================================
@@07_triggers/trg_crm_usuarios_actualizado.sql;

PROMPT =========================================================================
PROMPT [8/8] INSERTANDO DATOS SEMILLA (DEMO)
PROMPT =========================================================================
@@08_datos_semilla/seed_datos_demo.sql;

PROMPT =========================================================================
PROMPT [OK] BASE DE DATOS VIERNES CRM CONFIGURADA EXITOSAMENTE.
PROMPT =========================================================================
