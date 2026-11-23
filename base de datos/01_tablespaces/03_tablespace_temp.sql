-- =============================================================================
-- TABLESPACE TEMPORAL: VIERNES_TEMP
-- Propósito: Operaciones de ordenamiento (ORDER BY, GROUP BY) y hash joins.
-- Datafile: /opt/oracle/oradata/FREE/FREEPDB1/viernes_temp01.dbf
-- Tamaño inicial: 64 MB | Autoextensible: 32 MB hasta 1 GB
-- =============================================================================

ALTER SESSION SET CONTAINER = FREEPDB1;

CREATE TEMPORARY TABLESPACE VIERNES_TEMP
    TEMPFILE '/opt/oracle/oradata/FREE/FREEPDB1/viernes_temp01.dbf'
    SIZE 64M
    AUTOEXTEND ON NEXT 32M MAXSIZE 1G;

PROMPT [OK] Tablespace Temporal VIERNES_TEMP creado.
