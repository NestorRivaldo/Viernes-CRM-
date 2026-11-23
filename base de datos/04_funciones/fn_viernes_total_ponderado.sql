-- =============================================================================
-- FUNCIÓN: FN_VIERNES_TOTAL_PONDERADO
-- -----------------------------------------------------------------------------
-- TIPO       : Negocio (KPI comercial)
-- PROPÓSITO  : Suma el valor esperado (ponderado por probabilidad) de todo el
--              pipeline abierto. Es el "forecast" que se muestra en el
--              dashboard: no cuánto vale el pipeline, sino cuánto se espera
--              ganar realmente de él.
-- PARÁMETROS : p_etapa -> Etapa a filtrar (NULL = todas las abiertas).
-- RETORNA    : NUMBER con 2 decimales. 0 si no hay filas.
-- USO        : SELECT FN_VIERNES_TOTAL_PONDERADO()              FROM dual;
--              SELECT FN_VIERNES_TOTAL_PONDERADO('PROPUESTA')   FROM dual;
-- NOTAS      : Es la versión agregada de FN_VIERNES_VALOR_PONDERADO:
--              SUM(VALOR * PROBABILIDAD / 100) sobre las filas abiertas.
-- =============================================================================

CREATE OR REPLACE FUNCTION FN_VIERNES_TOTAL_PONDERADO (
    p_etapa IN VARCHAR2 DEFAULT NULL
) RETURN NUMBER IS
    v_total NUMBER;
BEGIN
    IF p_etapa IS NULL THEN
        SELECT NVL(SUM(ROUND(NVL(VALOR, 0)
                             * LEAST(GREATEST(NVL(PROBABILIDAD, 0), 0), 100)
                             / 100, 2)), 0)
          INTO v_total
          FROM CRM_OPORTUNIDADES
         WHERE ETAPA NOT IN ('GANADA', 'PERDIDA');
    ELSE
        SELECT NVL(SUM(ROUND(NVL(VALOR, 0)
                             * LEAST(GREATEST(NVL(PROBABILIDAD, 0), 0), 100)
                             / 100, 2)), 0)
          INTO v_total
          FROM CRM_OPORTUNIDADES
         WHERE ETAPA = UPPER(TRIM(p_etapa));
    END IF;

    RETURN ROUND(v_total, 2);
END FN_VIERNES_TOTAL_PONDERADO;
/
