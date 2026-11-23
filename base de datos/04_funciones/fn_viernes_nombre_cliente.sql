-- =============================================================================
-- FUNCIÓN: FN_VIERNES_NOMBRE_CLIENTE
-- -----------------------------------------------------------------------------
-- TIPO       : Utilidad (identidad del cliente)
-- PROPÓSITO  : Devuelve el nombre con el que se debe mostrar / comparar un
--              cliente: EMPRESA si existe (cliente corporativo), o NOMBRE si
--              es persona natural.
-- PARÁMETROS : p_id -> ID del cliente (CRM_CLIENTES.ID)
-- RETORNA    : VARCHAR2(150). NULL si el ID es NULL o el cliente no existe.
-- USO        : SELECT FN_VIERNES_NOMBRE_CLIENTE(1) FROM dual;
-- NOTAS      : CRM_OPORTUNIDADES guarda el nombre del cliente como texto
--              (CLIENTE_NOMBRE). Esa clave debe construirse igual en todos
--              lados; esta función es la única fuente de esa regla.
--              PR_VIERNES_RESUMEN_CLIENTE la reutiliza.
-- =============================================================================

CREATE OR REPLACE FUNCTION FN_VIERNES_NOMBRE_CLIENTE (
    p_id IN NUMBER
) RETURN VARCHAR2 IS
    v_nombre VARCHAR2(150);
BEGIN
    IF p_id IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT NVL(EMPRESA, NOMBRE)
      INTO v_nombre
      FROM CRM_CLIENTES
     WHERE ID = p_id;

    RETURN v_nombre;
EXCEPTION
    WHEN NO_DATA_FOUND THEN
        RETURN NULL;
END FN_VIERNES_NOMBRE_CLIENTE;
/
