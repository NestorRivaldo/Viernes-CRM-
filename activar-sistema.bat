@echo off
chcp 65001 >nul
title VIERNES CRM V1 - Lanzador Multi-PC
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0activar-sistema.ps1"
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Presiona una tecla para cerrar...
    pause >nul
)
