from documentos import *
from collections import Counter,defaultdict
D=OUT/'11_Datos'
def read(name):
    with (D/('oracle_'+name+'.csv')).open(encoding='utf-8-sig',newline='') as f:return list(csv.DictReader(f,strict=True))
cols=read('columnas');constraints=read('constraints');indices=read('indices');checks=read('checks');ctx=read('contexto')[0]
model=json.loads((D/'modelo_jpa.json').read_text(encoding='utf-8'));entities=model['entities'];jt={e['table'] for e in entities};ot={r['TABLE_NAME'] for r in cols}
assert jt<=ot
lookup={(r['TABLE_NAME'],r['COLUMN_NAME']):r for r in cols}; uniq={r['CONSTRAINT_NAME']:r for r in constraints}; fk=[r for r in constraints if r['CONSTRAINT_TYPE']=='R']; mappedfk=[r for r in fk if r['TABLE_NAME'] in jt]
nidx=len({r['INDEX_NAME'] for r in indices});extras=sorted(ot-jt)
rows=[];jkeys=set()
for e in entities:
    for f in e['fields']:
        key=(e['table'],f['column']);jkeys.add(key);r=lookup.get(key)
        assert r,key
        actual=r['DATA_TYPE']+(('('+r['DATA_PRECISION']+','+r['DATA_SCALE']+')') if r['DATA_PRECISION'] else '('+r['DATA_LENGTH']+' bytes)')
        cons=[x for x in constraints if x['TABLE_NAME']==key[0] and x['COLUMN_NAME']==key[1]]
        refs='; '.join(x['CONSTRAINT_NAME']+' → '+x['TABLA_REFERIDA']+'.'+x['COLUMNA_REFERIDA']+' [ON DELETE '+x['DELETE_RULE']+']' for x in cons if x['CONSTRAINT_TYPE']=='R') or 'Sin FK observada en esta columna'
        rows.append([e['table'],f['column'],f['type'],f['sql'],actual,r['CHAR_LENGTH'],r['CHAR_USED'],f['nullable'],r['NULLABLE'],refs,f['source']+':'+str(f['line'])])
extra_cols=[r for r in cols if r['TABLE_NAME'] in jt and (r['TABLE_NAME'],r['COLUMN_NAME']) not in jkeys]
csvfile('11_Datos/diccionario_conciliado.csv',['Tabla','Columna','Tipo Java','SQL inferido JPA','Tipo observado Oracle','Longitud caracteres Oracle','Semántica BYTE/CHAR','NULL JPA','NULL Oracle Y/N','FK física Oracle','Fuente JPA:línea'],rows)
csvfile('11_Datos/columnas_no_mapeadas.csv',list(cols[0].keys()),[[r[k] for k in cols[0].keys()] for r in extra_cols])
# Preserve a clearly labeled JPA-only view rather than rewriting its evidence level.
old=D/'11_Datos.html'; appendix=D/'11_Datos_JPA.html'
if not appendix.exists():
    source=old.read_text(encoding='utf-8')
    source=source.replace('Oracle no comprobado.','Vista de inferencias JPA; véase el informe conciliado para Oracle.').replace('La consulta de metadatos no pudo conectarse porque Docker estaba detenido. No se inició Docker ni se alteró la base de datos.','Esta vista fue generada antes de la consulta de metadatos. La revisión posterior sí dispone de evidencia Oracle separada.')
    source=source.replace('su existencia física, nombres, estado y reglas de borrado en Oracle siguen pendientes.','la comprobación física se presenta por separado en 11_Datos.html.')
    source=source.replace('11.6 Comprobación Oracle pendiente','11.6 Protocolo de comprobación Oracle').replace('No se ejecutó este archivo.','La extracción posterior ejecutó consultas SELECT equivalentes en transacciones READ ONLY; véanse los CSV oracle_*.')
    appendix.write_text(source,encoding='utf-8')
summary=Counter(r['CONSTRAINT_TYPE'] for r in uniq.values())
body=f'<p class="aviso"><strong>Dos fuentes de evidencia, sin confundirlas.</strong> JPA: 21 entidades y 152 columnas previstas. Oracle: consulta SELECT en transacciones READ ONLY al esquema <strong>{ctx["ESQUEMA"]}</strong>, fecha del servidor <strong>{ctx["FECHA"]}</strong>. Se observaron <strong>{len(ot)} tablas CRM y {len(cols)} columnas</strong>; {len(jt)} tablas corresponden al código actual y {len(extras)} no tienen entidad JPA en este árbol. No se ejecutaron DDL/DML en esta extracción.</p>'
body+='<h2>11.1 Conciliación del modelo y diferencias comprobadas</h2>'
body+=table(['Medida','JPA / árbol de código','Oracle observado'],[['Tablas / entidades',21,len(ot)],['Columnas de las 21 entidades (incluye join del padre)',152,sum(r['TABLE_NAME'] in jt for r in cols)],['PK','21 @Id IDENTITY',summary['P']],['FK','2 JoinColumn',str(len(fk))+' totales; '+str(len(mappedfk))+' en tablas mapeadas'],['UNIQUE','14 declaradas',summary['U']],['CHECK / NOT NULL','Sin @Check explícito',summary['C']],['Índices','Sin @Index explícito',str(nidx)+' índices; '+str(len(indices))+' filas de columnas indexadas']])
body+='<p>La diferencia no autoriza a borrar objetos ni a afirmar que pertenecen a funcionalidades implementadas. Las tablas no mapeadas son '+', '.join(extras)+'. Pueden ser históricas o externas; su propósito no se certifica con nombres solamente.</p>'
body+='<h3>Columnas físicas adicionales en tablas mapeadas</h3>'+table(['Tabla','Columna Oracle','Tipo','NULL Y/N'],[[r['TABLE_NAME'],r['COLUMN_NAME'],r['DATA_TYPE'],r['NULLABLE']] for r in extra_cols])
body+='<p>En particular, Oracle sí tiene CRM_OPORTUNIDADES.ID_CLIENTE y CRM_ACTIVIDADES.ID_USUARIO_NUM con FK, pero las entidades actuales no los mapean: Oportunidad usa clienteNombre y Actividad usa idUsuario String (ID o correo). No se infiere que el código mantenga esas columnas adicionales.</p>'
body+='<h2>11.2 ERD físico observado de las 21 tablas mapeadas</h2><p><a href="erd_oracle_21.svg">ERD físico SVG</a> · <a href="erd_oracle_21.puml">PlantUML editable</a> · <a href="erd_oracle_26.svg">Anexo: las 26 tablas Oracle</a> · <a href="erd_completo.svg">Vista de asociaciones inferidas de JPA</a></p><figure><img src="erd_oracle_21.svg" alt="ERD Oracle comprobado de las 21 tablas mapeadas"></figure>'
body+='<h2>11.3 FK físicas comprobadas</h2>'+table(['Tabla/columna hija','Constraint','Tabla/columna padre','Borrado','Estado'],[[r['TABLE_NAME']+'.'+r['COLUMN_NAME'],r['CONSTRAINT_NAME'],r['TABLA_REFERIDA']+'.'+r['COLUMNA_REFERIDA'],r['DELETE_RULE'],r['STATUS']+' / '+r['VALIDATED']] for r in fk])
body+='<p>Las siete tablas puente solicitadas tienen ambos extremos respaldados por FK físicas observadas, aunque sus entidades usan IDs escalares. Esa diferencia es central: “sin asociación JPA” no significa “sin FK en Oracle”. Los IDs de cliente/oportunidad en propuestas, producto en detalles y otros vínculos lógicos se documentan por separado; no se convierten en FK por aparecer en un DTO.</p>'
body+='<h2>11.4 Relaciones lógicas y puentes N:M</h2>'
rr=[]
for a,c,b,k,kind in model['relationships']:
    match=next((x for x in fk if x['TABLE_NAME']==a and x['COLUMN_NAME']==c and x['TABLA_REFERIDA']==b and x['COLUMNA_REFERIDA']==k),None)
    rr.append([a+'.'+c,b+'.'+k,kind,match['CONSTRAINT_NAME'] if match else 'Relación lógica; sin FK física observada'])
body+=table(['Origen','Destino','Declaración de código','Comprobación Oracle'],rr)
body+=table(['Puente','Extremos','Unicidad del par'],[[b[0],b[2]+' ↔ '+b[5],b[1]+' + '+b[4]] for b in model['bridges']])
body+='<h2>11.5 Diccionario campo por campo conciliado</h2><p><a href="diccionario_conciliado.csv">CSV conciliado (152 columnas JPA)</a> · <a href="oracle_columnas.csv">CSV físico completo (194 columnas)</a> · <a href="11_Datos_JPA.html">Detalle JPA: inicializadores, enums y reglas de mapeo</a>. Los tipos inferidos nunca sustituyen al tipo físico observado. NULL de Oracle usa Y/N; CHAR_USED refleja B/C. Longitud de NUMBER en bytes no es su precisión decimal. Las 7 columnas adicionales y las 5 tablas no mapeadas permanecen en el inventario físico.</p>'
for e in entities:
    body+='<h3>'+e['table']+'</h3>'+table(['Columna','Java','Oracle observado','Caracteres / B-C','NULL Oracle','Relación','Fuente'],[[r[1],r[2],r[4],r[5]+' / '+r[6],r[8],r[9],ref(f['source'],f['line'])] for r,f in zip([r for r in rows if r[0]==e['table']],e['fields'])])
body+='<h2>11.6 Restricciones e índices reales</h2><p>El catálogo físico completo se adjunta: <a href="oracle_constraints.csv">constraints y columnas</a>, <a href="oracle_checks.csv">expresiones CHECK/NOT NULL</a>, <a href="oracle_indices.csv">índices y orden de columnas</a> y <a href="oracle_identity.csv">IDENTITY</a>. Los conteos de constraints se deduplican por nombre; una UK compuesta ocupa varias filas. Los índices también se cuentan por nombre, no por fila de columna.</p>'
body+=table(['Tabla','PK/UK física','Tipo','Estado'],[[r['TABLE_NAME'],r['CONSTRAINT_NAME'],r['CONSTRAINT_TYPE'],r['STATUS']] for r in uniq.values() if r['CONSTRAINT_TYPE'] in ['P','U']])
body+='<h2>11.7 Alcance y reproducibilidad de la consulta</h2><p>Se consultaron únicamente metadatos USER_* y contexto de sesión: no se extrajeron datos de clientes o usuarios, no se inició Spring y no se aplicó ninguna migración. Cada consulta se delimitó con SET TRANSACTION READ ONLY y ROLLBACK. El archivo <a href="oracle_manifest.json">manifest Oracle</a> registra sentencias, fecha y SHA-256 de las salidas sin credenciales. Como son consultas en transacciones separadas, no se afirma una instantánea SCN común a todo el catálogo.</p><p>Advertencia sobre la prueba anterior: el log de contextLoads conecta a Oracle con dev/keycloak y ddl-auto:update. Esa ejecución no es certificable como estrictamente de solo lectura; se conserva como resultado de pruebas, sin asegurar que no emitiera DDL. No se volvió a ejecutar la suite.</p>'
page('11_Datos/11_Datos.html','11. Datos — JPA y Oracle conciliados',body)
# Physical ERDs use only catalog FKs, not inferred logical links.
for count,tables in [(21,jt),(26,ot)]:
    puml=['@startuml','!pragma layout smetana','title Oracle observado — '+str(count)+' tablas; '+ctx['FECHA'],'skinparam defaultFontName Segoe UI','skinparam defaultFontSize 12','hide methods','skinparam linetype ortho']
    for t in sorted(tables):
        puml.append('entity "'+t+'" as '+t+' {')
        keys={r['COLUMN_NAME']:r['CONSTRAINT_TYPE'] for r in constraints if r['TABLE_NAME']==t and r['CONSTRAINT_TYPE'] in ['P','R']}
        for r in cols:
            if r['TABLE_NAME']==t and (r['COLUMN_NAME'] in keys or (t,r['COLUMN_NAME']) not in jkeys):
                puml.append('  '+r['COLUMN_NAME']+' : '+r['DATA_TYPE']+' <<'+('PK' if keys.get(r['COLUMN_NAME'])=='P' else 'FK' if keys.get(r['COLUMN_NAME'])=='R' else 'sin mapeo JPA')+'>>')
        puml.append('}')
    for r in fk:
        if r['TABLE_NAME'] in tables and r['TABLA_REFERIDA'] in tables:
            optional=lookup[(r['TABLE_NAME'],r['COLUMN_NAME'])]['NULLABLE']=='Y'
            puml.append(r['TABLA_REFERIDA']+' "'+('0..1' if optional else '1')+'" -- "0..*" '+r['TABLE_NAME']+' : '+r['COLUMN_NAME'])
    puml+=['legend bottom',' Solo FK observadas en Oracle; vínculos lógicos del código van en informe/CSV.',' sin mapeo JPA = columna física no representada por una propiedad de la entidad.',' Los atributos no clave se encuentran en oracle_columnas.csv.','endlegend','@enduml']
    (D/('erd_oracle_'+str(count)+'.puml')).write_text('\n'.join(puml),encoding='utf-8')
(OUT/'verificacion/resumen_oracle.json').write_text(json.dumps(dict(tablas_oracle=len(ot),columnas_oracle=len(cols),columnas_jpa=152,columnas_extra_mapeadas=len(extra_cols),tablas_no_mapeadas=extras,fk_total=len(fk),fk_tablas_mapeadas=len(mappedfk),indices=nidx,constraints=dict(summary)),ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(dict(tablas=len(ot),columnas=len(cols),extras=len(extra_cols),fk=len(fk),indices=nidx,checks=len(checks))))
