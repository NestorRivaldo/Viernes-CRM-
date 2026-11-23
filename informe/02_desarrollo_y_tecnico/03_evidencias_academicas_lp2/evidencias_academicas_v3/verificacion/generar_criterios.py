from documentos import *
# IDs D = documentación derivada; NO sustituyen los identificadores académicos originales.
C=[]
def add(code,title,actor,source,line,pos,neg,alt):
    C.append(dict(code=code,title=title,actor=actor,source=source,line=line,scenarios=[pos,neg,alt]))
add('01','Autorizar operaciones por rol','Usuario autenticado','auth/keycloak/KeycloakSecurityConfig.java',121,
('un JWT válido de ADMIN','se solicita escribir usuarios','la regla de seguridad permite continuar; todavía aplican validaciones de negocio'),
('un JWT de VENDEDOR y perfil keycloak','se solicita aprobar una propuesta','se rechaza con 403, sin ejecutar la operación protegida'),
('una petición a una API protegida sin JWT válido','se solicita acceso','se responde 401; los endpoints públicos configurados son excepción'))
add('02','Administrar usuarios CRM','ADMIN','usuarios/application/service/UsuarioService.java',47,
('correo no registrado y datos válidos','se crea un usuario CRM','se guarda ACTIVO con passwordHash codificado; no se presupone alta en Keycloak'),
('correo ya registrado o contraseña vacía al crear','se solicita el alta','se rechaza con IllegalArgumentException (400 por handler)'),
('usuario existente','se actualiza sin una nueva contraseña','se conserva el hash anterior y se actualiza la fecha de modificación'))
add('03','Registrar y mantener clientes','Usuario autenticado','clientes/application/service/ClienteService.java',79,
('datos válidos y documento no duplicado','se crea el cliente sin estado explícito','se guarda ACTIVO y con fecha de creación'),
('un documento ya registrado','se solicita crear otro cliente con ese número','se rechaza la duplicidad según validación del servicio'),
('ningún tipo ni número de documento','se crea un cliente con el resto de datos válidos','se permite el registro sin consulta externa; desactivar es distinto de eliminar'))
add('04','Mantener contactos de cliente','Usuario autenticado','clientes/application/service/ClienteService.java',119,
('cliente existente y contacto válido','se agrega el contacto','se guarda dentro de la colección del cliente y se devuelve el contacto'),
('contacto que no pertenece al cliente solicitado','se intenta actualizar','se devuelve 404 y no se modifica un contacto ajeno'),
('contacto existente y principal omitido en actualización','se actualizan sus otros datos','se conserva el valor anterior de principal'))
add('05','Consultar DNI/RUC de forma controlada','Usuario autenticado','clientes/application/service/ClienteService.java',67,
('DNI/RUC válido y proveedor simulado con datos','se solicita consultar documento','el backend devuelve el DTO de consulta; no crea el cliente automáticamente'),
('número incompatible con el tipo','se solicita consultar','se rechaza antes de consumir el puerto externo'),
('consulta repetida cubierta por caché','se consulta la misma clave dentro del TTL','el proxy de caché puede reutilizar respuesta; el 404 negativo dura 10 min, no es una aprobación de la identidad'))
add('06','Registrar leads y cambiar estado','Usuario autenticado','leads/application/service/LeadService.java',47,
('datos válidos sin estado','se crea un lead','el estado inicial es NUEVO'),
('fuenteId inexistente','se registra o actualiza el lead','se rechaza con ResourceNotFoundException (404)'),
('lead existente incluso DESCARTADO','se solicita estado CONVERTIDO u otro enum válido','se asigna ese estado sin crear oportunidad; no hay guardas de ciclo'))
add('07','Mantener fuentes de captación','JEFE / ADMIN','leads/application/service/FuenteService.java',39,
('código único y datos válidos','se crea una fuente sin activa explícita','se guarda activa=true'),
('código ya existente','se solicita crear otra fuente','se rechaza por duplicidad en el servicio'),
('fuente existente','se cambia activa=false mediante actualización','se conserva el registro, distinto de eliminarlo físicamente'))
add('08','Registrar oportunidades y etapas','Usuario autenticado','oportunidades/application/service/OportunidadService.java',69,
('datos válidos sin etapa','se crea una oportunidad','se guarda PROSPECCION; no se exige que provenga de un lead'),
('ID de oportunidad inexistente','se solicita cambiar etapa','se devuelve 404'),
('oportunidad GANADA o PERDIDA','se solicita otra etapa válida','se permite reapertura y no se recalcula probabilidad por la etapa'))
add('09','Asociar contactos, productos y colaboradores','Usuario autenticado','oportunidades/application/service/OportunidadService.java',108,
('oportunidad y contacto existentes no asociados','se agrega el contacto','se guarda rol y esDecisor (false por defecto)'),
('el mismo par oportunidad-contacto ya existe','se intenta agregar nuevamente','se rechaza con 400; productos y colaboradores también verifican duplicidad'),
('asociación existente','se actualiza solo rol o esDecisor','solo cambian los campos no nulos; no se actualiza el contacto maestro'))
add('10','Crear propuesta válida','Usuario autenticado','propuestas/application/service/PropuestaService.java',32,
('cliente y oportunidad existentes; detalle de 3 × 250, descuento 10','se crea con producto de catálogo y sin descripción/precio','se guarda BORRADOR, subtotal 750 y total 740; se copia descripción/precio del catálogo'),
('oportunidad inexistente','se solicita crear una propuesta','se lanza 404 y no se invoca save'),
('detalle libre sin productoId pero con descripción/precio','se crea la propuesta','no se consulta el catálogo y se conservan los datos del detalle'))
add('11','Editar y resolver una propuesta','Usuario para editar/enviar; JEFE/ADMIN para decidir','propuestas/application/service/PropuestaService.java',118,
('propuesta BORRADOR','se solicita enviar','se persiste ENVIADA; no se demuestra entrega de correo'),
('propuesta ENVIADA','se intenta editar','se rechaza por estado incompatible (409 por handler)'),
('propuesta BORRADOR y actor autorizado','se solicita cancelar','se guarda CANCELADA; una APROBADA no puede cancelarse'))
add('12','Conservar snapshot comercial del detalle','Usuario autenticado','propuestas/application/service/PropuestaService.java',220,
('producto existente y descripción/precio personalizados','se crea el detalle con override','se respetan los valores comerciales enviados, sin sustituirlos por catálogo'),
('productoId inexistente','se crea la propuesta','se lanza 404 y no se persiste'),
('propuesta BORRADOR con un nuevo producto','se actualizan los detalles sin precio/descripcion','se completa desde catálogo y se recalculan subtotal y total'))
add('13','Gestionar actividades y participantes','Usuario autenticado','actividades/application/service/ActividadService.java',63,
('referencias válidas y datos obligatorios','se crea una actividad sin estado explícito','se guarda PENDIENTE y se sincronizan las etiquetas'),
('un contacto ya es participante de la actividad','se solicita agregar el mismo par','se rechaza con 400'),
('actividad existente','se actualiza omitiendo etiquetaIds','se conservan las etiquetas; enviar lista vacía pide reemplazarlas por ninguna'))
add('14','Normalizar y archivar etiquetas','JEFE / ADMIN','etiquetas/application/service/EtiquetaService.java',60,
('nombre nuevo y color omitido','se crea una etiqueta','se normaliza el nombre, se guarda activa y se usa #2563EB'),
('otro nombre equivalente tras quitar tildes y normalizar espacios/mayúsculas','se intenta crear etiqueta','se rechaza por nombre normalizado duplicado'),
('etiqueta existente','se solicita eliminar','se archiva con activa=false, conservando el registro y el histórico'))
add('15','Mantener catálogo de productos','JEFE / ADMIN','productos/application/service/ProductoService.java',50,
('código nuevo y DTO válido','se crea producto sin estado','se guarda ACTIVO'),
('código duplicado','se solicita crear otro producto','se rechaza con 400'),
('producto existente','se actualiza manteniendo su código','no se rechaza por duplicidad consigo mismo y se invalida caché de productos'))
add('16','Gestionar equipos y miembros','JEFE / ADMIN','equipos/application/service/EquipoService.java',99,
('equipo y usuario existentes no asociados','se agrega el miembro','se guarda la asociación equipo-usuario'),
('usuario ya miembro del equipo','se agrega nuevamente','se rechaza con 400'),
('equipo activo existente','se desactiva','se guarda INACTIVO sin borrar miembros; eliminar es una operación distinta'))
add('17','Gestionar campañas y sus leads','JEFE / ADMIN','marketing/application/service/CampaniaService.java',76,
('campaña y lead existentes no asociados','se asocia el lead','se guarda la relación campaña-lead'),
('par campaña-lead ya registrado','se asocia nuevamente','se rechaza con 400'),
('asociación existente','se desasocia el lead','se elimina solo la asociación, no el lead maestro'))
add('18','Consultar indicadores del dashboard','Usuario autenticado','dashboard/application/service/DashboardService.java',18,
('datos disponibles en el puerto de consulta','se obtiene resumen','se integran clientes, leads, oportunidades, actividades y propuestas'),
('falla del puerto al calcular un indicador','se consulta el resumen sin caché válida','no se inventa éxito ni se sustituye todo por cifras cero; el error sigue el handler'),
('sumValorPropuestasAprobadas devuelve null','se consulta ese resumen','valorTotalAprobadas se representa como cero'))
add('19','Consultar mapa de relaciones','Usuario autenticado','relationshipmap/application/service/RelationshipMapService.java',47,
('fixture de cliente, contacto, oportunidad, actividad y propuesta','se obtiene mapa del cliente','se devuelven raíz CLIENTE:1, cinco nodos y cuatro aristas en ese fixture, no como cantidad universal'),
('cliente solicitado inexistente','se consulta su mapa','se propaga ResourceNotFoundException (404 en API)'),
('contacto existente con relaciones','se expande CONTACTO','el subgrafo contiene cliente, oportunidad y actividad según las asociaciones existentes'))
add('20','Enviar y consultar mensajería interna','Usuario autenticado','mensajeria/application/service/MensajeService.java',29,
('correo del emisor disponible y contenido válido','se envía mensaje','se guarda el contenido recortado y la identidad visible del emisor'),
('contenido vacío tras trim o mayor de 2000 caracteres','se envía mensaje','se rechaza con 400'),
('despuesDeId y límite mayor de 100','se solicitan mensajes','se usa consulta incremental con límite ajustado a 100; no es WebSocket'))
(OUT/'05_Criterios_aceptacion/catalogo_derivado.json').write_text(json.dumps(C,ensure_ascii=False,indent=2),encoding='utf-8')
rows=[];body='''<p class="aviso"><strong>Criterios propuestos, sin validación de usuarios.</strong> No se localizó el Word ni el catálogo original de RF/HU. Los identificadores HU-004, HU-005, HU-008, HU-016 y HU-017 no pueden vincularse responsablemente a una funcionalidad sin su enunciado. Este apartado deja esos cinco pendientes explícitos y aporta 20 historias técnicas derivadas, con 60 escenarios listos para contrastar. Los códigos RF-D/HU-D son nuevos identificadores documentales, no los oficiales.</p>
<h2>05.1 Pendientes originales y control de correspondencia</h2>'''
body+=table(['HU original','Qué falta','Estado'],[[h,'Enunciado, actor, objetivo, RF relacionado y reglas del documento fuente.','Pendiente de correspondencia; no aprobada'] for h in ['HU-004','HU-005','HU-008','HU-016','HU-017']])
csvfile('05_Criterios_aceptacion/correspondencia_hu_originales.csv',['HU original','Enunciado original','RF original','HU-D equivalente','Criterios aceptados','Revisor','Evidencia/fecha','Estado'],[[h,'','','','','','','Pendiente de documento fuente'] for h in ['HU-004','HU-005','HU-008','HU-016','HU-017']])
body+='''<p>No se asegura haber revisado “las demás HU” originales: tampoco están disponibles. La revisión siguiente cubre capacidades observadas, con escenarios positivos, negativos y alternativos. Al recibir el catálogo, se conservan los IDs originales, se asigna equivalencia y se resuelven diferencias; no basta renumerar automáticamente. Véase <a href="correspondencia_hu_originales.csv">registro de correspondencia</a>.</p><h2>05.2 Condiciones comunes de evaluación</h2><p>Las operaciones protegidas requieren autenticación; escritura de catálogos y decisiones críticas exige los roles del perfil keycloak. Datos e identificadores son sintéticos. 400 significa entrada/regla inválida, 404 recurso ausente, 409 estado o integridad incompatible y 403 permiso insuficiente. Las precondiciones requieren entorno y fixtures adecuados. Cada escenario describe comportamiento esperado derivado del código, no un resultado ejecutado; la sección 12 identifica exactamente qué aserciones tienen evidencia.</p>'''
for c in C:
    body+='<h2>HU-D'+c['code']+' · '+c['title']+'</h2><p><strong>Historia técnica propuesta:</strong> Como '+c['actor']+', quiero '+c['title'].lower()+', para gestionar esa capacidad del CRM con reglas verificables. Correspondencia provisional: RF-D'+c['code']+'.</p><p>'+ref(c['source'],c['line'])+'</p>'
    for letter,typ,sc in zip(['P','N','A'],['Positivo','Negativo','Alternativo'],c['scenarios']):
        id='CA-D'+c['code']+'-'+letter; given,when,then=sc
        body+='<h3>'+id+' · '+typ+'</h3><p><strong>Dado</strong> '+given+'; <strong>cuando</strong> '+when+'; <strong>entonces</strong> '+then+'.</p>'
        rows.append(['RF-D'+c['code'],'HU-D'+c['code'],id,c['title'],typ,given,when,then,c['source']+':'+str(c['line']),'Propuesto; no aprobado por usuarios'])
body+='''<h2>05.3 Reglas que no deben declararse implementadas</h2><p>No se exige secuencia estricta de estados de lead/oportunidad; no hay conversión automática ni cierre automático tras aprobar propuesta. No se demuestra envío por correo, aceptación del cliente, integración contable, venta o notificación persistente. El descuento de propuesta puede superar el subtotal porque el código resta ambos sin guarda de techo: una política de total no negativo sería una decisión/requisito adicional, no un criterio actualmente satisfecho. No se presume que el usuario CRM sea una cuenta sincronizada de Keycloak.</p>
<h2>05.4 Registro de validación</h2><p>Todos los criterios permanecen propuestos. Para aprobar: revisar enunciado original, datos de prueba, pasos, resultado observado y evidencia; registrar nombre/rol del validador, fecha, versión y decisión. Una prueba JUnit verde no reemplaza esa validación.</p><p><a href="criterios_60_escenarios.csv">Descargar los 60 escenarios editables</a></p>'''
csvfile('05_Criterios_aceptacion/criterios_60_escenarios.csv',['RF derivado','HU derivada','Criterio','Capacidad','Tipo','Dado','Cuando','Entonces','Fuente:línea','Estado'],rows)
page('05_Criterios_aceptacion/05_Criterios_aceptacion.html','05. Criterios de aceptación',body)
print('20 HU derivadas; 60 criterios; 5 correspondencias originales pendientes')
