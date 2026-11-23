# Exporta la base VIERNES CRM a viernes-backend/db/backups/.
# Uso: .\db\export.ps1

$ErrorActionPreference = "Stop"

$backendDir = Split-Path -Parent $PSScriptRoot
$backupDir  = Join-Path $backendDir "db\backups"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

$stamp = Get-Date -Format "yyyyMMdd_HHmm"
$dump  = "viernes_$stamp.dmp"
$log   = "viernes_$stamp.log"

Write-Host "[1/2] Ejecutando expdp en el contenedor viernes-oracle..."
docker exec viernes-oracle expdp BOM_VENTAS/123456@//localhost:1521/FREEPDB1 `
    schemas=BOM_VENTAS `
    directory=VIERNES_BACKUP `
    dumpfile=$dump `
    logfile=$log `
    compression=ALL

if ($LASTEXITCODE -ne 0) { throw "expdp falló (exit $LASTEXITCODE)" }

Write-Host "[2/2] Backup generado: db\backups\$dump"
Write-Host "       Log: db\backups\$log"
