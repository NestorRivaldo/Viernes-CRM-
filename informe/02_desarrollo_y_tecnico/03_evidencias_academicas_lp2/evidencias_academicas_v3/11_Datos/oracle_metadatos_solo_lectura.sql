-- Evidencia pendiente: ejecutar con una conexión YA autorizada al esquema CRM.
-- No contiene credenciales ni DDL/DML. No cambia datos ni configuración.
SELECT USER AS esquema, CURRENT_TIMESTAMP AS fecha_consulta FROM dual;
SELECT table_name, column_id, column_name, data_type, data_length,
       char_length, char_used, data_precision, data_scale, nullable, data_default
FROM user_tab_columns WHERE table_name LIKE 'CRM\_%' ESCAPE '\'
ORDER BY table_name, column_id;
SELECT c.table_name, c.constraint_name, c.constraint_type, c.status,
       c.validated, c.deferrable, c.deferred, c.delete_rule,
       cc.column_name, cc.position,
       p.table_name AS tabla_referida, pc.column_name AS columna_referida,
       c.search_condition
FROM user_constraints c
LEFT JOIN user_cons_columns cc ON cc.constraint_name = c.constraint_name
LEFT JOIN user_constraints p ON p.constraint_name = c.r_constraint_name
LEFT JOIN user_cons_columns pc ON pc.constraint_name = p.constraint_name
                              AND pc.position = cc.position
WHERE c.table_name LIKE 'CRM\_%' ESCAPE '\'
ORDER BY c.table_name, c.constraint_name, cc.position;
SELECT i.table_name, i.index_name, i.index_type, i.uniqueness, i.status,
       ic.column_position, ic.column_name, ic.descend
FROM user_indexes i JOIN user_ind_columns ic ON ic.index_name = i.index_name
WHERE i.table_name LIKE 'CRM\_%' ESCAPE '\'
ORDER BY i.table_name, i.index_name, ic.column_position;
SELECT table_name, column_name, generation_type, sequence_name, identity_options
FROM user_tab_identity_cols WHERE table_name LIKE 'CRM\_%' ESCAPE '\'
ORDER BY table_name, column_name;
