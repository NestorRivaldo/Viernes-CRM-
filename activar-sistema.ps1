# ==============================================================================
# VIERNES CRM V1 - LANZADOR AUTOMATICO MULTI-PC (VERSION ROBUSTA)
# Maneja: puerto ocupado, volumen corrupto, contenedor colgado, primera vez.
# ==============================================================================

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "VIERNES CRM V1 - Lanzador"

Clear-Host
Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "             VIERNES CRM V1 - LANZADOR AUTOMATICO DE SERVICIOS                " -ForegroundColor Cyan
Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host ""

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location -Path $RootDir

# Detectar compose de BD
$ComposeDB = if (Test-Path "$RootDir\base de datos\compose-dev.yml") {
    "$RootDir\base de datos\compose-dev.yml"
} else {
    "$RootDir\viernes-backend\compose-dev.yml"
}

# ==============================================================================
# PASO 0 - JAVA 21
# ==============================================================================
Write-Host "[0/5] Buscando Java 21..." -ForegroundColor Yellow

$JavaHome = $null

if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $JavaHome = $env:JAVA_HOME
}
if (-not $JavaHome) {
    $LocalJdk = Get-Item "$RootDir\.tools\jdk21\jdk-*" -ErrorAction SilentlyContinue |
        Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
    if ($LocalJdk) { $JavaHome = $LocalJdk.FullName }
}
if (-not $JavaHome) {
    $Candidates = @(
        "C:\Program Files\Microsoft\jdk-21*",
        "C:\Program Files\Eclipse Adoptium\jdk-21*",
        "C:\Program Files\Java\jdk-21*",
        "C:\Program Files\Amazon Corretto\jdk21*",
        "C:\Program Files\BellSoft\LibericaJDK-21*",
        "C:\Program Files\Semeru\jdk-21*",
        "$env:LOCALAPPDATA\Programs\Eclipse Adoptium\jdk-21*"
    )
    foreach ($p in $Candidates) {
        $f = Get-Item $p -ErrorAction SilentlyContinue |
            Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
        if ($f) { $JavaHome = $f.FullName; break }
    }
}
if (-not $JavaHome) {
    $j = Get-Command java -ErrorAction SilentlyContinue
    if ($j) {
        $p = Split-Path -Parent (Split-Path -Parent $j.Source)
        if (Test-Path "$p\bin\java.exe") { $JavaHome = $p }
    }
}

if ($JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;" + $env:Path
    Write-Host "      [OK] Java 21: $JavaHome" -ForegroundColor Green
} else {
    Write-Host "[ERROR] No se encontro Java 21." -ForegroundColor Red
    Write-Host "Instala OpenJDK 21 desde: https://adoptium.net/temurin/releases/?version=21" -ForegroundColor Cyan
    Read-Host "Presiona Enter para salir..."; exit 1
}

# ==============================================================================
# PASO 1 - NODE.JS
# ==============================================================================
Write-Host "[1/5] Verificando Node.js..." -ForegroundColor Yellow

if (-not (Get-Command node -ErrorAction SilentlyContinue) -or
    -not (Get-Command npm  -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Node.js no esta instalado." -ForegroundColor Red
    Write-Host "Instala Node.js LTS desde: https://nodejs.org/" -ForegroundColor Cyan
    Read-Host "Presiona Enter para salir..."; exit 1
}
Write-Host "      [OK] Node.js $(node -v) listo." -ForegroundColor Green

# ==============================================================================
# PASO 2 - DOCKER
# ==============================================================================
Write-Host "[2/5] Verificando Docker Desktop..." -ForegroundColor Yellow

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "[ERROR] Docker Desktop no esta instalado." -ForegroundColor Red
    Write-Host "Instala desde: https://www.docker.com/products/docker-desktop/" -ForegroundColor Cyan
    Read-Host "Presiona Enter para salir..."; exit 1
}

$DockerOk = $false
$Wait = 0
while (-not $DockerOk -and $Wait -lt 30) {
    try { docker info 2>&1 | Out-Null; if ($LASTEXITCODE -eq 0) { $DockerOk = $true } }
    catch {}
    if (-not $DockerOk) { Start-Sleep -Seconds 3; $Wait++ }
}
if (-not $DockerOk) {
    Write-Host "[ERROR] Docker no responde. Abre Docker Desktop y reintenta." -ForegroundColor Red
    Read-Host "Presiona Enter para salir..."; exit 1
}
Write-Host "      [OK] Docker activo." -ForegroundColor Green

# ==============================================================================
# PASO 3 - ORACLE Y KEYCLOAK
# ==============================================================================
Write-Host "[3/5] Iniciando contenedores..." -ForegroundColor Yellow

# --- Liberar puerto 1521 si esta ocupado por otro contenedor ---
Write-Host "      Verificando puerto 1521..." -ForegroundColor Gray
$OcupantId = docker ps -q --filter "publish=1521" 2>$null
if ($OcupantId) {
    $OcupantName = docker inspect --format "{{.Name}}" $OcupantId 2>$null
    if ($OcupantName -notlike "*viernes-oracle*") {
        Write-Host "      Puerto 1521 ocupado por: $OcupantName — parando..." -ForegroundColor Yellow
        docker stop $OcupantId | Out-Null
        Write-Host "      [OK] Puerto 1521 liberado." -ForegroundColor Green
    }
}

# --- Arrancar Oracle ---
Write-Host "      Iniciando Oracle Database 23c..." -ForegroundColor Gray
docker compose -f $ComposeDB up -d 2>&1 | Out-Null

# --- Esperar healthy con recuperacion automatica ---
Write-Host "      Esperando que Oracle quede listo (max 5 min)..." -ForegroundColor Gray
$OracleOk  = $false
$Intentos  = 0
$MaxIntentos = 60   # 60 x 5s = 5 minutos

while (-not $OracleOk -and $Intentos -lt $MaxIntentos) {
    $status = docker inspect -f "{{.State.Health.Status}}" viernes-oracle 2>$null
    
    if ($status -eq "healthy") {
        $OracleOk = $true
        break
    }

    # Si lleva mas de 2 minutos unhealthy -> limpiar volumen y reiniciar
    if ($status -eq "unhealthy" -and $Intentos -ge 24) {
        Write-Host "" 
        Write-Host "      Oracle no pudo iniciar (volumen posiblemente corrupto)." -ForegroundColor Yellow
        Write-Host "      Limpiando volumen y reiniciando Oracle..." -ForegroundColor Yellow
        docker compose -f $ComposeDB down -v 2>&1 | Out-Null
        Start-Sleep -Seconds 3
        docker compose -f $ComposeDB up -d 2>&1 | Out-Null
        $Intentos = 0
        Write-Host "      Reiniciado. Esperando de nuevo..." -ForegroundColor Gray
        Start-Sleep -Seconds 5
        continue
    }

    $Intentos++
    if ($Intentos % 6 -eq 0) {
        $mins = [math]::Round($Intentos * 5 / 60, 1)
        Write-Host "      Oracle: $status ... ($mins min)" -ForegroundColor DarkGray
    }
    Start-Sleep -Seconds 5
}

if (-not $OracleOk) {
    Write-Host "[ERROR] Oracle no quedo listo." -ForegroundColor Red
    Write-Host "Revisa con: docker logs viernes-oracle" -ForegroundColor Cyan
    Read-Host "Presiona Enter para salir..."; exit 1
}
Write-Host "      [OK] Oracle listo." -ForegroundColor Green

# --- Keycloak ---
Write-Host "      Iniciando Keycloak..." -ForegroundColor Gray
docker compose -f "$RootDir\compose-keycloak.yml" up -d 2>&1 | Out-Null
Write-Host "      [OK] Keycloak iniciado." -ForegroundColor Green

# --- Verificar y cargar tablas BOM_VENTAS si es PC nueva ---
Write-Host "      Verificando esquema BOM_VENTAS..." -ForegroundColor Gray
$UserCheck = "SET HEADING OFF FEEDBACK OFF; SELECT count(*) FROM all_users WHERE username='BOM_VENTAS'; EXIT;" |
    docker exec -i viernes-oracle sqlplus -s system/123456@//localhost:1521/FREEPDB1 2>$null
$HasBomVentas = ($UserCheck -match '\b1\b')

if (-not $HasBomVentas) {
    Write-Host "      PC nueva detectada. Cargando tablas demo..." -ForegroundColor Yellow
    $sqlFile = if (Test-Path "$RootDir\base de datos\ejecutar_todo.sql") {
        "$RootDir\base de datos\ejecutar_todo.sql"
    } else { $null }

    if ($sqlFile) {
        Get-Content $sqlFile | docker exec -i viernes-oracle sqlplus -s system/123456@//localhost:1521/FREEPDB1 | Out-Null
        Write-Host "      [OK] Tablas BOM_VENTAS cargadas." -ForegroundColor Green
    } else {
        Write-Host "      [AVISO] No se encontro ejecutar_todo.sql. Carga las tablas manualmente si es necesario." -ForegroundColor Yellow
    }
} else {
    Write-Host "      [OK] Esquema BOM_VENTAS verificado." -ForegroundColor Green
}

# ==============================================================================
# PASO 4 - BACKEND SPRING BOOT
# ==============================================================================
Write-Host ""
Write-Host "[4/5] Iniciando Backend Spring Boot (puerto 8080)..." -ForegroundColor Yellow

if (-not (Test-Path "$RootDir\viernes-backend\.env.local") -and
    (Test-Path "$RootDir\viernes-backend\.env.local.example")) {
    Copy-Item "$RootDir\viernes-backend\.env.local.example" "$RootDir\viernes-backend\.env.local"
    Write-Host "      [OK] .env.local creado desde plantilla." -ForegroundColor Gray
}

$BackendCmd = "chcp 65001 >nul && set `"JAVA_HOME=$JavaHome`" && set `"PATH=$JavaHome\bin;%PATH%`" && cd /d `"$RootDir\viernes-backend`" && mvnw.cmd spring-boot:run"
Start-Process cmd.exe -ArgumentList "/k `"$BackendCmd`""
Write-Host "      [OK] Backend ejecutandose en su consola (puerto 8080)." -ForegroundColor Green

# ==============================================================================
# PASO 5 - FRONTEND ANGULAR
# ==============================================================================
Write-Host ""
Write-Host "[5/5] Iniciando Frontend Angular (puerto 4200)..." -ForegroundColor Yellow

if (-not (Test-Path "$RootDir\viernes-frontend\node_modules")) {
    Write-Host "      Primera vez en esta PC. Instalando dependencias npm..." -ForegroundColor Yellow
    Push-Location "$RootDir\viernes-frontend"
    & npm install
    Pop-Location
    Write-Host "      [OK] Dependencias instaladas." -ForegroundColor Green
}

$FrontendCmd = "chcp 65001 >nul && cd /d `"$RootDir\viernes-frontend`" && npm start"
Start-Process cmd.exe -ArgumentList "/k `"$FrontendCmd`""
Write-Host "      [OK] Frontend ejecutandose en su consola (puerto 4200)." -ForegroundColor Green

# ==============================================================================
# RESUMEN
# ==============================================================================
Write-Host ""
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host "                      SISTEMA VIERNES CRM ACTIVADO                            " -ForegroundColor Green
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host ""
Write-Host " Acceso:" -ForegroundColor Cyan
Write-Host "  Aplicacion:   http://localhost:4200" -ForegroundColor White
Write-Host "  Swagger:      http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "  Keycloak:     http://localhost:8180" -ForegroundColor White
Write-Host ""
Write-Host " Credenciales:" -ForegroundColor Cyan
Write-Host "  Usuario:      admin@viernes.com" -ForegroundColor White
Write-Host "  Contrasena:   Admin12345" -ForegroundColor White
Write-Host "==============================================================================" -ForegroundColor Gray

Start-Sleep -Seconds 5
Start-Process "http://localhost:4200"