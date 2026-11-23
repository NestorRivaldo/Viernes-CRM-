-- =============================================================================
-- PROCEDIMIENTO: PR_VIERNES_RESUMEN_CLIENTE
-- -----------------------------------------------------------------------------
-- PROPÓSITO  : Obtiene el resumen comercial de un cliente de una sola pasada:
--              número de oportunidades, propuestas emitidas y monto cotizado.
-- PARÁMETROS :
--   p_id_cliente       IN  -> ID del cliente (CRM_CLIENTES.ID)
--   p_oportunidades    OUT -> Total de oportunidades asociadas
--   p_propuestas       OUT -> Total de propuestas generadas
--   p_total_propuestas OUT -> Suma en dinero de las propuestas
-- USO        :
--   VARIABLE o NUMBER; VARIABLE p NUMBER; VARIABLE t NUMBER;
--   EXEC PR_VIERNES_RESUMEN_CLIENTE(1, :o, :p, :t);
--   PRINT o; PRINT p; PRINT t;
-- NOTAS      : Las oportunidades se asocian por nombre porque CRM_OPORTUNIDADES
--              guarda CLIENTE_NOMBRE como texto. La regla "empresa o nombre"
--              vive en FN_VIERNES_NOMBRE_CLIENTE para no duplicarla aquí.
--              Si el cliente no existe, los tres OUT quedan en 0.
-- =============================================================================

CREATE OR REPLACE PROCEDURE PR_VIERNES_RESUMEN_CLIENTE (
    p_id_cliente       IN  NUMBER,
    p_oportunidades    OUT NUMBER,
    p_propuestas       OUT NUMBER,
    p_total_propuestas OUT NUMBER
) IS
    v_clave VARCHAR2(150);
BEGIN
    -- Clave comercial del cliente: EMPRESA si es corporativo, NOMBRE si no.
    v_clave := FN_VIERNES_NOMBRE_CLIENTE(p_id_cliente);

    IF v_clave IS NULL THEN
        -- Cliente inexistente (o ID nulo): resumen vacío, sin excepción.
        p_oportunidades    := 0;
        p_propuestas       := 0;
        p_total_propuestas := 0;
        RETURN;
    END IF;

    SELECT COUNT(*)
      INTO p_oportunidades
      FROM CRM_OPORTUNIDADES
     WHERE CLIENTE_NOMBRE = v_clave;

    SELECT COUNT(*), NVL(SUM(TOTAL), 0)
      INTO p_propuestas, p_total_propuestas
      FROM CRM_PROPUESTAS
     WHERE ID_CLIENTE = p_id_cliente;
END PR_VIERNES_RESUMEN_CLIENTE;
/
