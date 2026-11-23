from documentos import *
import xml.etree.ElementTree as ET
import datetime
T=ROOT/'viernes-backend/src/test/java';REPORTS=ROOT/'viernes-backend/target/surefire-reports'
C=json.loads((OUT/'05_Criterios_aceptacion/catalogo_derivado.json').read_text(encoding='utf-8'))
LOG=Path('C:/Users/Nesto/AppData/Local/Temp/qoder-cli/E--Viernes/05e6f361-8cf2-4ee5-a424-8b1d08e19865/tasks/b6ga2mr2p.output')
log=LOG.read_text(encoding='utf-8',errors='replace');assert 'Tests run: 291, Failures: 0, Errors: 0, Skipped: 0' in log and 'BUILD SUCCESS' in log
extract='EVIDENCIA EXTRAÍDA; no es una ejecución nueva.\nComando registrado: cmd.exe //c "mvnw.cmd test"\nDirectorio: E:/Viernes/Viernes/viernes-backend\nFuente: '+str(LOG)+'\nSHA256 fuente: '+digest(LOG)+'\n\n'
for n,line in enumerate(log.splitlines(),1):
    if any(x in line for x in ['[INFO] Tests run:','[INFO] BUILD SUCCESS','[INFO] Total time:','[INFO] Finished at:','The following 2 profiles are active','Added connection oracle.jdbc','Database version:']):extract+=str(n)+': '+line+'\n'
(OUT/'12_Pruebas/ejecucion_maven_extracto.txt').write_text(extract,encoding='utf-8')
index={};source_inventory=[]
for p in sorted(T.rglob('*.java')):
    s=p.read_text(encoding='utf-8-sig');pm=re.search(r'package\s+([\w.]+);',s)
    if not pm:continue
    classname=pm.group(1)+'.'+p.stem
    for m in re.finditer(r'@Test\b[\s\S]*?\bvoid\s+(\w+)\s*\(',s):
        name=m.group(1);line=s[:m.start()].count('\n')+1
        key=(classname,name);index[key]=(p,line)
        source_inventory.append((classname,name,p,line))
assert len(source_inventory)==291
all_results={};summaries=[];safe=ET.Element('testsuites',description='Extracto sanitizado de Surefire. Sin properties, system-out ni credenciales.')
manifest=[]
for p in sorted(REPORTS.glob('TEST-*.xml')):
    root=ET.parse(p).getroot();ns=ET.SubElement(safe,'testsuite',**{k:root.get(k,'') for k in ['name','tests','failures','errors','skipped','time']})
    summaries.append([root.get('name'),*[root.get(k,'0') for k in ['tests','failures','errors','skipped','time']]])
    for tc in root.findall('testcase'):
        key=(tc.get('classname'),tc.get('name'));assert key in index,key
        result='FAIL' if tc.find('failure') is not None else 'ERROR' if tc.find('error') is not None else 'SKIPPED' if tc.find('skipped') is not None else 'PASS'
        all_results[key]=dict(result=result,time=tc.get('time',''),source=p.name)
        ET.SubElement(ns,'testcase',classname=key[0],name=key[1],time=tc.get('time',''),result=result)
    manifest.append(dict(archivo=p.name,sha256_original=digest(p),fecha_archivo=datetime.datetime.fromtimestamp(p.stat().st_mtime).isoformat(),testcases=len(root.findall('testcase'))))
assert len(all_results)==291 and all(r['result']=='PASS' for r in all_results.values())
ET.indent(safe);ET.ElementTree(safe).write(OUT/'12_Pruebas/resultados_junit_sanitizados.xml',encoding='utf-8',xml_declaration=True)
(OUT/'12_Pruebas/manifest_reportes.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
rows=[]
for classname,name,p,line in source_inventory:
    result=all_results[(classname,name)]
    scope='Servicio/dominio aislado con mocks'
    text=p.read_text(encoding='utf-8-sig')
    if 'standaloneSetup' in text:scope='HTTP MockMvc standalone; NO valida filtro Keycloak'
    elif 'ModularityTests' in classname:scope='Arquitectura Spring Modulith'
    elif '@SpringBootTest' in text:scope='Arranque Spring; conexión Oracle observada; ddl-auto:update'
    elif 'ApisPeruDocumentoAdapter' in classname:scope='Adaptador con proveedor simulado; no consulta real APIsPERU'
    rows.append([classname,name,p.relative_to(ROOT).as_posix()+':'+str(line),scope,result['result'],result['time'],result['source']])
csvfile('12_Pruebas/inventario_291_pruebas.csv',['Clase','Método @Test','Fuente:línea','Nivel/alcance','Resultado observado','Segundos','Surefire original'],rows)
csvfile('12_Pruebas/resumen_32_clases.csv',['Clase','Tests','Failures','Errors','Skipped','Tiempo segundos'],summaries)
# Only links whose assertions were inspected are classified as traced, not the entire suite.
P='com.viernes.propuestas.application.service.PropuestaServiceTest';M='com.viernes.relationshipmap.RelationshipMapServiceTest'
trace={
 '10-P':(P,'crear_conProductoValido_prefillDesdeCatalogo','Comprueba snapshot 250, subtotal 750 y total 740 con ArgumentCaptor; no Oracle.'),
 '10-N':(P,'crear_oportunidadInexistente_lanzaNotFound','Comprueba excepción y never().save; HTTP 404 deriva del handler, no de este test.'),
 '10-A':(P,'crear_detalleLibreSinProducto_noConsultaCatalogo','Comprueba ausencia de consulta al catálogo; no un E2E.'),
 '11-P':(P,'enviar_desdeBorrador_cambiaEstado','Comprueba ENVIADA; no autorización ni correo.'),
 '11-N':(P,'actualizar_enEnviada_lanzaConflict','Comprueba IllegalStateException y no save; el código HTTP 409 no se evalúa aquí.'),
 '11-A':(P,'cancelar_desdeBorrador_cambiaEstado','Comprueba CANCELADA desde BORRADOR; complemento cancelar_desdeAprobada_lanzaException verifica rechazo.'),
 '12-P':(P,'crear_conProductoYOverride_respetaValoresEnviados','Captura descripción/precio personalizados en save.'),
 '12-N':(P,'crear_conProductoInexistente_lanzaNotFound','Comprueba excepción y no persistencia con puerto mock.'),
 '12-A':(P,'actualizar_conProductoNuevo_aplicaPrefillYRecalculaTotales','Comprueba prefill y total 990 en objeto capturado.'),
 '19-P':(M,'obtenerMapaCliente_Exitoso','Comprueba cinco nodos y cuatro aristas para fixture; no universal.'),
 '19-N':(M,'obtenerMapaCliente_NoEncontrado_LanzaExcepcion','Comprueba excepción, no HTTP ni conexión Oracle.'),
 '19-A':(M,'expandirNodo_Contacto_Exitoso','Comprueba asociaciones en subgrafo; no interfaz Cytoscape.')
}
matrix=[]
for c in C:
    for letter,sc in zip(['P','N','A'],c['scenarios']):
        key=c['code']+'-'+letter;name='CP-D'+key;tr=trace.get(key)
        if tr:
            clazz,method,limit=tr;result=all_results[(clazz,method)];p,line=index[(clazz,method)]
            test=clazz+'#'+method;state='EXISTENTE Y EJECUTADO: PASS (alcance acotado)';evidence=p.relative_to(ROOT).as_posix()+':'+str(line)
        else:
            test='Sin vinculación puntual verificada; consultar inventario del módulo';state='ESPECIFICADO, NO EJECUTADO COMO ACEPTACIÓN';evidence='Pendiente de vinculación/ejecución del escenario';limit='Puede haber pruebas unitarias relacionadas; no se afirma cobertura ni ausencia solo por el nombre.'
        matrix.append(['RF-D'+c['code'],'HU-D'+c['code'],'CA-D'+key,name,c['title'],letter,'Dado '+sc[0]+'; cuando '+sc[1]+'; entonces '+sc[2],state,test,evidence,limit,'RF/HU originales pendientes'])
csvfile('12_Pruebas/matriz_RF_HU_CP.csv',['RF derivado','HU derivada','Criterio','Caso propuesto','Capacidad','Tipo P/N/A','Escenario','Estado','Test existente','Evidencia','Límite de cobertura','Trazabilidad oficial'],matrix)
gaps=[('CP-F01','RF-D01 / HU-D01','JWT ausente/expirado y VENDEDOR contra aprobación o escritura de usuarios','E2E/seguridad con cadena real Keycloak; MockMvc standalone no acredita roles'),('CP-F02','RF-D10 / HU-D10','Dos creaciones concurrentes con el mismo correlativo COUNT+1','Concurrencia con Oracle y captura de conflicto/reintento; sin evidencia específica localizada'),('CP-F03','RF-D10 / HU-D10','Vencimiento anterior a emisión, descuento mayor al subtotal y detalles al límite','Casos de borde; decidir con equipo si total negativo debe prohibirse antes de cambiar expectativa'),('CP-F04','RF-D06 / HU-D06','Marcar lead CONVERTIDO y comprobar que no crea oportunidad','Integración de módulos; ausencia de endpoint de conversión no sustituye ejecución'),('CP-F05','RF-D08 / HU-D08','Cambiar etapa GANADA a PROSPECCION y verificar probabilidad independiente','Caso alternativo de negocio; sin aceptación ejecutada documentada'),('CP-F06','RF-D19 / HU-D19','Expandir, colapsar y reexpandir grafo sin duplicar nodos en Angular','E2E frontend; los siete tests backend no prueban interacción visual'),('CP-F07','RF-D05 / HU-D05','Caché con proxy real, expiración, rate-limit y errores del proveedor','Integración controlada con servidor simulado; no consumir cuota sin autorización'),('CP-F08','RF-D09 / HU-D09','Eliminar entidades con FK reales de Oracle y verificar CASCADE/NO ACTION','Integración sobre base de prueba aislada; jamás contra datos operativos'),('CP-F09','Todas las HU originales','Validación de aceptación por el equipo/usuario','Pendiente catálogo original, testigo, fecha y evidencia; no reemplazable por JUnit')]
csvfile('12_Pruebas/casos_faltantes_o_no_acreditados.csv',['ID','RF-HU derivados','Caso','Evidencia pendiente'],gaps)
body='''<p class="aviso"><strong>291 tests, 0 fallos, 0 errores, 0 omitidos</strong> en 32 clases. La ejecución ya registrada finalizó el 11/11/2026 a las 09:46:08 −05:00, duración informada por Maven 38,405 s. Se comprobó el log y se cruzaron los 291 testcases de Surefire con métodos @Test existentes. Esta continuación reutiliza ese resultado; no se volvió a ejecutar la suite.</p>
<h2>12.1 Procedencia y límites</h2><p><a href="ejecucion_maven_extracto.txt">Extracto verificable del log</a> · <a href="resultados_junit_sanitizados.xml">Resultados JUnit sanitizados</a> · <a href="manifest_reportes.json">Hashes de reportes originales</a>. Se excluyen properties y salidas potencialmente sensibles de los XML. PASS significa únicamente que las aserciones existentes no fallaron.</p><p>La prueba contextLoads sí conectó a Oracle; no prueba transacciones comerciales ni integridad de todas las relaciones. Además, el perfil dev usa ddl-auto:update: aquella ejecución no se puede presentar como estrictamente de solo lectura ni certificar que no generó DDL. No hay evidencia para atribuirle cambios concretos. Por seguridad no se repitió. La extracción posterior de metadatos de la sección 11 sí usó exclusivamente SELECT/READ ONLY.</p>'''
body+='<p>'+ref('viernes-backend/src/test/java/com/viernes/ViernesApplicationTests.java',6)+'<br>'+ref('viernes-backend/src/main/resources/application-dev.yml',13)+'<br>'+ref('viernes-backend/src/test/java/com/viernes/propuestas/adapters/in/web/PropuestaControllerTest.java',51)+'</p>'
body+='<h2>12.2 Inventario existente y ejecutado</h2><p><a href="inventario_291_pruebas.csv">291 métodos con fuente, resultado y nivel</a> · <a href="resumen_32_clases.csv">Resumen por clase</a>. El conteo no es porcentaje de cobertura de líneas ni de requisitos; no se presentó un informe JaCoCo ni UAT.</p>'+table(['Clase','Tests','Fallos','Errores','Omitidos','Segundos'],summaries)
body+='<h2>12.3 Matriz RF–HU–casos de prueba</h2><p>RF-D/HU-D son una correspondencia técnica provisional; los RF/HU del documento académico no están disponibles. La matriz tiene 60 escenarios propuestos. Doce poseen una vinculación puntual a tests existentes cuyas aserciones fueron revisadas y cuyo PASS está en Surefire; no se amplía su cobertura más allá de esas aserciones. Los otros 48 no se declaran fallidos ni inexistentes: falta vincular o ejecutar la aceptación. Véase <a href="matriz_RF_HU_CP.csv">matriz completa editable</a>.</p>'
body+=table(['RF / HU','Caso','Prueba existente','Estado y límite'],[[r[0]+' / '+r[1],r[3],r[8],r[7]+'<br>'+r[10]] for r in matrix])
body+='<h2>12.4 Pruebas faltantes o sin evidencia específica</h2>'+table(['ID','Trazabilidad provisional','Escenario','Qué falta'],gaps)
body+='<h2>12.5 Registro de resultados de aceptación</h2><p>Para cada ejecución futura: ID de caso, versión del código y configuración, entorno aislado, datos sintéticos, pasos, resultado esperado, resultado observado, evidencia y responsable. No usar credenciales de chat ni almacenar tokens. Las pruebas que escriban se ejecutarán únicamente en una base de prueba autorizada, no como parte de esta entrega documental.</p>'
page('12_Pruebas/12_Pruebas.html','12. Pruebas y trazabilidad',body)
print('291 testcases cruzados; 32 suites; 60 filas de matriz; 12 vinculaciones verificadas; 9 brechas')
