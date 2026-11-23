-- =============================================================================
-- TRIGGER: TRG_CRM_USUARIOS_ACTUALIZADO
-- Propósito: Auditoría automática. Actualiza el campo ACTUALIZADO_EN al modificar un usuario.
-- =============================================================================

CREATE OR REPLACE TRIGGER TRG_CRM_USUARIOS_ACTUALIZADO
BEFORE UPDATE ON CRM_USUARIOS
FOR EACH ROW
BEGIN
    :NEW.ACTUALIZADO_EN := SYSTIMESTAMP;
END;
/
