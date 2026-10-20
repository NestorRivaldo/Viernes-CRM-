@echo off
chcp 65001 >nul
title VIERNES CRM V1 - Lanzador Automático Multi-Entorno
color 0B

echo ==============================================================================
echo              VIERNES CRM V1 - LANZADOR AUTOMÁTICO DE SERVICIOS
echo ==============================================================================
echo.

:: 1. Verificar Docker
echo [1/4] Verificando Docker Desktop...
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ADVERTENCIA] Docker Desktop no parece estar iniciado.
    echo Por favor, abre Docker Desktop y espera a que inicie antes de continuar.
    echo.
    pause
)

:: 2. Iniciar Bases de Datos y Keycloak en Docker
echo [2/5] Iniciando Oracle Database 23c en puerto 1521...
docker compose -f viernes-backend\compose-dev.yml up -d

echo [3/5] Iniciando Keycloak 25 (OAuth2 / OIDC) en puerto 8180...
docker compose -f compose-keycloak.yml up -d
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Falló el inicio de Keycloak en Docker. Verifica los logs de Docker.
) else (
    echo [OK] Keycloak y PostgreSQL iniciados exitosamente en http://localhost:8180
)
echo.

:: 4. Iniciar Backend Spring Boot
echo [4/5] Iniciando Backend Spring Boot (Java 21) en puerto 8080...
start "VIERNES - Backend Spring Boot (8080)" cmd /k "cd /d %~dp0viernes-backend && mvnw.cmd spring-boot:run"
echo [OK] Backend ejecutándose en segundo plano (Revisa la consola abierta para ver el progreso de arranque).
echo.

:: 5. Iniciar Frontend Angular
echo [5/5] Verificando e iniciando Frontend Angular (Node.js) en puerto 4200...
if not exist "%~dp0viernes-frontend\node_modules\" (
    echo Instalando paquetes de node_modules por primera vez...
    cmd /c "cd /d %~dp0viernes-frontend && npm.cmd install"
)

start "VIERNES - Frontend Angular (4200)" cmd /k "cd /d %~dp0viernes-frontend && npm.cmd start"
echo [OK] Frontend ejecutándose en segundo plano (Abriendo en http://localhost:4200)...
echo.

:: Resumen de credenciales
echo ==============================================================================
echo                            SISTEMA VIERNES CRM LISTO
echo ==============================================================================
echo  - Frontend Web:        http://localhost:4200
echo  - Backend API:         http://localhost:8080/swagger-ui.html
echo  - Keycloak Auth:       http://localhost:8180 (Realm: viernes-realm)
echo.
echo  Credencial Maestro de Demostración:
echo   - Correo:      admin@viernes.com
echo   - Contraseña:  Admin12345
echo   - Roles:       ADMIN, JEFE, VENDEDOR (Acceso Total)
echo ==============================================================================
echo.
timeout /t 5 >nul
start http://localhost:4200
pause
