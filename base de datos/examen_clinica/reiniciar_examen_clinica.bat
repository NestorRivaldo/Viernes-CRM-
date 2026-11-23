@echo off
chcp 65001 >nul
echo ====================================================================
echo       REINICIO Y CARGA LIMPIA DEL EXAMEN V7 (CLINICA)
echo ====================================================================

:: Detección de entorno: Docker o Oracle XE nativo
where docker >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=*" %%i in ('docker ps --filter "name=oracle" --format "{{.Names}}"') do set "CONTAINER=%%i"
)

if not "%CONTAINER%"=="" (
    echo [INFO] Detectado contenedor Docker: %CONTAINER%
    docker exec -i %CONTAINER% sqlplus -s system/123456@//localhost:1521/FREEPDB1 < "%~dp001_esquema_y_semillas.sql"
    echo.
    echo [INFO] Verificando tablas del esquema EXA_CLINICA...
    docker exec -i %CONTAINER% sqlplus -s system/123456@//localhost:1521/FREEPDB1 < "%~dp002_verificar_tablas.sql"
) else (
    echo [INFO] Usando Oracle XE local en localhost:1521/XEPDB1...
    sqlplus -s system/123456@//localhost:1521/XEPDB1 < "%~dp001_esquema_y_semillas.sql"
    echo.
    echo [INFO] Verificando tablas del esquema EXA_CLINICA...
    sqlplus -s system/123456@//localhost:1521/XEPDB1 < "%~dp002_verificar_tablas.sql"
)

echo.
echo ====================================================================
echo  Esquema EXA_CLINICA reiniciado con éxito. Listo para practicar.
echo ====================================================================
