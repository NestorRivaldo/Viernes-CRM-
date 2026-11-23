from pathlib import Path
from html import escape
import csv, json, re, hashlib
ROOT = Path('E:/Viernes/Viernes')
OUT = ROOT / 'informe/evidencias_academicas_v3'
JAVA = ROOT / 'viernes-backend/src/main/java/com/viernes'
CSS = '''body{font:16px/1.65 Calibri,Segoe UI,sans-serif;color:#163047;background:#f2f5f8;margin:0}main{max-width:1180px;margin:auto;background:white;padding:45px}h1,h2,h3{color:#153e63;line-height:1.25}h1{border-bottom:4px solid #1e8293;padding-bottom:18px}h2{margin-top:40px}a{color:#086b94}table{border-collapse:collapse;width:100%;font-size:13px;margin:18px 0;overflow-wrap:anywhere}td,th{border:1px solid #d3dce4;padding:8px;text-align:left;vertical-align:top}th{background:#e7eef5}code,pre{font-family:Consolas,monospace;font-size:12px;overflow-wrap:anywhere}pre{white-space:pre-wrap;background:#eef2f6;padding:15px}.aviso{border-left:5px solid #dc9b35;background:#fff6e5;padding:16px}.banner{font-size:13px;color:#526c83}img{max-width:100%;height:auto}figure{margin:24px 0}button{padding:9px;background:#153e63;color:white;border:0;cursor:pointer}@media print{body{background:white}main{padding:0;max-width:none}button,nav{display:none}h2,h3{break-after:avoid}tr,figure{break-inside:avoid}a{color:inherit}}'''
def table(headers, rows):
    return '<table><thead><tr>'+''.join('<th>'+escape(str(x))+'</th>' for x in headers)+'</tr></thead><tbody>'+''.join('<tr>'+''.join('<td>'+str(x)+'</td>' for x in row)+'</tr>' for row in rows)+'</tbody></table>'
def ref(path, line=1):
    p=JAVA/path if not str(path).startswith('viernes-') else ROOT/path
    assert p.exists(), p
    assert 0 < line <= len(p.read_text(encoding='utf-8-sig').splitlines()), (p,line)
    rel=p.relative_to(ROOT).as_posix()
    return f'<a class="source" data-line="{line}" href="{p.as_uri()}">{rel}:{line}</a>'
def page(relative,title,body):
    p=OUT/relative
    p.write_text('<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>'+escape(title)+'</title><style>'+CSS+'</style><main><div class="banner">VIERNES CRM V1 · Evidencias académicas · 11/11/2026<br>Encargo docente ↔ implementación comprobada en E:\\Viernes\\Viernes</div><h1>'+title+'</h1>'+body+'<footer><hr>Documentación técnica, no aprobación de usuarios. Archivos fuente y líneas corresponden al árbol de trabajo local; no se atribuyen al último commit.</footer></main></html>',encoding='utf-8')
def csvfile(relative,headers,rows):
    with (OUT/relative).open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.writer(f);w.writerow(headers);w.writerows(rows)
def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
