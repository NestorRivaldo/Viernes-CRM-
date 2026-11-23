param(
    [Parameter(Mandatory = $true)]
    [string]$DumpFile
)

# Restaura un backup en la base VIERNES CRM.
# Uso: .\db\import.ps1 -DumpFile viernes_20260101_1200.dmp

$ErrorActionPreference = "Stop"

$backendDir = Split-Path -Parent $PSScriptRoot
$backupDir  = Join-Path $backendDir "db\backups"
$dumpPath   = Join-Path $backupDir $DumpFile

if (-not (Test-Path $dumpPath)) {
    throw "No se encontró $dumpPath"
}

Write-Host "[1/2] Ejecutando impdp sobre $DumpFile..."
docker exec viernes-oracle impdp BOM_VENTAS/123456@//localhost:1521/FREEPDB1 `
    directory=VIERNES_BACKUP `
    dumpfile=$DumpFile `
    schemas=BOM_VENTAS `
    table_exists_action=REPLACE `
    logfile="import_$DumpFile.log"

if ($LASTEXITCODE -ne 0) { throw "impdp falló (exit $LASTEXITCODE)" }

Write-Host "[2/2] Restauración completada."
