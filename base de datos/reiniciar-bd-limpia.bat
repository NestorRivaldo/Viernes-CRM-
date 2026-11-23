@echo off
chcp 65001 >nul
title Reiniciar Base de Datos desde Cero - VIERNES CRM
color 0C
echo ==============================================================================
echo     ADVERTENCIA: Esto eliminará el volumen actual y recreará
echo     todos los tablespaces, tablas, funciones y datos demo desde cero.
echo ==============================================================================
pause
cd /d "%~dp0"
echo Limpiando volumen viejo...
docker compose -f compose-dev.yml down -v
echo Levantando base de datos y ejecutando scripts de init/...
docker compose -f compose-dev.yml up -d
echo.
echo Siguiendo logs de inicialización (espera a ver "DATABASE IS READY TO USE!")...
docker logs -f viernes-oracle
