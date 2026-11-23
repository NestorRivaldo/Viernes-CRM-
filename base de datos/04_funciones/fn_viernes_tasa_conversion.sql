-- =============================================================================
-- FUNCIÓN: FN_VIERNES_TASA_CONVERSION
-- -----------------------------------------------------------------------------
-- TIPO       : Negocio (KPI comercial)
-- PROPÓSITO  : Calcula el porcentaje de oportunidades GANADAS sobre el total
--              de oportunidades ya cerradas (GANADAS + PERDIDAS).
--              Responde: "de cada 100 negocios que cerramos, ¿cuántos ganamos?"
-- PARÁMETROS : p_responsable -> Corresponsal comercial a filtrar
--              (CRM_OPORTUNIDADES.RESPONSABLE). NULL = toda la empresa.
-- RETORNA    : NUMBER con 2 decimales (0 a 100). 0 si aún no hay cierres.
-- USO        : SELECT FN_VIERNES_TASA_CONVERSION()                   FROM dual;
--              SELECT FN_VIERNES_TASA_CONVERSION('jefe@viernes.com') FROM dual;
-- NOTAS      : Las oportunidades abiertas NO entran al denominador: todavía
--              no se cerraron, así que no cuentan ni a favor ni en contra.
-- =============================================================================

CREATE OR REPLACE FUNCTION FN_VIERNES_TASA_CONVERSION (
    p_responsable IN VARCHAR2 DEFAULT NULL
) RETURN NUMBER IS
    v_ganadas  NUMBER := 0;
    v_perdidas NUMBER := 0;
BEGIN
    IF p_responsable IS NULL THEN
        SELECT COUNT(CASE WHEN ETAPA = 'GANADA'  THEN 1 END),
               COUNT(CASE WHEN ETAPA = 'PERDIDA' THEN 1 END)
          INTO v_ganadas, v_perdidas
          FROM CRM_OPORTUNIDADES;
    ELSE
        SELECT COUNT(CASE WHEN ETAPA = 'GANADA'  THEN 1 END),
               COUNT(CASE WHEN ETAPA = 'PERDIDA' THEN 1 END)
          INTO v_ganadas, v_perdidas
          FROM CRM_OPORTUNIDADES
         WHERE RESPONSABLE = TRIM(p_responsable);
    END IF;

    IF v_ganadas + v_perdidas = 0 THEN
        RETURN 0;
    END IF;

    RETURN ROUND(v_ganadas * 100 / (v_ganadas + v_perdidas), 2);
END FN_VIERNES_TASA_CONVERSION;
/
