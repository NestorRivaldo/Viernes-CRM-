# 13 — AUDITORÍA DE CÓDIGO AJENO Y RESIDUOS

**Fecha:** 22 de noviembre de 2026
**Alcance:** `E:\Viernes\Viernes` (raíz), backend, frontend, base de datos, observabilidad e informes.
**Motivo:** el repositorio contiene archivos y configuraciones que **no pertenecen a Viernes CRM V1**.

---

## Veredicto

Sí: hay **tres capas de contaminación** distintas, y conviene tratarlas por separado porque tienen riesgos distintos:

| Capa | Qué es | Riesgo | Volumen |
|---|---|---|---|
| **A. Otro sistema** | Módulo Odoo `club_automation` + asistente Ollama | Alto (confusión, agentes IA mal instruidos) | 20 archivos, ~117 KB, ~1.990 líneas |
| **B. Otro proyecto** | `README.md` y `CLAUDE.md` de **LP2 / BomERP** | **Muy alto** (documentación raíz falsa + instrucciones erróneas para IA) | 2 archivos, ~13 KB |
| **C. Nombres heredados** | `bomerp-*` en logs y observabilidad | Medio (dashboards y logs no cuadran) | 7 archivos |
| **D. Versión anterior** | Frontend vanilla embebido en el backend | Medio (código muerto servido en :8080) | 3 archivos, ~45 KB |
| **E. Duplicación** | SQL en 3 copias, informes en 4 versiones | Bajo, pero alto en mantenimiento | ~56 MB en ZIP + cientos de archivos |

Lo importante: **el código Java y TypeScript de Viernes está limpio**. No hay paquetes `pe.edu.upeu.bomerp`, ni entidades con `schema = "BOM_*"`, ni referencias a Odoo dentro de `com.viernes` ni en `src/app`. La contaminación está **alrededor** del código, no dentro.

---

## Hallazgo A — Archivos de otro sistema en la raíz (20 archivos)

Todos **no rastreados por git** (`??`), es decir: se copiaron a la carpeta pero nunca se commitearon.

### A.1 Módulo Odoo `club_automation`

| Archivo | Tamaño | Qué es |
|---|---|---|
| `club_automation_wizard_new.py` | 30,9 KB | Modelo/wizard de Odoo (Python) |
| `automation_wizard_v2.py` | 31,3 KB | Segunda versión del mismo wizard |
| `automation_wizard_views_new.xml` | 3,6 KB | Vistas XML de Odoo |
| `assistant_capsule.xml` | 1,9 KB | Plantilla **Owl** (framework de Odoo) |
| `assistant_capsule.js` | 5,0 KB | Componente Owl |
| `assistant_capsule.css` | 2,2 KB | Estilos del componente |
| `automation_conversation.py` | 1,5 KB | Conversación del asistente |
| `assistant_controller.py` | 2,8 KB | Controlador HTTP del asistente |
| `models_init.py` | 0,1 KB | `__init__` de modelos Odoo |
| `controllers_init.py` | 0,1 KB | `__init__` de controladores Odoo |
| `fix_manifest.py` | 0,2 KB | Parche de manifiesto |
| `gen_assets.py` | 1,1 KB | Generador de assets |
| `gen_patch.py` / `gen_patch2.py` | 8,8 / 8,6 KB | Generadores de parches |
| `patch_args.py` | 1,3 KB | Parche |
| `patch_assets2.py` | 0,5 KB | Parche |
| `patch_csv.py` | 0,4 KB | Parche |
| `patch_tools4.py` | 5,3 KB | Parche |

**Evidencia de que es Odoo:** `assistant_capsule.xml` usa `<t t-name="club_automation.AssistantCapsule" owl="1">`, es decir, plantillas **Owl** del módulo `club_automation`. Nada de eso existe en Viernes.

### A.2 Plan del asistente Ollama

| Archivo | Tamaño | Qué es |
|---|---|---|
| `PLAN_OLLAMA_ASISTENTE.md` | 11,6 KB | Plan de integración de Ollama (`llama3.2:1b`) en un CRM, con host EC2 |
| `test_ollama.json` | 0,3 KB | Payload de prueba |

**Ojo:** el documento habla de *"el CRM Viernes"* y de un asistente conversacional. Es un plan **para** Viernes, pero **no implementado**: no existe ningún módulo `asistente` en el backend ni en el frontend (verificado con búsqueda de `ollama|asistente|assistant|odoo` en `com.viernes` y `src/app`: **0 resultados**). Es documentación de una idea descartada o pendiente, no código del sistema.

### A.3 Totales

```
20 archivos · 117,4 KB · 1.988 líneas de Python/JS/XML/CSS
```

**Riesgo concreto:** si alguien hace `git add .`, estos archivos entran al repositorio del proyecto académico. Y si un agente IA los lee, puede creer que el proyecto usa Odoo.

---

## Hallazgo B — README y CLAUDE.md de OTRO proyecto (los dos rastreados por git)

Este es el hallazgo más grave, porque está **commiteado** y es lo primero que ve cualquiera.

### B.1 `README.md` (raíz)

Empieza con:

```markdown
# LP2

Carpeta preparada para artefactos del curso **Lenguaje de Programacion II**.
...
El backend es un unico proyecto Spring Boot organizado por modulos de negocio mediante Spring
Modulith (ver [`docs/lp2/adr/`](../docs/lp2/adr/))...
El proyecto backend vive en [`bomerp-backend/`](bomerp-backend/)...
```

Problemas:

- Título **"LP2"**, no "Viernes CRM".
- Referencia `bomerp-backend/` y `bomerp-frontend/`, que **no existen** (las carpetas reales son `viernes-backend/` y `viernes-frontend/`).
- Referencia `docs/lp2/adr/`, que **no existe** (la carpeta `docs/` fue borrada).
- Habla de módulos `catalogo` y `ventas` con `Categoria`/`Producto`/`Venta`, que **no existen** en Viernes.
- El comando de arranque dice `cd lp2/bomerp-backend`, ruta inexistente.

La última sección (DNI/RUC con APIsPERU) sí es de Viernes. Es decir: el README es un **híbrido** entre el README de BomERP y un anexo de Viernes.

### B.2 `CLAUDE.md` (raíz)

```markdown
# LP2 - BomERP backend/frontend
...
Paquete raíz `pe.edu.upeu.bomerp`.
...
Entidades JPA con `schema = "BOM_<MODULO>"` en `@Table`.
...
Los mappers de ambos son **MapStruct** (interfaces `@Mapper`), no clases manuales.
```

**Este archivo es peligroso en la práctica.** Cualquier agente IA que abra el proyecto lee `CLAUDE.md` como instrucciones del repositorio y va a intentar:

- usar el paquete `pe.edu.upeu.bomerp` (el real es `com.viernes`),
- poner `schema = "BOM_<MODULO>"` en las entidades (el real no usa `schema`),
- crear mappers MapStruct (el real usa mapeo manual `toDomain`/`toEntity`, **0 imports de MapStruct**),
- seguir el flujo de sesiones S1–S14 de un curso distinto.

De hecho, ese archivo se cargó como contexto en esta conversación y describe reglas que **no aplican** a Viernes.

### B.3 `docs/` 

Git dice que existen 2 archivos rastreados en `docs/` y que fueron borrados del working tree (`D docs/KEYCLOAK_IMPLEMENTATION_GUIDE.md`, `D docs/informe_keycloak_v1.html`). La carpeta ya no existe. Es basura pendiente de commit.

---

## Hallazgo C — Nombres `bomerp` dentro de configuración real

| Archivo | Línea / valor | Debería ser |
|---|---|---|
| `viernes-backend/src/main/resources/logback-spring.xml` | `<file>logs/bomerp.log</file>` | `logs/viernes.log` |
| ídem | `logs/bomerp-%d{yyyy-MM-dd}.log` | `logs/viernes-%d{yyyy-MM-dd}.log` |
| `obs/compose-dev.yml` | `name: bomerp-obs-dev` | `viernes-obs-dev` |
| ídem | contenedores `bomerp-prometheus-dev`, `bomerp-loki-dev`, `bomerp-promtail-dev`, `bomerp-grafana-dev` | `viernes-*` |
| ídem | `../viernes-backend/logs:/var/log/bomerp-backend:ro` | `/var/log/viernes-backend` |
| `obs/promtail/promtail-config.yml` | `job_name: bomerp-backend`, `application: bomerp-backend`, `__path__: /var/log/bomerp-backend/*.log` | `viernes-backend` |
| `obs/promtail/promtail-config-dev.yml` | idem | idem |
| `obs/prometheus/prometheus.yml` y `-dev.yml` | `job_name: "bomerp-backend"`, target `bomerp-backend:8080` | `viernes-backend:8080` |
| `obs/grafana/provisioning/dashboards/dashboards.yml` | carpeta `BomERP` | `Viernes CRM` |
| `obs/grafana/provisioning/datasources/datasources.yml` | `http://bomerp-prometheus:9090`, `http://bomerp-loki:3100` | `viernes-*` |
| `obs/compose.yml` | comentarios "PLACEHOLDER ... bomerp-backend ... bomerp-prod-net" | `viernes-*` |

**Consecuencia real:** el `promtail` monta `../viernes-backend/logs` pero busca archivos en `/var/log/bomerp-backend/`. Como el log se llama `bomerp.log`, el pipeline "funciona por casualidad". Si renombras el log sin tocar el promtail, se rompe la observabilidad.

**Nota aparte:** `BOM_VENTAS` (usuario Oracle) también viene de "BOM ERP". Pero ese nombre está en 40+ scripts, en la BD ya creada y en las credenciales. **No recomiendo renombrarlo**: el costo es alto y el beneficio cosmético. Solo conviene documentarlo como "nombre histórico".

---

## Hallazgo D — Frontend vanilla de una versión anterior, servido por el backend

`viernes-backend/src/main/resources/static/`:

```
app.js      21,3 KB   (fecha 2026-11-01)
index.html   9,7 KB   (fecha 2026-11-01)
style.css   13,8 KB   (fecha 2026-11-01)
```

Contenido: un CRM completo en **JavaScript puro** (sin Angular), con `fetch` a la API, gráficos con Chart.js desde CDN y navegación por `onclick`.

```javascript
/* VIERNES CRM — app.js */
const API = '';  // mismo origen, Spring Boot sirve el frontend
```

Y `KeycloakSecurityConfig` lo deja público explícitamente:

```java
.requestMatchers("/static/**", "/", "/index.html").permitAll()
```

**Riesgo:** quien abra `http://localhost:8080/` verá **esa** versión vieja, no la Angular (`http://localhost:4200`). Y no tiene autenticación: es un CRM completo sin login. En una demo, eso confunde.

Es código muerto: la app real es `viernes-frontend/`. No lo usa ningún test ni configuración (salvo el `permitAll`).

---

## Hallazgo E — SQL duplicado (tres copias del mismo esquema)

```
base de datos/
├── 01_tablespaces/ … 08_datos_semilla/   ← estructura modular, la que usa ejecutar_todo.sql
├── init/001…006                         ← copia consolidada para Docker
└── (idéntica a) viernes-backend/db/init/001…006
```

Verificado por hash MD5: los 6 archivos de `base de datos/init/` y `viernes-backend/db/init/` son **idénticos**.

Es decir, el mismo DDL existe **tres veces**:

1. `base de datos/03_tablas/*.sql` (11 archivos, modular)
2. `base de datos/init/003_esquema.sql` (consolidado)
3. `viernes-backend/db/init/003_esquema.sql` (copia idéntica)

Además:

- `viernes-backend/seed-dev.sql` está marcado como **DEPRECADO** pero sigue en el repo.
- `base de datos/README.md` dice `01_usuario_bom_ventas.sql` en la raíz del árbol, pero el archivo está en `02_usuarios_y_permisos/`. La ruta del README está rota.
- `base de datos/capturas/` contiene 2 PNG que no son SQL.

**Riesgo:** alguien edita `init/003_esquema.sql` (porque es el que corre Docker) y no actualiza `03_tablas/04_productos.sql`. Las dos versiones divergen en silencio.

---

## Hallazgo F — Basura de informes (≈ 56 MB en ZIP + duplicados)

### F.1 ZIP sueltos (12 archivos, ~56 MB)

| ZIP | Tamaño |
|---|---|
| `informe/evidencias_academicas_v3 (2).zip` | **27,0 MB** |
| `informe/manual_usuario_v2.zip` | 5,0 MB |
| `informe/V2.zip` | 5,0 MB |
| `informe/capturas_manual_v2.zip` | 4,9 MB |
| `informe/manual_usuario_v1.zip` | 4,8 MB |
| `informe/capturas_manual_v1.zip` | 4,7 MB |
| `informe/capturas_sistema_v1.zip` | 2,6 MB |
| `informe/capturas_sistema_v1 (2).zip` | 2,6 MB |
| `informe/c4_nivel_3.zip` | 1,2 MB |
| `informe/evidencias_academicas_v3.zip` | 0,7 MB |
| `informe/informes.zip`, `12_Pruebas.zip` | ~0 MB |

Los ZIP son **copias comprimidas de carpetas que ya están descomprimidas al lado**. El sufijo `(2)` indica descarga duplicada desde el navegador.

### F.2 Carpetas duplicadas

| Ruta A | Ruta B | Estado |
|---|---|---|
| `V2/manual_usuario_v2.html` | `informe/manual_usuario_v2/manual_usuario_v2.html` | **IDÉNTICOS** (mismo MD5) |
| `V2/` (31 archivos) | `informe/V2/` (31 archivos) | Duplicado |
| `informe/manual_usuario_v1/` | `informe/capturas_manual_v1/` | Capturas repetidas |
| `informe/manual_usuario_v2/` | `informe/capturas_manual_v2/` | Capturas repetidas |
| `informe/evidencias_academicas_v3/` | `informe/evidencias_academicas_v3 (2).zip` | ZIP de la misma carpeta |

### F.3 Carpetas con nombres que admiten obsolescencia

- `informe/informes de desarrollo desactualizados/` (10 archivos)
- `informe/informes de sistema V1/` (11 archivos)
- `informe/capturas_sistema_v1/` (18 archivos)

### F.4 Cifras desactualizadas en los informes

Busqué `26 tablas|22 tablas|10 tablas|BOM_VENTAS` en `informe/`:

| Archivo | Menciones | Problema |
|---|---|---|
| `informe/informe_tecnico_sistema_v1.html` | 7 | Dice **26 tablas** (hoy son 21) |
| `informe/informes de sistema V1/informe_tecnico_sistema_v1.html` | 7 | idem |
| `informe/informes de desarrollo desactualizados/AUDITORIA_MODELO_DATOS_NM_VIERNES_V1.md` | 6 | idem |
| `informe/informe_guia_activacion_otra_pc.html` | 5 | idem |
| `informe/c4_nivel_3/DOCUMENTO_ARQUITECTURA_C3.md` | 1 | Dice "26 tablas" |

Los informes **vigentes** (`informe/informes/08…12`, generados en esta etapa) sí dicen 21. El problema son los antiguos, que siguen visibles.

### F.5 Estado en git

Solo **14 archivos** de `informe/` están rastreados. Los otros cientos son `??` (no rastreados). Es decir, el repositorio no tiene ni la mitad de lo que se ve.

---

## Hallazgo G — Estado de git

```
Rama: sin remoto configurado
Último commit: 298ac64 feat(crm-v1): wave 1 - productos, equipos, participantes, fuentes y campanias
```

- **No hay remoto** (`git remote -v` vacío): todo vive solo en esta máquina.
- Hay **~380 archivos sin rastrear**, incluidos módulos completos (`etiquetas`, `mensajeria`, `relationshipmap`, `shared/cache`, `shared/ratelimit`) y casi todo el frontend nuevo.
- Hay **borrados sin commitear**: el auth local antiguo (`AuthController`, `JwtService`, `JwtAuthFilter`, `SecurityConfig`) fue eliminado del working tree pero sigue en `HEAD`.
- Los 20 archivos ajenos del Hallazgo A **no están ignorados** por `.gitignore`, así que un `git add .` los subiría.

**Riesgo real:** si alguien hace `git checkout .` o clona el repo, **pierde** etiquetas, mensajería, relationshipmap, caché, rate limit y casi todo el frontend.

---

## Resumen de impacto

| # | Hallazgo | Archivos | Peso | ¿Rastreado? | Prioridad |
|---|---|---|---|---|---|
| A | Odoo + Ollama ajenos | 20 | 117 KB | No | **Alta** |
| B | README + CLAUDE.md de BomERP | 2 | 13 KB | **Sí** | **Crítica** |
| C | Nombres `bomerp` en logs/obs | 7 | — | Parcial | Media |
| D | Frontend vanilla embebido | 3 | 45 KB | No | Media |
| E | SQL triplicado | 15 | 0,6 MB | Parcial | Media |
| F | ZIPs y carpetas duplicadas | ~400 | ~56 MB | No | Baja |
| G | Git atrasado / sin remoto | ~380 | — | — | **Alta** |

---

## Plan de limpieza (por fases)

> **Fase 1 ejecutada el 22-11-2026.** Las fases 2–7 siguen solo propuestas.

### Fase 1 — Aislar lo ajeno ✅ EJECUTADA

**Decisión del usuario (22-11-2026):** mover únicamente lo que pertenece a Odoo/Ollama; el resto del proyecto se deja como está.

**Acción aplicada:**

- Los 20 archivos se **movieron** (no se borraron) a `E:\_cuarentena_viernes\odoo-ollama\`.
- Se conservó el bloque de `.gitignore` con los nombres exactos (no con comodines amplios), como protección por si alguien los copia de vuelta al repo.

```gitignore
# --- Trabajo externo: modulo Odoo club_automation + asistente Ollama ---
# Se conservan a proposito en la raiz, pero NO pertenecen a Viernes CRM.
assistant_capsule.css
assistant_capsule.js
assistant_capsule.xml
assistant_controller.py
automation_conversation.py
automation_wizard_v2.py
automation_wizard_views_new.xml
club_automation_wizard_new.py
controllers_init.py
fix_manifest.py
gen_assets.py
gen_patch.py
gen_patch2.py
models_init.py
patch_args.py
patch_assets2.py
patch_csv.py
patch_tools4.py
PLAN_OLLAMA_ASISTENTE.md
test_ollama.json
__pycache__/
*.pyc
```

**Verificado:**

- Raíz del repo: solo quedan `.gitignore`, `README.md`, `CLAUDE.md`, `activar-sistema.bat` y `compose-keycloak.yml`.
- `git status` no muestra ningún rastro de `assistant*`, `automation*`, `ollama`, `club_automation`, `patch_*`, `gen_*`.
- Cuarentena: **20 archivos, 117,4 KB** en `E:\_cuarentena_viernes\odoo-ollama\`.

**Para revertir:** mover los archivos de `E:\_cuarentena_viernes\odoo-ollama\` de vuelta a `E:\Viernes\Viernes\`.

**Consecuencia:** si en el futuro se quiere versionar ese trabajo Odoo/Ollama, debe ir a **su propio repositorio** (este ya no lo rastrea).

### Fase 2 — Corregir la documentación raíz (lo más importante)

1. **`CLAUDE.md`**: reemplazarlo por instrucciones reales de Viernes (paquete `com.viernes`, módulos hexagonales, Oracle `BOM_VENTAS`, Vite/Angular 22, etc.) o eliminarlo.
2. **`README.md`**: escribir uno nuevo con:
   - qué es Viernes CRM V1,
   - estructura real (`viernes-backend/`, `viernes-frontend/`, `base de datos/`, `obs/`, `informe/`),
   - arranque real (`activar-sistema.bat` o los 3 pasos),
   - credenciales DEV,
   - enlaces a `informe/informes/`.
   El contenido útil de APIsPERU se conserva; el resto de LP2/BomERP se va.

### Fase 3 — Renombrar `bomerp` → `viernes` (cuidado con observabilidad)

Cambiar en conjunto (si se cambia uno, se cambia todo):

- `logback-spring.xml`: `bomerp.log` → `viernes.log`
- `obs/compose-dev.yml` y `obs/compose.yml`: nombres de servicio y contenedor
- `obs/promtail/*.yml`: `job_name` y `__path__`
- `obs/prometheus/*.yml`: `job_name` y target
- `obs/grafana/provisioning/*`: datasource URLs y nombre de carpeta

### Fase 4 — Retirar el frontend vanilla

```powershell
# Mover a cuarentena en lugar de borrar
Move-Item "viernes-backend\src\main\resources\static" "E:\_cuarentena_viernes\static-legacy"
```

Y quitar del `KeycloakSecurityConfig` los `permitAll` de `/static/**`, `/`, `/index.html` (o dejarlos si se quiere una página de bienvenida).

### Fase 5 — Unificar el SQL

Elegir **una** fuente de verdad. Recomendación:

- Dejar `base de datos/init/` como fuente canónica (es la que usa Docker).
- Convertir `base de datos/01…08/` en la vista "didáctica" solo si se necesita para la sustentación; si no, archivarla.
- Eliminar `viernes-backend/db/init/` (copia idéntica) o dejar un README que apunte a `base de datos/init/`.
- Borrar `viernes-backend/seed-dev.sql` (ya marcado DEPRECADO).
- Corregir la ruta en `base de datos/README.md`.

### Fase 6 — Limpiar informes

- Borrar los 12 ZIP (son copias de carpetas existentes).
- Eliminar `V2/` (duplicado exacto de `informe/manual_usuario_v2/`).
- Eliminar `informe/V2/`, `informe/capturas_manual_v1/`, `informe/capturas_manual_v2/` (repetidos).
- Decidir si `informe/informes de sistema V1/` e `informe/informes de desarrollo desactualizados/` se archivan o se borran.
- Actualizar o retirar los HTML que dicen "26 tablas".

### Fase 7 — Poner git al día

```powershell
# 1. Revisar qué entra
git status --short

# 2. Commit del estado real (después de la limpieza)
git add -A
git commit -m "chore: limpieza de artefactos ajenos y consolidacion del estado real"

# 3. Crear remoto y subir (recomendado)
git remote add origin <url-del-repositorio>
git push -u origin main
```

---

## Qué NO tocar

- **`BOM_VENTAS`**: nombre heredado, pero está en la BD creada, en las credenciales, en 40+ scripts y en los `.dmp`. Renombrarlo exige recrear el esquema. Documentarlo como histórico.
- **`com.viernes`**: es el paquete correcto del proyecto.
- **`informe/informes/08…12`**: son los informes vigentes.
- **`V2/capturas_manual_v2/`**: si se decide conservar el manual, conservar una sola copia (la de `informe/`).
- **Los `.log` de `obs/`**: están ignorados por git; se pueden borrar sin miedo, pero no afectan al código.

---

## Conclusión

El sistema Viernes está bien construido y su código de negocio está limpio. Lo que sobra es **contexto de otros proyectos** que se fue acumulando alrededor:

1. Un módulo **Odoo** con su asistente **Ollama** (nunca integrado).
2. El **README y CLAUDE.md de BomERP/LP2** (commiteados, y el `CLAUDE.md` desvía a cualquier agente IA).
3. Nombres **`bomerp`** en logs y observabilidad.
4. El **frontend vanilla** de la primera versión, todavía servido en `:8080`.
5. **SQL triplicado** e **informes cuadruplicados**.

La Fase 1 y la Fase 2 son las que más valor dan por minuto invertido. La Fase 7 (git) es la que evita perder trabajo.
