# Base de datos VIERNES CRM (Oracle Free, Docker)

Esta carpeta hace que la base de datos sea **reproducible en cualquier PC**:
no depende de un contenedor con volumen "fantasma" ni de un Oracle local.

## Estructura

```
db/
├── init/               # Scripts que se ejecutan UNA vez al crear el volumen
│   ├── 001_tablespaces.sql   # 21 tablespaces (uno por tabla) + VIERNES_TEMP + datafiles
│   ├── 002_usuario.sql       # BOM_VENTAS sobre esos tablespaces + privilegios
│   ├── 003_esquema.sql      # 21 tablas, constraints e índices
│   ├── 004_plsql.sql        # 6 funciones, procedimiento, 5 vistas y trigger
│   ├── 005_seed.sql         # Usuarios demo y datos de ejemplo
│   └── 006_verificacion.sql # Inventario y conteos
├── backups/            # Salida de expdp/impdp (montado en el contenedor)
├── export.ps1          # Backup de la base a ./backups
├── import.ps1          # Restauración desde ./backups
├── README.md
```

## Levantar la base

```powershell
cd viernes-backend
docker compose -f compose-dev.yml up -d
```

La primera vez tarda ~2-5 min (Oracle se inicializa y ejecuta los 6 scripts).
Confirma con:

```powershell
docker inspect -f "{{.State.Health.Status}}" viernes-oracle
# debe decir: healthy
```

## Conexión desde SQL Developer

| Campo    | Valor                  |
|----------|------------------------|
| Tipo     | Oracle (JDBC Thin)     |
| Host     | `localhost`            |
| Puerto   | `1521`                 |
| Servicio | `FREEPDB1`             |
| Usuario  | `BOM_VENTAS`           |
| Clave    | `123456`               |

Para ver **tablespaces y datafiles** en el panel "Storage", conecta una segunda
conexión como `SYSTEM` / `123456` (rol SYSDBA) o consulta las vistas del
esquema:

```sql
SELECT * FROM VW_VIERNES_TABLESPACES;
SELECT * FROM VW_VIERNES_DATAFILES;
SELECT * FROM VW_VIERNES_OBJETOS;
```

Para ver **funciones**: expande `BOM_VENTAS` → `Functions` → `FN_VIERNES_*`.
Para ver **vistas**: `VW_VIERNES_PIPELINE`, `VW_VIERNES_AGENDA`.

### Funciones PL/SQL (6)

| Función | Qué devuelve |
|---|---|
| `FN_VIERNES_NOMBRE_USUARIO(id)` | `"Nombre Apellido"` del usuario |
| `FN_VIERNES_NOMBRE_CLIENTE(id)` | `EMPRESA` o `NOMBRE` del cliente |
| `FN_VIERNES_VALOR_PONDERADO(id_op)` | `VALOR * PROBABILIDAD / 100` de una oportunidad |
| `FN_VIERNES_TOTAL_PIPELINE(etapa, solo_abiertas)` | Monto del pipeline (por defecto solo abierto) |
| `FN_VIERNES_TOTAL_PONDERADO(etapa)` | Pronóstico: valor esperado del pipeline |
| `FN_VIERNES_TASA_CONVERSION(responsable)` | % de oportunidades ganadas sobre las cerradas |

```sql
SELECT FN_VIERNES_TOTAL_PIPELINE()            FROM dual;  -- 20900
SELECT FN_VIERNES_TOTAL_PONDERADO()           FROM dual;  -- 12150
SELECT FN_VIERNES_TASA_CONVERSION()           FROM dual;  -- 0 (sin cierres)
SELECT FN_VIERNES_NOMBRE_CLIENTE(1)           FROM dual;  -- Acme S.A.
```

## Credenciales demo

| Correo               | Clave      | Rol      |
|----------------------|------------|----------|
| admin@viernes.com    | Admin12345 | ADMIN    |
| jefe@viernes.com     | Admin12345 | JEFE     |
| vendedor@viernes.com | Admin12345 | VENDEDOR |

## Reiniciar desde cero (borra los datos)

```powershell
docker compose -f compose-dev.yml down -v
docker compose -f compose-dev.yml up -d
```

## Backup y restauración (independencia de la PC)

```powershell
# Exportar (genera viernes-backend/db/backups/viernes_YYYYMMDD_HHMM.dmp)
.\db\export.ps1

# Restaurar en otra PC (después de levantar la base allí)
.\db\import.ps1 -DumpFile viernes_20260101_1200.dmp
```

## Notas

- Los scripts de `init/` solo se ejecutan cuando el volumen está **vacío**.
  Si ya tienes datos, usa `down -v` para reiniciar.
- `application-dev.yml` usa `ddl-auto: update`: si falta una columna, el backend
  la agrega al arrancar. El DDL de `init/` es la fuente de verdad del esquema.
- El volumen se llama `viernes-oracle-data` (fijo), no depende del nombre de
  la carpeta del proyecto.
