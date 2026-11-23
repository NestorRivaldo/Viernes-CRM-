#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Genera los C3 detallados de Oportunidades y Actividades como SVG.
La fuente editable es este script: las cajas, textos y conexiones se
declaran en las funciones generar_* y se dibujan con un layout fijo para
evitar el desorden de los motores automaticos.

Uso:
    python generar_c3_detalle.py

Luego, para PNG, abrir el SVG con Chrome/Edge o usar:
    chrome --headless --screenshot=... --window-size=ANCHO,ALTO archivo.svg
"""
from __future__ import annotations

import html
import math
from pathlib import Path
from typing import Iterable

BASE = Path(__file__).resolve().parent
FONT = "Segoe UI, Arial, sans-serif"

COLORS = {
    "entrada": ("#2F6FA8", "#173F61", "#FFFFFF"),
    "aplicacion": ("#2E7D8C", "#194A54", "#FFFFFF"),
    "dominio": ("#6A4C93", "#3F2C58", "#FFFFFF"),
    "puerto": ("#A15C00", "#623800", "#FFFFFF"),
    "adaptador": ("#2E7D5B", "#194A36", "#FFFFFF"),
    "externo": ("#5B6770", "#333B41", "#FFFFFF"),
    "modulo": ("#E8EEF7", "#8A94A6", "#1F2A37"),
    "nota": ("#FFF8D6", "#B8A24A", "#3B3420"),
    "grupo": ("#FBFCFE", "#B9C2D0", "#1F2A37"),
}


def esc(text: object) -> str:
    return html.escape(str(text), quote=True)


def wrap(text: str, max_chars: int) -> list[str]:
    words = text.split()
    if not words:
        return [""]
    # Divide palabras largas (nombres de clase) para que no se salgan de la caja.
    pieces: list[str] = []
    for word in words:
        if len(word) <= max_chars:
            pieces.append(word)
        else:
            while len(word) > max_chars:
                pieces.append(word[:max_chars])
                word = word[max_chars:]
            if word:
                pieces.append(word)
    lines: list[str] = []
    current = pieces[0]
    for piece in pieces[1:]:
        if len(current) + 1 + len(piece) <= max_chars:
            current += " " + piece
        else:
            lines.append(current)
            current = piece
    lines.append(current)
    return lines


def text_block(
    x: float,
    y: float,
    lines: Iterable[str],
    size: float,
    color: str,
    weight: str = "normal",
    anchor: str = "middle",
    line_height: float | None = None,
    italic: bool = False,
    opacity: float = 1.0,
) -> str:
    line_height = line_height or size * 1.25
    style = f"font-family:{FONT};font-size:{size}px;font-weight:{weight};fill:{color};"
    if italic:
        style += "font-style:italic;"
    style += f"opacity:{opacity};"
    out = [f'<text x="{x:.1f}" y="{y:.1f}" text-anchor="{anchor}" style="{style}">']
    for i, line in enumerate(lines):
        dy = 0 if i == 0 else line_height
        out.append(f'<tspan x="{x:.1f}" dy="{dy:.1f}">{esc(line)}</tspan>')
    out.append("</text>")
    return "\n".join(out)


def box(
    x: float,
    y: float,
    w: float,
    h: float,
    title: str,
    stereotype: str,
    lines: list[str],
    kind: str,
    *,
    title_size: float = 15,
    stereo_size: float = 10.5,
    line_size: float = 11.5,
    title_chars: int = 28,
    line_chars: int = 30,
    radius: float = 9,
) -> str:
    fill, border, fg = COLORS[kind]
    title_lines: list[str] = []
    for part in title.split("\n"):
        title_lines.extend(wrap(part, title_chars))
    title_lh = title_size * 1.18
    stereo_lh = stereo_size * 1.25
    line_lh = line_size * 1.25
    total = len(title_lines) * title_lh + stereo_lh + 6 + len(lines) * line_lh
    top = y + max(15.0, (h - total) / 2)

    out = [
        f'<g class="c3-box">',
        f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{radius}" '
        f'fill="{fill}" stroke="{border}" stroke-width="1.4" filter="url(#shadow)"/>',
    ]
    cy = top + title_size
    out.append(
        text_block(
            x + w / 2,
            cy,
            title_lines,
            title_size,
            fg,
            weight="600",
            line_height=title_lh,
        )
    )
    cy += len(title_lines) * title_lh
    if stereotype:
        out.append(
            text_block(
                x + w / 2,
                cy + 1,
                [stereotype],
                stereo_size,
                fg,
                italic=True,
                opacity=0.88,
                line_height=stereo_lh,
            )
        )
        cy += stereo_lh
    cy += 6
    if lines:
        out.append(
            text_block(
                x + w / 2,
                cy + line_size,
                lines,
                line_size,
                fg,
                line_height=line_lh,
                opacity=0.96,
            )
        )
    out.append("</g>")
    return "\n".join(out)


def boundary(
    x: float,
    y: float,
    w: float,
    h: float,
    label: str,
    *,
    dashed: bool = True,
    fill: str = "none",
    stroke: str = "#8A94A6",
    label_color: str = "#344054",
    label_size: float = 13,
    radius: float = 12,
) -> str:
    dash = ' stroke-dasharray="8 5"' if dashed else ""
    return "\n".join(
        [
            f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{radius}" '
            f'fill="{fill}" stroke="{stroke}" stroke-width="1.2"{dash}/>',
            text_block(
                x + 18,
                y + 20,
                [label],
                label_size,
                label_color,
                weight="600",
                anchor="start",
            ),
        ]
    )


def arrow(
    points: list[tuple[float, float]],
    *,
    color: str = "#42526E",
    dashed: bool = False,
    label: str = "",
    label_at: tuple[float, float] | None = None,
    label_size: float = 10.5,
    width: float = 1.4,
) -> str:
    if len(points) < 2:
        return ""
    path = "M " + " L ".join(f"{x:.1f} {y:.1f}" for x, y in points)
    dash = ' stroke-dasharray="6 4"' if dashed else ""
    out = [
        f'<path d="{path}" fill="none" stroke="{color}" stroke-width="{width}"'
        f'{dash} marker-end="url(#arrow)" stroke-linejoin="round"/>'
    ]
    if label:
        lx, ly = label_at or points[len(points) // 2]
        approx = max(38, len(label) * label_size * 0.56)
        out.append(
            f'<rect x="{lx - approx/2:.1f}" y="{ly - 9:.1f}" width="{approx:.1f}" height="15" '
            f'rx="4" fill="#FFFFFF" stroke="#D0D5DD" stroke-width="0.7"/>'
        )
        out.append(
            text_block(lx, ly + 2, [label], label_size, color, weight="600")
        )
    return "\n".join(out)


def note(
    x: float,
    y: float,
    w: float,
    h: float,
    title: str,
    lines: list[str],
    *,
    size: float = 11,
) -> str:
    fill, border, fg = COLORS["nota"]
    out = [
        f'<g class="c3-note">',
        f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="8" '
        f'fill="{fill}" stroke="{border}" stroke-width="1.1"/>',
        f'<path d="M {x+w-18:.1f} {y:.1f} L {x+w:.1f} {y:.1f} L {x+w:.1f} {y+18:.1f} Z" '
        f'fill="#E8D98D" stroke="{border}" stroke-width="1"/>',
        text_block(x + 12, y + 19, [title], size + 0.8, fg, weight="700", anchor="start"),
    ]
    out.append(
        text_block(
            x + 12,
            y + 38,
            lines,
            size,
            fg,
            anchor="start",
            line_height=size * 1.35,
        )
    )
    out.append("</g>")
    return "\n".join(out)


def header(width: int, title: str, subtitle: str) -> str:
    return "\n".join(
        [
            text_block(
                width / 2,
                27,
                [title],
                22,
                "#101828",
                weight="700",
                line_height=26,
            ),
            text_block(
                width / 2,
                53,
                [subtitle],
                12.5,
                "#475467",
                line_height=16,
            ),
        ]
    )


def svg_document(width: int, height: int, body: str) -> str:
    return f'''<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}">
  <defs>
    <filter id="shadow" x="-20%" y="-20%" width="140%" height="140%">
      <feDropShadow dx="0" dy="2" stdDeviation="2.2" flood-color="#101828" flood-opacity="0.16"/>
    </filter>
    <marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="#42526E"/>
    </marker>
  </defs>
  <rect width="{width}" height="{height}" fill="#FFFFFF"/>
  {body}
</svg>
'''


def save(name: str, width: int, height: int, body: str) -> None:
    svg = svg_document(width, height, body)
    (BASE / f"{name}.svg").write_text(svg, encoding="utf-8")
    # HTML auxiliar para rasterizar con Chrome/Edge sin depender de librerias SVG.
    html_doc = f'''<!doctype html>
<html lang="es"><head><meta charset="utf-8"><style>
html,body{{margin:0;padding:0;background:#fff;width:{width}px;height:{height}px;overflow:hidden}}
</style></head><body>{svg}</body></html>'''
    (BASE / f"{name}.html").write_text(html_doc, encoding="utf-8")


# ---------------------------------------------------------------------------
# C3 - Oportunidades
# ---------------------------------------------------------------------------
def generar_oportunidades() -> None:
    W, H = 1700, 1370
    body: list[str] = [header(W, "C3 - Módulo Oportunidades", "Vista de componentes internos y relaciones N:M verificadas - paquete com.viernes.oportunidades")]

    # Limites del modulo y grupos de capas
    body.append(boundary(30, 85, 1640, 1030, "Módulo Oportunidades (com.viernes.oportunidades)", fill="#F7F9FC"))
    body.append(boundary(55, 120, 1590, 150, "Entrada REST", fill="#FFFFFF"))

    # Fila 1: controlador
    body.append(box(700, 145, 300, 110, "OportunidadController", "@RestController",
                    ["CRUD en /api/v1/oportunidades", "Subrecursos: /{id}/contactos,", "/{id}/productos y /{id}/colaboradores"],
                    "entrada", title_chars=26, line_chars=34))

    # Fila 2: aplicacion
    body.append(boundary(55, 300, 1590, 150, "Aplicación", fill="#FFFFFF"))
    body.append(box(505, 320, 310, 110, "ManageOportunidadUseCase", "puerto de entrada · interface",
                    ["CRUD, etapa, contactos,", "productos y colaboradores"],
                    "aplicacion", title_chars=26, line_chars=30))
    body.append(box(885, 320, 310, 110, "OportunidadService", "@Service",
                    ["Implementa el puerto de entrada", "Orquesta 4 puertos de persistencia", "y 3 puertos de consulta"],
                    "aplicacion", title_chars=24, line_chars=34))

    # Fila 3: dominio
    body.append(boundary(55, 480, 1590, 175, "Dominio", fill="#FFFFFF"))
    domain = [
        (95, "Oportunidad", "entidad de dominio", ["Datos de la oportunidad", "EtapaOportunidad"]),
        (455, "OportunidadContacto", "entidad puente N:M", ["idOportunidad + idContacto", "rol + esDecisor"]),
        (815, "OportunidadProducto", "entidad puente N:M", ["idOportunidad + idProducto", "cantidad"]),
        (1175, "OportunidadColaborador", "entidad puente N:M", ["idOportunidad + idUsuario", "creadoEn"]),
    ]
    for x, title, stereo, lines in domain:
        body.append(box(x, 505, 320 if len(title) < 20 else 340, 120, title, stereo, lines, "dominio",
                        title_chars=30 if len(title) < 20 else 32, line_chars=34))

    # Fila 4: puertos (una fila de 7)
    body.append(boundary(55, 685, 1590, 155, "Puertos de salida (7 interfaces)", fill="#FFFFFF"))
    ports = [
        ("ContactoQueryPort", ["buscarPorId(idContacto)"]),
        ("ProductoQueryPort", ["buscarPorId(idProducto)"]),
        ("UsuarioQueryPort", ["buscarPorId(idUsuario)"]),
        ("OportunidadContacto\nPersistencePort", ["save · find · exists", "deleteByIdOportunidad"]),
        ("OportunidadProducto\nPersistencePort", ["save · find · exists", "deleteByIdOportunidad"]),
        ("OportunidadColaborador\nPersistencePort", ["save · find · exists", "deleteByIdOportunidad"]),
        ("OportunidadPersistence\nPort", ["CRUD, filtros y pipeline"]),
    ]
    port_x = [75 + i * 222 for i in range(7)]
    for x, (title, lines) in zip(port_x, ports):
        body.append(box(x, 715, 205, 105, title, "interface", lines, "puerto", title_chars=24, line_chars=24,
                        title_size=13.5, line_size=11))

    # Fila 5: adaptadores (alineados con los puertos)
    body.append(boundary(55, 875, 1590, 165, "Adaptadores de salida (7 clases)", fill="#FFFFFF"))
    adapters = [
        ("ContactoQueryAdapter", ["-> ManageClienteUseCase"]),
        ("ProductoQueryAdapter", ["-> ManageProductoUseCase"]),
        ("UsuarioQueryAdapter", ["-> UsuariosApi"]),
        ("OportunidadContacto\nPersistenceAdapter", ["Repository -> Entity", "CRM_OPORTUNIDAD_CONTACTOS"]),
        ("OportunidadProducto\nPersistenceAdapter", ["Repository -> Entity", "CRM_OPORTUNIDAD_PRODUCTOS"]),
        ("OportunidadColaborador\nPersistenceAdapter", ["Repository -> Entity", "CRM_OPORTUNIDAD_COLABORADORES"]),
        ("OportunidadPersistence\nAdapter", ["Repository -> Entity", "CRM_OPORTUNIDADES"]),
    ]
    for x, (title, lines) in zip(port_x, adapters):
        body.append(box(x, 905, 205, 115, title, "@Component", lines, "adaptador", title_chars=24, line_chars=24,
                        title_size=13.5, line_size=10.5))

    # Conexiones principales
    body.append(arrow([(850, 255), (850, 320)], label="invoca", label_at=(915, 288)))
    body.append(arrow([(885, 375), (815, 375)], label="implementa", label_at=(850, 360)))
    body.append(arrow([(1040, 430), (1000, 480)], label="orquesta 4 clases", label_at=(1090, 462)))
    # service -> ports: ruta por el borde derecho para no cruzar dominio
    body.append(arrow([(1195, 395), (1645, 395), (1645, 685)], label="usa 7 puertos", label_at=(1598, 600)))

    # puerto -> adaptador (7 flechas verticales)
    for i, x in enumerate(port_x):
        cx = x + 102
        body.append(arrow([(cx, 820), (cx, 905)], width=1.2))

    # adaptadores de consulta -> modulos externos (se dibujan en la zona inferior)
    # Zona externa: fuera del limite del modulo
    body.append(box(95, 1120, 240, 95, "Clientes", "módulo externo", ["ManageClienteUseCase"], "externo", title_chars=22))
    body.append(box(365, 1120, 240, 95, "Productos", "módulo externo", ["ManageProductoUseCase"], "externo", title_chars=22))
    body.append(box(635, 1120, 240, 95, "Usuarios", "módulo externo", ["UsuariosApi"], "externo", title_chars=22))
    body.append(box(1359, 1120, 300, 95, "Oracle Database", "sistema externo", ["Tablas CRM_* - JPA/Hibernate"], "externo", title_chars=24))

    # consultas externas: adaptadores 0,1,2 -> Clientes, Productos, Usuarios
    for ax, tx in [(177, 215), (399, 485), (621, 755)]:
        body.append(arrow([(ax, 1020), (ax, 1060), (tx, 1060), (tx, 1120)], width=1.2))
    # persistencia: 4 adaptadores -> bus -> Oracle
    persist_centers = [port_x[i] + 102 for i in range(3, 7)]
    for cx in persist_centers:
        body.append(f'<path d="M {cx} 1020 L {cx} 1070" fill="none" stroke="#42526E" stroke-width="1.4"/>')
    body.append(f'<path d="M {persist_centers[0]} 1070 L 1509 1070 L 1509 1120" fill="none" stroke="#42526E" stroke-width="1.4" marker-end="url(#arrow)"/>')
    body.append(text_block((persist_centers[0] + 1509) / 2, 1088, ["4 adaptadores JPA"], 10.5, "#42526E", weight="600"))

    # Nota N:M
    body.append(note(75, 1240, 760, 105, "Materialización N:M verificada", [
        "No existe @ManyToMany ni @JoinTable. Cada relación usa entidad puente con IDs escalares,",
        "puerto de persistencia, adaptador JPA y repositorio Spring Data. Oracle define FK físicas",
        "y restricción única (oportunidad, contraparte) en las tres tablas. El mapeo exacto consta",
        "en la Tabla 6. Este C3 no representa tablas: solo clases, interfaces y módulos reales.",
    ]))
    body.append(note(860, 1240, 760, 105, "Lectura de la figura", [
        "Azul: entrada REST. Turquesa: aplicación. Morado: dominio. Naranja: puertos de salida.",
        "Verde: adaptadores. Gris: módulos o sistemas externos. Las flechas entre capas son",
        "agregadas; el detalle puerto -> adaptador esta alineado verticalmente y en la Tabla 6.",
        "El servicio usa shared.exception.ResourceNotFoundException para respuestas 404.",
    ]))

    save("C3_oportunidades", W, H, "\n".join(body))


def generar_actividades() -> None:
    W, H = 1700, 1450
    body: list[str] = [header(W, "C3 - Módulo Actividades", "Vista de componentes internos, participantes y etiquetas N:M - paquete com.viernes.actividades")]

    body.append(boundary(30, 85, 1640, 1000, "Módulo Actividades (com.viernes.actividades)", fill="#F7F9FC"))
    body.append(boundary(55, 120, 1590, 150, "Entrada REST", fill="#FFFFFF"))
    body.append(box(700, 145, 300, 110, "ActividadController", "@RestController",
                    ["CRUD en /api/v1/actividades", "Participantes: /{id}/participantes", "Filtros y PATCH /{id}/estado"],
                    "entrada", title_chars=24, line_chars=34))

    # Aplicacion
    body.append(boundary(55, 300, 1590, 150, "Aplicación", fill="#FFFFFF"))
    body.append(box(400, 320, 310, 110, "ManageActividadUseCase", "puerto de entrada · interface",
                    ["CRUD, filtros, estado", "participantes y etiquetas"],
                    "aplicacion", title_chars=26, line_chars=30))
    body.append(box(760, 320, 310, 110, "ActividadService", "@Service",
                    ["Implementa el puerto de entrada", "Orquesta 5 puertos internos", "y depende de EtiquetaPersistencePort"],
                    "aplicacion", title_chars=22, line_chars=34))
    body.append(box(1180, 320, 340, 110, "EtiquetaPersistencePort", "puerto del módulo Etiquetas",
                    ["findById · findAllById", "dependencia directa entre módulos"],
                    "externo", title_chars=26, line_chars=30))
    body.append(arrow([(1070, 375), (1180, 375)], label="usa", label_at=(1125, 360)))

    # Dominio
    body.append(boundary(55, 480, 1590, 175, "Dominio", fill="#FFFFFF"))
    body.append(box(95, 505, 480, 120, "Actividad", "entidad de dominio",
                    ["idUsuario, idCliente, idLead, idOportunidad", "tipo, estado, fecha y descripción",
                     "TipoActividad · EstadoActividad"],
                    "dominio", title_chars=24, line_chars=52))
    body.append(box(615, 505, 430, 120, "ActividadParticipante", "entidad puente N:M",
                    ["idActividad + idContacto", "creadoEn"],
                    "dominio", title_chars=24, line_chars=40))
    body.append(box(1085, 505, 430, 120, "ActividadEtiqueta", "entidad puente N:M",
                    ["idActividad + idEtiqueta", "orden + creadoEn"],
                    "dominio", title_chars=24, line_chars=40))

    # Puertos internos
    body.append(boundary(55, 690, 1590, 150, "Puertos de salida internos (5 interfaces)", fill="#FFFFFF"))
    ports = [
        ("ContactoQueryPort", ["buscarPorId(idContacto)"]),
        ("RelacionValidationPort", ["existeCliente · existeLead", "existeOportunidad · existeUsuario"]),
        ("ActividadPersistencePort", ["CRUD, filtros y findByEtiquetaId"]),
        ("ActividadParticipante\nPersistencePort", ["save · find · exists · delete"]),
        ("ActividadEtiqueta\nPersistencePort", ["find · reemplazarEtiquetas · delete"]),
    ]
    port_x = [75 + i * 320 for i in range(5)]
    for x, (title, lines) in zip(port_x, ports):
        body.append(box(x, 715, 300, 105, title, "interface", lines, "puerto", title_chars=28, line_chars=30,
                        title_size=13.5, line_size=11))

    # Adaptadores
    body.append(boundary(55, 875, 1590, 160, "Adaptadores de salida (5 clases)", fill="#FFFFFF"))
    adapters = [
        ("ContactoQueryAdapter", ["-> ManageClienteUseCase", "recorre clientes y contactos"]),
        ("RelacionValidationAdapter", ["-> repositorios OPEN de", "Clientes, Leads y Oportunidades", "-> UsuariosApi"]),
        ("ActividadPersistenceAdapter", ["Repository -> Entity", "CRM_ACTIVIDADES"]),
        ("ActividadParticipante\nPersistenceAdapter", ["Repository -> Entity", "CRM_ACTIVIDAD_PARTICIPANTES"]),
        ("ActividadEtiqueta\nPersistenceAdapter", ["Repository -> Entity", "CRM_ACTIVIDAD_ETIQUETAS"]),
    ]
    for x, (title, lines) in zip(port_x, adapters):
        body.append(box(x, 895, 300, 120, title, "@Component", lines, "adaptador", title_chars=28, line_chars=30,
                        title_size=13.5, line_size=10.5))

    # Conexiones
    body.append(arrow([(850, 255), (850, 320)], label="invoca", label_at=(915, 288)))
    body.append(arrow([(760, 375), (710, 375)], label="implementa", label_at=(735, 360)))
    body.append(arrow([(915, 430), (915, 480)], label="orquesta 3 clases", label_at=(985, 462)))
    # service -> ports: baja por el borde derecho, por debajo del puerto externo de Etiquetas
    body.append(arrow([(1060, 430), (1060, 465), (1645, 465), (1645, 690)], label="usa 5 puertos", label_at=(1595, 565)))

    # puerto -> adaptador
    for x in port_x:
        cx = x + 150
        body.append(arrow([(cx, 820), (cx, 895)], width=1.2))

    # adaptadores -> externos
    # Consulta de contactos: adaptador 0 -> Clientes dentro del grupo de validacion
    body.append(arrow([(225, 1015), (225, 1060), (175, 1060), (175, 1125)], width=1.2))
    # Validacion: adaptador 1 -> grupo de modulos OPEN + Usuarios
    body.append(arrow([(545, 1015), (545, 1090)], label="4 validaciones", label_at=(615, 1050)))
    # Persistencia: adaptadores 2,3,4 -> bus -> Oracle
    persist_centers = [port_x[i] + 150 for i in range(2, 5)]
    for cx in persist_centers:
        body.append(f'<path d="M {cx} 1015 L {cx} 1075" fill="none" stroke="#42526E" stroke-width="1.4"/>')
    body.append(f'<path d="M {persist_centers[0]} 1075 L 1470 1075 L 1470 1125" fill="none" stroke="#42526E" stroke-width="1.4" marker-end="url(#arrow)"/>')
    body.append(text_block((persist_centers[0] + 1470) / 2, 1093, ["3 adaptadores JPA"], 10.5, "#42526E", weight="600"))

    # Grupo externo de validacion (etiqueta en la parte inferior para no cruzar flechas)
    body.append(boundary(55, 1090, 1000, 210, "", fill="#FFFFFF"))
    body.append(text_block(70, 1283, ["Validación de relaciones: módulos OPEN + Usuarios"], 11, "#344054", weight="600", anchor="start"))
    body.append(box(70, 1125, 210, 90, "Clientes", "módulo OPEN", ["ClienteSpringDataRepository"], "externo", title_chars=18))
    body.append(box(300, 1125, 210, 90, "Leads", "módulo OPEN", ["LeadSpringDataRepository"], "externo", title_chars=18))
    body.append(box(530, 1125, 240, 90, "Oportunidades", "módulo OPEN", ["OportunidadSpringDataRepository"], "externo", title_chars=18))
    body.append(box(790, 1125, 200, 90, "Usuarios", "módulo de administración", ["UsuariosApi"], "externo", title_chars=18))
    # Oracle fuera del grupo
    body.append(box(1320, 1125, 300, 90, "Oracle Database", "sistema externo", ["Tablas CRM_* - JPA/Hibernate"], "externo", title_chars=24))

    body.append(note(75, 1325, 760, 105, "Materialización N:M verificada", [
        "ActividadParticipante y ActividadEtiqueta son entidades puente con IDs escalares; no hay",
        "@ManyToMany ni @JoinTable. Cada una tiene puerto, adaptador, repositorio y entidad JPA.",
        "Oracle define FK físicas y restricción única (actividad, contacto/etiqueta). Las etiquetas",
        "se reemplazan en bloque (máximo 8) y se ordenan por la columna ORDEN.",
    ]))
    body.append(note(860, 1325, 760, 105, "Lectura de la figura", [
        "Azul: entrada REST. Turquesa: aplicación. Morado: dominio. Naranja: puertos internos.",
        "Verde: adaptadores. Gris: puertos/módulos/sistemas externos. El servicio depende",
        "directamente de EtiquetaPersistencePort (módulo Etiquetas), una dependencia verificada.",
        "RelacionValidationAdapter accede a repositorios de módulos OPEN y a UsuariosApi.",
    ]))

    save("C3_actividades", W, H, "\n".join(body))


def main() -> None:
    generar_oportunidades()
    generar_actividades()
    print("SVG generados en", BASE)


if __name__ == "__main__":
    main()





