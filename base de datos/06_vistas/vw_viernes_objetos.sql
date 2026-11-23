-- =============================================================================
-- VISTA DE CONTROL: VW_VIERNES_OBJETOS
-- Propósito: Inventario de todos los objetos creados bajo el esquema BOM_VENTAS.
-- =============================================================================

CREATE OR REPLACE VIEW VW_VIERNES_OBJETOS AS
SELECT OBJECT_TYPE,
       OBJECT_NAME,
       STATUS,
       CREATED,
       LAST_DDL_TIME
FROM USER_OBJECTS
WHERE OBJECT_TYPE IN ('TABLE', 'VIEW', 'FUNCTION', 'PROCEDURE', 'TRIGGER', 'INDEX', 'SEQUENCE')
ORDER BY OBJECT_TYPE, OBJECT_NAME;
