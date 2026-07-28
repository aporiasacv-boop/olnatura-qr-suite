# Investigación trazabilidad Estado Operativo — solo lectura OData.
# Requiere APP_DYNAMICS_TENANT_ID / CLIENT_ID / CLIENT_SECRET (o -TenantId/-ClientId/-ClientSecret).
#
# Uso:
#   .\dynamics-traceability-probe.ps1
#   .\dynamics-traceability-probe.ps1 -BatchSuffixes @('MEM0003675','MPM0003363')

[CmdletBinding()]
param(
  [string[]]$BatchSuffixes = @(
    "MEM0003675",
    "MEM0003559",
    "MEM0003666",
    "MEM0003625",
    "MPM0003416",
    "MPM0003395",
    "MPM0003109",
    "MPM0003363"
  ),
  [string]$BaseUrl = "",
  [string]$TenantId = "",
  [string]$ClientId = "",
  [string]$ClientSecret = "",
  [string]$Resource = "",
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

if ([string]::IsNullOrWhiteSpace($TenantId) -or [string]::IsNullOrWhiteSpace($ClientId) -or [string]::IsNullOrWhiteSpace($ClientSecret)) {
  throw "Faltan credenciales OAuth Dynamics."
}

$BaseUrl = $BaseUrl.TrimEnd("/")
if ([string]::IsNullOrWhiteSpace($Resource)) { $Resource = $BaseUrl }
$Resource = $Resource.TrimEnd("/")
$tokenUrl = "https://login.microsoftonline.com/" + $TenantId.Trim() + "/oauth2/token"

if (-not $OutDir) {
  $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
  $OutDir = Join-Path $PSScriptRoot ("out\dynamics-trazabilidad-" + $stamp)
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
Write-Host "OutDir=$OutDir"

Write-Host "OAuth token..."
$token = Invoke-RestMethod -Method Post -Uri $tokenUrl -Body @{
  grant_type    = "client_credentials"
  client_id     = $ClientId.Trim()
  client_secret = $ClientSecret.Trim()
  resource      = $Resource
} -ContentType "application/x-www-form-urlencoded" -TimeoutSec 60
$headers = @{ Authorization = ("Bearer " + $token.access_token); Accept = "application/json" }

function Save-Json([string]$Name, $Object) {
  $path = Join-Path $OutDir $Name
  ($Object | ConvertTo-Json -Depth 30) | Set-Content -Path $path -Encoding UTF8
  return $path
}

function Invoke-ODataGet([string]$Uri) {
  try {
    $resp = Invoke-RestMethod -Method Get -Uri $Uri -Headers $headers -TimeoutSec 120
    return [pscustomobject]@{ Ok = $true; Status = 200; Body = $resp; Error = $null; Uri = $Uri }
  } catch {
    $status = $null; $body = $null
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
    return [pscustomobject]@{
      Ok = $false; Status = $status; Body = $null
      Error = [pscustomobject]@{ message = $_.Exception.Message; status = $status; body = $body; uri = $Uri }
      Uri = $Uri
    }
  }
}

function Get-Entity([string]$Entity, [string]$Filter, [string]$Select = $null, [int]$Top = 50, [string]$OrderBy = $null) {
  $qb = New-Object System.Collections.Generic.List[string]
  [void]$qb.Add("`$filter=" + [uri]::EscapeDataString($Filter))
  [void]$qb.Add("`$top=$Top")
  if ($Select) { [void]$qb.Add("`$select=" + [uri]::EscapeDataString($Select)) }
  if ($OrderBy) { [void]$qb.Add("`$orderby=" + [uri]::EscapeDataString($OrderBy)) }
  $uri = $BaseUrl + "/data/" + $Entity + "?" + ($qb -join "&")
  return Invoke-ODataGet $uri
}

function Get-EntityNoFilter([string]$Entity, [string]$Select = $null, [int]$Top = 5) {
  $qb = New-Object System.Collections.Generic.List[string]
  [void]$qb.Add("`$top=$Top")
  if ($Select) { [void]$qb.Add("`$select=" + [uri]::EscapeDataString($Select)) }
  $uri = $BaseUrl + "/data/" + $Entity + "?" + ($qb -join "&")
  return Invoke-ODataGet $uri
}

function Normalize-Wh([string]$w) {
  if ([string]::IsNullOrWhiteSpace($w)) { return "" }
  return $w.Trim().ToUpperInvariant()
}

function Resolve-OperationalStatus($inventLocationIds, $qualityWarehouseId, $batchDispositionCode) {
  $warehouses = New-Object System.Collections.Generic.List[string]
  foreach ($id in @($inventLocationIds)) {
    if (-not [string]::IsNullOrWhiteSpace($id)) { [void]$warehouses.Add($id.Trim()) }
  }
  if (-not [string]::IsNullOrWhiteSpace($qualityWarehouseId)) { [void]$warehouses.Add($qualityWarehouseId.Trim()) }
  $seen = @{}
  $unique = @()
  foreach ($w in $warehouses) {
    $k = Normalize-Wh $w
    if (-not $seen.ContainsKey($k)) { $seen[$k] = $true; $unique += $w }
  }
  foreach ($w in $unique) {
    if ((Normalize-Wh $w) -eq "REM") { return [pscustomobject]@{ status = "RECHAZADO"; rule = "Almacén REM"; warehouse = $w } }
  }
  foreach ($w in $unique) {
    if ((Normalize-Wh $w) -eq "RES") { return [pscustomobject]@{ status = "RECHAZADO"; rule = "Almacén RES"; warehouse = $w } }
  }
  foreach ($w in $unique) {
    if ((Normalize-Wh $w) -eq "CUARENTENA") { return [pscustomobject]@{ status = "CUARENTENA"; rule = "Almacén CUARENTENA"; warehouse = $w } }
  }
  $disp = if ($batchDispositionCode) { $batchDispositionCode.Trim().ToUpperInvariant() } else { "" }
  if ($disp -match "APROB|APPROV|AVAILABLE|DISPONIB") {
    return [pscustomobject]@{ status = "APROBADO"; rule = "BatchDispositionCode"; warehouse = $(if ($unique.Count -gt 0) { $unique[0] } else { $null }) }
  }
  if ($disp -match "RECHAZ|REJECT") {
    return [pscustomobject]@{ status = "RECHAZADO"; rule = "BatchDispositionCode"; warehouse = $(if ($unique.Count -gt 0) { $unique[0] } else { $null }) }
  }
  if ($disp -match "CUARENT|QUARANT|HOLD") {
    return [pscustomobject]@{ status = "CUARENTENA"; rule = "BatchDispositionCode"; warehouse = $(if ($unique.Count -gt 0) { $unique[0] } else { $null }) }
  }
  if ($unique.Count -gt 0 -and [string]::IsNullOrWhiteSpace($disp)) {
    return [pscustomobject]@{ status = "APROBADO"; rule = "BatchDispositionCode"; warehouse = $unique[0] }
  }
  return [pscustomobject]@{ status = "DESCONOCIDO"; rule = "Información insuficiente"; warehouse = $null }
}

# --- Metadata probes: BaseWorkers + InventTrans full row keys ---
Write-Host "`n=== Metadata probes ==="
$metaWorkers = @("BaseWorkers", "Workers", "HcmWorkers", "DirPeopleV2", "Employees")
$workerEntityFound = $null
$workerSample = $null
foreach ($ent in $metaWorkers) {
  $r = Get-EntityNoFilter $ent $null 1
  [void](Save-Json ("meta-$ent.json") $(if ($r.Ok) { $r.Body } else { $r.Error }))
  Write-Host ("  $ent -> ok=$($r.Ok) status=$($r.Status)")
  if ($r.Ok -and $r.Body.value -and @($r.Body.value).Count -gt 0) {
    $workerEntityFound = $ent
    $workerSample = @($r.Body.value)[0]
    break
  }
}

$transProbe = Get-EntityNoFilter "InventTransCDSEntities" $null 1
[void](Save-Json "meta-InventTransCDSEntities-sample.json" $(if ($transProbe.Ok) { $transProbe.Body } else { $transProbe.Error }))
$transKeys = @()
if ($transProbe.Ok -and $transProbe.Body.value -and @($transProbe.Body.value).Count -gt 0) {
  $transKeys = @(@($transProbe.Body.value)[0].PSObject.Properties.Name)
}
[void](Save-Json "meta-InventTransCDSEntities-keys.json" $transKeys)
Write-Host ("  InventTrans keys count=" + $transKeys.Count)

$qualityProbe = Get-EntityNoFilter "QualityOrderHeaders" $null 1
[void](Save-Json "meta-QualityOrderHeaders-sample.json" $(if ($qualityProbe.Ok) { $qualityProbe.Body } else { $qualityProbe.Error }))
$qualityKeys = @()
if ($qualityProbe.Ok -and $qualityProbe.Body.value -and @($qualityProbe.Body.value).Count -gt 0) {
  $qualityKeys = @(@($qualityProbe.Body.value)[0].PSObject.Properties.Name)
}
[void](Save-Json "meta-QualityOrderHeaders-keys.json" $qualityKeys)

# Preferred InventTrans select: only keys that exist
$wantedTrans = @(
  "inventDimId","StatusIssue","StatusReceipt","DatePhysical","DateFinancial",
  "ModifiedDateTime","CreatedDateTime","ModifiedBy","CreatedBy","Worker",
  "PersonnelNumber","UserId","InventLocationId","InventSiteId","ReferenceId",
  "ReferenceCategory","ReferenceType","InventTransType","ItemNumber","Qty",
  "Voucher","InventTransOrigin","TransactionId"
)
$transSelect = ($wantedTrans | Where-Object { $_ -in $transKeys -or $transKeys.Count -eq 0 }) -join ","
if ([string]::IsNullOrWhiteSpace($transSelect)) {
  # fallback if sample empty: try common set
  $transSelect = "inventDimId,StatusIssue,StatusReceipt,DatePhysical,DateFinancial,ModifiedDateTime,CreatedDateTime,ModifiedBy,CreatedBy,ReferenceId,ReferenceCategory,InventTransType,ItemNumber,Qty"
}

$qualitySelect = @(
  "QualityOrderNumber","ItemBatchNumber","ItemNumber","InventoryBatchId",
  "QualityOrderStatus","ValidationStatus","ValidatedDateTime","ValidatingPersonnelNumber",
  "PassedBatchDispositionCode","FailedBatchDispositionCode","WarehouseId","WarehouseLocationId"
) -join ","

$personnelNumbers = New-Object System.Collections.Generic.HashSet[string]
$lotSummaries = @()

foreach ($suffix in $BatchSuffixes) {
  Write-Host "`n========== $suffix =========="
  $tag = $suffix

  # Resolve full BatchNumber
  $batchR = Get-Entity "ItemBatches" "endswith(BatchNumber,'$suffix')" "ItemNumber,BatchNumber,BatchExpirationDate,BatchDispositionCode" 10
  [void](Save-Json "$tag-01-ItemBatches.json" $(if ($batchR.Ok) { $batchR.Body } else { $batchR.Error }))
  if (-not $batchR.Ok -or -not $batchR.Body.value -or @($batchR.Body.value).Count -eq 0) {
    # try contains
    $batchR = Get-Entity "ItemBatches" "contains(BatchNumber,'$suffix')" "ItemNumber,BatchNumber,BatchExpirationDate,BatchDispositionCode" 10
    [void](Save-Json "$tag-01b-ItemBatches-contains.json" $(if ($batchR.Ok) { $batchR.Body } else { $batchR.Error }))
  }

  $batches = @()
  if ($batchR.Ok -and $batchR.Body.value) { $batches = @($batchR.Body.value) }
  if ($batches.Count -eq 0) {
    Write-Host "  NO ItemBatches"
    $lotSummaries += [pscustomobject]@{
      suffix = $suffix; batchNumber = $null; found = $false; note = "ItemBatches vacío"
    }
    continue
  }

  # Prefer exact endswith match; if multiple, take all but primary = first
  $primary = $batches[0]
  $lote = [string]$primary.BatchNumber
  $item = [string]$primary.ItemNumber
  $disp = [string]$primary.BatchDispositionCode
  Write-Host "  BatchNumber=$lote ItemNumber=$item Disposition=$disp (matches=$($batches.Count))"

  # InventDim
  $dimR = Get-Entity "InventDimBiEntities" "inventBatchId eq '$lote'" "inventDimId,inventBatchId,InventLocationId,wMSLocationId,InventSiteId" 50
  [void](Save-Json "$tag-02-InventDim.json" $(if ($dimR.Ok) { $dimR.Body } else { $dimR.Error }))
  $dims = @()
  if ($dimR.Ok -and $dimR.Body.value) { $dims = @($dimR.Body.value) }
  $locations = @($dims | ForEach-Object { $_.InventLocationId } | Where-Object { $_ } | Select-Object -Unique)
  Write-Host ("  InventDim locations: " + ($locations -join ", "))

  # Quality orders — by ItemBatchNumber
  $qR = Get-Entity "QualityOrderHeaders" "ItemBatchNumber eq '$lote'" $qualitySelect 20
  [void](Save-Json "$tag-03-QualityOrderHeaders.json" $(if ($qR.Ok) { $qR.Body } else { $qR.Error }))
  if (-not $qR.Ok) {
    # retry without InventoryBatchId / ValidationStatus if bad select
    $qR2 = Get-Entity "QualityOrderHeaders" "ItemBatchNumber eq '$lote'" $null 20
    [void](Save-Json "$tag-03b-QualityOrderHeaders-full.json" $(if ($qR2.Ok) { $qR2.Body } else { $qR2.Error }))
    if ($qR2.Ok) { $qR = $qR2 }
  }
  $orders = @()
  if ($qR.Ok -and $qR.Body.value) { $orders = @($qR.Body.value) }
  Write-Host ("  QualityOrders=" + $orders.Count)
  foreach ($o in $orders) {
    $pn = [string]$o.ValidatingPersonnelNumber
    if (-not [string]::IsNullOrWhiteSpace($pn)) { [void]$personnelNumbers.Add($pn.Trim()) }
  }

  $qualityWh = $null
  if ($orders.Count -gt 0) { $qualityWh = [string]$orders[0].WarehouseId }
  $op = Resolve-OperationalStatus $locations $qualityWh $disp

  # InventTrans for each dim — especially REM/RES/CUARENTENA
  $targetDims = @($dims | Where-Object {
    $n = Normalize-Wh ([string]$_.InventLocationId)
    $n -in @("REM","RES","CUARENTENA")
  })
  if ($targetDims.Count -eq 0) {
    # still sample first dim for baseline
    $targetDims = @($dims | Select-Object -First 2)
  }

  $transAll = @()
  $ti = 0
  foreach ($d in $targetDims) {
    $ti++
    $dimId = [string]$d.inventDimId
    $loc = [string]$d.InventLocationId
    $tR = Get-Entity "InventTransCDSEntities" "inventDimId eq '$dimId'" $transSelect 100 "DatePhysical asc"
    [void](Save-Json ("$tag-04-InventTrans-$ti-$loc.json") $(if ($tR.Ok) { $tR.Body } else { $tR.Error }))
    if (-not $tR.Ok) {
      $tR2 = Get-Entity "InventTransCDSEntities" "inventDimId eq '$dimId'" $null 20
      [void](Save-Json ("$tag-04b-InventTrans-$ti-$loc-full.json") $(if ($tR2.Ok) { $tR2.Body } else { $tR2.Error }))
      if ($tR2.Ok) { $tR = $tR2 }
    }
    if ($tR.Ok -and $tR.Body.value) {
      foreach ($row in @($tR.Body.value)) {
        $transAll += [pscustomobject]@{
          inventDimId = $dimId
          InventLocationId = $loc
          wMSLocationId = [string]$d.wMSLocationId
          row = $row
        }
      }
    }
  }

  # Also try filter by ItemNumber if inventory has batch field — skip if no field

  $summary = [pscustomobject]@{
    suffix = $suffix
    found = $true
    batchNumber = $lote
    itemNumber = $item
    batchDispositionCode = $disp
    batchExpirationDate = [string]$primary.BatchExpirationDate
    inventLocations = $locations
    operationalStatus = $op.status
    operationalRule = $op.rule
    operationalWarehouse = $op.warehouse
    qualityOrderCount = $orders.Count
    qualityOrders = @($orders | ForEach-Object {
      [pscustomobject]@{
        QualityOrderNumber = $_.QualityOrderNumber
        InventoryBatchId = $_.InventoryBatchId
        ItemBatchNumber = $_.ItemBatchNumber
        QualityOrderStatus = $_.QualityOrderStatus
        ValidationStatus = $_.ValidationStatus
        ValidatedDateTime = $_.ValidatedDateTime
        ValidatingPersonnelNumber = $_.ValidatingPersonnelNumber
        PassedBatchDispositionCode = $_.PassedBatchDispositionCode
        FailedBatchDispositionCode = $_.FailedBatchDispositionCode
        WarehouseId = $_.WarehouseId
        WarehouseLocationId = $_.WarehouseLocationId
      }
    })
    inventDimCount = $dims.Count
    inventDims = @($dims | ForEach-Object {
      [pscustomobject]@{
        inventDimId = $_.inventDimId
        InventLocationId = $_.InventLocationId
        wMSLocationId = $_.wMSLocationId
        InventSiteId = $_.InventSiteId
      }
    })
    inventTransSampleCount = $transAll.Count
    inventTransTargetLocations = @($targetDims | ForEach-Object { $_.InventLocationId } | Select-Object -Unique)
  }
  [void](Save-Json "$tag-00-summary.json" $summary)
  $lotSummaries += $summary
}

# --- BaseWorkers lookups ---
Write-Host "`n=== BaseWorkers / workers ==="
$workerResults = @()
$workerEntity = if ($workerEntityFound) { $workerEntityFound } else { "BaseWorkers" }
foreach ($pn in ($personnelNumbers | Sort-Object)) {
  Write-Host "  PersonnelNumber=$pn via $workerEntity"
  $filters = @(
    "PersonnelNumber eq '$pn'",
    "PersonnelNumber eq '$pn'"
  )
  $found = $false
  foreach ($f in $filters) {
    $wR = Get-Entity $workerEntity $f $null 5
    [void](Save-Json ("worker-$pn-$workerEntity.json") $(if ($wR.Ok) { $wR.Body } else { $wR.Error }))
    if ($wR.Ok -and $wR.Body.value -and @($wR.Body.value).Count -gt 0) {
      $row = @($wR.Body.value)[0]
      $workerResults += [pscustomobject]@{
        queryPersonnelNumber = $pn
        entity = $workerEntity
        found = $true
        PersonnelNumber = $row.PersonnelNumber
        Name = $row.Name
        FirstName = $row.FirstName
        LastName = $row.LastName
        rawKeys = @($row.PSObject.Properties.Name)
        raw = $row
      }
      $found = $true
      break
    }
  }
  if (-not $found) {
    # try alternate entities
    foreach ($alt in @("BaseWorkers","Workers","HcmWorkers")) {
      if ($alt -eq $workerEntity) { continue }
      $wR = Get-Entity $alt "PersonnelNumber eq '$pn'" $null 5
      [void](Save-Json ("worker-$pn-$alt.json") $(if ($wR.Ok) { $wR.Body } else { $wR.Error }))
      if ($wR.Ok -and $wR.Body.value -and @($wR.Body.value).Count -gt 0) {
        $row = @($wR.Body.value)[0]
        $workerResults += [pscustomobject]@{
          queryPersonnelNumber = $pn
          entity = $alt
          found = $true
          PersonnelNumber = $row.PersonnelNumber
          Name = $row.Name
          FirstName = $row.FirstName
          LastName = $row.LastName
          rawKeys = @($row.PSObject.Properties.Name)
          raw = $row
        }
        $found = $true
        break
      }
    }
  }
  if (-not $found) {
    $workerResults += [pscustomobject]@{
      queryPersonnelNumber = $pn
      entity = $workerEntity
      found = $false
      PersonnelNumber = $null
      Name = $null
      FirstName = $null
      LastName = $null
      rawKeys = @()
      raw = $null
    }
  }
}

[void](Save-Json "00-lot-summaries.json" $lotSummaries)
[void](Save-Json "00-workers.json" $workerResults)
[void](Save-Json "00-meta.json" ([pscustomobject]@{
  workerEntityFound = $workerEntityFound
  inventTransKeys = $transKeys
  qualityOrderKeys = $qualityKeys
  personnelNumbers = @($personnelNumbers)
  generatedAt = (Get-Date).ToString("o")
  baseUrl = $BaseUrl
}))

Write-Host "`nDONE OutDir=$OutDir"
Write-Host ("Lots=" + $lotSummaries.Count + " Workers=" + $workerResults.Count)
