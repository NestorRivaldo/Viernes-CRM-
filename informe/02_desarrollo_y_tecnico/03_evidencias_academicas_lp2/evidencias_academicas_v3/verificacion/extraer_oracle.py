# Solo lectura. Obtiene la conexión existente sin imprimir ni guardar la contraseña.
from documentos import *
import subprocess, io, datetime
cfg=(ROOT/'viernes-backend/src/main/resources/application-dev.yml').read_text(encoding='utf-8-sig')
user=re.search(r'^\s*username:\s*(\S+)',cfg,re.M).group(1)
pwd=re.search(r'^\s*password:\s*(\S+)',cfg,re.M).group(1).strip('\"\'')
queries={
 'contexto':"SELECT USER AS esquema, TO_CHAR(CURRENT_TIMESTAMP, 'YYYY-MM-DD HH24:MI:SS TZH:TZM') AS fecha FROM dual",
 'columnas':"SELECT table_name,column_id,column_name,data_type,data_length,char_length,char_used,data_precision,data_scale,nullable FROM user_tab_columns WHERE table_name LIKE 'CRM\\_%' ESCAPE '\\' ORDER BY table_name,column_id",
 'constraints':"SELECT c.table_name,c.constraint_name,c.constraint_type,c.status,c.validated,c.delete_rule,cc.column_name,cc.position,p.table_name AS tabla_referida,pc.column_name AS columna_referida FROM user_constraints c LEFT JOIN user_cons_columns cc ON cc.constraint_name=c.constraint_name LEFT JOIN user_constraints p ON p.constraint_name=c.r_constraint_name LEFT JOIN user_cons_columns pc ON pc.constraint_name=p.constraint_name AND pc.position=cc.position WHERE c.table_name LIKE 'CRM\\_%' ESCAPE '\\' ORDER BY c.table_name,c.constraint_name,cc.position",
 'indices':"SELECT i.table_name,i.index_name,i.index_type,i.uniqueness,i.status,ic.column_position,ic.column_name,ic.descend FROM user_indexes i JOIN user_ind_columns ic ON ic.index_name=i.index_name WHERE i.table_name LIKE 'CRM\\_%' ESCAPE '\\' ORDER BY i.table_name,i.index_name,ic.column_position",
 'identity':"SELECT table_name,column_name,generation_type,sequence_name,identity_options FROM user_tab_identity_cols WHERE table_name LIKE 'CRM\\_%' ESCAPE '\\' ORDER BY table_name,column_name",
 'checks':"SELECT table_name,constraint_name,search_condition_vc AS search_condition FROM user_constraints WHERE table_name LIKE 'CRM\\_%' ESCAPE '\\' AND constraint_type='C' ORDER BY table_name,constraint_name"
}
manifest=[]
for name,q in queries.items():
    sql='whenever sqlerror exit failure rollback\nwhenever oserror exit failure rollback\nset echo off\nset verify off\nset feedback off\nset heading on\nset long 100000\nset markup csv on quote on\nconnect '+user+'/'+pwd+'@//localhost:1521/FREEPDB1\nSET TRANSACTION READ ONLY;\n'+q+';\nROLLBACK;\nexit\n'
    r=subprocess.run(['docker','exec','-i','viernes-oracle','sqlplus','-s','/nolog'],input=sql,text=True,encoding='utf-8',errors='replace',capture_output=True,timeout=35)
    output=(r.stdout+'\n'+r.stderr).strip()
    if r.returncode or re.search(r'ORA-\d|SP2-\d',output):
        raise RuntimeError('Consulta '+name+' falló; no se publica la salida de conexión.')
    assert pwd not in output, 'Salida contiene dato de conexión; no se guarda'
    rows=list(csv.DictReader(io.StringIO(output)))
    assert rows, 'Sin filas '+name
    dest=OUT/'11_Datos'/('oracle_'+name+'.csv');dest.write_text(output+'\n',encoding='utf-8-sig')
    manifest.append(dict(archivo=dest.name,filas=len(rows),sha256=digest(dest),consulta=q,fecha_extraccion=datetime.datetime.now().isoformat()))
(OUT/'11_Datos/oracle_manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps([dict(archivo=x['archivo'],filas=x['filas']) for x in manifest]))
