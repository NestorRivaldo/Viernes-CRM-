-- =============================================================================
-- VISTA: VW_VIERNES_PIPELINE
-- Propósito: Visualiza el pipeline de oportunidades con orden de etapas y valor ponderado.
-- =============================================================================

CREATE OR REPLACE VIEW VW_VIERNES_PIPELINE AS
SELECT o.ID,
       o.NOMBRE,
       o.CLIENTE_NOMBRE,
       o.VALOR,
       o.PROBABILIDAD,
       o.ETAPA,
       o.RESPONSABLE,
       o.FECHA_CIERRE,
       CASE o.ETAPA
           WHEN 'PROSPECCION'  THEN 1
           WHEN 'CONTACTO'     THEN 2
           WHEN 'CALIFICACION' THEN 3
           WHEN 'PROPUESTA'    THEN 4
           WHEN 'NEGOCIACION'  THEN 5
           WHEN 'GANADA'       THEN 6
           WHEN 'PERDIDA'      THEN 7
       END AS ORDEN_ETAPA,
       ROUND(NVL(o.VALOR, 0) * NVL(o.PROBABILIDAD, 0) / 100, 2) AS VALOR_PONDERADO
FROM CRM_OPORTUNIDADES o;
