-- Usuarios DEV de Viernes V1 — ejecutar DESPUÉS de que el backend haya
-- arrancado una vez (ddl-auto: update crea las tablas CRM_*).
-- Uso desde la raíz de viernes-backend:
--   docker exec -i bomerp-oracle sqlplus BOM_VENTAS/123456@//localhost:1521/FREEPDB1 < seed-dev.sql
-- Idempotente: si el correo ya existe, no hace nada (CORREO es UNIQUE).

MERGE INTO CRM_USUARIOS u
USING (SELECT 'admin@viernes.com' AS CORREO FROM dual) s
ON (u.CORREO = s.CORREO)
WHEN NOT MATCHED THEN
  INSERT (NOMBRE, APELLIDO, CORREO, PASSWORD_HASH, ROL, ESTADO, CREADO_EN, ACTUALIZADO_EN)
  VALUES ('Admin', 'Viernes', 'admin@viernes.com',
          '$2a$12$EUN9TjMLLm6Bxy05shdOkO06tr0xp3AeG.GXaY1k1JrQ1T82HAqAS',
          'ADMIN', 'ACTIVO', SYSTIMESTAMP, SYSTIMESTAMP);

MERGE INTO CRM_USUARIOS u
USING (SELECT 'vendedor@viernes.com' AS CORREO FROM dual) s
ON (u.CORREO = s.CORREO)
WHEN NOT MATCHED THEN
  INSERT (NOMBRE, APELLIDO, CORREO, PASSWORD_HASH, ROL, ESTADO, CREADO_EN, ACTUALIZADO_EN)
  VALUES ('Vendedor', 'Prueba', 'vendedor@viernes.com',
          '$2a$12$QwYhMKcM.IOuKiLxj6WZkuW0Q5c9GAlmulVJ8ZjaBzgFrOd/NemZO',
          'VENDEDOR', 'ACTIVO', SYSTIMESTAMP, SYSTIMESTAMP);

COMMIT;
