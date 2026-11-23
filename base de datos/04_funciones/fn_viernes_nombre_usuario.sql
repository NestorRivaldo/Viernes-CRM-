-- =============================================================================
-- FUNCIÓN: FN_VIERNES_NOMBRE_USUARIO
-- -----------------------------------------------------------------------------
-- TIPO       : Utilidad (formateo de datos)
-- PROPÓSITO  : Devuelve el nombre completo de un usuario ("Nombre Apellido")
--              a partir de su ID, listo para mostrar en reportes o vistas.
-- PARÁMETROS : p_id -> ID del usuario (CRM_USUARIOS.ID)
-- RETORNA    : VARCHAR2(220) con el nombre completo.
--              NULL si el ID es NULL o el usuario no existe.
-- USO        : SELECT FN_VIERNES_NOMBRE_USUARIO(1) FROM dual;
--              SELECT ID, FN_VIERNES_NOMBRE_USUARIO(ID) FROM CRM_USUARIOS;
-- NOTAS      : Centraliza el formato para que vistas, reportes y backend no
--              repitan "NOMBRE || ' ' || APELLIDO" (y no dupliquen espacios
--              cuando NOMBRE o APELLIDO vienen en NULL).
-- =============================================================================

CREATE OR REPLACE FUNCTION FN_VIERNES_NOMBRE_USUARIO (
    p_id IN NUMBER
) RETURN VARCHAR2 IS
    v_nombre VARCHAR2(220);
BEGIN
    IF p_id IS NULL THEN
        RETURN NULL;
    END IF;

    -- TRIM quita espacios sobrantes; NVL evita que un apellido NULL
    -- convierta todo el resultado en NULL.
    SELECT TRIM(NVL(NOMBRE, '') || ' ' || NVL(APELLIDO, ''))
      INTO v_nombre
      FROM CRM_USUARIOS
     WHERE ID = p_id;

    RETURN v_nombre;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
END FN_VIERNES_NOMBRE_USUARIO;
/
