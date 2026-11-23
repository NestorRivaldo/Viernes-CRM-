from documentos import *
from html.parser import HTMLParser
from urllib.parse import urlparse, unquote
import xml.etree.ElementTree as ET
from PIL import Image
from collections import Counter
import subprocess, zipfile
sections=[('06_BPMN','06_BPMN.html','06. BPMN'),('07_UML','07_UML.html','07. UML'),('11_Datos','11_Datos.html','11. Datos'),('05_Criterios_aceptacion','05_Criterios_aceptacion.html','05. Criterios de aceptación'),('12_Pruebas','12_Pruebas.html','12. Pruebas')]
intro='''<p>Continuación de entrega_analisis_v2, conservada sin sobrescribir. Se revisaron los entregables pendientes contra el árbol real; no se repitió la auditoría general ni se implementaron funcionalidades.</p><p class="aviso"><strong>Pendiente académico:</strong> los enunciados originales de HU-004, HU-005, HU-008, HU-016, HU-017 y del resto del catálogo no se localizaron. Se entregan 20 historias derivadas y 60 escenarios propuestos, pero no se inventa correspondencia ni aprobación. AS-IS se entrega solo como plantilla, tal como pide el encargo.</p>'''
nav='<nav>'+''.join('<p><a href="'+d+'/'+f+'">'+title+'</a></p>' for d,f,title in sections)+'</nav>'
summary=table(['Sección','Antes','Entregable de esta continuación','Estado'],[['06 BPMN','Propuesta exportada y plantilla sin levantamiento','TO-BE comercial + detalle, dos BPMN 2.0/SVG, plantilla AS-IS CSV','BPMN validado contra XSD oficial; AS-IS real no afirmado'],['07 UML','8 fuentes e imágenes, con errores semánticos','8 PlantUML revisados, 8 SVG y 8 PNG; explicaciones con código','Seis bloques cubiertos; estados desglosados en tres'],['11 Datos','Inventarios parciales','Diccionario JPA 152 campos y conciliación Oracle 194 columnas; ERD físicos; 7 puentes','Oracle: 26 tablas, 22 FK y 51 índices; distinguir de 21 entidades'],['05 Criterios','Sin catálogo original localizable','20 HU-D / 60 escenarios; registro de las 5 HU pendientes','Propuesto; correspondencia original y validación pendientes'],['12 Pruebas','Conteos y ejecución disponible','Inventario 291 tests / 32 clases; matriz 60 casos; extracto y hashes','291 PASS previos; 12 vínculos puntuales revisados; no UAT']])
notes='''<h2>Hallazgos que cambian la interpretación</h2><p>Oracle tiene cinco tablas CRM no mapeadas y siete columnas adicionales en tablas mapeadas. Las siete tablas puente sí tienen FK físicas en el catálogo, aunque JPA use IDs escalares. La documentación anterior que deducía “sin FK física” a partir del código no debe usarse como prueba del esquema.</p><p>La ejecución Maven previa de 291 tests incluyó contextLoads con conexión Oracle y perfil dev/keycloak. Como dev tiene ddl-auto:update, no se puede certificar esa ejecución como solo lectura ni asegurar que no emitiera DDL. No se repitió. Las nuevas extracciones de metadatos sí se limitaron a transacciones READ ONLY, SELECT y ROLLBACK.</p><h2>Cómo utilizar la entrega</h2><p>Abra las secciones HTML para copiar texto y tablas a Word. <a href="Informe_academico_consolidado.html">Informe HTML consolidado</a> y <a href="Texto_para_Word.txt">texto plano consolidado</a>. Los CSV son editables y cada UML/ERD incluye PlantUML; los procesos se abren como .bpmn en un modelador compatible. Los SVG se conservan vectoriales para ampliar sin perder legibilidad. No escale los diagramas grandes a una sola página A4 sin dividirlos.</p><h2>Revisión y procedencia</h2><p>Ver <a href="verificacion/resultado_verificacion.json">resultado de verificaciones</a> y <a href="verificacion/manifest_entrega.json">manifest de archivos</a>. Se verifican XML BPMN contra esquemas OMG, integridad de referencias y flujos, anclas locales, imágenes, cardinalidades de inventario y correspondencia de JUnit. No se ha abierto este texto en Microsoft Word ni probado importación en cada modelador BPMN.</p><p>Esquemas oficiales utilizados: <a href="https://www.omg.org/spec/BPMN/20100501/BPMN20.xsd">BPMN20.xsd (OMG)</a>. Render UML/ERD: PlantUML 1.2026.2 con layout local; no se subió código a un servidor de renderizado.</p>'''
page('index.html','VIERNES CRM V1 — Evidencias académicas pendientes',intro+nav+summary+notes)
combined=intro+summary
for d,f,title in sections:
    raw=(OUT/d/f).read_text(encoding='utf-8');inner=raw.split('<main>',1)[1].split('</main>',1)[0]
    # Relative assets now point from the package root.
    def prefix(m):
        attr,url=m.groups()
        if url.startswith(('file:','http:','https:','#')):return m.group(0)
        return attr+'="'+d+'/'+url+'"'
    inner=re.sub(r'(href|src)="([^"]+)"',prefix,inner)
    combined+='<section>'+inner+'</section>'
page('Informe_academico_consolidado.html','Informe académico consolidado',combined)
class Plain(HTMLParser):
    def __init__(self):super().__init__();self.skip=0;self.parts=[]
    def handle_starttag(self,tag,attrs):
        if tag in ['style','script','title']:self.skip+=1
        if tag in ['h1','h2','h3','p','tr','li','section','br']:self.parts.append('\n')
        if tag=='td':self.parts.append('\t')
    def handle_endtag(self,tag):
        if tag in ['style','script','title']:self.skip-=1
        if tag in ['h1','h2','h3','p','tr','li']:self.parts.append('\n')
    def handle_data(self,data):
        if not self.skip:self.parts.append(data)
plain=Plain();plain.feed((OUT/'Informe_academico_consolidado.html').read_text(encoding='utf-8'))
(OUT/'Texto_para_Word.txt').write_text(re.sub(r'\n{3,}','\n\n',''.join(plain.parts)),encoding='utf-8-sig')
# Validation: local anchors, images, documents, schema consistency, test evidence.
class Links(HTMLParser):
    def __init__(self):super().__init__();self.links=[];self.refs=[]
    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        for key in ['href','src']:
            if key in a:self.links.append(a[key])
        if 'data-line' in a:self.refs.append((a['href'],int(a['data-line'])))
checks=[];source_paths=set()
def ok(name,details):checks.append(dict(check=name,result='PASS',details=details))
for p in OUT.rglob('*.html'):
    parser=Links();parser.feed(p.read_text(encoding='utf-8'))
    for url in parser.links:
        if url.startswith(('http:','https:','#')):continue
        if url.startswith('file:'):target=Path(unquote(urlparse(url).path).lstrip('/'))
        else:target=p.parent/unquote(url)
        # Two validation outputs are written later in this script.
        if target.name in ['resultado_verificacion.json','manifest_entrega.json']:continue
        assert target.exists(), (p,url)
    for url,line in parser.refs:
        q=Path(unquote(urlparse(url).path).lstrip('/'));source_paths.add(q)
        assert line<=len(q.read_text(encoding='utf-8-sig').splitlines()),(q,line)
ok('HTML y anclas locales',str(len(list(OUT.rglob('*.html'))))+' documentos; fuentes existentes y líneas dentro de rango')
for p in OUT.rglob('*.svg'):
    r=ET.parse(p).getroot();text=' '.join(r.itertext());assert 'Syntax Error' not in text and 'An error has occured' not in text,p
    assert r.tag.endswith('svg') and r.get('viewBox'),p
for p in (OUT/'07_UML').glob('*.png'):
    with Image.open(p) as im:im.verify()
assert len(list((OUT/'07_UML').glob('*.puml')))==len(list((OUT/'07_UML').glob('*.svg')))==len(list((OUT/'07_UML').glob('*.png')))==8
ok('UML, ERD e imágenes','8 tríos PlantUML/SVG/PNG; SVG XML válidos sin mensajes de error; PNG decodificables')
B='{http://www.omg.org/spec/BPMN/20100524/MODEL}';DI='{http://www.omg.org/spec/BPMN/20100524/DI}'
for p in (OUT/'06_BPMN').glob('*.bpmn'):
    r=ET.parse(p).getroot();ids=[e.get('id') for e in r.iter() if e.get('id')];assert len(ids)==len(set(ids)),p
    idset=set(ids);byid={e.get('id'):e for e in r.iter() if e.get('id')}
    for e in r.iter():
        for attr in ['sourceRef','targetRef','processRef','bpmnElement','default']:
            if e.get(attr):assert e.get(attr) in idset,(p,e.get(attr))
        if e.tag in [B+'incoming',B+'outgoing',B+'flowNodeRef']:assert e.text in idset,(p,e.text)
    for proc in r.findall(B+'process'):
        nodes={e.get('id') for e in proc if e.tag in [B+x for x in ['startEvent','endEvent','userTask','serviceTask','manualTask','exclusiveGateway']]}
        flows=proc.findall(B+'sequenceFlow');adj={n:[] for n in nodes}
        for f in flows:
            a,b=f.get('sourceRef'),f.get('targetRef');assert a in nodes and b in nodes
            adj[a].append(b)
            assert f.get('id') in [x.text for x in byid[a].findall(B+'outgoing')]
            assert f.get('id') in [x.text for x in byid[b].findall(B+'incoming')]
        queue=[n for n in nodes if byid[n].tag==B+'startEvent'];seen=set(queue)
        while queue:
            n=queue.pop()
            for x in adj[n]:
                if x not in seen:seen.add(x);queue.append(x)
        assert seen==nodes, (p,nodes-seen)
        ends={n for n in nodes if byid[n].tag==B+'endEvent'}
        for n in nodes:
            reach={n};q=[n]
            while q:
                for x in adj[q.pop()]:
                    if x not in reach:reach.add(x);q.append(x)
            assert reach & ends, (p,n,'no alcanza fin')
        shapes={e.get('bpmnElement') for e in r.iter(DI+'BPMNShape')}
        assert nodes<=shapes,(p,nodes-shapes)
    ok('BPMN integridad '+p.name,'IDs únicos; referencias resueltas; flujos coherentes; todos los nodos alcanzables y con ruta a fin; DI presente')
cmd=['java',str(OUT/'verificacion/ValidarBpmn.java'),str(OUT/'verificacion/BPMN20.xsd')]+[str(p) for p in sorted((OUT/'06_BPMN').glob('*.bpmn'))]
r=subprocess.run(cmd,text=True,capture_output=True,encoding='utf-8',errors='replace',timeout=90);assert r.returncode==0,r.stderr
(OUT/'verificacion/validacion_xsd.txt').write_text(r.stdout,encoding='utf-8');ok('XSD OMG BPMN20',r.stdout.strip())
with (OUT/'12_Pruebas/inventario_291_pruebas.csv').open(encoding='utf-8-sig',newline='') as f:tests=list(csv.DictReader(f))
assert len(tests)==291 and all(r['Resultado observado']=='PASS' for r in tests)
with (OUT/'12_Pruebas/matriz_RF_HU_CP.csv').open(encoding='utf-8-sig',newline='') as f:matrix=list(csv.DictReader(f))
assert len(matrix)==60 and sum(r['Estado'].startswith('EXISTENTE') for r in matrix)==12
ok('Pruebas y matriz','291 PASS preservados; 60 casos; 12 vínculos revisados; no se ejecutó Maven de nuevo')
for name,total in [('oracle_columnas.csv',194),('diccionario_conciliado.csv',152),('oracle_checks.csv',148),('columnas_no_mapeadas.csv',7)]:
    with (OUT/'11_Datos'/name).open(encoding='utf-8-sig',newline='') as f:rows=list(csv.DictReader(f,strict=True))
    assert len(rows)==total,(name,len(rows))
ok('Diccionario y conciliación','21 entidades / 152 columnas JPA; 26 tablas / 194 columnas Oracle; 7 columnas extra en tablas mapeadas')
(OUT/'verificacion/fuentes_sha256.json').write_text(json.dumps([dict(archivo=p.relative_to(ROOT).as_posix(),sha256=digest(p)) for p in sorted(source_paths)],indent=2),encoding='utf-8')
report=dict(fecha='2026-11-11',checks=checks,limites=['Enunciados RF/HU originales no disponibles; 5 HU pendientes de correspondencia.','No hay aprobación UAT ni AS-IS real.','No se abrió en Microsoft Word ni en todos los modeladores BPMN.','La ejecución Maven anterior con ddl-auto:update no es certificable como estrictamente de solo lectura.','El chequeo de anclas valida existencia y rango; las referencias principales también se contrastaron por lectura.'])
(OUT/'verificacion/resultado_verificacion.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
files=[p for p in OUT.rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix!='.jar' and p.name!='manifest_entrega.json']
manifest=[dict(archivo=p.relative_to(OUT).as_posix(),bytes=p.stat().st_size,sha256=digest(p)) for p in sorted(files)]
(OUT/'verificacion/manifest_entrega.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
zip_path=OUT.parent/'evidencias_academicas_v3.zip'
with zipfile.ZipFile(zip_path,'w',compression=zipfile.ZIP_DEFLATED) as z:
    for p in sorted(OUT.rglob('*')):
        if p.is_file() and '__pycache__' not in p.parts and p.suffix!='.jar':z.write(p,OUT.name+'/'+p.relative_to(OUT).as_posix())
with zipfile.ZipFile(zip_path) as z:assert z.testzip() is None
print(json.dumps(dict(verificaciones=len(checks),archivos=len(manifest),zip=str(zip_path),bytes_zip=zip_path.stat().st_size),ensure_ascii=False))
