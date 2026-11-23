-- =============================================================================
-- VISTA: VW_VIERNES_AGENDA
-- Propósito: Agenda consolidada de actividades uniendo cliente, lead y oportunidad.
-- =============================================================================

CREATE OR REPLACE VIEW VW_VIERNES_AGENDA AS
SELECT a.ID_ACTIVIDAD,
       a.TITULO,
       a.TIPO,
       a.ESTADO,
       a.FECHA,
       c.NOMBRE AS CLIENTE,
       l.NOMBRE AS LEAD,
       o.NOMBRE AS OPORTUNIDAD
FROM CRM_ACTIVIDADES a
LEFT JOIN CRM_CLIENTES c      ON c.ID = a.ID_CLIENTE
LEFT JOIN CRM_LEADS l         ON l.ID = a.ID_LEAD
LEFT JOIN CRM_OPORTUNIDADES o ON o.ID = a.ID_OPORTUNIDAD;
