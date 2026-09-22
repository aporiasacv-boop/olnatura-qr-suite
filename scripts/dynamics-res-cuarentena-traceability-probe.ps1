[CmdletBinding()]
param()

$ErrorActionPreference = "Stop"
$termLog = "C:\Users\BecarioQR\.cursor\projects\c-Users-BecarioQR-OneDrive-OLNATURA-S-A-DE-CV-Escritorio-olnatura-qr-suite-qr-suite-olnatura-qr-suite\terminals\176660.txt"
$raw = Get-Content -LiteralPath $termLog -Raw
function Grab([string]$pattern) {
  $m = [regex]::Match($raw, $pattern)
  if (-not $m.Success) { throw "missing $pattern" }
  return $m.Groups[1].Value
}
$TenantId = Grab "\`$tenant = '([^']+)'"
$ClientId = Grab "\`$clientId = '([^']+)'"
$ClientSecret = Grab "\`$clientSecret = '([^']+)'"
$BaseUrl = (Grab "\`$base = '([^']+)'").TrimEnd("/")

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$OutDir = Join-Path $PSScriptRoot ("out\dynamics-trazabilidad-res-cuarentena-" + $stamp)
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
Write-Host "OutDir=$OutDir"

$token = Invoke-RestMethod -Method Post -Uri ("https://login.microsoftonline.com/$TenantId/oauth2/token") -Body @{
  grant_type = "client_credentials"; client_id = $ClientId; client_secret = $ClientSecret; resource = $BaseUrl
} -ContentType "application/x-www-form-urlencoded" -TimeoutSec 60
$headers = @{ Authorization = ("Bearer " + $token.access_token); Accept = "application/json" }
Write-Host "OAuth OK"

function Save-Json([string]$Name, $Object) {
  ($Object | ConvertTo-Json -Depth 40) | Set-Content -Path (Join-Path $OutDir $Name) -Encoding UTF8
}
function OData([string]$Entity, [string]$Filter, [string]$Select = $null, [int]$Top = 100, [string]$OrderBy = $null) {
  $qb = New-Object System.Collections.Generic.List[string]
  [void]$qb.Add(("`$filter=" + [uri]::EscapeDataString($Filter)))
  [void]$qb.Add(("`$top=" + $Top))
  if ($Select) { [void]$qb.Add(("`$select=" + [uri]::EscapeDataString($Select))) }
  if ($OrderBy) { [void]$qb.Add(("`$orderby=" + [uri]::EscapeDataString($OrderBy))) }
  $uri = $BaseUrl + "/data/" + $Entity + "?" + ($qb -join "&")
  try {
    $body = Invoke-RestMethod -Method Get -Uri $uri -Headers $headers -TimeoutSec 120
    return [pscustomobject]@{ Ok = $true; Body = $body; Uri = $uri; Error = $null }
  } catch {
    $status = $null
    try { $status = [int]$_.Exception.Response.StatusCode } catch {}
    $detail = if ($_.ErrorDetails) { $_.ErrorDetails.Message } else { $_.Exception.Message }
    return [pscustomobject]@{ Ok = $false; Body = $null; Uri = $uri; Error = [pscustomobject]@{ status = $status; message = $detail } }
  }
}
function NormWh([string]$w) {
  if ([string]::IsNullOrWhiteSpace($w)) { return "" }
  return $w.Trim().ToUpperInvariant()
}
function Resolve-Op($locs, $qWh, $disp) {
  $all = @(); foreach ($x in @($locs)) { if ($x) { $all += $x.Trim() } }; if ($qWh) { $all += $qWh.Trim() }
  $uniq = @($all | Select-Object -Unique)
  foreach ($w in $uniq) { if ((NormWh $w) -eq "REM") { return @{ status = "RECHAZADO"; rule = "Almacen REM"; wh = $w } } }
  foreach ($w in $uniq) { if ((NormWh $w) -eq "RES") { return @{ status = "RECHAZADO"; rule = "Almacen RES"; wh = $w } } }
  foreach ($w in $uniq) { if ((NormWh $w) -eq "CUARENTENA") { return @{ status = "CUARENTENA"; rule = "Almacen CUARENTENA"; wh = $w } } }
  $d = if ($disp) { $disp.Trim().ToUpperInvariant() } else { "" }
  if ($d -match "APROB|APPROV|AVAILABLE|DISPONIB") { return @{ status = "APROBADO"; rule = "BatchDispositionCode"; wh = $(if ($uniq) { $uniq[0] } else { $null }) } }
  if ($d -match "RECHAZ|REJECT") { return @{ status = "RECHAZADO"; rule = "BatchDispositionCode"; wh = $(if ($uniq) { $uniq[0] } else { $null }) } }
  if ($d -match "CUARENT|QUARANT|HOLD") { return @{ status = "CUARENTENA"; rule = "BatchDispositionCode"; wh = $(if ($uniq) { $uniq[0] } else { $null }) } }
  if ($uniq.Count -gt 0 -and [string]::IsNullOrWhiteSpace($d)) { return @{ status = "APROBADO"; rule = "BatchDispositionCode"; wh = $uniq[0] } }
  return @{ status = "DESCONOCIDO"; rule = "Informacion insuficiente"; wh = $null }
}
function IsSentinelDate($v) {
  if (-not $v) { return $true }
  $s = [string]$v
  return ($s -eq "1900-01-01T00:00:00Z" -or $s.StartsWith("1900-01-01"))
}
function Pick-DefinitiveMovement($rows) {
  $sorted = @($rows | Sort-Object {
    if (IsSentinelDate $_.DatePhysical) { "9999-12-31T00:00:00Z" } else { [string]$_.DatePhysical }
  })
  $candidates = @($sorted | Where-Object {
    (-not (IsSentinelDate $_.DatePhysical)) -and (
      ([string]$_.StatusReceipt -match "Received|Purchased|Arrived") -or
      ($null -ne $_.Qty -and [double]$_.Qty -ne 0)
    )
  })
  if ($candidates.Count -eq 0) {
    $candidates = @($sorted | Where-Object { -not (IsSentinelDate $_.DatePhysical) })
  }
  if ($candidates.Count -eq 0) { return $null }
  return $candidates[0]
}
function Resolve-FullBatch([string]$suffix) {
  # Prefer known hit + date sweep (yyMMdd-SUFFIX)
  $known = @{
    "MPS0006465" = "260410-MPS0006465"
  }
  if ($known.ContainsKey($suffix)) {
    $bn = $known[$suffix]
    $r = OData "ItemBatches" "BatchNumber eq '$bn'" "ItemNumber,BatchNumber,BatchExpirationDate,BatchDispositionCode" 3
    if ($r.Ok -and $r.Body.value -and @($r.Body.value).Count -gt 0) { return @($r.Body.value)[0] }
  }
  $day = Get-Date "2024-01-01"
  $end = Get-Date "2026-07-28"
  while ($day -le $end) {
    $bn = $day.ToString("yyMMdd") + "-" + $suffix
    $r = OData "ItemBatches" "BatchNumber eq '$bn'" "ItemNumber,BatchNumber,BatchExpirationDate,BatchDispositionCode" 1
    if ($r.Ok -and $r.Body.value -and @($r.Body.value).Count -gt 0) { return @($r.Body.value)[0] }
    $day = $day.AddDays(1)
  }
  return $null
}

$lots = @(
  @{ suffix = "MPS0006465"; expected = "CUARENTENA"; expectedWh = "CUARENTENA" }
  @{ suffix = "MPS0006246"; expected = "CUARENTENA"; expectedWh = "CUARENTENA" }
  @{ suffix = "MPS0006641"; expected = "CUARENTENA"; expectedWh = "CUARENTENA" }
  @{ suffix = "MPS0005078"; expected = "RECHAZADO"; expectedWh = "RES" }
  @{ suffix = "MPS0005089"; expected = "RECHAZADO"; expectedWh = "RES" }
)

$qSelect = "QualityOrderNumber,ItemBatchNumber,ItemNumber,QualityOrderStatus,ValidatedDateTime,ValidatingPersonnelNumber,PassedBatchDispositionCode,FailedBatchDispositionCode,WarehouseId,WarehouseLocationId,QMSAssignedToPersonnelNumber"
$transSelect = "inventDimId,StatusIssue,StatusReceipt,DatePhysical,DateFinancial,DateInvent,DateStatus,DateClosed,Qty,ItemId,Voucher,VoucherPhysical,InventTransOrigin,PackingSlipId,InvoiceId,ReceiptId,TransChildType,TransChildRefId,RecordId"

$transProbe = OData "InventTransCDSEntities" "RecordId ne 0" $null 1
$transKeys = @()
if ($transProbe.Ok -and $transProbe.Body.value -and @($transProbe.Body.value).Count -gt 0) {
  $transKeys = @(@($transProbe.Body.value)[0].PSObject.Properties.Name)
}
Save-Json "meta-InventTrans-keys.json" $transKeys

$comparative = @()
$summaries = @()

foreach ($L in $lots) {
  $tag = $L.suffix
  Write-Host "`n===== $tag ====="
  Write-Host "  Resolving full BatchNumber..."
  $batch = Resolve-FullBatch $tag
  if (-not $batch) {
    Write-Host "  NO BATCH"
    $comparative += [pscustomobject]@{
      lote = $tag; estadoOperativo = $null; almacen = $null; inventDimId = $null; inventSiteId = $null
      datePhysical = $null; sysModifiedBy = $null; campoFecha = $null; campoCuenta = $null
      confianza = "Baja"; nota = "BatchNumber no encontrado"; expected = $L.expected; expectedWh = $L.expectedWh; matchExpected = $false
    }
    continue
  }
  $lote = [string]$batch.BatchNumber
  $item = [string]$batch.ItemNumber
  $disp = [string]$batch.BatchDispositionCode
  Write-Host "  BatchNumber=$lote Item=$item Disp=$disp"
  Save-Json "$tag-01-ItemBatches.json" $batch

  $dR = OData "InventDimBiEntities" "inventBatchId eq '$lote'" "inventDimId,inventBatchId,InventLocationId,wMSLocationId,InventSiteId" 50
  Save-Json "$tag-02-InventDim.json" $(if ($dR.Ok) { $dR.Body } else { $dR.Error })
  $dims = @(); if ($dR.Ok -and $dR.Body.value) { $dims = @($dR.Body.value) }
  $locs = @($dims | ForEach-Object { $_.InventLocationId } | Where-Object { $_ } | Select-Object -Unique)
  Write-Host ("  locs=" + ($locs -join ","))

  $qR = OData "QualityOrderHeaders" "ItemBatchNumber eq '$lote'" $qSelect 20
  Save-Json "$tag-03-Quality.json" $(if ($qR.Ok) { $qR.Body } else { $qR.Error })
  $orders = @(); if ($qR.Ok -and $qR.Body.value) { $orders = @($qR.Body.value) }
  $qWh = if ($orders.Count -gt 0) { [string]$orders[0].WarehouseId } else { $null }
  $op = Resolve-Op $locs $qWh $disp
  Write-Host ("  OP=" + $op.status + " / " + $op.rule + " / " + $op.wh)

  $focus = @($dims | Where-Object {
    $n = NormWh $_.InventLocationId
    $n -in @("REM", "RES", "CUARENTENA")
  })
  if ($focus.Count -eq 0 -and $op.wh) {
    $focus = @($dims | Where-Object { (NormWh $_.InventLocationId) -eq (NormWh $op.wh) })
  }
  if ($focus.Count -eq 0) { $focus = @($dims | Select-Object -First 1) }

  $movementDetails = @()
  $ti = 0
  foreach ($d in $focus) {
    $ti++
    $dimId = [string]$d.inventDimId
    $loc = [string]$d.InventLocationId
    $site = [string]$d.InventSiteId

    $dimFull = OData "InventDimBiEntities" "inventDimId eq '$dimId'" $null 5
    Save-Json ("$tag-05-InventDim-full-$ti-$loc.json") $(if ($dimFull.Ok) { $dimFull.Body } else { $dimFull.Error })
    $dimRow = $null
    if ($dimFull.Ok -and $dimFull.Body.value -and @($dimFull.Body.value).Count -gt 0) {
      $dimRow = @($dimFull.Body.value)[0]
    }

    $tR = OData "InventTransCDSEntities" "inventDimId eq '$dimId'" $transSelect 100 "DatePhysical asc"
    Save-Json ("$tag-04-Trans-$ti-$loc.json") $(if ($tR.Ok) { $tR.Body } else { $tR.Error })
    if (-not $tR.Ok) {
      $tR2 = OData "InventTransCDSEntities" "inventDimId eq '$dimId'" $null 50
      Save-Json ("$tag-04b-Trans-$ti-$loc-full.json") $(if ($tR2.Ok) { $tR2.Body } else { $tR2.Error })
      if ($tR2.Ok) { $tR = $tR2 }
    }
    $rows = @(); if ($tR.Ok -and $tR.Body.value) { $rows = @($tR.Body.value) }
    Write-Host ("  Trans $loc dim=$dimId rows=" + $rows.Count)

    $def = Pick-DefinitiveMovement $rows
    $sysModBy = if ($dimRow) { [string]$dimRow.SysModifiedBy } else { $null }
    $sysCreated = if ($dimRow) { [string]$dimRow.SysCreatedDateTime } else { $null }
    $sysModified = if ($dimRow) { [string]$dimRow.SysModifiedDateTime } else { $null }
    $sysCreatedBy = $null
    if ($dimRow -and ($dimRow.PSObject.Properties.Name -contains "SysCreatedBy")) { $sysCreatedBy = [string]$dimRow.SysCreatedBy }

    $createdEqualsModified = ($sysCreated -and $sysModified -and ($sysCreated -eq $sysModified))
    $datePhys = if ($def) { [string]$def.DatePhysical } else { $null }
    $datePhysDay = $null; $sysCreatedDay = $null
    if ($datePhys -and -not (IsSentinelDate $datePhys)) { $datePhysDay = ([datetime]$datePhys).ToString("yyyy-MM-dd") }
    if ($sysCreated -and -not (IsSentinelDate $sysCreated)) { $sysCreatedDay = ([datetime]$sysCreated).ToString("yyyy-MM-dd") }
    $dateAligns = ($datePhysDay -and $sysCreatedDay -and ($datePhysDay -eq $sysCreatedDay))

    $movementDetails += [pscustomobject]@{
      inventDimId = $dimId
      InventLocationId = $loc
      InventSiteId = $site
      wMSLocationId = [string]$d.wMSLocationId
      inventTransRowCount = $rows.Count
      definitive = if ($def) {
        [pscustomobject]@{
          DatePhysical = $def.DatePhysical; DateFinancial = $def.DateFinancial
          DateInvent = $def.DateInvent; DateStatus = $def.DateStatus; DateClosed = $def.DateClosed
          StatusIssue = $def.StatusIssue; StatusReceipt = $def.StatusReceipt; Qty = $def.Qty
          Voucher = $def.Voucher; InventTransOrigin = $def.InventTransOrigin
          PackingSlipId = $def.PackingSlipId; InvoiceId = $def.InvoiceId; ReceiptId = $def.ReceiptId
          TransChildType = $def.TransChildType; TransChildRefId = $def.TransChildRefId; RecordId = $def.RecordId
          inventDimId = $def.inventDimId
          CreatedDateTime = $null; ModifiedDateTime = $null; SysModifiedBy = $null
          CreatedBy = $null; ModifiedBy = $null; ReferenceId = $null
          ReferenceCategory = $null; ReferenceType = $null; InventTransType = $null
        }
      } else { $null }
      allRows = @($rows | ForEach-Object {
        [pscustomobject]@{
          DatePhysical = $_.DatePhysical; DateFinancial = $_.DateFinancial
          StatusIssue = $_.StatusIssue; StatusReceipt = $_.StatusReceipt
          Qty = $_.Qty; Voucher = $_.Voucher; InventTransOrigin = $_.InventTransOrigin; RecordId = $_.RecordId
        }
      })
      inventDimAudit = [pscustomobject]@{
        SysModifiedBy = $sysModBy; SysCreatedBy = $sysCreatedBy
        SysCreatedDateTime = $sysCreated; SysModifiedDateTime = $sysModified
        createdEqualsModified = $createdEqualsModified
        datePhysicalDayAlignsWithSysCreated = $dateAligns
      }
    }
  }

  $primaryMove = $movementDetails | Where-Object { (NormWh $_.InventLocationId) -eq (NormWh $L.expectedWh) } | Select-Object -First 1
  if (-not $primaryMove) { $primaryMove = $movementDetails | Where-Object { (NormWh $_.InventLocationId) -eq (NormWh $op.wh) } | Select-Object -First 1 }
  if (-not $primaryMove) { $primaryMove = $movementDetails | Select-Object -First 1 }

  $matchExpected = ($op.status -eq $L.expected) -and (
    (NormWh $op.wh) -eq (NormWh $L.expectedWh) -or (NormWh $primaryMove.InventLocationId) -eq (NormWh $L.expectedWh)
  )
  $audit = $primaryMove.inventDimAudit
  $confianza = "Baja"
  if ($primaryMove -and $primaryMove.definitive -and $audit.SysModifiedBy -and $audit.createdEqualsModified -and $audit.datePhysicalDayAlignsWithSysCreated -and $matchExpected) {
    $confianza = "Alta"
  } elseif ($primaryMove -and $primaryMove.definitive -and $audit.SysModifiedBy -and $matchExpected) {
    $confianza = "Media"
  } elseif ($matchExpected -and $primaryMove) {
    $confianza = "Media"
  }

  $row = [pscustomobject]@{
    lote = $lote
    suffix = $tag
    estadoOperativo = $op.status
    regla = $op.rule
    almacen = $primaryMove.InventLocationId
    inventDimId = $primaryMove.inventDimId
    inventSiteId = $primaryMove.InventSiteId
    wMSLocationId = $primaryMove.wMSLocationId
    datePhysical = if ($primaryMove.definitive) { $primaryMove.definitive.DatePhysical } else { $null }
    dateFinancial = if ($primaryMove.definitive) { $primaryMove.definitive.DateFinancial } else { $null }
    statusReceipt = if ($primaryMove.definitive) { $primaryMove.definitive.StatusReceipt } else { $null }
    statusIssue = if ($primaryMove.definitive) { $primaryMove.definitive.StatusIssue } else { $null }
    qty = if ($primaryMove.definitive) { $primaryMove.definitive.Qty } else { $null }
    inventTransOrigin = if ($primaryMove.definitive) { $primaryMove.definitive.InventTransOrigin } else { $null }
    voucher = if ($primaryMove.definitive) { $primaryMove.definitive.Voucher } else { $null }
    sysModifiedBy = $audit.SysModifiedBy
    sysCreatedBy = $audit.SysCreatedBy
    sysCreatedDateTime = $audit.SysCreatedDateTime
    sysModifiedDateTime = $audit.SysModifiedDateTime
    createdEqualsModified = $audit.createdEqualsModified
    dateAlignsWithDimCreate = $audit.datePhysicalDayAlignsWithSysCreated
    campoFechaRecomendado = "InventTransCDSEntities.DatePhysical"
    campoCuentaRecomendado = "InventDimBiEntities.SysModifiedBy"
    confianza = $confianza
    expected = $L.expected
    expectedWh = $L.expectedWh
    matchExpected = $matchExpected
    inventTransMissingAuditFields = $true
  }
  $comparative += $row

  $sum = [pscustomobject]@{
    suffix = $tag; batchNumber = $lote; itemNumber = $item; batchDispositionCode = $disp
    inventLocations = $locs; operationalStatus = $op.status; operationalRule = $op.rule; operationalWarehouse = $op.wh
    expectedStatus = $L.expected; expectedWarehouse = $L.expectedWh; matchExpected = $matchExpected
    qualityOrderCount = $orders.Count; qualityOrders = $orders
    inventDims = @($dims | ForEach-Object {
      [pscustomobject]@{ inventDimId = $_.inventDimId; InventLocationId = $_.InventLocationId; wMSLocationId = $_.wMSLocationId; InventSiteId = $_.InventSiteId }
    })
    movements = $movementDetails
    comparativeRow = $row
  }
  Save-Json "$tag-00-summary.json" $sum
  $summaries += $sum
}

$remRefPath = Join-Path $PSScriptRoot "out\dynamics-trazabilidad-full-20260728-102937\00-rem-dim-sysmodified.json"
$remRef = $null
if (Test-Path $remRefPath) { $remRef = Get-Content $remRefPath -Raw | ConvertFrom-Json }

Save-Json "00-comparative.json" $comparative
Save-Json "00-lot-summaries.json" $summaries
Save-Json "00-rem-reference.json" $remRef
Save-Json "00-meta.json" ([pscustomobject]@{
  generatedAt = (Get-Date).ToString("o")
  baseUrl = $BaseUrl
  inventTransKeys = $transKeys
  inventTransHasCreatedBy = ($transKeys -contains "CreatedBy")
  inventTransHasModifiedBy = ($transKeys -contains "ModifiedBy")
  inventTransHasSysModifiedBy = ($transKeys -contains "SysModifiedBy")
  inventTransHasReferenceId = ($transKeys -contains "ReferenceId")
  inventTransHasReferenceCategory = ($transKeys -contains "ReferenceCategory")
})

Write-Host "`nDONE"
$comparative | ForEach-Object {
  Write-Host ("{0} | {1}/{2} | dim={3} | DP={4} | SysMod={5} | conf={6} | match={7}" -f `
    $_.lote, $_.estadoOperativo, $_.almacen, $_.inventDimId, $_.datePhysical, $_.sysModifiedBy, $_.confianza, $_.matchExpected)
}
Write-Host "OutDir=$OutDir"
