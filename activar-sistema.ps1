# ==============================================================================
# VIERNES CRM V1 - LANZADOR AUTOMÁTICO MULTI-PC
# Diseñado para funcionar en cualquier máquina Windows con 1 solo clic o comando.
# ==============================================================================

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "VIERNES CRM V1 - Lanzador Automático Multi-PC"

Clear-Host
Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host "             VIERNES CRM V1 - LANZADOR AUTOMÁTICO DE SERVICIOS                " -ForegroundColor Cyan
Write-Host "==============================================================================" -ForegroundColor Cyan
Write-Host ""

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location -Path $RootDir

# ------------------------------------------------------------------------------
# 0. DETECCIÓN ROBUSTA DE JAVA 21
# ------------------------------------------------------------------------------
Write-Host "[0/5] Buscando Java 21 en el equipo..." -ForegroundColor Yellow

$JavaHome = $null

# A. Si JAVA_HOME ya está configurado
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $JavaHome = $env:JAVA_HOME
}

# B. Buscar en .tools\jdk21 local
if (-not $JavaHome) {
    $LocalJdk = Get-Item "$RootDir\.tools\jdk21\jdk-*" -ErrorAction SilentlyContinue | Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
    if ($LocalJdk) { $JavaHome = $LocalJdk.FullName }
}

# C. Buscar en rutas estándar de instalación de Windows
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
    foreach ($pattern in $Candidates) {
        $found = Get-Item $pattern -ErrorAction SilentlyContinue | Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1
        if ($found) {
            $JavaHome = $found.FullName
            break
        }
    }
}

# D. Buscar en PATH si es Java 21
if (-not $JavaHome) {
    $JavaCmd = Get-Command java -ErrorAction SilentlyContinue
    if ($JavaCmd) {
        $Resolved = $JavaCmd.Source
        $Parent = Split-Path -Parent (Split-Path -Parent $Resolved)
        if (Test-Path "$Parent\bin\java.exe") {
            $JavaHome = $Parent
        }
    }
}

if ($JavaHome) {
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$JavaHome\bin;" + $env:Path
    Write-Host "      [OK] Java 21 detectado en: $JavaHome" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[ERROR] No se encontró Java 21 en esta computadora." -ForegroundColor Red
    Write-Host ""
    Write-Host "Solución rápida en 1 minuto:" -ForegroundColor White
    Write-Host " 1. Descarga e instala OpenJDK 21 (Temurin o Microsoft):" -ForegroundColor Gray
    Write-Host "    https://adoptium.net/temurin/releases/?version=21" -ForegroundColor Cyan
    Write-Host " 2. Durante la instalación, marca 'Set JAVA_HOME variable'." -ForegroundColor Gray
    Write-Host " 3. Vuelve a ejecutar este lanzador." -ForegroundColor Gray
    Write-Host ""
    Read-Host "Presiona Enter para salir..."
    exit 1
}

# ------------------------------------------------------------------------------
# 1. DETECCIÓN DE NODE.JS Y NPM
# ------------------------------------------------------------------------------
Write-Host "[1/5] Verificando Node.js y npm..." -ForegroundColor Yellow

$NodeCmd = Get-Command node -ErrorAction SilentlyContinue
$NpmCmd = Get-Command npm -ErrorAction SilentlyContinue

if (-not $NodeCmd -or -not $NpmCmd) {
    Write-Host ""
    Write-Host "[ERROR] Node.js o npm no están instalados en esta PC." -ForegroundColor Red
    Write-Host "Descarga e instala Node.js LTS (v20 o v22) desde: https://nodejs.org/" -ForegroundColor Cyan
    Write-Host ""
    Read-Host "Presiona Enter para salir..."
    exit 1
}

$NodeVersion = & node -v
Write-Host "      [OK] Node.js $NodeVersion y npm listos." -ForegroundColor Green

# ------------------------------------------------------------------------------
# 2. DETECCIÓN DE DOCKER DESKTOP
# ------------------------------------------------------------------------------
Write-Host "[2/5] Verificando Docker Desktop..." -ForegroundColor Yellow

$DockerCmd = Get-Command docker -ErrorAction SilentlyContinue
if (-not $DockerCmd) {
    Write-Host ""
    Write-Host "[ERROR] Docker Desktop no está instalado en esta computadora." -ForegroundColor Red
    Write-Host "Instálalo desde: https://www.docker.com/products/docker-desktop/" -ForegroundColor Cyan
    Write-Host ""
    Read-Host "Presiona Enter para salir..."
    exit 1
}

# Comprobar si el motor de Docker está corriendo
$DockerRunning = $false
try {
    $info = docker info 2>&1
    if ($LASTEXITCODE -eq 0) { $DockerRunning = $true }
} catch {}

if (-not $DockerRunning) {
    Write-Host ""
    Write-Host "[AVISO] Docker Desktop está instalado pero no se ha iniciado." -ForegroundColor Yellow
    Write-Host "Por favor, abre la aplicación Docker Desktop en Windows y espera unos segundos." -ForegroundColor White
    Write-Host "Esperando a que el motor de Docker responda..." -ForegroundColor Gray
    
    $WaitCount = 0
    while (-not $DockerRunning -and $WaitCount -lt 30) {
        Start-Sleep -Seconds 3
        $WaitCount++
        try {
            $info = docker info 2>&1
            if ($LASTEXITCODE -eq 0) { $DockerRunning = $true }
        } catch {}
        Write-Host -NoNewline "."
    }
    Write-Host ""
    if (-not $DockerRunning) {
        Write-Host "[ERROR] Docker Desktop sigue sin responder. Inícialo manualmente y reintenta." -ForegroundColor Red
        Read-Host "Presiona Enter para salir..."
        exit 1
    }
}
Write-Host "      [OK] Docker Desktop está activo y respondiendo." -ForegroundColor Green

# ------------------------------------------------------------------------------
# 3. INICIO DE ORACLE Y KEYCLOAK EN DOCKER
# ------------------------------------------------------------------------------
Write-Host "[3/5] Iniciando Contenedores en Docker..." -ForegroundColor Yellow

# Iniciar Oracle
Write-Host "      Iniciando Oracle Database 23c (puerto 1521)..." -ForegroundColor Gray
if (Test-Path "$RootDir\base de datos\compose-dev.yml") {
    docker compose -f "$RootDir\base de datos\compose-dev.yml" up -d | Out-Null
} else {
    docker compose -f "$RootDir\viernes-backend\compose-dev.yml" up -d | Out-Null
}

# Iniciar Keycloak
Write-Host "      Iniciando Keycloak 25 (puerto 8180)..." -ForegroundColor Gray
docker compose -f "$RootDir\compose-keycloak.yml" up -d | Out-Null

# Esperar a que Oracle quede healthy
Write-Host "      Esperando a que Oracle Database quede listo (healthy)..." -ForegroundColor Gray
$OracleHealthy = $false
$Attempts = 0

while (-not $OracleHealthy -and $Attempts -lt 60) {
    $status = docker inspect -f "{{.State.Health.Status}}" viernes-oracle 2>$null
    if ($status -eq "healthy") {
        $OracleHealthy = $true
        break
    }
    $Attempts++
    Write-Host "      ... Oracle: $status (intento $Attempts/60)..." -ForegroundColor DarkGray
    Start-Sleep -Seconds 5
}

if (-not $OracleHealthy) {
    Write-Host "[ERROR] Oracle Database no quedó healthy a tiempo. Revisa: docker logs viernes-oracle" -ForegroundColor Red
    Read-Host "Presiona Enter para salir..."
    exit 1
}
Write-Host "      [OK] Oracle Database está saludable y listo." -ForegroundColor Green

# Auto-comprobación de tablas en Oracle (si la PC o el volumen es nuevo)
Write-Host "      Verificando tablas del CRM en Oracle..." -ForegroundColor Gray
$UserCheck = "SET HEADING OFF FEEDBACK OFF; SELECT count(*) FROM all_users WHERE username='BOM_VENTAS'; EXIT;" | docker exec -i viernes-oracle sqlplus -s system/123456@//localhost:1521/FREEPDB1 2>$null
$HasBomVentas = ($UserCheck -match '\b1\b')

if (-not $HasBomVentas) {
    Write-Host ""
    Write-Host "      [AVISO] Base de datos limpia detectada. Inicializando tablas y datos demo..." -ForegroundColor Yellow
    if (Test-Path "$RootDir\base de datos\ejecutar_todo.sql") {
        Get-Content "$RootDir\base de datos\ejecutar_todo.sql" | docker exec -i viernes-oracle sqlplus -s system/123456@//localhost:1521/FREEPDB1 | Out-Null
        Write-Host "      [OK] Tablas de BOM_VENTAS cargadas exitosamente." -ForegroundColor Green
    }
} else {
    Write-Host "      [OK] Esquema BOM_VENTAS verificado y listo." -ForegroundColor Green
}

# ------------------------------------------------------------------------------
# 4. INICIAR BACKEND (SPRING BOOT)
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[4/5] Iniciando Backend Spring Boot (puerto 8080)..." -ForegroundColor Yellow

if (-not (Test-Path "$RootDir\viernes-backend\.env.local") -and (Test-Path "$RootDir\viernes-backend\.env.local.example")) {
    Copy-Item "$RootDir\viernes-backend\.env.local.example" "$RootDir\viernes-backend\.env.local"
    Write-Host "      [OK] Archivo .env.local creado desde plantilla example." -ForegroundColor Gray
}

# Iniciar proceso del backend en una ventana independiente de cmd
$BackendCmd = "chcp 65001 >nul && set `"JAVA_HOME=$JavaHome`" && set `"PATH=$JavaHome\bin;%PATH%`" && cd /d `"$RootDir\viernes-backend`" && mvnw.cmd spring-boot:run"
Start-Process cmd.exe -ArgumentList "/k `"$BackendCmd`""

Write-Host "      [OK] Backend ejecutándose en su propia consola (puerto 8080)." -ForegroundColor Green

# ------------------------------------------------------------------------------
# 5. INICIAR FRONTEND (ANGULAR)
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "[5/5] Iniciando Frontend Angular (puerto 4200)..." -ForegroundColor Yellow

if (-not (Test-Path "$RootDir\viernes-frontend\node_modules")) {
    Write-Host ""
    Write-Host "      [AVISO] Primera ejecución detectada en esta PC." -ForegroundColor Yellow
    Write-Host "      Instalando dependencias de Angular (npm install)..." -ForegroundColor Gray
    Push-Location "$RootDir\viernes-frontend"
    & npm install
    Pop-Location
    Write-Host "      [OK] Dependencias instaladas." -ForegroundColor Green
}

$FrontendCmd = "chcp 65001 >nul && cd /d `"$RootDir\viernes-frontend`" && npm start"
Start-Process cmd.exe -ArgumentList "/k `"$FrontendCmd`""

Write-Host "      [OK] Frontend ejecutándose en su propia consola (puerto 4200)." -ForegroundColor Green

# ------------------------------------------------------------------------------
# RESUMEN FINAL
# ------------------------------------------------------------------------------
Write-Host ""
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host "                      ¡SISTEMA VIERNES CRM ACTIVADO!                          " -ForegroundColor Green
Write-Host "==============================================================================" -ForegroundColor Green
Write-Host ""
Write-Host " Enlaces de acceso:" -ForegroundColor Cyan
Write-Host "  - Aplicación Web:      http://localhost:4200" -ForegroundColor White
Write-Host "  - Backend Swagger:     http://localhost:8080/swagger-ui.html" -ForegroundColor White
Write-Host "  - Keycloak Auth:       http://localhost:8180 (Realm: viernes-realm)" -ForegroundColor White
Write-Host ""
Write-Host " Credenciales de acceso al CRM:" -ForegroundColor Cyan
Write-Host "  - Usuario:     admin@viernes.com" -ForegroundColor White
Write-Host "  - Contraseña:  Admin12345" -ForegroundColor White
Write-Host "  - Rol:         ADMIN (Acceso Total)" -ForegroundColor White
Write-Host ""
Write-Host "==============================================================================" -ForegroundColor Gray
Write-Host "Abriendo http://localhost:4200 en tu navegador..." -ForegroundColor Gray
Write-Host "==============================================================================" -ForegroundColor Gray

Start-Sleep -Seconds 5
Start-Process "http://localhost:4200"
