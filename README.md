# LP2

Carpeta preparada para artefactos del curso **Lenguaje de Programacion II**.

## Alcance

LP2 continúa el dominio comercial construido en POO y LP1: `Producto`,
`Categoria–Producto`, `Venta–DetalleVenta` y `Usuario`. El backend es un
único proyecto Spring Boot organizado por módulos de negocio mediante Spring
Modulith (ver [`docs/lp2/adr/`](../docs/lp2/adr/)), que también deja
preparado `Compra–DetalleCompra` como segundo flujo transaccional cuando el
equipo lo amplíe. En U1 desarrolla el backend REST; en U2 incorpora la SPA y
la seguridad JWT; en U3 lo optimiza, integra y estabiliza.

## Entregables Previstos

* Backend REST único (Spring Boot + Spring Modulith) con JPA y Oracle.
* Frontend SPA.
* DTO, servicios, repositorios y validaciones.
* Seguridad JWT y control de acceso.
* Integración, pruebas, optimización y monitoreo.

El proyecto backend vive en [`bomerp-backend/`](bomerp-backend/): ya tiene
los módulos `catalogo` (S1, `Categoria`/`Producto`) y `ventas` (S4,
`Venta`/`DetalleVenta`), el paquete compartido `exception`/`filter` y
`ModularityTests` en verde. El frontend vive en
[`bomerp-frontend/`](bomerp-frontend/) (Angular, desde S7): ya tiene el
CRUD de `Categoria`. Las decisiones de arquitectura del workspace están en
[`docs/lp2/adr/`](../docs/lp2/adr/).

## Arranque rápido en DEV

El **orden importa**: primero la base de datos, después el backend, y
recién con el backend respondiendo, el frontend. Los comandos son de
PowerShell; en macOS/Linux, `.\mvnw.cmd` es `./mvnw`.

### 1. Backend (`bomerp-backend/`)

Base de datos (Oracle, contenedor `bomerp-oracle`):

```powershell
cd lp2/bomerp-backend
docker compose -f compose-dev.yml up -d
```

La primera vez, el contenedor tarda un poco más en quedar listo
(`healthcheck` de Oracle). Confírmalo con `docker ps` antes de levantar el
backend — debe aparecer `healthy`, no solo `Up`.

Backend (Maven Wrapper, no hace falta instalar Maven):

```powershell
.\mvnw.cmd spring-boot:run
```

Verificación:

- Swagger: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`
- Un endpoint real: `http://localhost:8080/api/v1/categorias`

### 2. Frontend (`bomerp-frontend/`)

Con el backend ya respondiendo, en otra terminal:

```powershell
cd lp2/bomerp-frontend
npm install
npm start
```

Abre `http://localhost:4200`. El frontend apunta al backend por
`environment.apiBaseUrl` (`src/environments/environment.ts`), hoy
`http://localhost:8080` — si cambias el puerto del backend, ese es el
archivo que hay que actualizar. El backend ya acepta ese origen por CORS
(`bomerp.cors.allowed-origins`, `application-dev.yml`).

### Detener

```powershell
docker compose -f compose-dev.yml down
```

`Ctrl+C` detiene el backend y el frontend en sus propias terminales.

## Consulta DNI/RUC (APIsPERU) — token del backend

Aplica al backend `viernes-backend/` (`com.viernes`). El botón **Consultar** del
alta de clientes llama a APIsPERU **desde el backend** para traer el nombre (DNI)
o la razón social (RUC). Requiere un token personal:

1. Consigue tu token gratuito en <https://apisperu.com/admin/dniruc> (plan free: 1.000 consultas/mes).
2. Copia la plantilla: `viernes-backend/.env.local.example` → `viernes-backend/.env.local`.
3. Edita `.env.local` y define `APISPERU_TOKEN=<tu token>`.
4. Ejecuta `activar-sistema.bat`: carga `.env.local` automáticamente antes de arrancar el backend.

Seguridad:

- `.env.local` está **git-ignored**: nunca lo commitees ni lo compartas por chat o repositorio.
- El token viaja como *query param* a APIsPERU y **no se registra en logs**.
- Sin token, el resto del sistema funciona; solo el botón Consultar devuelve *"La consulta externa DNI/RUC no está configurada. Defina la variable APISPERU_TOKEN."*
- **Cada quien usa su propio token** (su cuota). Si se comparte uno, todos consumen las mismas 1.000 consultas/mes.
