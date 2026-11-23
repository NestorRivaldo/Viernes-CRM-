@echo off
chcp 65001 >nul
title Iniciar Base de Datos Oracle - VIERNES CRM
color 0A
echo ==============================================================================
echo             INICIANDO BASE DE DATOS ORACLE 23c (FREEPDB1)
echo ==============================================================================
cd /d "%~dp0"
docker compose -f compose-dev.yml up -d
echo.
echo Estado del contenedor:
docker ps -f name=viernes-oracle
echo.
echo Para ver los logs en vivo: docker logs -f viernes-oracle
echo Conexión SQL Developer: localhost:1521 / FREEPDB1 / BOM_VENTAS / 123456
echo ==============================================================================
pause
