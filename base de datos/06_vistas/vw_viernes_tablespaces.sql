-- =============================================================================
-- VISTA DE ALMACENAMIENTO: VW_VIERNES_TABLESPACES
-- Propósito: Monitoreo de los tablespaces del proyecto desde SQL Developer.
-- =============================================================================

CREATE OR REPLACE VIEW VW_VIERNES_TABLESPACES AS
SELECT TABLESPACE_NAME,
       BLOCK_SIZE,
       STATUS,
       CONTENTS,
       LOGGING,
       EXTENT_MANAGEMENT,
       ALLOCATION_TYPE,
       SEGMENT_SPACE_MANAGEMENT
FROM DBA_TABLESPACES
WHERE TABLESPACE_NAME LIKE 'VIERNES_%';
