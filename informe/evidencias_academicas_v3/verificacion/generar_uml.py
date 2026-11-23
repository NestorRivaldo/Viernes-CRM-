from documentos import *
OLD=Path('E:/Viernes/entrega_analisis_v2/uml')
U=OUT/'07_UML'
base='@startuml\n!pragma layout smetana\nskinparam defaultFontName Segoe UI\nskinparam defaultFontSize 13\nskinparam shadowing false\n'
def save(name,s): (U/(name+'.puml')).write_text(s,encoding='utf-8')
save('01_casos_uso_general',base+r'''title Casos de uso — alcance implementado y permisos HTTP (perfil keycloak)
left to right direction
actor "Usuario autenticado" as U
actor VENDEDOR as V
actor JEFE as J
actor ADMIN as A
actor Keycloak as K
actor APIsPERU as P
U <|-- V
U <|-- J
J <|-- A
rectangle "Viernes CRM V1" {
 usecase "Iniciar/cerrar sesión" as Sesion
 usecase "Consultar clientes, leads, oportunidades,\npropuestas, actividades y catálogos" as Lectura
 usecase "Crear/editar clientes y contactos;\nconsultar DNI/RUC" as Clientes
 usecase "Crear/editar leads y cambiar estado" as Leads
 usecase "Crear/editar oportunidades y etapa;\nasociar contactos/productos/colaboradores" as Opp
 usecase "Crear/editar borrador y enviar propuesta" as Prop
 usecase "Crear/editar actividades;\nasignar participantes y etiquetas" as Act
 usecase "Consultar dashboard y mapa de relaciones" as Mapa
 usecase "Enviar/consultar mensajes internos" as Chat
 usecase "Mantener productos, fuentes, equipos,\ncampañas y etiquetas" as Catalogos
 usecase "Aprobar/rechazar/cancelar propuesta" as Aprobar
 usecase "Escribir usuarios CRM" as Usuarios
 usecase "Eliminar registros de negocio" as Borrar
}
U --> Sesion
K --> Sesion
U --> Lectura
U --> Clientes
P --> Clientes : consulta documental
U --> Leads
U --> Opp
U --> Prop
U --> Act
U --> Mapa
U --> Chat
J --> Catalogos
J --> Aprobar
A --> Usuarios
A --> Borrar
note bottom of U
 Usuario autenticado resume la regla HTTP final.
 ADMIN hereda aquí capacidades de JEFE,
 no una jerarquía declarada en Keycloak.
 Las reglas de estado siguen aplicando a todos.
end note
note bottom of Prop
 Enviar = cambiar estado; no envío de correo.
 No se modela conversión automática de lead.
 No se asignan RF/HU originales no localizados.
end note
@enduml
''')
save('02_actividad_propuesta',base+r'''title Actividad de propuesta — acciones disponibles, no motor de workflow
|Usuario autenticado|
start
:Elegir cliente y oportunidad existentes;
:Preparar cabecera y uno o más detalles;
|Backend|
:Bean Validation del request;
if (¿Request válido?) then (no)
 :400; no crear;
 stop
endif
:Validar existencia de cliente y oportunidad;
if (¿Existen?) then (no)
 :404; no crear;
 stop
endif
:Validar vencimiento >= emisión;
if (¿Fechas válidas?) then (no)
 :400; no crear;
 stop
endif
:Numerar PROP-año-(count+1);
:Resolver producto opcional; completar\ndescripción/precio si no se enviaron;
if (¿Producto y detalle resolubles?) then (no)
 :404 producto inexistente o 400 datos faltantes;
 stop
endif
:Calcular cantidad × precio;\nsubtotal y total = subtotal - descuento;
:Guardar cabecera/detalles BORRADOR\nen una transacción;
|Usuario autenticado|
if (¿Enviar ahora?) then (no)
 :Mantener borrador; se puede editar;
 |ADMIN|
 :Opcional: eliminar únicamente en BORRADOR;
 stop
else (sí)
 |Backend|
 :Exigir BORRADOR y guardar ENVIADA;
 |JEFE / ADMIN|
 if (¿Decisión?) then (aprobar)
  |Backend|
  :Exigir ENVIADA y guardar APROBADA;
 elseif (¿Rechazar?) then (sí)
  |Backend|
  :Exigir ENVIADA y guardar RECHAZADA;
 else (cancelar)
  |Backend|
  :Exigir estado no terminal y guardar CANCELADA;
 endif
endif
stop
note right
 Cancelar también puede solicitarse desde BORRADOR.
 Un estado inválido produce 409; rol insuficiente 403.
 El cierre de oportunidad es otra acción de usuario.
 No se envía email ni se crea una venta automáticamente.
end note
@enduml
''')
s=(OLD/'03_clases_dominio.puml').read_text(encoding='utf-8')
s=s.replace('@startuml','@startuml\n!pragma layout smetana',1).replace('Clases de dominio y entidades JPA; commit 298ac64','Extracto del dominio; árbol de trabajo revisado 11/11/2026')
s=s.replace('  + desactivar() : void\n','').replace('  + cambiarEstado(estado) : void\n','')
s=s.replace('FK física JPA:', 'Asociación declarada en JPA:\\nCotejo físico en sección 11:').replace('referencias lógicas por ID, sin FK física.','referencias lógicas sin FK declarada en JPA.')
for cls in ['Campania','Oportunidad','Actividad','Equipo']:
    s=re.sub(r'^'+cls+r' "1" -- "0\.\.\*"',cls+' "0..*" .. "0..*"',s,flags=re.M)
s=s.replace('Cliente "1" -- "0..*" Propuesta','Cliente "1" .. "0..*" Propuesta').replace('Oportunidad "1" -- "0..*" Propuesta','Oportunidad "1" .. "0..*" Propuesta')
# la expresión anterior generaliza Oportunidad/Propuesta; restaurar su cardinalidad 1:N lógica.
s=s.replace('Oportunidad "0..*" .. "0..*" Propuesta','Oportunidad "1" .. "0..*" Propuesta')
s=s.replace('Lead "0..*" -->','Lead "0..*" ..>').replace('DetallePropuesta "0..*" -->','DetallePropuesta "0..*" ..>')
s=s.replace('tablas puente con IDs lógicos (sin FK física):','tablas puente con IDs lógicos (sin FK JPA):').replace('no existe FK física entre los extremos.','sin asociación JPA entre los extremos.').replace('Línea punteada: referencia lógica por ID.','Línea punteada: relación de dominio, incluidas N:M.\\n  Las siete puentes sí tienen FK físicas en Oracle: ver sección 11.')
save('03_clases_dominio',s)
save('04_secuencia_crear_propuesta',base+r'''title Secuencia — POST /api/v1/propuestas (ruta de éxito y errores)
autonumber
actor Usuario as U
participant "Angular / PropuestasService" as UI
participant "PropuestaController" as C
participant "ManagePropuestaUseCase\nPropuestaService" as S
participant "PropuestaRelacionValidationPort\n(adaptador de consulta)" as V
participant "ManageProductoUseCase" as P
participant "PropuestaPersistencePort\nPropuestaPersistenceAdapter" as A
participant "PropuestaSpringDataRepository\nJPA / Hibernate" as R
database Oracle as DB
U -> UI : Guardar formulario
UI -> C : POST + PropuestaRequest
C -> C : @Valid (DTO y detalles)
C -> S : crear(request)
group @Transactional — PropuestaService.crear
 S -> V : existeOportunidad(id)
 V --> S : boolean
 S -> V : existeCliente(id)
 V --> S : boolean
 S -> S : validarFechas()
 S -> A : countByNumeroPropuestaStartsWith(prefijo)
 A -> R : countByNumeroPropuestaStartsWith(prefijo)
 R -> DB : consulta de conteo
 DB --> R : count
 R --> A : count
 A --> S : count
 S -> S : construir BORRADOR con número count+1
 loop Cada detalle
  opt productoId != null
   S -> P : obtener(productoId)
   P --> S : ProductoResponse
   S -> S : completar descripción/precio ausentes
  end
  S -> S : exigir descripción/precio y calcular subtotal
 end
 S -> S : recalcularTotales()
 S -> A : save(propuesta)
 A -> A : toEntity()
 A -> R : save(entity)
 R -> DB : persistencia cabecera + detalles (cascada JPA)
 DB --> R : datos persistidos
 R --> A : entity
 A -> A : toDomain()
 A --> S : Propuesta
 S -> S : toResponse()
end
S --> C : PropuestaResponse
C --> UI : 201 Created
UI --> U : Mostrar resultado
note over C,DB
 Errores abortan la ruta: DTO inválido 400; relación/producto inexistente 404;
 fechas/detalle inválidos 400; integridad UNIQUE 409 si el esquema la aplica.
 La transacción debe confirmarse antes de responder exitosamente.
 COUNT+1 puede colisionar: no es secuencia ni reintento automático.
 No se ha observado SQL Oracle en esta revisión; se modela el mapeo.
end note
@enduml
''')
save('05_secuencia_mapa_relaciones',base+r'''title Secuencia — mapa de cliente, expansión y detalle
autonumber
actor Usuario as U
participant "Angular / RelationshipMapService" as UI
participant "RelationshipMapController" as C
participant "RelationshipMapService (backend)" as S
participant "ManageClienteUseCase" as CL
participant "ManageOportunidadUseCase" as O
participant "ManageActividadUseCase" as A
participant "ManagePropuestaUseCase" as P
U -> UI : Abrir mapa cliente
UI -> C : GET /api/v1/relationship-map/clientes/{id}
C -> S : obtenerMapaCliente(id)
S -> CL : obtener(id)
CL --> S : ClienteResponse
S -> CL : listarContactos(id)
CL --> S : contactos
S -> O : listar()
O --> S : oportunidades
loop Oportunidades
 alt Coincide clienteNombre con nombre/empresa
  S -> S : incluir oportunidad
 else No coincide
  S -> O : listarContactos(oportunidadId)
  O --> S : contactos asociados
  S -> S : incluir si comparten contacto del cliente
 end
end
S -> A : filtrarPorCliente(id)
A --> S : actividades
S -> P : listarPorCliente(id)
P --> S : propuestas
S -> S : deduplicar nodos/aristas por ID
S --> C : RelationshipMapResponse\nrootNodeId, nodes, edges, totalNodes, totalEdges
C --> UI : 200 JSON
UI --> U : Renderizar grafo
== Expansión ==
UI -> C : GET /nodes/oportunidad/{id}/connections
C -> S : expandirNodo(OPORTUNIDAD, id)
S -> S : obtenerMapaOportunidad(id)
S -> O : obtener(id), listarContactos(id)
O --> S : oportunidad y contactos
S -> CL : resolver cliente por nombre/contactos
CL --> S : cliente si se encuentra
S -> O : listarProductos(id), listarColaboradores(id)
O --> S : productos y colaboradores
S -> A : filtrarPorOportunidad(id)
A --> S : actividades
S -> P : listarPorOportunidad(id)
P --> S : propuestas
S --> C : subgrafo
C --> UI : 200 JSON
UI -> UI : fusionar sin duplicar IDs
== Detalle ==
UI -> C : GET /nodes/{type}/{id}
C -> S : obtenerDetalleNodo(NodeType, id)
S -> S : delegar al caso de uso según tipo
S --> C : GraphNodeDto
C --> UI : 200 JSON
note over C,S
 Tipo inválido: 400; raíz inexistente: 404 (handler).
 @Transactional(readOnly=true). No modifica entidades.
 Asociación cliente/oportunidad heurística: no FK.
 Recorridos de listas y consultas por oportunidad: coste creciente.
end note
@enduml
''')
s=(OLD/'06a_estado_propuesta.puml').read_text(encoding='utf-8').replace('@startuml','@startuml\n!pragma layout smetana',1)
s=s.replace('B --> B : actualizar() / eliminar()\\n[solo BORRADOR; eliminar exige ADMIN]','B --> B : actualizar()\\n[solo BORRADOR]\nB --> [*] : eliminar()\\n[ADMIN; eliminación del registro]')
save('06a_estado_propuesta',s)
for stem,states,method,default in [('06b_estado_lead',['NUEVO','CONTACTADO','CALIFICADO','CONVERTIDO','DESCARTADO'],'cambiarEstado','NUEVO'),('06c_estado_oportunidad',['PROSPECCION','CONTACTO','CALIFICACION','PROPUESTA','NEGOCIACION','GANADA','PERDIDA'],'cambiarEtapa','PROSPECCION')]:
    text=base+'title '+stem.replace('_',' ')+' — comportamiento implementado\nstate "Registro existente" as R {\n'
    text+='state "Cualquier valor válido del enum" as ANY\n'
    for i,st in enumerate(states):text+='state "'+st+'" as E'+str(i)+'\nANY --> E'+str(i)+' : '+method+'('+st+')\n'
    text+='}\n[*] --> R : crear (default '+default+'; admite otro valor)\n'
    text+='note bottom of R\n  ANY resume el estado de origen: no es un valor persistido.\n  Se permite cualquier origen → cualquier destino válido, incluso el mismo.\n  No se comprueba secuencia ni se bloquea reapertura.\n  '+('CONVERTIDO no crea oportunidad.' if 'lead' in stem else 'GANADA/PERDIDA no son terminales; no exige propuesta aprobada.')+'\n  Las acciones residen en el servicio; no hay motor de estados.\nend note\n@enduml\n'
    save(stem,text)
items=[('01_casos_uso_general','Casos de uso generales','Agrupa capacidades existentes y diferencia permisos por operación. La política depende del perfil keycloak. Se eliminaron HU/RF no trazables y funcionalidades deseadas del diagrama de alcance implementado.', [('auth/keycloak/KeycloakSecurityConfig.java',121),('clientes/adapters/in/web/ClienteController.java',1)]),('02_actividad_propuesta','Actividad del proceso de propuestas','Representa validación, cálculo y decisiones de usuario. Un BORRADOR puede conservarse o editarse; solo ADMIN puede eliminarlo. JEFE/ADMIN deciden las transiciones críticas. Enviar significa cambiar estado y no demuestra entrega de un documento al cliente.', [('propuestas/application/service/PropuestaService.java',32),('propuestas/application/dto/PropuestaRequest.java',12)]),('03_clases_dominio','Clases del dominio comercial','Extracto de 14 clases de dominio: no inventario de todas las clases Java. Las siete asociaciones entre extremos son N:M y se materializan en entidades puente. Se retiraron métodos desactivar/cambiarEstado inexistentes en Cliente y Lead. La composición de detalles exige uno o más en el contrato de creación, no por cardinalidad SQL.', [('clientes/domain/Cliente.java',9),('leads/domain/Lead.java',8),('propuestas/domain/Propuesta.java',35),('oportunidades/domain/Oportunidad.java',22)]),('04_secuencia_crear_propuesta','Secuencia de creación','Distingue entrada REST, puerto de entrada, servicio, validación, catálogo y adapter de persistencia. El SQL es conceptual: no se conectó Oracle. Numeración COUNT+1 y persistencia dentro de @Transactional; la asociación padre-detalle usa cascada JPA.', [('propuestas/adapters/in/web/PropuestaController.java',27),('propuestas/application/service/PropuestaService.java',209),('propuestas/adapters/out/persistence/PropuestaPersistenceAdapter.java',25)]),('05_secuencia_mapa_relaciones','Secuencia de consulta del mapa','La raíz agrupa contactos, oportunidades inferidas, actividades y propuestas. Expandir pide un subgrafo; el cliente combina nodos/aristas. El DTO contiene totalNodes/totalEdges, no un campo counts. La asociación por nombre es heurística y no integridad relacional.', [('relationshipmap/adapters/in/web/RelationshipMapController.java',21),('relationshipmap/application/service/RelationshipMapService.java',47),('relationshipmap/application/service/RelationshipMapService.java',677)]),('06a_estado_propuesta','Estados de propuesta','BORRADOR→ENVIADA; ENVIADA→APROBADA/RECHAZADA; BORRADOR o ENVIADA→CANCELADA. Los tres últimos son terminales. Eliminar se representa como fin de existencia, no como un bucle. Estado incompatible devuelve 409.', [('propuestas/application/service/PropuestaService.java',118),('propuestas/domain/Propuesta.java',45)]),('06b_estado_lead','Estados de lead','El enum enumera valores pero no impone un ciclo. Cualquier valor válido se asigna desde cualquier estado. CONVERTIDO no genera oportunidad, contacto ni venta.', [('leads/application/service/LeadService.java',47),('leads/application/service/LeadService.java',66)]),('06c_estado_oportunidad','Etapas de oportunidad','La asignación de etapa carece de guardas de transición. Crear una propuesta no avanza el pipeline ni aprobarla gana la oportunidad. La probabilidad y etapa son independientes.', [('oportunidades/application/service/OportunidadService.java',69),('oportunidades/application/service/OportunidadService.java',88)])]
body='<p>Se entregan los seis bloques solicitados como ocho figuras: el bloque de estados se desglosa en tres. Cada figura tiene PlantUML editable y SVG/PNG regenerados. Reutiliza y corrige entrega_analisis_v2; no modifica los originales.</p>'
for stem,title,desc,refs in items:
    body+='<h2>'+title+'</h2><p>'+desc+'</p><p>'+ ' · '.join('<a href="'+stem+'.'+ext+'">'+ext.upper()+'</a>' for ext in ['puml','svg','png'])+'</p><figure><img src="'+stem+'.svg" alt="'+title+'"></figure><p>'+ '<br>'.join(ref(p,l) for p,l in refs)+'</p>'
page('07_UML/07_UML.html','07. UML',body)
print('8 fuentes UML y explicaciones generadas')
