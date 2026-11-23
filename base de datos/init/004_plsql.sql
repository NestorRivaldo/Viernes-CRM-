WHENEVER SQLERROR EXIT SQL.SQLCODE

ALTER SESSION SET CONTAINER = FREEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = BOM_VENTAS;

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

-- =============================================================================
-- VISTA DE ALMACENAMIENTO: VW_VIERNES_DATAFILES
-- Propósito: Consulta los archivos físicos (datafiles/tempfiles), su tamaño y autoextend.
-- =============================================================================

CREATE OR REPLACE VIEW VW_VIERNES_DATAFILES AS
SELECT 'DATA' AS TIPO,
       TABLESPACE_NAME,
       FILE_NAME,
       ROUND(BYTES / 1024 / 1024, 2)     AS TAMANO_MB,
       AUTOEXTENSIBLE,
       ROUND(MAXBYTES / 1024 / 1024, 2)  AS MAXIMO_MB
FROM DBA_DATA_FILES
WHERE TABLESPACE_NAME LIKE 'VIERNES_%'
UNION ALL
SELECT 'TEMP' AS TIPO,
       TABLESPACE_NAME,
       FILE_NAME,
       ROUND(BYTES / 1024 / 1024, 2),
       AUTOEXTENSIBLE,
       ROUND(MAXBYTES / 1024 / 1024, 2)
FROM DBA_TEMP_FILES
WHERE TABLESPACE_NAME LIKE 'VIERNES_%';

-- =============================================================================
-- VISTA DE CONTROL: VW_VIERNES_OBJETOS
-- Propósito: Inventario de todos los objetos creados bajo el esquema BOM_VENTAS.
-- =============================================================================

CREATE OR REPLACE VIEW VW_VIERNES_OBJETOS AS
SELECT OBJECT_TYPE,
       OBJECT_NAME,
       STATUS,
       CREATED,
       LAST_DDL_TIME
FROM USER_OBJECTS
WHERE OBJECT_TYPE IN ('TABLE', 'VIEW', 'FUNCTION', 'PROCEDURE', 'TRIGGER', 'INDEX', 'SEQUENCE')
ORDER BY OBJECT_TYPE, OBJECT_NAME;

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

-- =============================================================================
-- VISTA DE ALMACENAMIENTO: VW_VIERNES_TABLESPACES
-- Propósito: Monitoreo de los tablespaces del proyecto desde SQL Developer.
-- =============================================================================

CREATE OR REPLACE VIEW VW_VIERNES_TABLESPACES AS
SELECT TABLESPACE_NAME,
       BLOCK_SIZE,
       STATUS,
       CONTENTS,
       LOGGING,
       EXTENT_MANAGEMENT,
       ALLOCATION_TYPE,
       SEGMENT_SPACE_MANAGEMENT
FROM DBA_TABLESPACES
WHERE TABLESPACE_NAME LIKE 'VIERNES_%';

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

PROMPT [OK] 6 funciones, 1 procedimiento, 5 vistas y 1 trigger creados.

