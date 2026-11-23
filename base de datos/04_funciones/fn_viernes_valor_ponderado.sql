-- =============================================================================
-- FUNCIÓN: FN_VIERNES_VALOR_PONDERADO
-- -----------------------------------------------------------------------------
-- TIPO       : Negocio (pronóstico de venta)
-- PROPÓSITO  : Calcula el valor esperado de UNA oportunidad según la
--              probabilidad de cierre:  VALOR * PROBABILIDAD / 100
-- PARÁMETROS : p_id_oportunidad -> ID de la oportunidad (CRM_OPORTUNIDADES.ID)
-- RETORNA    : NUMBER con 2 decimales. 0 si el ID es NULL o no existe.
-- USO        : SELECT FN_VIERNES_VALOR_PONDERADO(1) FROM dual;
--              SELECT NOMBRE, FN_VIERNES_VALOR_PONDERADO(ID) FROM CRM_OPORTUNIDADES;
-- NOTAS      : La probabilidad se acota a 0..100 con LEAST/GREATEST para que un
--              dato corrupto (ej. 150 %) no infle el pronóstico. El cálculo por
--              fila de la vista VW_VIERNES_PIPELINE se mantiene en SQL puro
--              (misma fórmula) para no pagar una llamada PL/SQL por fila;
--              esta función es para consultas de una sola oportunidad o para
--              el backend.
-- =============================================================================

CREATE OR REPLACE FUNCTION FN_VIERNES_VALOR_PONDERADO (
    p_id_oportunidad IN NUMBER
) RETURN NUMBER IS
    v_valor NUMBER;
BEGIN
    IF p_id_oportunidad IS NULL THEN
        RETURN 0;
    END IF;

    SELECT ROUND(
               NVL(VALOR, 0)
               * LEAST(GREATEST(NVL(PROBABILIDAD, 0), 0), 100)
               / 100,
           2)
      INTO v_valor
      FROM CRM_OPORTUNIDADES
     WHERE ID = p_id_oportunidad;

    RETURN NVL(v_valor, 0);
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN 0;
END FN_VIERNES_VALOR_PONDERADO;
/
