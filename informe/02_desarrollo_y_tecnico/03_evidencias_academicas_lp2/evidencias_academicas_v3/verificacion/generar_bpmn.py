from documentos import *
import xml.etree.ElementTree as ET
import textwrap
B='http://www.omg.org/spec/BPMN/20100524/MODEL';DI='http://www.omg.org/spec/BPMN/20100524/DI';DC='http://www.omg.org/spec/DD/20100524/DC';D='http://www.omg.org/spec/DD/20100524/DI'
for prefix,ns in [('bpmn',B),('bpmndi',DI),('dc',DC),('di',D)]:ET.register_namespace(prefix,ns)
def tag(ns,x):return '{'+ns+'}'+x
def render(path):
    root=ET.parse(path).getroot();ids={e.get('id'):e for e in root.iter() if e.get('id')}
    shapes=root.findall('.//'+tag(DI,'BPMNShape')); edges=root.findall('.//'+tag(DI,'BPMNEdge'))
    boxes={e.get('bpmnElement'):[float(e.find(tag(DC,'Bounds')).get(k)) for k in ['x','y','width','height']] for e in shapes}
    maxx=max(x+w for x,y,w,h in boxes.values())+30; maxy=max(y+h for x,y,w,h in boxes.values())+55
    svg=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{maxx}" height="{maxy}" viewBox="0 0 {maxx} {maxy}"><style>text{{font:14px Segoe UI,sans-serif;fill:#14344d}}</style><defs><marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="3" orient="auto"><path d="M0,0 L9,3 L0,6 z" fill="#36566b"/></marker><marker id="open" markerWidth="12" markerHeight="10" refX="10" refY="4" orient="auto"><path d="M1,0 L10,4 L1,8" fill="none" stroke="#36566b"/></marker></defs><rect width="100%" height="100%" fill="white"/>']
    def text(x,y,name,width=28):
        lines=[]
        for line in name.splitlines():lines.extend(textwrap.wrap(line,width) or [''])
        return '<text text-anchor="middle">'+''.join(f'<tspan x="{x}" y="{y+(i-(len(lines)-1)/2)*18}">{escape(l)}</tspan>' for i,l in enumerate(lines))+'</text>'
    # Containers first, then connectors, then nodes.
    for sh in shapes:
        id=sh.get('bpmnElement');e=ids[id];kind=e.tag.split('}')[-1]
        if kind not in ['participant','lane']:continue
        x,y,w,h=boxes[id]
        svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="'+('#eef4f9' if kind=='lane' else '#fff')+'" stroke="#547186"/>')
        if kind=='participant' and 'processRef' in e.attrib:svg.append(text(x+20,y+30,'Equipo',7))
        else:svg.append(text(x+w/2,y+23,e.get('name',''),50))
    for ed in edges:
        eid=ed.get('bpmnElement'); e=ids[eid]; pts=[(float(q.get('x')),float(q.get('y'))) for q in ed.findall(tag(D,'waypoint'))]
        kind=e.tag.split('}')[-1];dashed=kind in ['messageFlow','association']
        svg.append('<polyline points="'+' '.join(f'{x},{y}' for x,y in pts)+'" fill="none" stroke="#36566b" stroke-width="1.6" '+('stroke-dasharray="6 4" ' if dashed else '')+'marker-end="url(#'+('open' if kind=='messageFlow' else 'arrow')+')"/>')
        if e.get('name'):
            x,y=pts[len(pts)//2];svg.append(text(x+12,y-10,e.get('name'),22))
    for sh in shapes:
        id=sh.get('bpmnElement');e=ids[id];kind=e.tag.split('}')[-1];x,y,w,h=boxes[id]
        if kind in ['participant','lane']:continue
        if kind.endswith('Event'):
            svg.append(f'<circle cx="{x+w/2}" cy="{y+h/2}" r="{w/2}" fill="white" stroke="#36566b" stroke-width="'+('4' if kind=='endEvent' else '1.8')+'"/>')
            if e.find(tag(B,'messageEventDefinition')) is not None:svg.append(f'<rect x="{x+8}" y="{y+12}" width="20" height="13" fill="none" stroke="#36566b"/><path d="M{x+8},{y+12} l10,7 l10,-7" fill="none" stroke="#36566b"/>')
            svg.append(text(x+w/2,y+h+22,e.get('name',''),25))
        elif 'Gateway' in kind:
            svg.append(f'<polygon points="{x+w/2},{y} {x+w},{y+h/2} {x+w/2},{y+h} {x},{y+h/2}" fill="#fff5d7" stroke="#36566b"/>')
            svg.append(text(x+w/2,y+h/2+5,'X',4));svg.append(text(x+w/2,y-18,e.get('name',''),25))
        elif kind=='textAnnotation':
            svg.append(f'<path d="M{x+12},{y} H{x} V{y+h} H{x+12}" fill="none" stroke="#36566b"/>');svg.append(text(x+w/2,y+h/2,e.findtext(tag(B,'text'),''),38))
        else:
            svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="10" fill="white" stroke="#36566b"/>');svg.append(text(x+w/2,y+h/2+5,e.get('name',''),max(12,int(w/8))))
            # Miniaturas vectoriales de tarea: persona o engranaje abstracto.
            if kind=='userTask':svg.append(f'<circle cx="{x+14}" cy="{y+12}" r="4" fill="none" stroke="#36566b"/><path d="M{x+8},{y+26} v-5 q6,-9 12,0 v5" fill="none" stroke="#36566b"/>')
            elif kind=='serviceTask':svg.append(f'<circle cx="{x+15}" cy="{y+16}" r="7" fill="none" stroke="#36566b"/><path d="M{x+15},{y+5} v22 M{x+4},{y+16} h22" stroke="#36566b"/>')
    svg+=['</svg>'];path.with_suffix('.svg').write_text(''.join(svg),encoding='utf-8')

def commercial():
    root=ET.Element(tag(B,'definitions'),id='Defs_Comercial',targetNamespace='https://viernes.local/documentacion',exporter='Evidencias Viernes',exporterVersion='3')
    coll=ET.SubElement(root,tag(B,'collaboration'),id='Collab')
    ET.SubElement(coll,tag(B,'participant'),id='ClienteExterno',name='Cliente potencial — comunicación externa al CRM')
    ET.SubElement(coll,tag(B,'participant'),id='Equipo',name='Equipo comercial / Viernes CRM',processRef='ProcesoComercial')
    ET.SubElement(coll,tag(B,'messageFlow'),id='MensajeNecesidad',name='Necesidad (canal externo)',sourceRef='ClienteExterno',targetRef='Inicio')
    proc=ET.SubElement(root,tag(B,'process'),id='ProcesoComercial',name='TO-BE comercial — registro humano y apoyo CRM',isExecutable='false')
    ET.SubElement(proc,tag(B,'documentation')).text='Modelo analítico, no workflow ejecutable. Los registros y decisiones de negocio no implican que exista automatización o comunicación externa implementada. Lead CONVERTIDO solo cambia estado.'
    ls=ET.SubElement(proc,tag(B,'laneSet'),id='Lanes')
    lanes={n:ET.SubElement(ls,tag(B,'lane'),id=n,name=name) for n,name in [('Vendedor','Vendedor / usuario'),('CRM','CRM — apoyo del sistema'),('Jefe','JEFE / ADMIN')]}
    nodes=[('Inicio','startEvent','Necesidad detectada','Vendedor',200,125,36,36),('Lead','userTask','Registrar lead y fuente opcional','Vendedor',95,225,245,70),('Calificar','userTask','Contactar y evaluar lead; actualizar estado','Vendedor',95,345,245,80),('Viable','exclusiveGateway','¿Continuar gestión?','Vendedor',190,490,55,55),('Descartar','serviceTask','Guardar DESCARTADO solicitado por usuario','CRM',435,485,245,70),('FinDesc','endEvent','Gestión descartada','CRM',535,625,36,36),('Registrar','userTask','Registrar/seleccionar cliente y oportunidad por acción del usuario','Vendedor',95,625,245,90),('Preparar','userTask','Preparar propuesta y al menos un detalle','Vendedor',95,780,245,80),('Validar','serviceTask','Validar DTO, referencias, fechas y detalle','CRM',435,780,245,80),('Valido','exclusiveGateway','¿Solicitud válida?','CRM',530,945,55,55),('Error','userTask','Corregir datos y reenviar solicitud','Vendedor',95,950,245,80),('Guardar','serviceTask','Numerar, calcular y guardar BORRADOR','CRM',435,1115,245,80),('Enviar','userTask','Solicitar enviar BORRADOR (o conservarlo)','Vendedor',95,1115,245,80),('Envio','exclusiveGateway','¿Enviar ahora?','Vendedor',190,1270,55,55),('FinBorrador','endEvent','Borrador conservado','CRM',535,1280,36,36),('EstadoEnviada','serviceTask','Validar BORRADOR; guardar ENVIADA','CRM',435,1420,245,80),('Revisar','userTask','Revisar propuesta y elegir decisión','Jefe',775,1420,245,80),('Decision','exclusiveGateway','¿Decisión?','Jefe',870,1585,55,55),('Aprobar','userTask','Solicitar aprobar ENVIADA; API guarda APROBADA','Jefe',775,1740,245,80),('Rechazar','serviceTask','Rechazar: exigir ENVIADA; guardar RECHAZADA','CRM',435,1590,245,80),('Cancelar','serviceTask','Cancelar: exigir estado no terminal; guardar CANCELADA','CRM',435,1740,245,80),('FinRechazo','endEvent','Propuesta rechazada','Vendedor',200,1600,36,36),('FinCanc','endEvent','Propuesta cancelada','Vendedor',200,1760,36,36),('Cierre','userTask','Actualizar oportunidad por decisión comercial independiente','Jefe',775,1920,245,80),('Fin','endEvent','Gestión documentada','Jefe',880,2080,36,36)]
    els={};boxes={}
    for id,kind,name,l,x,y,w,h in nodes:
        el=ET.SubElement(proc,tag(B,kind),id=id,name=name);els[id]=el;boxes[id]=(x,y,w,h);ET.SubElement(lanes[l],tag(B,'flowNodeRef')).text=id
        if id=='Inicio':ET.SubElement(el,tag(B,'messageEventDefinition'),id='MessageStart')
    flows=[('Inicio','Lead',''),('Lead','Calificar',''),('Calificar','Viable',''),('Viable','Descartar','No'),('Descartar','FinDesc',''),('Viable','Registrar','Sí'),('Registrar','Preparar',''),('Preparar','Validar',''),('Validar','Valido',''),('Valido','Error','No: 400/404'),('Error','Preparar','Corregir'),('Valido','Guardar','Sí'),('Guardar','Enviar',''),('Enviar','Envio',''),('Envio','FinBorrador','No'),('Envio','EstadoEnviada','Sí'),('EstadoEnviada','Revisar',''),('Revisar','Decision',''),('Decision','Aprobar','Aprobar'),('Decision','Rechazar','Rechazar'),('Decision','Cancelar','Cancelar'),('Rechazar','FinRechazo',''),('Cancelar','FinCanc',''),('Aprobar','Cierre',''),('Cierre','Fin','')]
    di=ET.SubElement(root,tag(DI,'BPMNDiagram'),id='Diagrama');plane=ET.SubElement(di,tag(DI,'BPMNPlane'),id='Plano',bpmnElement='Collab')
    for id,(x,y,w,h) in dict(ClienteExterno=(40,10,1020,65),Equipo=(5,85,1055,2110),Vendedor=(40,85,340,2110),CRM=(380,85,340,2110),Jefe=(720,85,340,2110),**boxes).items():
        sh=ET.SubElement(plane,tag(DI,'BPMNShape'),id='DI_'+id,bpmnElement=id,isHorizontal='false');ET.SubElement(sh,tag(DC,'Bounds'),x=str(x),y=str(y),width=str(w),height=str(h))
    def edge(id,pts):
        ed=ET.SubElement(plane,tag(DI,'BPMNEdge'),id='DI_'+id,bpmnElement=id)
        for x,y in pts:ET.SubElement(ed,tag(D,'waypoint'),x=str(x),y=str(y))
    edge('MensajeNecesidad',[(218,75),(218,125)])
    for i,(a,b,name) in enumerate(flows):
        id='F'+str(i+1);ET.SubElement(proc,tag(B,'sequenceFlow'),id=id,sourceRef=a,targetRef=b,**({'name':name} if name else {}))
        # Incoming/outgoing precede event definitions in XSD ordering.
        ie=ET.Element(tag(B,'outgoing'));ie.text=id;els[a].insert(0,ie)
        ie=ET.Element(tag(B,'incoming'));ie.text=id;els[b].insert(0,ie)
        x,y,w,h=boxes[a];X,Y,W,H=boxes[b]
        if abs(x-X)<60:pts=[(x+w/2,y+h),(X+W/2,Y)]
        elif Y<y-60:pts=[(x,y+h/2),(65,y+h/2),(65,Y+H/2),(X,Y+H/2)]
        else:pts=[(x+w if X>x else x,y+h/2),(X if X>x else X+W,Y+H/2)]
        if (a,b)==('Decision','Cancelar'):pts=[(x,y+h/2),(705,y+h/2),(705,Y+H/2),(X+W,Y+H/2)]
        edge(id,pts)
    # Canonical incoming before outgoing then definitions, preserving schema order.
    for el in els.values():
        children=list(el);el[:]=sorted(children,key=lambda c:0 if c.tag==tag(B,'incoming') else 1 if c.tag==tag(B,'outgoing') else 2)
    # All exclusive branches are explicit named business choices; non-executable analytical model.
    path=OUT/'06_BPMN/01_comercial_to_be.bpmn';ET.indent(root);ET.ElementTree(root).write(path,encoding='utf-8',xml_declaration=True);render(path)
commercial()
# Reuse the more detailed proposal process, with small semantic corrections.
p=Path('E:/Viernes/entrega_analisis_v2/bpmn/01_bpmn_to_be_propuesta.bpmn');root=ET.parse(p).getroot()
for e in root.iter():
    if e.tag==tag(B,'manualTask'):e.tag=tag(B,'userTask')
    if e.text:
        e.text=e.text.replace('(commit 298ac64)','(árbol de trabajo revisado 11/11/2026)').replace('construirDetalles()','toDetalleDomain()').replace('Tarea manual del vendedor.','Acción del vendedor en la aplicación.').replace('La conversión lead → oportunidad es manual.','No existe conversión automática lead → oportunidad.').replace('La UI muestra el error 400/404','La API devuelve error 400/404')
    if e.tag==tag(B,'sequenceFlow') and e.get('id') in ['Flow_Valido_No','Flow_Enviar_No']:
        for c in list(e):
            if c.tag==tag(B,'conditionExpression'):e.remove(c) # default flows must not have conditions
# BPMN XSD exige todos los flowElement antes de artifacts en process.
for process in root.findall(tag(B,'process')):
    artifacts=[e for e in list(process) if e.tag in [tag(B,'textAnnotation'),tag(B,'association'),tag(B,'group')]]
    for e in artifacts: process.remove(e)
    for e in artifacts: process.append(e)
out=OUT/'06_BPMN/02_propuesta_detallada.bpmn';ET.indent(root);ET.ElementTree(root).write(out,encoding='utf-8',xml_declaration=True);render(out)
body='''<p class="aviso">TO-BE analítico fundamentado en capacidades del código. No es una ejecución de un motor BPMN, una observación del AS-IS ni una aprobación de usuarios. Los archivos son BPMN 2.0 XML con elementos semánticos y BPMNDI, no diagramas Mermaid rebautizados.</p>
<h2>06.1 Proceso comercial TO-BE</h2><p>La comunicación de una necesidad ocurre fuera del CRM (participante cliente potencial). El equipo comercial registra el lead, realiza contacto y calificación como trabajo humano, decide si continúa y registra o selecciona separadamente cliente y oportunidad. Marcar un lead CONVERTIDO no crea estos registros. Desde una oportunidad existente se prepara una propuesta, se valida y se conserva un borrador o se solicita su envío. JEFE/ADMIN puede aprobar, rechazar o cancelar según estado. El cierre de la oportunidad es una decisión posterior e independiente.</p><p>Los carriles expresan responsabilidad analítica, no una orquestación implementada. Las tareas de servicio muestran apoyo automático del backend invocado por una acción HTTP. “Enviar” solo actualiza ENVIADA: no está acreditado un canal de entrega por correo. El TO-BE propone un recorrido de trabajo; las etapas del lead/oportunidad no están impuestas por el servicio.</p>
<p><a href="01_comercial_to_be.bpmn">BPMN editable</a> · <a href="01_comercial_to_be.svg">SVG comercial</a></p><figure><img src="01_comercial_to_be.svg" alt="BPMN comercial TO-BE"></figure>
<h2>06.2 Detalle de propuestas reutilizado</h2><p>Se conserva la estructura del BPMN anterior y se corrige manualTask por userTask: registrar una oportunidad en el sistema es una acción humana asistida por software. Se corrige el helper de construcción de detalle y las condiciones en flujos por defecto. El subproceso representa el camino habitual de revisión, no todos los endpoints: cancelar también se admite desde BORRADOR y la eliminación por ADMIN no se dibuja aquí. Errores 401/403, conflicto 409 o disponibilidad interrumpen la operación; no hay reintentos automáticos modelados.</p><p><a href="02_propuesta_detallada.bpmn">BPMN editable</a> · <a href="02_propuesta_detallada.svg">SVG de propuesta</a></p><figure><img src="02_propuesta_detallada.svg" alt="BPMN detallado de propuesta"></figure>
<h2>06.3 Elementos BPMN y fundamento</h2>'''
body+=table(['Elemento','Interpretación'],[['Participantes','Cliente potencial (caja negra) y equipo/CRM (proceso).'],['Carriles','Vendedor/usuario, CRM y JEFE/ADMIN. No son servicios distribuidos.'],['Eventos','Inicio por necesidad externa y finales por descarte, borrador o resolución de propuesta.'],['Tareas de usuario','Decisiones y registros explícitos; no se asume conversión automática.'],['Tareas de servicio','Validación, cálculos y persistencia invocados por la aplicación.'],['Compuertas exclusivas','Continuar, validez de solicitud, enviar y decisión. Las ramas son opciones mutuamente excluyentes.'],['Flujo de mensaje','Necesidad del cliente por canal externo, no integración implementada.'],['Flujo de secuencia','Orden TO-BE dentro del participante equipo.']])
body+='<p>'+ref('leads/application/service/LeadService.java',66)+'<br>'+ref('oportunidades/application/service/OportunidadService.java',69)+'<br>'+ref('propuestas/application/service/PropuestaService.java',118)+'<br>'+ref('auth/keycloak/KeycloakSecurityConfig.java',127)+'</p>'
body+='''<h2>06.4 AS-IS: exclusivamente plantilla de levantamiento</h2><p>No existe evidencia suficiente para dibujar el proceso actual de la organización. Por ello no se presenta un AS-IS inventado ni se confunde el código con una observación operativa. El equipo debe completar la <a href="levantamiento_as_is.csv">plantilla editable</a> y adjuntar evidencia antes de diagramarlo.</p>'''
questions=[('Inicio y final','¿Qué hecho inicia el caso y cuándo se considera cerrado?','Caso real anonimizado y responsable'),('Participantes','¿Quién registra, valida, aprueba, entrega y recibe?','Entrevistas y responsabilidades'),('Secuencia actual','¿Qué pasos se realizan realmente, incluso fuera del CRM?','Observación y documentos utilizados'),('Canales','¿Correo, llamada, Excel, papel o aplicación?','Ejemplos anonimizados'),('Decisiones','¿Quién decide calificación, precio, aprobación y excepciones?','Regla vigente y autoridad'),('Lead y oportunidad','¿Se transcriben datos? ¿Quién registra cada entidad?','Recorrido de un caso'),('Errores y retrabajo','¿Qué se devuelve, corrige, cancela o reabre?','Incidencias y motivos'),('Tiempos y volumen','¿Cuánto dura y cuántos casos se tramitan?','Muestra, período y método'),('Datos y privacidad','¿Qué campos son imprescindibles y quién accede?','Formatos sin datos personales'),('Validación','¿Quién revisó y aprobó el levantamiento y en qué fecha?','Acta o confirmación verificable')]
csvfile('06_BPMN/levantamiento_as_is.csv',['Tema','Pregunta','Evidencia requerida','Respuesta del equipo','Responsable','Fecha'],[list(q)+['','',''] for q in questions])
body+=table(['Tema','Información a aportar','Soporte'],questions)
body+='<p>Regla de cierre: identificar fuentes, contrastar variantes con los actores, separar hechos de propuestas y obtener revisión documentada. Solo entonces crear el BPMN AS-IS. La plantilla antigua con campos “completar” se mantiene en entrega_analisis_v2 como antecedente, no como evidencia de un proceso observado.</p>'
page('06_BPMN/06_BPMN.html','06. BPMN',body)
print('2 BPMN semánticos y SVG; plantilla AS-IS generados')
