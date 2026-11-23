-- =============================================================================
-- VISTA DE ALMACENAMIENTO: VW_VIERNES_DATAFILES
-- Propósito: Consulta los archivos físicos (datafiles/tempfiles), su tamaño y autoextend.
-- =============================================================================

CREATE OR REPLACE VIEW VW_VIERNES_DATAFILES AS
SELECT 'DATA' AS TIPO,
       TABLESPACE_NAME,
       FILE_NAME,
       ROUND(BYTES / 1024 / 1024, 2)     AS TAMANO_MB,
       AUTOEXTENSIBLE,
       ROUND(MAXBYTES / 1024 / 1024, 2)  AS MAXIMO_MB
FROM DBA_DATA_FILES
WHERE TABLESPACE_NAME LIKE 'VIERNES_%'
UNION ALL
SELECT 'TEMP' AS TIPO,
       TABLESPACE_NAME,
       FILE_NAME,
       ROUND(BYTES / 1024 / 1024, 2),
       AUTOEXTENSIBLE,
       ROUND(MAXBYTES / 1024 / 1024, 2)
FROM DBA_TEMP_FILES
WHERE TABLESPACE_NAME LIKE 'VIERNES_%';
