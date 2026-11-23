-- =============================================================================
-- FUNCIÓN: FN_VIERNES_TOTAL_PIPELINE
-- -----------------------------------------------------------------------------
-- TIPO       : Negocio (KPI comercial)
-- PROPÓSITO  : Suma el valor monetario del pipeline de oportunidades.
-- PARÁMETROS :
--   p_etapa          -> Etapa a filtrar ('PROSPECCION', 'NEGOCIACION', ...).
--                       NULL = todas.
--   p_solo_abiertas  -> 'S' (default) cuenta solo oportunidades abiertas
--                       (excluye GANADA y PERDIDA, que ya no son pipeline).
--                       'N' cuenta absolutamente todas.
-- RETORNA    : NUMBER. 0 si no hay filas que cumplan el filtro.
-- USO        :
--   SELECT FN_VIERNES_TOTAL_PIPELINE()                  FROM dual; -- abierto
--   SELECT FN_VIERNES_TOTAL_PIPELINE('NEGOCIACION')     FROM dual; -- etapa
--   SELECT FN_VIERNES_TOTAL_PIPELINE(NULL, 'N')         FROM dual; -- todo
-- NOTAS      : Se usan ramas separadas en lugar de
--              "WHERE p_etapa IS NULL OR ETAPA = p_etapa" porque el OR impide
--              que Oracle use el índice IX_CRM_OPORTUNIDADES_ETAPA.
--              La etapa se normaliza a mayúsculas para que
--              FN_VIERNES_TOTAL_PIPELINE('negociacion') también funcione.
-- =============================================================================

CREATE OR REPLACE FUNCTION FN_VIERNES_TOTAL_PIPELINE (
    p_etapa         IN VARCHAR2 DEFAULT NULL,
    p_solo_abiertas IN CHAR     DEFAULT 'S'
) RETURN NUMBER IS
    v_total NUMBER;
BEGIN
    IF p_etapa IS NULL THEN
        IF UPPER(NVL(TRIM(p_solo_abiertas), 'S')) = 'S' THEN
            -- Rama 1: todo el pipeline abierto
            SELECT NVL(SUM(VALOR), 0)
              INTO v_total
              FROM CRM_OPORTUNIDADES
             WHERE ETAPA NOT IN ('GANADA', 'PERDIDA');
        ELSE
            -- Rama 2: histórico completo
            SELECT NVL(SUM(VALOR), 0)
              INTO v_total
              FROM CRM_OPORTUNIDADES;
        END IF;
    ELSE
        -- Rama 3: una etapa concreta (usa IX_CRM_OPORTUNIDADES_ETAPA)
        SELECT NVL(SUM(VALOR), 0)
          INTO v_total
          FROM CRM_OPORTUNIDADES
         WHERE ETAPA = UPPER(TRIM(p_etapa));
    END IF;

    RETURN v_total;
END FN_VIERNES_TOTAL_PIPELINE;
/
