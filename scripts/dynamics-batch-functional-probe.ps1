# Prueba funcional Dynamics 365 F&O: BatchNumber como identificador principal del QR.
# No toca la app ni la base de datos. Solo consulta OData y arma un objeto de prueba.
#
# Uso (OAuth client_credentials, mismas vars que la API):
#   $env:APP_DYNAMICS_TENANT_ID = '...'
#   $env:APP_DYNAMICS_CLIENT_ID = '...'
#   $env:APP_DYNAMICS_CLIENT_SECRET = '...'
#   $env:APP_DYNAMICS_BASE_URL = 'https://olnatura-produccion.operations.dynamics.com'
#   $env:APP_DYNAMICS_RESOURCE = 'https://olnatura-produccion.operations.dynamics.com'  # opcional
#   .\dynamics-batch-functional-probe.ps1

[CmdletBinding()]
param(
  [string]$BatchNumber = "260713-MEM0003662",
  [string]$BaseUrl = "",
  [string]$TenantId = "",
  [string]$ClientId = "",
  [string]$ClientSecret = "",
  [string]$Resource = "",
  [string]$TokenUrl = "",
  [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"

if (-not $BaseUrl) {
  if ($env:APP_DYNAMICS_BASE_URL) { $BaseUrl = $env:APP_DYNAMICS_BASE_URL }
  else { $BaseUrl = "https://olnatura-produccion.operations.dynamics.com" }
}
if (-not $TenantId) { $TenantId = $env:APP_DYNAMICS_TENANT_ID }
if (-not $ClientId) { $ClientId = $env:APP_DYNAMICS_CLIENT_ID }
if (-not $ClientSecret) { $ClientSecret = $env:APP_DYNAMICS_CLIENT_SECRET }
if (-not $Resource) { $Resource = $env:APP_DYNAMICS_RESOURCE }
if (-not $TokenUrl) { $TokenUrl = $env:APP_DYNAMICS_TOKEN_URL }

if ([string]::IsNullOrWhiteSpace($TenantId) -or [string]::IsNullOrWhiteSpace($ClientId) -or [string]::IsNullOrWhiteSpace($ClientSecret)) {
  throw "Faltan credenciales OAuth. Define APP_DYNAMICS_TENANT_ID, APP_DYNAMICS_CLIENT_ID y APP_DYNAMICS_CLIENT_SECRET (o parametros -TenantId/-ClientId/-ClientSecret)."
}

$BaseUrl = $BaseUrl.TrimEnd("/")
if ([string]::IsNullOrWhiteSpace($Resource)) { $Resource = $BaseUrl }
$Resource = $Resource.TrimEnd("/")
if ([string]::IsNullOrWhiteSpace($TokenUrl)) {
  $TokenUrl = "https://login.microsoftonline.com/" + $TenantId.Trim() + "/oauth2/token"
}

if (-not $OutDir) {
  $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
  $OutDir = Join-Path $PSScriptRoot ("out\dynamics-probe-" + $stamp)
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

Write-Host "Solicitando access_token (client_credentials)..."
$tokenBody = @{
  grant_type    = "client_credentials"
  client_id     = $ClientId.Trim()
  client_secret = $ClientSecret.Trim()
  resource      = $Resource
}
$tokenResponse = Invoke-RestMethod -Method Post -Uri $TokenUrl -Body $tokenBody -ContentType "application/x-www-form-urlencoded" -TimeoutSec 60
if (-not $tokenResponse.access_token) {
  throw "OAuth no devolvio access_token. Revisa tenant/client/secret/resource."
}
$AccessToken = [string]$tokenResponse.access_token

$headers = @{
  Authorization = ("Bearer " + $AccessToken)
  Accept        = "application/json"
}

function Save-Json([string]$Name, $Object) {
  $path = Join-Path $OutDir $Name
  ($Object | ConvertTo-Json -Depth 20) | Set-Content -Path $path -Encoding UTF8
  return $path
}

function Invoke-OData {
  param(
    [string]$Step,
    [string]$Path,
    [string]$Filter,
    [string]$Select = $null,
    [int]$Top = 5
  )

  $qb = New-Object System.Collections.Generic.List[string]
  [void]$qb.Add(("`$filter=" + [uri]::EscapeDataString($Filter)))
  [void]$qb.Add(("`$top=" + $Top))
  if ($Select) {
    [void]$qb.Add(("`$select=" + [uri]::EscapeDataString($Select)))
  }

  $uri = $BaseUrl + $Path + "?" + ($qb -join "&")

  Write-Host ""
  Write-Host ("=== " + $Step + " ===")
  Write-Host ("GET " + $uri)

  try {
    $response = Invoke-RestMethod -Method Get -Uri $uri -Headers $headers -TimeoutSec 60
    $rawPath = Save-Json ($Step + ".json") $response
    Write-Host ("OK -> " + $rawPath)

    $count = 0
    if ($null -ne $response.value) { $count = @($response.value).Count }
    Write-Host ("Filas: " + $count)
    if ($count -gt 0) {
      Write-Host ((@($response.value)[0] | ConvertTo-Json -Depth 8))
    } else {
      Write-Host "ADVERTENCIA: value vacio"
    }

    return [pscustomobject]@{
      Ok       = $true
      Uri      = $uri
      Response = $response
      Error    = $null
      Status   = 200
    }
  }
  catch {
    $status = $null
    $body = $null
    if ($_.Exception.Response) {
      try { $status = [int]$_.Exception.Response.StatusCode } catch {}
      try {
        $stream = $_.Exception.Response.GetResponseStream()
        if ($stream) {
          $reader = New-Object System.IO.StreamReader($stream)
          $body = $reader.ReadToEnd()
          $reader.Close()
        }
      } catch {}
    }

    $errObj = [pscustomobject]@{
      message = $_.Exception.Message
      status  = $status
      body    = $body
      uri     = $uri
    }
    [void](Save-Json ($Step + ".error.json") $errObj)
    Write-Host ("ERROR status=" + $status + " message=" + $_.Exception.Message)
    if ($body) { Write-Host $body }

    return [pscustomobject]@{
      Ok       = $false
      Uri      = $uri
      Response = $null
      Error    = $errObj
      Status   = $status
    }
  }
}

Write-Host ("BaseUrl     : " + $BaseUrl)
Write-Host ("Resource    : " + $Resource)
Write-Host ("BatchNumber : " + $BatchNumber)
Write-Host ("OutDir      : " + $OutDir)
Write-Host ("Token length: " + $AccessToken.Length)

# 1) ItemBatches
$r1 = Invoke-OData -Step "01-ItemBatches" `
  -Path "/data/ItemBatches" `
  -Filter ("BatchNumber eq '" + $BatchNumber + "'") `
  -Select "ItemNumber,BatchNumber,BatchExpirationDate" `
  -Top 1

$batch = $null
if ($r1.Ok -and $r1.Response -and $r1.Response.value -and (@($r1.Response.value).Count -gt 0)) {
  $batch = @($r1.Response.value)[0]
}

$itemNumber = $null
$batchNumberOut = $BatchNumber
$caducidad = $null
if ($batch) {
  $itemNumber = [string]$batch.ItemNumber
  if ($batch.BatchNumber) { $batchNumberOut = [string]$batch.BatchNumber }
  $caducidad = $batch.BatchExpirationDate
}

# 2) InventorySitesOnHand (preferido) + fallback V2
$r2 = $null
$onHandEntityUsed = "InventorySitesOnHand"
$onHand = $null

if (-not [string]::IsNullOrWhiteSpace($itemNumber)) {
  $r2 = Invoke-OData -Step "02-InventorySitesOnHand" `
    -Path "/data/InventorySitesOnHand" `
    -Filter ("ItemNumber eq '" + $itemNumber + "'") `
    -Select "ItemNumber,ProductName,AvailableOnHandQuantity,InventorySiteId" `
    -Top 5

  if ($r2.Ok -and $r2.Response -and $r2.Response.value -and (@($r2.Response.value).Count -gt 0)) {
    $onHand = @($r2.Response.value)[0]
  }
  else {
    Write-Host "Fallback a InventorySitesOnHandV2..."
    $onHandEntityUsed = "InventorySitesOnHandV2"
    $r2 = Invoke-OData -Step "02b-InventorySitesOnHandV2" `
      -Path "/data/InventorySitesOnHandV2" `
      -Filter ("ItemNumber eq '" + $itemNumber + "'") `
      -Select "ItemNumber,ProductName,AvailableOnHandQuantity,InventorySiteId" `
      -Top 5
    if ($r2.Ok -and $r2.Response -and $r2.Response.value -and (@($r2.Response.value).Count -gt 0)) {
      $onHand = @($r2.Response.value)[0]
    }
  }
}
else {
  Write-Host "Se omite InventorySitesOnHand: no hay ItemNumber"
}

$productName = $null
$cantidad = $null
$siteId = $null
if ($onHand) {
  $productName = [string]$onHand.ProductName
  $cantidad = $onHand.AvailableOnHandQuantity
  $siteId = [string]$onHand.InventorySiteId
}

# 3) QualityOrderHeaders - prefer ItemBatchNumber, fallback ItemNumber
$r3 = $null
$quality = $null
$qualityFilterUsed = $null

$r3 = Invoke-OData -Step "03-QualityOrderHeaders-by-ItemBatchNumber" `
  -Path "/data/QualityOrderHeaders" `
  -Filter ("ItemBatchNumber eq '" + $BatchNumber + "'") `
  -Select "ItemBatchNumber,ItemNumber,QualityOrderStatus,PassedBatchDispositionCode,WarehouseId,WarehouseLocationId" `
  -Top 5

$qualityFilterUsed = ("ItemBatchNumber eq '" + $BatchNumber + "'")
if ($r3.Ok -and $r3.Response -and $r3.Response.value -and (@($r3.Response.value).Count -gt 0)) {
  $quality = @($r3.Response.value)[0]
}
elseif (-not [string]::IsNullOrWhiteSpace($itemNumber)) {
  Write-Host "Fallback QualityOrderHeaders por ItemNumber..."
  $r3 = Invoke-OData -Step "03b-QualityOrderHeaders-by-ItemNumber" `
    -Path "/data/QualityOrderHeaders" `
    -Filter ("ItemNumber eq '" + $itemNumber + "'") `
    -Select "ItemBatchNumber,ItemNumber,QualityOrderStatus,PassedBatchDispositionCode,WarehouseId,WarehouseLocationId" `
    -Top 5
  $qualityFilterUsed = ("ItemNumber eq '" + $itemNumber + "'")
  if ($r3.Ok -and $r3.Response -and $r3.Response.value -and (@($r3.Response.value).Count -gt 0)) {
    $quality = @($r3.Response.value)[0]
  }
}

$status = $null
$almacen = $null
$ubicacion = $null
if ($quality) {
  if ($quality.QualityOrderStatus) { $status = [string]$quality.QualityOrderStatus }
  elseif ($quality.PassedBatchDispositionCode) { $status = [string]$quality.PassedBatchDispositionCode }
  if ($quality.WarehouseId) { $almacen = [string]$quality.WarehouseId }
  elseif ($siteId) { $almacen = $siteId }
  if ($quality.WarehouseLocationId) { $ubicacion = [string]$quality.WarehouseLocationId }
}
elseif ($siteId) {
  $almacen = $siteId
}

$objeto = [ordered]@{
  codigo          = $itemNumber
  nombre          = $productName
  lote            = $batchNumberOut
  caducidad       = $caducidad
  cantidadAlmacen = $cantidad
  status          = $status
  almacen         = $almacen
  ubicacion       = $ubicacion
  fuente          = "REAL_DYNAMICS"
}

$objetoPath = Save-Json "00-objeto-prueba.json" $objeto

$campos = @(
  @{ name = "codigo"; value = $objeto.codigo },
  @{ name = "nombre"; value = $objeto.nombre },
  @{ name = "lote"; value = $objeto.lote },
  @{ name = "caducidad"; value = $objeto.caducidad },
  @{ name = "cantidadAlmacen"; value = $objeto.cantidadAlmacen },
  @{ name = "status"; value = $objeto.status },
  @{ name = "almacen"; value = $objeto.almacen },
  @{ name = "ubicacion"; value = $objeto.ubicacion },
  @{ name = "fuente"; value = $objeto.fuente }
)

$okCount = 0
$failCount = 0
$checklist = @()
foreach ($c in $campos) {
  $present = ($null -ne $c.value) -and (-not [string]::IsNullOrWhiteSpace([string]$c.value))
  if ($present) { $okCount++ } else { $failCount++ }
  $checklist += [pscustomobject]@{
    campo = $c.name
    ok    = $present
    valor = $c.value
  }
}

$summary = [ordered]@{
  batchNumber       = $BatchNumber
  baseUrl           = $BaseUrl
  outDir            = $OutDir
  itemBatchesOk     = [bool]$batch
  onHandEntityUsed  = $onHandEntityUsed
  onHandOk          = [bool]$onHand
  qualityOk         = [bool]$quality
  qualityFilterUsed = $qualityFilterUsed
  camposOk          = $okCount
  camposFaltantes   = $failCount
  checklist         = $checklist
  objeto            = $objeto
  pasos = @{
    ItemBatches = @{
      ok     = $r1.Ok
      status = $r1.Status
      uri    = $r1.Uri
    }
    InventoryOnHand = @{
      ok     = $(if ($r2) { $r2.Ok } else { $false })
      status = $(if ($r2) { $r2.Status } else { $null })
      uri    = $(if ($r2) { $r2.Uri } else { $null })
      entity = $onHandEntityUsed
    }
    QualityOrderHeaders = @{
      ok     = $(if ($r3) { $r3.Ok } else { $false })
      status = $(if ($r3) { $r3.Status } else { $null })
      uri    = $(if ($r3) { $r3.Uri } else { $null })
      filter = $qualityFilterUsed
    }
  }
}

[void](Save-Json "00-summary.json" $summary)

Write-Host ""
Write-Host "========== OBJETO DE PRUEBA =========="
Write-Host ($objeto | ConvertTo-Json -Depth 5)
Write-Host ("Guardado en: " + $objetoPath)
Write-Host ""
Write-Host "========== CHECKLIST =========="
$checklist | Format-Table -AutoSize | Out-String | Write-Host
Write-Host ("Campos OK: {0} | Faltantes: {1}" -f $okCount, $failCount)

if (($failCount -gt 0) -or (-not $batch)) {
  Write-Host "RESULTADO: INCOMPLETO - no todos los campos se obtuvieron desde BatchNumber"
  exit 2
}

Write-Host "RESULTADO: OK - todos los campos se obtuvieron a partir del BatchNumber"
exit 0
