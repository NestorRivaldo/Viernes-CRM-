@echo off
chcp 65001 >nul
title VIERNES CRM V1 - Lanzador Automático Multi-Entorno
color 0B

echo ==============================================================================
echo              VIERNES CRM V1 - LANZADOR AUTOMÁTICO DE SERVICIOS
echo ==============================================================================
echo.

:: 0. Localizar Java 21 (JDK portable en .tools o Java del sistema)
if not defined JAVA_HOME (
    if exist "%~dp0.tools\jdk21" (
        for /d %%D in ("%~dp0.tools\jdk21\jdk-*") do set "JAVA_HOME=%%~D"
    )
)
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
echo [0/5] Java: %JAVA_HOME%
if not defined JAVA_HOME (
    echo [ERROR] No se encontro Java 21. Instala un JDK 21 o coloca uno en .tools\jdk21\
    pause
    exit /b 1
)

:: 1. Verificar Docker
echo [1/5] Verificando Docker Desktop...
where docker >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker no esta instalado.
    echo Instala Docker Desktop desde https://www.docker.com/products/docker-desktop/
    echo y vuelve a ejecutar este lanzador.
    echo.
    pause
    exit /b 1
)
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker Desktop no esta iniciado.
    echo Abre Docker Desktop y espera a que termine de iniciar antes de continuar.
    echo.
    pause
    exit /b 1
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
:: 4a. Cargar secretos locales (viernes-backend\.env.local) antes de arrancar.
::     Spring no lee .env solo: sin esto, APISPERU_TOKEN queda vacío y la
::     consulta DNI/RUC falla. No se imprime el valor; solo se setea en el
::     entorno que hereda el proceso del backend.
if exist "%~dp0viernes-backend\.env.local" (
    echo [4/5] Cargando variables locales desde viernes-backend\.env.local...
    for /f "usebackq eol=# tokens=1,* delims==" %%A in ("%~dp0viernes-backend\.env.local") do (
        set "%%A=%%B"
    )
) else (
    echo [4/5] AVISO: no existe viernes-backend\.env.local.
    echo         La consulta DNI/RUC no funcionará hasta definir APISPERU_TOKEN.
    echo         Copia viernes-backend\.env.local.example a .env.local y pon tu token.
)
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
