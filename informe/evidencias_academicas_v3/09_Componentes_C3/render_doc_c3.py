#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Renderiza 09_Componentes_C3.md a HTML y TXT, y exporta tablas clave a CSV.
No usa dependencias externas: parser Markdown acotado a la sintaxis usada
en el documento (titulos, parrafos, listas, tablas, figuras y bloques).
"""
from __future__ import annotations

import csv
import html
import re
from pathlib import Path

BASE = Path(__file__).resolve().parent
MD = BASE / "09_Componentes_C3.md"


def inline(text: str) -> str:
    text = html.escape(text.strip(), quote=False)
    text = re.sub(r"`([^`]+)`", r"<code>\1</code>", text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"<em>\1</em>", text)
    return text


def parse_table(lines: list[str]) -> list[list[str]]:
    rows: list[list[str]] = []
    for raw in lines:
        if not raw.strip().startswith("|"):
            continue
        parts = [p.strip() for p in raw.strip().strip("|").split("|")]
        if all(re.fullmatch(r":?-{2,}:?", p or "") for p in parts):
            continue
        rows.append(parts)
    return rows


def table_html(rows: list[list[str]]) -> str:
    if not rows:
        return ""
    out = ["<table>", "<thead><tr>"]
    for cell in rows[0]:
        out.append(f"<th>{inline(cell)}</th>")
    out.append("</tr></thead><tbody>")
    for row in rows[1:]:
        out.append("<tr>")
        for cell in row:
            out.append(f"<td>{inline(cell)}</td>")
        out.append("</tr>")
    out.append("</tbody></table>")
    return "\n".join(out)


def render_markdown(md: str) -> tuple[str, list[tuple[str, list[list[str]]]]]:
    lines = md.splitlines()
    html_parts: list[str] = []
    tables: list[tuple[str, list[list[str]]]] = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if not line.strip():
            i += 1
            continue
        if line.strip() == "<!-- pagebreak -->":
            html_parts.append('<div class="page-break"></div>')
            i += 1
            continue
        if line.startswith("```"):
            i += 1
            code: list[str] = []
            while i < len(lines) and not lines[i].startswith("```"):
                code.append(lines[i])
                i += 1
            i += 1
            html_parts.append(
                "<pre><code>" + html.escape("\n".join(code), quote=False) + "</code></pre>"
            )
            continue
        if line.startswith("# "):
            html_parts.append(f"<h1>{inline(line[2:])}</h1>")
            i += 1
            continue
        if line.startswith("## "):
            html_parts.append(f"<h2>{inline(line[3:])}</h2>")
            i += 1
            continue
        if line.startswith("### "):
            html_parts.append(f"<h3>{inline(line[4:])}</h3>")
            i += 1
            continue
        if line.startswith("!["):
            m = re.match(r"!\[([^\]]*)\]\(([^)]+)\)", line.strip())
            if m:
                alt, src = m.group(1), m.group(2)
                html_parts.append(
                    f'<figure><img src="{html.escape(src, quote=True)}" alt="{html.escape(alt, quote=True)}">'
                    f"<figcaption>{inline(alt)}</figcaption></figure>"
                )
                i += 1
                continue
        if line.strip().startswith("|"):
            table_lines: list[str] = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                table_lines.append(lines[i])
                i += 1
            rows = parse_table(table_lines)
            if rows:
                tables.append((rows[0][0] if rows[0] else "", rows))
                html_parts.append(table_html(rows))
            continue
        if line.startswith("> "):
            quote: list[str] = []
            while i < len(lines) and lines[i].startswith("> "):
                quote.append(lines[i][2:])
                i += 1
            html_parts.append("<blockquote><p>" + inline(" ".join(quote)) + "</p></blockquote>")
            continue
        if line.startswith("- "):
            items: list[str] = []
            while i < len(lines) and lines[i].startswith("- "):
                items.append(lines[i][2:])
                i += 1
            html_parts.append("<ul>" + "".join(f"<li>{inline(x)}</li>" for x in items) + "</ul>")
            continue
        if line.strip() == "---":
            html_parts.append("<hr>")
            i += 1
            continue
        # Parrafo: unir lineas consecutivas hasta una linea especial o vacia.
        para: list[str] = []
        while i < len(lines):
            cur = lines[i]
            if not cur.strip():
                break
            if (
                cur.startswith("#")
                or cur.startswith("![")
                or cur.startswith("> ")
                or cur.startswith("- ")
                or cur.startswith("```")
                or cur.strip().startswith("|")
                or cur.strip() == "---"
                or cur.strip() == "<!-- pagebreak -->"
            ):
                break
            para.append(cur.strip())
            i += 1
        if para:
            html_parts.append("<p>" + inline(" ".join(para)) + "</p>")
    return "\n".join(html_parts), tables


def html_document(body: str) -> str:
    return f'''<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>C3 actualizado - Componentes de Viernes CRM V1</title>
<style>
@page {{ size: A4; margin: 2cm 1.7cm 2cm 1.7cm; }}
body {{
  font-family: Calibri, "Segoe UI", Arial, sans-serif;
  font-size: 10.5pt;
  line-height: 1.38;
  color: #1f2937;
  max-width: 18.2cm;
  margin: 0 auto;
  padding: 1.1cm 0.2cm;
}}
h1 {{ font-size: 19pt; line-height: 1.2; margin: 0 0 0.35em; color: #101828; }}
h2 {{ font-size: 14pt; line-height: 1.25; margin: 1.25em 0 0.4em; color: #173f61; page-break-after: avoid; }}
h3 {{ font-size: 11.8pt; line-height: 1.3; margin: 1em 0 0.3em; color: #1f4e79; page-break-after: avoid; }}
p {{ margin: 0.35em 0 0.55em; text-align: justify; }}
ul {{ margin: 0.3em 0 0.7em 1.2em; padding: 0; }}
li {{ margin: 0.15em 0; }}
strong {{ color: #101828; }}
hr {{ border: 0; border-top: 1px solid #d0d5dd; margin: 1.1em 0; }}
blockquote {{
  border-left: 4px solid #2f6fa8;
  margin: 0.8em 0 0.9em;
  padding: 0.35em 0.9em;
  background: #f5f8fc;
  color: #344054;
}}
table {{
  width: 100%;
  border-collapse: collapse;
  margin: 0.55em 0 1em;
  font-size: 8.2pt;
  line-height: 1.26;
  table-layout: fixed;
  page-break-inside: auto;
}}
thead {{ display: table-header-group; }}
th, td {{
  border: 1px solid #aab4c0;
  padding: 3.5px 4.5px;
  vertical-align: top;
  word-wrap: break-word;
  overflow-wrap: anywhere;
}}
th {{ background: #e8eef7; font-weight: 700; color: #101828; }}
tr:nth-child(even) td {{ background: #f8fafc; }}
tr {{ page-break-inside: avoid; }}
figure {{
  margin: 0.9em 0 1em;
  text-align: center;
  page-break-inside: avoid;
}}
figure img {{
  max-width: 100%;
  height: auto;
  border: 1px solid #d0d5dd;
  background: #fff;
}}
figcaption {{
  font-size: 8.8pt;
  color: #475467;
  margin-top: 4px;
  text-align: center;
}}
code {{
  font-family: Consolas, "Courier New", monospace;
  background: #f2f4f7;
  padding: 1px 3px;
  border-radius: 3px;
  font-size: 8.8pt;
}}
pre {{
  background: #f8fafc;
  border: 1px solid #d0d5dd;
  padding: 8px;
  overflow: auto;
  font-size: 8.2pt;
  line-height: 1.3;
}}
.page-break {{ page-break-before: always; }}
@media print {{
  body {{ max-width: none; margin: 0; padding: 0; }}
  .page-break {{ page-break-before: always; }}
}}
</style>
</head>
<body>
{body}
</body>
</html>
'''


def table_to_txt(rows: list[list[str]]) -> list[str]:
    out: list[str] = []
    for row in rows:
        out.append("\t".join(re.sub(r"[`*]", "", cell) for cell in row))
    return out


def markdown_to_txt(md: str) -> str:
    lines = md.splitlines()
    out: list[str] = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.strip() == "<!-- pagebreak -->":
            out.append("\n\n")
            i += 1
            continue
        if line.startswith("```"):
            i += 1
            code: list[str] = []
            while i < len(lines) and not lines[i].startswith("```"):
                code.append("    " + lines[i])
                i += 1
            i += 1
            out.extend(code)
            out.append("")
            continue
        if line.startswith("#"):
            title = line.lstrip("#").strip()
            out.append("")
            out.append(title.upper() if line.startswith("# ") else title)
            out.append("=" * min(len(title), 72) if line.startswith("## ") else "")
            i += 1
            continue
        if line.startswith("!["):
            m = re.match(r"!\[([^\]]*)\]\(([^)]+)\)", line.strip())
            if m:
                out.append(f"[FIGURA] {m.group(1)}")
                out.append(f"Archivo: {m.group(2)}")
                out.append("")
                i += 1
                continue
        if line.strip().startswith("|"):
            table_lines: list[str] = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                table_lines.append(lines[i])
                i += 1
            rows = parse_table(table_lines)
            out.extend(table_to_txt(rows))
            out.append("")
            continue
        if line.startswith("> "):
            out.append(line[2:])
            i += 1
            continue
        if line.startswith("- "):
            while i < len(lines) and lines[i].startswith("- "):
                out.append("  - " + lines[i][2:])
                i += 1
            out.append("")
            continue
        if line.strip() == "---":
            out.append("-" * 72)
            i += 1
            continue
        if not line.strip():
            out.append("")
            i += 1
            continue
        para: list[str] = []
        while i < len(lines):
            cur = lines[i]
            if not cur.strip():
                break
            if (
                cur.startswith("#")
                or cur.startswith("![")
                or cur.startswith("> ")
                or cur.startswith("- ")
                or cur.startswith("```")
                or cur.strip().startswith("|")
                or cur.strip() == "---"
                or cur.strip() == "<!-- pagebreak -->"
            ):
                break
            para.append(cur.strip())
            i += 1
        text = " ".join(para)
        text = re.sub(r"[`*]", "", text)
        out.append(text)
        out.append("")
    # Normalizar espacios verticales.
    result: list[str] = []
    blank = False
    for line in out:
        if line.strip():
            result.append(line.rstrip())
            blank = False
        elif not blank:
            result.append("")
            blank = True
    return "\r\n".join(result).strip() + "\r\n"


def write_csv(name: str, rows: list[list[str]]) -> None:
    path = BASE / name
    with path.open("w", encoding="utf-8-sig", newline="") as fh:
        writer = csv.writer(fh, quoting=csv.QUOTE_MINIMAL)
        for row in rows:
            writer.writerow(row)


def main() -> None:
    md = MD.read_text(encoding="utf-8")
    body, tables = render_markdown(md)
    (BASE / "09_Componentes_C3.html").write_text(html_document(body), encoding="utf-8")
    (BASE / "Texto_para_Word_C3.txt").write_text(markdown_to_txt(md), encoding="utf-8")

    for header, rows in tables:
        if header == "Relación":
            write_csv("C3_relaciones_NM.csv", rows)
        elif header == "Origen":
            write_csv("C3_dependencias.csv", rows)
        elif header == "Módulo" and any("Tipo Modulith" in c for c in rows[0]):
            write_csv("C3_componentes.csv", rows)

    print("Generado:", BASE / "09_Componentes_C3.html")
    print("Generado:", BASE / "Texto_para_Word_C3.txt")
    print("Tablas detectadas:", len(tables))
    for header, rows in tables:
        print(f"  - {header}: {len(rows)-1} filas")


if __name__ == "__main__":
    main()
