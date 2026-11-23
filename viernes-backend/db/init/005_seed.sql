-- =============================================================================
-- VIERNES CRM - 005: DATOS DEMO (seed)
-- -----------------------------------------------------------------------------
-- Se ejecuta una sola vez en la primera inicialización del contenedor.
-- Idempotente: cada bloque verifica existencia por clave natural antes de
-- insertar, para que re-ejecutar el script no genere duplicados.
-- =============================================================================

WHENEVER SQLERROR EXIT SQL.SQLCODE

ALTER SESSION SET CONTAINER = FREEPDB1;
ALTER SESSION SET CURRENT_SCHEMA = BOM_VENTAS;

-- -----------------------------------------------------------------------------
-- Usuarios (bcrypt, mismas credenciales que el seed original)
-- -----------------------------------------------------------------------------
MERGE INTO CRM_USUARIOS u
USING (SELECT 'admin@viernes.com' AS CORREO FROM dual) s
ON (u.CORREO = s.CORREO)
WHEN NOT MATCHED THEN
  INSERT (NOMBRE, APELLIDO, CORREO, PASSWORD_HASH, ROL, ESTADO, CREADO_EN, ACTUALIZADO_EN)
  VALUES ('Admin', 'Viernes', 'admin@viernes.com',
          '$2a$12$EUN9TjMLLm6Bxy05shdOkO06tr0xp3AeG.GXaY1k1JrQ1T82HAqAS',
          'ADMIN', 'ACTIVO', SYSTIMESTAMP, SYSTIMESTAMP);

MERGE INTO CRM_USUARIOS u
USING (SELECT 'jefe@viernes.com' AS CORREO FROM dual) s
ON (u.CORREO = s.CORREO)
WHEN NOT MATCHED THEN
  INSERT (NOMBRE, APELLIDO, CORREO, PASSWORD_HASH, ROL, ESTADO, CREADO_EN, ACTUALIZADO_EN)
  VALUES ('Jefe', 'Comercial', 'jefe@viernes.com',
          '$2a$12$QwYhMKcM.IOuKiLxj6WZkuW0Q5c9GAlmulVJ8ZjaBzgFrOd/NemZO',
          'JEFE', 'ACTIVO', SYSTIMESTAMP, SYSTIMESTAMP);

MERGE INTO CRM_USUARIOS u
USING (SELECT 'vendedor@viernes.com' AS CORREO FROM dual) s
ON (u.CORREO = s.CORREO)
WHEN NOT MATCHED THEN
  INSERT (NOMBRE, APELLIDO, CORREO, PASSWORD_HASH, ROL, ESTADO, CREADO_EN, ACTUALIZADO_EN)
  VALUES ('Vendedor', 'Prueba', 'vendedor@viernes.com',
          '$2a$12$QwYhMKcM.IOuKiLxj6WZkuW0Q5c9GAlmulVJ8ZjaBzgFrOd/NemZO',
          'VENDEDOR', 'ACTIVO', SYSTIMESTAMP, SYSTIMESTAMP);

-- -----------------------------------------------------------------------------
-- Fuentes
-- -----------------------------------------------------------------------------
INSERT INTO CRM_FUENTES (CODIGO, NOMBRE, DESCRIPCION, ACTIVA, CREADO_EN)
SELECT 'WEB', 'Sitio web', 'Leads captados desde el sitio institucional', 1, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_FUENTES WHERE CODIGO = 'WEB');

INSERT INTO CRM_FUENTES (CODIGO, NOMBRE, DESCRIPCION, ACTIVA, CREADO_EN)
SELECT 'REFERIDO', 'Referido', 'Recomendación de cliente existente', 1, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_FUENTES WHERE CODIGO = 'REFERIDO');

INSERT INTO CRM_FUENTES (CODIGO, NOMBRE, DESCRIPCION, ACTIVA, CREADO_EN)
SELECT 'FERIA', 'Feria comercial', 'Contacto en feria o evento sectorial', 1, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_FUENTES WHERE CODIGO = 'FERIA');

INSERT INTO CRM_FUENTES (CODIGO, NOMBRE, DESCRIPCION, ACTIVA, CREADO_EN)
SELECT 'CAMPANA', 'Campaña digital', 'Campaña de marketing digital', 1, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_FUENTES WHERE CODIGO = 'CAMPANA');

-- -----------------------------------------------------------------------------
-- Etiquetas
-- -----------------------------------------------------------------------------
INSERT INTO CRM_ETIQUETAS (NOMBRE, NOMBRE_NORMALIZADO, COLOR, DESCRIPCION, ACTIVA, CREADO_EN)
SELECT 'VIP', 'vip', '#F59E0B', 'Cliente de alto valor', 1, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_ETIQUETAS WHERE NOMBRE_NORMALIZADO = 'vip');

INSERT INTO CRM_ETIQUETAS (NOMBRE, NOMBRE_NORMALIZADO, COLOR, DESCRIPCION, ACTIVA, CREADO_EN)
SELECT 'Caliente', 'caliente', '#EF4444', 'Oportunidad con alta probabilidad', 1, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_ETIQUETAS WHERE NOMBRE_NORMALIZADO = 'caliente');

INSERT INTO CRM_ETIQUETAS (NOMBRE, NOMBRE_NORMALIZADO, COLOR, DESCRIPCION, ACTIVA, CREADO_EN)
SELECT 'Seguimiento', 'seguimiento', '#3B82F6', 'Requiere seguimiento cercano', 1, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_ETIQUETAS WHERE NOMBRE_NORMALIZADO = 'seguimiento');

INSERT INTO CRM_ETIQUETAS (NOMBRE, NOMBRE_NORMALIZADO, COLOR, DESCRIPCION, ACTIVA, CREADO_EN)
SELECT 'Recurrente', 'recurrente', '#10B981', 'Cliente con compras recurrentes', 1, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_ETIQUETAS WHERE NOMBRE_NORMALIZADO = 'recurrente');

-- -----------------------------------------------------------------------------
-- Productos
-- -----------------------------------------------------------------------------
INSERT INTO CRM_PRODUCTOS (CODIGO, NOMBRE, DESCRIPCION, CATEGORIA, PRECIO_BASE, ESTADO, CREADO_EN)
SELECT 'SRV-CONS', 'Consultoría CRM', 'Servicio de consultoría y parametrización del CRM', 'Servicios', 3500.00, 'ACTIVO', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_PRODUCTOS WHERE CODIGO = 'SRV-CONS');

INSERT INTO CRM_PRODUCTOS (CODIGO, NOMBRE, DESCRIPCION, CATEGORIA, PRECIO_BASE, ESTADO, CREADO_EN)
SELECT 'SRV-IMPL', 'Implementación', 'Implementación llave en mano del CRM', 'Servicios', 8000.00, 'ACTIVO', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_PRODUCTOS WHERE CODIGO = 'SRV-IMPL');

INSERT INTO CRM_PRODUCTOS (CODIGO, NOMBRE, DESCRIPCION, CATEGORIA, PRECIO_BASE, ESTADO, CREADO_EN)
SELECT 'LIC-PRO', 'Licencia Pro', 'Licencia anual plan Pro', 'Licencias', 1200.00, 'ACTIVO', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_PRODUCTOS WHERE CODIGO = 'LIC-PRO');

INSERT INTO CRM_PRODUCTOS (CODIGO, NOMBRE, DESCRIPCION, CATEGORIA, PRECIO_BASE, ESTADO, CREADO_EN)
SELECT 'LIC-ENT', 'Licencia Enterprise', 'Licencia anual plan Enterprise', 'Licencias', 4500.00, 'ACTIVO', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_PRODUCTOS WHERE CODIGO = 'LIC-ENT');

INSERT INTO CRM_PRODUCTOS (CODIGO, NOMBRE, DESCRIPCION, CATEGORIA, PRECIO_BASE, ESTADO, CREADO_EN)
SELECT 'SRV-SOP', 'Soporte premium', 'Soporte y SLA 24x7', 'Servicios', 1800.00, 'ACTIVO', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_PRODUCTOS WHERE CODIGO = 'SRV-SOP');

-- -----------------------------------------------------------------------------
-- Clientes y contactos
-- -----------------------------------------------------------------------------
INSERT INTO CRM_CLIENTES (NOMBRE, EMPRESA, TIPO_DOCUMENTO, NUMERO_DOCUMENTO, CORREO, TELEFONO, ESTADO, RESPONSABLE, CREADO_EN)
SELECT 'María', 'Acme S.A.', 'RUC', '20123456789', 'maria@acme.com', '999111222', 'ACTIVO', 'jefe@viernes.com', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_CLIENTES WHERE NUMERO_DOCUMENTO = '20123456789');

INSERT INTO CRM_CLIENTES (NOMBRE, EMPRESA, TIPO_DOCUMENTO, NUMERO_DOCUMENTO, CORREO, TELEFONO, ESTADO, RESPONSABLE, CREADO_EN)
SELECT 'Carlos', 'TechSoft', 'RUC', '20987654321', 'carlos@techsoft.com', '999333444', 'ACTIVO', 'vendedor@viernes.com', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_CLIENTES WHERE NUMERO_DOCUMENTO = '20987654321');

INSERT INTO CRM_CLIENTES (NOMBRE, EMPRESA, TIPO_DOCUMENTO, NUMERO_DOCUMENTO, CORREO, TELEFONO, ESTADO, RESPONSABLE, CREADO_EN)
SELECT 'Lucía', 'Innova SAC', 'DNI', '45678912', 'lucia@innova.com', '999555666', 'ACTIVO', 'vendedor@viernes.com', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_CLIENTES WHERE NUMERO_DOCUMENTO = '45678912');

INSERT INTO CRM_CONTACTOS (ID_CLIENTE, NOMBRE, APELLIDO, CARGO, CORREO, TELEFONO, PRINCIPAL, CREADO_EN)
SELECT c.ID, 'Juan', 'Pérez', 'Gerente General', 'juan@acme.com', '999111223', 1, SYSTIMESTAMP
FROM CRM_CLIENTES c
WHERE c.NUMERO_DOCUMENTO = '20123456789'
  AND NOT EXISTS (SELECT 1 FROM CRM_CONTACTOS ct WHERE ct.ID_CLIENTE = c.ID AND ct.CORREO = 'juan@acme.com');

INSERT INTO CRM_CONTACTOS (ID_CLIENTE, NOMBRE, APELLIDO, CARGO, CORREO, TELEFONO, PRINCIPAL, CREADO_EN)
SELECT c.ID, 'Ana', 'Gómez', 'CTO', 'ana@techsoft.com', '999333445', 1, SYSTIMESTAMP
FROM CRM_CLIENTES c
WHERE c.NUMERO_DOCUMENTO = '20987654321'
  AND NOT EXISTS (SELECT 1 FROM CRM_CONTACTOS ct WHERE ct.ID_CLIENTE = c.ID AND ct.CORREO = 'ana@techsoft.com');

-- -----------------------------------------------------------------------------
-- Leads
-- -----------------------------------------------------------------------------
INSERT INTO CRM_LEADS (NOMBRE, EMPRESA, CORREO, TELEFONO, FUENTE, FUENTE_ID, ESTADO, RESPONSABLE, FECHA, CREADO_EN)
SELECT 'Pedro', 'Beta Corp', 'pedro@beta.com', '999777888', 'WEB',
       (SELECT ID FROM CRM_FUENTES WHERE CODIGO = 'WEB'),
       'NUEVO', 'vendedor@viernes.com', SYSDATE, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_LEADS WHERE CORREO = 'pedro@beta.com');

INSERT INTO CRM_LEADS (NOMBRE, EMPRESA, CORREO, TELEFONO, FUENTE, FUENTE_ID, ESTADO, RESPONSABLE, FECHA, CREADO_EN)
SELECT 'Sofía', 'Gamma Ltda', 'sofia@gamma.com', '999888777', 'REFERIDO',
       (SELECT ID FROM CRM_FUENTES WHERE CODIGO = 'REFERIDO'),
       'CALIFICADO', 'jefe@viernes.com', SYSDATE, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_LEADS WHERE CORREO = 'sofia@gamma.com');

INSERT INTO CRM_LEADS (NOMBRE, EMPRESA, CORREO, TELEFONO, FUENTE, FUENTE_ID, ESTADO, RESPONSABLE, FECHA, CREADO_EN)
SELECT 'Luis', 'Delta SA', 'luis@delta.com', '999666555', 'FERIA',
       (SELECT ID FROM CRM_FUENTES WHERE CODIGO = 'FERIA'),
       'CONTACTADO', 'vendedor@viernes.com', SYSDATE, SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_LEADS WHERE CORREO = 'luis@delta.com');

-- -----------------------------------------------------------------------------
-- Oportunidades
-- -----------------------------------------------------------------------------
INSERT INTO CRM_OPORTUNIDADES (NOMBRE, CLIENTE_NOMBRE, VALOR, PROBABILIDAD, ETAPA, RESPONSABLE, FECHA_CIERRE, DESCRIPCION, CREADO_EN)
SELECT 'Implementación CRM Acme', 'Acme S.A.', 12000.00, 70, 'NEGOCIACION', 'jefe@viernes.com', SYSDATE + 30, 'Implementación completa para Acme', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_OPORTUNIDADES WHERE NOMBRE = 'Implementación CRM Acme');

INSERT INTO CRM_OPORTUNIDADES (NOMBRE, CLIENTE_NOMBRE, VALOR, PROBABILIDAD, ETAPA, RESPONSABLE, FECHA_CIERRE, DESCRIPCION, CREADO_EN)
SELECT 'Licencias TechSoft', 'TechSoft', 5400.00, 50, 'PROPUESTA', 'vendedor@viernes.com', SYSDATE + 15, '3 licencias Pro anuales', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_OPORTUNIDADES WHERE NOMBRE = 'Licencias TechSoft');

INSERT INTO CRM_OPORTUNIDADES (NOMBRE, CLIENTE_NOMBRE, VALOR, PROBABILIDAD, ETAPA, RESPONSABLE, FECHA_CIERRE, DESCRIPCION, CREADO_EN)
SELECT 'Consultoría Innova', 'Innova SAC', 3500.00, 30, 'CALIFICACION', 'vendedor@viernes.com', SYSDATE + 45, 'Consultoría inicial', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_OPORTUNIDADES WHERE NOMBRE = 'Consultoría Innova');

-- -----------------------------------------------------------------------------
-- Productos en oportunidades
-- -----------------------------------------------------------------------------
INSERT INTO CRM_OPORTUNIDAD_PRODUCTOS (ID_OPORTUNIDAD, ID_PRODUCTO, CANTIDAD, CREADO_EN)
SELECT o.ID, p.ID, 1, SYSTIMESTAMP
FROM CRM_OPORTUNIDADES o, CRM_PRODUCTOS p
WHERE o.NOMBRE = 'Implementación CRM Acme' AND p.CODIGO = 'SRV-IMPL'
  AND NOT EXISTS (SELECT 1 FROM CRM_OPORTUNIDAD_PRODUCTOS op WHERE op.ID_OPORTUNIDAD = o.ID AND op.ID_PRODUCTO = p.ID);

-- -----------------------------------------------------------------------------
-- Colaboradores en oportunidades
-- -----------------------------------------------------------------------------
INSERT INTO CRM_OPORTUNIDAD_COLABORADORES (ID_OPORTUNIDAD, ID_USUARIO, CREADO_EN)
SELECT o.ID, u.ID, SYSTIMESTAMP
FROM CRM_OPORTUNIDADES o, CRM_USUARIOS u
WHERE o.NOMBRE = 'Implementación CRM Acme' AND u.CORREO = 'vendedor@viernes.com'
  AND NOT EXISTS (SELECT 1 FROM CRM_OPORTUNIDAD_COLABORADORES oc WHERE oc.ID_OPORTUNIDAD = o.ID AND oc.ID_USUARIO = u.ID);

-- -----------------------------------------------------------------------------
-- Contactos en oportunidades
-- -----------------------------------------------------------------------------
INSERT INTO CRM_OPORTUNIDAD_CONTACTOS (ID_OPORTUNIDAD, ID_CONTACTO, ROL, ES_DECISOR, CREADO_EN)
SELECT o.ID, c.ID, 'Decisor', 1, SYSTIMESTAMP
FROM CRM_OPORTUNIDADES o, CRM_CONTACTOS c
WHERE o.NOMBRE = 'Implementación CRM Acme' AND c.CORREO = 'juan@acme.com'
  AND NOT EXISTS (SELECT 1 FROM CRM_OPORTUNIDAD_CONTACTOS oc WHERE oc.ID_OPORTUNIDAD = o.ID AND oc.ID_CONTACTO = c.ID);

-- -----------------------------------------------------------------------------
-- Propuestas
-- -----------------------------------------------------------------------------
INSERT INTO CRM_PROPUESTAS (NUMERO_PROPUESTA, ID_OPORTUNIDAD, ID_CLIENTE, CLIENTE_NOMBRE, ESTADO, VERSION, SUBTOTAL, DESCUENTO, TOTAL, RESPONSABLE, FECHA_EMISION, FECHA_VENCIMIENTO, CREADO_EN)
SELECT 'PROP-001', o.ID, c.ID, 'Acme S.A.', 'ENVIADA', 1, 12000.00, 500.00, 11500.00, 'jefe@viernes.com', SYSDATE, SYSDATE + 30, SYSTIMESTAMP
FROM CRM_OPORTUNIDADES o, CRM_CLIENTES c
WHERE o.NOMBRE = 'Implementación CRM Acme' AND c.NUMERO_DOCUMENTO = '20123456789'
  AND NOT EXISTS (SELECT 1 FROM CRM_PROPUESTAS p WHERE p.NUMERO_PROPUESTA = 'PROP-001');

INSERT INTO CRM_PROPUESTA_DETALLES (ID_PROPUESTA, ID_PRODUCTO, DESCRIPCION, CANTIDAD, PRECIO_UNITARIO, SUBTOTAL)
SELECT p.ID, pr.ID, 'Implementación llave en mano', 1, 8000.00, 8000.00
FROM CRM_PROPUESTAS p, CRM_PRODUCTOS pr
WHERE p.NUMERO_PROPUESTA = 'PROP-001' AND pr.CODIGO = 'SRV-IMPL'
  AND NOT EXISTS (SELECT 1 FROM CRM_PROPUESTA_DETALLES d WHERE d.ID_PROPUESTA = p.ID AND d.ID_PRODUCTO = pr.ID);

INSERT INTO CRM_PROPUESTA_DETALLES (ID_PROPUESTA, ID_PRODUCTO, DESCRIPCION, CANTIDAD, PRECIO_UNITARIO, SUBTOTAL)
SELECT p.ID, pr.ID, 'Licencias Pro (3)', 3, 1200.00, 3600.00
FROM CRM_PROPUESTAS p, CRM_PRODUCTOS pr
WHERE p.NUMERO_PROPUESTA = 'PROP-001' AND pr.CODIGO = 'LIC-PRO'
  AND NOT EXISTS (SELECT 1 FROM CRM_PROPUESTA_DETALLES d WHERE d.ID_PROPUESTA = p.ID AND d.ID_PRODUCTO = pr.ID);

-- -----------------------------------------------------------------------------
-- Actividades
-- -----------------------------------------------------------------------------
INSERT INTO CRM_ACTIVIDADES (ID_USUARIO, ID_CLIENTE, TIPO, TITULO, DESCRIPCION, FECHA, ESTADO, CREADO_EN)
SELECT 'vendedor@viernes.com', c.ID, 'REUNION', 'Reunión de cierre Acme', 'Presentación final de propuesta', SYSTIMESTAMP + 2, 'PENDIENTE', SYSTIMESTAMP
FROM CRM_CLIENTES c
WHERE c.NUMERO_DOCUMENTO = '20123456789'
  AND NOT EXISTS (SELECT 1 FROM CRM_ACTIVIDADES a WHERE a.TITULO = 'Reunión de cierre Acme');

INSERT INTO CRM_ACTIVIDADES (ID_USUARIO, ID_OPORTUNIDAD, TIPO, TITULO, DESCRIPCION, FECHA, ESTADO, CREADO_EN)
SELECT 'jefe@viernes.com', o.ID, 'LLAMADA', 'Llamada de seguimiento TechSoft', 'Confirmar interés en licencias', SYSTIMESTAMP + 1, 'PENDIENTE', SYSTIMESTAMP
FROM CRM_OPORTUNIDADES o
WHERE o.NOMBRE = 'Licencias TechSoft'
  AND NOT EXISTS (SELECT 1 FROM CRM_ACTIVIDADES a WHERE a.TITULO = 'Llamada de seguimiento TechSoft');

INSERT INTO CRM_ACTIVIDADES (ID_USUARIO, ID_LEAD, TIPO, TITULO, DESCRIPCION, FECHA, ESTADO, CREADO_EN)
SELECT 'vendedor@viernes.com', l.ID, 'TAREA', 'Calificar lead Pedro', 'Completar ficha de calificación', SYSTIMESTAMP + 3, 'PENDIENTE', SYSTIMESTAMP
FROM CRM_LEADS l
WHERE l.CORREO = 'pedro@beta.com'
  AND NOT EXISTS (SELECT 1 FROM CRM_ACTIVIDADES a WHERE a.TITULO = 'Calificar lead Pedro');

INSERT INTO CRM_ACTIVIDADES (ID_USUARIO, ID_CLIENTE, TIPO, TITULO, DESCRIPCION, FECHA, ESTADO, CREADO_EN)
SELECT 'admin@viernes.com', c.ID, 'SEGUIMIENTO', 'Revisión trimestral Acme', 'Revisar KPIs y renovación', SYSTIMESTAMP + 10, 'PENDIENTE', SYSTIMESTAMP
FROM CRM_CLIENTES c
WHERE c.NUMERO_DOCUMENTO = '20123456789'
  AND NOT EXISTS (SELECT 1 FROM CRM_ACTIVIDADES a WHERE a.TITULO = 'Revisión trimestral Acme');

-- Etiquetas en actividades
INSERT INTO CRM_ACTIVIDAD_ETIQUETAS (ID_ACTIVIDAD, ID_ETIQUETA, CREADO_EN, ORDEN)
SELECT a.ID_ACTIVIDAD, e.ID_ETIQUETA, SYSTIMESTAMP, 1
FROM CRM_ACTIVIDADES a, CRM_ETIQUETAS e
WHERE a.TITULO = 'Reunión de cierre Acme' AND e.NOMBRE_NORMALIZADO = 'caliente'
  AND NOT EXISTS (SELECT 1 FROM CRM_ACTIVIDAD_ETIQUETAS ae WHERE ae.ID_ACTIVIDAD = a.ID_ACTIVIDAD AND ae.ID_ETIQUETA = e.ID_ETIQUETA);

-- -----------------------------------------------------------------------------
-- Campaña
-- -----------------------------------------------------------------------------
INSERT INTO CRM_CAMPANIAS (NOMBRE, DESCRIPCION, FECHA_INICIO, FECHA_FIN, ESTADO, CREADO_EN)
SELECT 'Campaña Q4', 'Campaña de cierre de año', SYSDATE, SYSDATE + 60, 'ACTIVA', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_CAMPANIAS WHERE NOMBRE = 'Campaña Q4');

INSERT INTO CRM_CAMPANIA_LEADS (CAMPANIA_ID, LEAD_ID, CREADO_EN)
SELECT ca.ID, l.ID, SYSTIMESTAMP
FROM CRM_CAMPANIAS ca, CRM_LEADS l
WHERE ca.NOMBRE = 'Campaña Q4' AND l.CORREO IN ('pedro@beta.com', 'sofia@gamma.com')
  AND NOT EXISTS (SELECT 1 FROM CRM_CAMPANIA_LEADS cl WHERE cl.CAMPANIA_ID = ca.ID AND cl.LEAD_ID = l.ID);

-- -----------------------------------------------------------------------------
-- Equipo comercial
-- -----------------------------------------------------------------------------
INSERT INTO CRM_EQUIPOS (NOMBRE, DESCRIPCION, ESTADO, CREADO_EN)
SELECT 'Equipo Norte', 'Equipo comercial región norte', 'ACTIVO', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_EQUIPOS WHERE NOMBRE = 'Equipo Norte');

INSERT INTO CRM_EQUIPO_MIEMBROS (EQUIPO_ID, USUARIO_ID, CREADO_EN)
SELECT e.ID, u.ID, SYSTIMESTAMP
FROM CRM_EQUIPOS e, CRM_USUARIOS u
WHERE e.NOMBRE = 'Equipo Norte' AND u.CORREO IN ('jefe@viernes.com', 'vendedor@viernes.com')
  AND NOT EXISTS (SELECT 1 FROM CRM_EQUIPO_MIEMBROS em WHERE em.EQUIPO_ID = e.ID AND em.USUARIO_ID = u.ID);

-- -----------------------------------------------------------------------------
-- Mensajes de la sala general
-- -----------------------------------------------------------------------------
INSERT INTO CRM_MENSAJES (USUARIO_CORREO, USUARIO_NOMBRE, CONTENIDO, CREADO_EN)
SELECT 'admin@viernes.com', 'Admin Viernes', 'Bienvenidos al CRM Viernes. Coordinemos el cierre del trimestre.', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_MENSAJES WHERE CONTENIDO = 'Bienvenidos al CRM Viernes. Coordinemos el cierre del trimestre.');

INSERT INTO CRM_MENSAJES (USUARIO_CORREO, USUARIO_NOMBRE, CONTENIDO, CREADO_EN)
SELECT 'vendedor@viernes.com', 'Vendedor Prueba', 'Acme confirmó la reunión para el viernes.', SYSTIMESTAMP FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CRM_MENSAJES WHERE CONTENIDO = 'Acme confirmó la reunión para el viernes.');

COMMIT;

PROMPT [OK] Datos demo insertados.
