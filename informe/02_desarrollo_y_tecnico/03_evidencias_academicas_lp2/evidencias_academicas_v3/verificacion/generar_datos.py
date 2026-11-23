from documentos import *
entities=[]
for p in sorted(JAVA.glob('**/*Entity.java')):
    s=p.read_text(encoding='utf-8-sig')
    t=re.search(r'@Table\(\s*name\s*=\s*"([^"]+)"',s).group(1)
    fields=[]; previous=0
    for m in re.finditer(r'private\s+([\w<>]+)\s+(\w+)\s*(?:=\s*([^;]+))?;',s):
        ann=s[previous:m.start()];previous=m.end()
        col=re.search(r'@Column\(([^)]*)\)',ann)
        if not col: continue
        a=col.group(1); typ,name,default=m.groups()
        cname=re.search(r'name\s*=\s*"([^"]+)"',a).group(1)
        params=dict(re.findall(r'(nullable|unique|length|precision|scale|insertable|updatable)\s*=\s*(\w+)',a))
        pk='@Id' in ann
        nullable='No' if pk or params.get('nullable')=='false' else 'Sí (default JPA)'
        ln=s[:m.start()].count('\n')+1
        enum='';lob='@Lob' in ann
        if '@Enumerated' in ann:
            ep=list(JAVA.glob('**/'+typ+'.java'));assert len(ep)==1
            es=ep[0].read_text(encoding='utf-8-sig'); enum=', '.join(re.findall(r'\b[A-Z][A-Z_]+\b',es.split('{',1)[1].split('}',1)[0]))
        if typ=='String' or enum:
            sql='CLOB' if lob else 'VARCHAR2('+params.get('length','255')+')'
        else: sql={'Long':'NUMBER(19,0)','Integer':'NUMBER(10,0)','BigDecimal':'NUMBER('+params.get('precision','dependiente del proveedor')+','+params.get('scale','0')+')','LocalDate':'DATE','LocalDateTime':'TIMESTAMP (precisión por proveedor)','Boolean':'BOOLEAN o NUMBER(1,0), depende del dialecto'}.get(typ,typ)
        flags=[]
        if pk: flags+=['PK', 'IDENTITY']
        if params.get('unique')=='true':flags+=['UNIQUE']
        if params.get('updatable')=='false':flags+=['updatable=false']
        if params.get('insertable')=='false':flags+=['insertable=false (escritura por asociación)']
        if enum: flags+=['Enum STRING: '+enum+'; CHECK físico no comprobado']
        fields.append(dict(column=cname.upper(),java=name,type=typ,sql=sql,nullable=nullable,flags='; '.join(flags) or '—',default=(default or 'Sin inicializador Java'),line=ln,source=p.relative_to(JAVA).as_posix(),pk=pk,enum=enum))
    assert len(fields)==len(re.findall(r'@Column\(',s)),p
    uniques=[]
    for u in re.finditer(r'@UniqueConstraint\(([^)]+)\)',s):
        us=u.group(1); nm=re.search(r'name\s*=\s*"([^"]+)"',us)
        cols=re.search(r'columnNames\s*=\s*(\{[^}]+\}|"[^"]+")',us).group(1)
        uniques.append((nm.group(1) if nm else 'Nombre por proveedor',', '.join(re.findall('"([^"]+)"',cols)).upper()))
    for f in fields:
        if 'UNIQUE' in f['flags']:uniques.append(('Nombre por proveedor',f['column']))
    entities.append(dict(table=t,entity=p.stem,module=p.relative_to(JAVA).parts[0],source=p.relative_to(JAVA).as_posix(),fields=fields,uniques=uniques))
assert len(entities)==21
bytable={e['table']:e for e in entities}
# JoinColumn unidireccional del padre: la columna existe en el hijo aunque no tenga campo Java allí.
bytable['CRM_PROPUESTA_DETALLES']['fields'].append(dict(column='ID_PROPUESTA',java='PropuestaEntity.detalles (join)',type='Long (ID del padre)',sql='NUMBER(19,0)',nullable='No',flags='FK declarada por @OneToMany + @JoinColumn',default='Sin default DDL declarado',line=71,source='propuestas/adapters/out/persistence/entity/PropuestaEntity.java',pk=False,enum=''))
rels=[('CRM_CONTACTOS','ID_CLIENTE','CRM_CLIENTES','ID','FK JPA'),('CRM_PROPUESTA_DETALLES','ID_PROPUESTA','CRM_PROPUESTAS','ID','FK JPA'),('CRM_LEADS','FUENTE_ID','CRM_FUENTES','ID','Lógica'),('CRM_PROPUESTAS','ID_CLIENTE','CRM_CLIENTES','ID','Lógica'),('CRM_PROPUESTAS','ID_OPORTUNIDAD','CRM_OPORTUNIDADES','ID','Lógica'),('CRM_PROPUESTA_DETALLES','ID_PRODUCTO','CRM_PRODUCTOS','ID','Lógica'),('CRM_ACTIVIDADES','ID_CLIENTE','CRM_CLIENTES','ID','Lógica'),('CRM_ACTIVIDADES','ID_LEAD','CRM_LEADS','ID','Lógica'),('CRM_ACTIVIDADES','ID_OPORTUNIDAD','CRM_OPORTUNIDADES','ID','Lógica')]
bridges=[('CRM_ACTIVIDAD_ETIQUETAS','ID_ACTIVIDAD','CRM_ACTIVIDADES','ID_ACTIVIDAD','ID_ETIQUETA','CRM_ETIQUETAS','ID_ETIQUETA'),('CRM_ACTIVIDAD_PARTICIPANTES','ID_ACTIVIDAD','CRM_ACTIVIDADES','ID_ACTIVIDAD','ID_CONTACTO','CRM_CONTACTOS','ID'),('CRM_OPORTUNIDAD_CONTACTOS','ID_OPORTUNIDAD','CRM_OPORTUNIDADES','ID','ID_CONTACTO','CRM_CONTACTOS','ID'),('CRM_OPORTUNIDAD_PRODUCTOS','ID_OPORTUNIDAD','CRM_OPORTUNIDADES','ID','ID_PRODUCTO','CRM_PRODUCTOS','ID'),('CRM_OPORTUNIDAD_COLABORADORES','ID_OPORTUNIDAD','CRM_OPORTUNIDADES','ID','ID_USUARIO','CRM_USUARIOS','ID'),('CRM_EQUIPO_MIEMBROS','EQUIPO_ID','CRM_EQUIPOS','ID','USUARIO_ID','CRM_USUARIOS','ID'),('CRM_CAMPANIA_LEADS','CAMPANIA_ID','CRM_CAMPANIAS','ID','LEAD_ID','CRM_LEADS','ID')]
for t,c1,t1,k1,c2,t2,k2 in bridges:rels += [(t,c1,t1,k1,'Lógica'),(t,c2,t2,k2,'Lógica')]
for a,c,b,k,kind in rels:
    assert c in [f['column'] for f in bytable[a]['fields']]
    assert k in [f['column'] for f in bytable[b]['fields']]
(OUT/'11_Datos/modelo_jpa.json').write_text(json.dumps(dict(entities=entities,relationships=rels,bridges=bridges,oracle_verified=False),ensure_ascii=False,indent=2),encoding='utf-8')
rows=[]
for e in entities:
    for f in e['fields']:
        links=[b+'.'+k+' ['+kind+']' for a,c,b,k,kind in rels if a==e['table'] and c==f['column']]
        f['relation']='; '.join(links) or '—'
        rows.append([e['table'],f['column'],f['java'],f['type'],f['sql'],f['nullable'],f['flags'],f['default'],f['relation'],f['source']+':'+str(f['line']),'JPA; NO verificado en Oracle'])
csvfile('11_Datos/diccionario_campos.csv',['Tabla','Columna','Campo Java','Tipo Java','Tipo Oracle orientativo','Acepta NULL (JPA)','Restricciones','Inicializador Java, NO DEFAULT SQL','Relación','Fuente:línea','Evidencia'],rows)
csvfile('11_Datos/relaciones.csv',['Tabla hija','Columna','Tabla referenciada','Columna destino','Tipo (Oracle no comprobado)'],rels)
urows=[[e['table'],name,cols,'JPA; índice físico pendiente'] for e in entities for name,cols in e['uniques']]
csvfile('11_Datos/restricciones_unicas.csv',['Tabla','Nombre declarado','Columnas','Evidencia'],urows)
body='<p class="aviso"><strong>Oracle no comprobado.</strong> La consulta de metadatos no pudo conectarse porque Docker estaba detenido. No se inició Docker ni se alteró la base de datos. Las 21 tablas, columnas y restricciones siguientes se deducen del mapeo JPA actual. Dos asociaciones declaran FK en JPA; su existencia física, nombres, estado y reglas de borrado en Oracle siguen pendientes.</p>'
body+=f'<p>El inventario contiene <strong>{len(entities)} entidades, {len(rows)} columnas físicas previstas</strong> (incluida ID_PROPUESTA declarada desde el padre), 21 PK IDENTITY, {len(urows)} restricciones UNIQUE declaradas y siete tablas puente N:M. No se hallaron @Index ni @Check en las entidades. No equivale a afirmar que Oracle carece de índices o CHECK.</p>'
body+='<h2>11.1 Modelo y regla de lectura</h2><p>Los nombres se normalizan a mayúsculas por legibilidad. NUMBER, VARCHAR2, DATE, CLOB y TIMESTAMP son traducciones orientativas: el dialecto y esquema efectivo pueden variar. El inicializador Java no es un DEFAULT SQL. La columna VERSION de propuestas es un entero comercial, no un @Version de bloqueo optimista. Las colecciones no son columnas; el ID_CLIENTE de Contacto se cuenta una sola vez aunque también lo use cliente.</p><p><a href="erd_completo.svg">Abrir ERD completo (SVG)</a> · <a href="erd_completo.puml">PlantUML editable</a> · <a href="diccionario_campos.csv">Diccionario CSV</a> · <a href="relaciones.csv">Relaciones CSV</a></p><figure><img src="erd_completo.svg" alt="ERD completo inferido de JPA"></figure>'
body+='<h2>11.2 Integridad referencial y límites</h2>'+table(['Origen','Destino','Evidencia'],[[a+'.'+c,b+'.'+k,kind+'; pendiente Oracle'] for a,c,b,k,kind in rels])
body+='<p>Las líneas continuas del ERD representan asociaciones con JoinColumn; las discontinuas son IDs lógicos, sin FK declarada en este código. Sus cardinalidades son las esperadas por la aplicación, no prueba de inexistencia de huérfanos. CRM_ACTIVIDADES.ID_USUARIO es String y se resuelve como ID numérico o correo; no debe dibujarse como una FK Long simple. CRM_MENSAJES conserva correo y nombre del emisor, no una FK de usuario. CLIENTE_NOMBRE en oportunidades es texto y el mapa deduce relaciones por nombre/contactos, no por ID_CLIENTE.</p><p>'+ref('actividades/adapters/out/persistence/RelacionValidationAdapter.java',52)+'</p>'
body+='<h2>11.3 Las siete asociaciones N:M</h2>'+table(['Puente','Extremo A','Extremo B','Atributos propios'],[[t,t1,t2,', '.join(f['column'] for f in bytable[t]['fields'] if not f['pk'] and f['column'] not in [c1,c2])] for t,c1,t1,k1,c2,t2,k2 in bridges])
body+='<h2>11.4 Restricciones e índices</h2>'+table(['Tabla','UNIQUE','Columnas','Estado'],urows)+'<p>La unicidad de una PK/UK suele apoyarse en índices físicos; no se inventa su nombre ni su cantidad. nullable=false declara obligatoriedad en el mapeo, y @Enumerated(STRING) declara la serialización del enum; un CHECK efectivo depende del DDL aplicado. CascadeType.ALL y orphanRemoval son semántica ORM, no evidencia de ON DELETE CASCADE.</p>'
body+='<h2>11.5 Diccionario campo por campo</h2>'
for e in entities:
    body+='<h3>'+e['table']+' · '+e['module']+'</h3><p>'+ref(e['source'],1)+'</p>'
    body+=table(['Columna / Java','Tipo Java → SQL orientativo','NULL','Regla y relación','Inicialización Java','Fuente'],[[f['column']+'<br>'+f['java'],f['type']+' → '+f['sql'],f['nullable'],f['flags']+'<br>'+f['relation'],escape(f['default']),ref(f['source'],f['line'])] for f in e['fields']])
body+='<h2>11.6 Comprobación Oracle pendiente</h2><p>Se adjunta <a href="oracle_metadatos_solo_lectura.sql">SQL de solo lectura</a>: USER_TAB_COLUMNS, USER_CONSTRAINTS, USER_CONS_COLUMNS, USER_INDEXES, USER_IND_COLUMNS y USER_TAB_IDENTITY_COLS. Ejecutarlo conectado al esquema de la aplicación, sin incluir contraseñas en comandos o informes. No se ejecutó este archivo. Registrar fecha, esquema, versión, salida y discrepancias antes de convertir cualquier inferencia en hecho Oracle.</p>'
page('11_Datos/11_Datos.html','11. Datos',body)
# ERD acotado a claves; el diccionario documenta el resto de atributos.
puml=['@startuml','!pragma layout smetana','title ERD completo — 21 tablas inferidas de JPA; Oracle NO comprobado','skinparam defaultFontName Segoe UI','skinparam defaultFontSize 12','skinparam linetype ortho','hide methods']
for e in entities:
    puml.append('entity "'+e['table']+'" as '+e['entity']+' {')
    for f in e['fields']:
        if f['pk'] or f['relation']!='—':puml.append(('  * ' if f['pk'] else '  ')+f['column']+' : '+f['type']+(' <<PK>>' if f['pk'] else ''))
    puml.append('}')
for a,c,b,k,kind in rels:
    field=next(f for f in bytable[a]['fields'] if f['column']==c)
    mult='"1"' if field['nullable']=='No' else '"0..1"'
    puml.append(f'{bytable[b]["entity"]} {mult} '+('--' if kind=='FK JPA' else '..')+f' "0..*" {bytable[a]["entity"]} : {c}')
puml+=['note bottom of MensajeEntity','  Emisor como correo/nombre, sin FK JPA.','end note','note bottom of ActividadEntity','  ID_USUARIO: texto; ID numérico o correo.','end note','legend bottom','  Continua = FK declarada por JPA, NO comprobada en Oracle.','  Discontinua = referencia lógica; cardinalidad de aplicación.','  Se muestran claves; diccionario CSV/HTML contiene todas las columnas.','endlegend','@enduml']
(OUT/'11_Datos/erd_completo.puml').write_text('\n'.join(puml),encoding='utf-8')
print(json.dumps(dict(entidades=21,columnas=len(rows),unique=len(urows),relaciones=len(rels),fk_jpa=2,puentes=7)))
