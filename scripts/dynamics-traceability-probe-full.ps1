# Fase 2: probe trazabilidad con BatchNumber exactos (solo lectura).
[CmdletBinding()]
param(
  [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"
$BaseUrl = ($env:APP_DYNAMICS_BASE_URL).TrimEnd("/")
$TenantId = $env:APP_DYNAMICS_TENANT_ID
$ClientId = $env:APP_DYNAMICS_CLIENT_ID
$ClientSecret = $env:APP_DYNAMICS_CLIENT_SECRET
if (-not $OutDir) {
  $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
  $OutDir = Join-Path $PSScriptRoot ("out\dynamics-trazabilidad-full-" + $stamp)
}
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
Write-Host "OutDir=$OutDir"

$token = Invoke-RestMethod -Method Post -Uri ("https://login.microsoftonline.com/$TenantId/oauth2/token") -Body @{
  grant_type="client_credentials"; client_id=$ClientId; client_secret=$ClientSecret; resource=$BaseUrl
} -ContentType "application/x-www-form-urlencoded" -TimeoutSec 60
$headers = @{ Authorization = "Bearer $($token.access_token)"; Accept = "application/json" }

function Save-Json($Name, $Object) {
  ($Object | ConvertTo-Json -Depth 30) | Set-Content -Path (Join-Path $OutDir $Name) -Encoding UTF8
}
function OData($Entity, $Filter, $Select=$null, $Top=100, $OrderBy=$null) {
  $qb = New-Object System.Collections.Generic.List[string]
  [void]$qb.Add("`$filter=" + [uri]::EscapeDataString($Filter))
  [void]$qb.Add("`$top=$Top")
  if ($Select) { [void]$qb.Add("`$select=" + [uri]::EscapeDataString($Select)) }
  if ($OrderBy) { [void]$qb.Add("`$orderby=" + [uri]::EscapeDataString($OrderBy)) }
  $uri = $BaseUrl + "/data/" + $Entity + "?" + ($qb -join "&")
  try {
    $body = Invoke-RestMethod -Method Get -Uri $uri -Headers $headers -TimeoutSec 120
    return [pscustomobject]@{ Ok=$true; Body=$body; Uri=$uri; Error=$null }
  } catch {
    $errBody = $null; $status=$null
    try { $status=[int]$_.Exception.Response.StatusCode } catch {}
    return [pscustomobject]@{ Ok=$false; Body=$null; Uri=$uri; Error=[pscustomobject]@{ message=$_.Exception.Message; status=$status } }
  }
}
function NormWh($w) { if ([string]::IsNullOrWhiteSpace($w)) { return "" }; return $w.Trim().ToUpperInvariant() }
function Resolve-Op($locs, $qWh, $disp) {
  $all = @(); foreach ($x in @($locs)) { if ($x) { $all += $x.Trim() } }; if ($qWh) { $all += $qWh.Trim() }
  $uniq = @($all | Select-Object -Unique)
  foreach ($w in $uniq) { if ((NormWh $w) -eq "REM") { return @{ status="RECHAZADO"; rule="Almacén REM"; wh=$w } } }
  foreach ($w in $uniq) { if ((NormWh $w) -eq "RES") { return @{ status="RECHAZADO"; rule="Almacén RES"; wh=$w } } }
  foreach ($w in $uniq) { if ((NormWh $w) -eq "CUARENTENA") { return @{ status="CUARENTENA"; rule="Almacén CUARENTENA"; wh=$w } } }
  $d = if ($disp) { $disp.Trim().ToUpperInvariant() } else { "" }
  if ($d -match "APROB|APPROV|AVAILABLE|DISPONIB") { return @{ status="APROBADO"; rule="BatchDispositionCode"; wh=$(if($uniq){$uniq[0]}else{$null}) } }
  if ($d -match "RECHAZ|REJECT") { return @{ status="RECHAZADO"; rule="BatchDispositionCode"; wh=$(if($uniq){$uniq[0]}else{$null}) } }
  if ($d -match "CUARENT|QUARANT|HOLD") { return @{ status="CUARENTENA"; rule="BatchDispositionCode"; wh=$(if($uniq){$uniq[0]}else{$null}) } }
  if ($uniq.Count -gt 0 -and [string]::IsNullOrWhiteSpace($d)) { return @{ status="APROBADO"; rule="BatchDispositionCode"; wh=$uniq[0] } }
  return @{ status="DESCONOCIDO"; rule="Información insuficiente"; wh=$null }
}

$lots = @(
  @{ suffix="MEM0003675"; batch="260724-MEM0003675" },
  @{ suffix="MEM0003559"; batch="260406-MEM0003559" },
  @{ suffix="MEM0003666"; batch="260716-MEM0003666" },
  @{ suffix="MEM0003625"; batch="260619-MEM0003625" },
  @{ suffix="MPM0003416"; batch="260525-MPM0003416" },
  @{ suffix="MPM0003395"; batch="260410-MPM0003395" },
  @{ suffix="MPM0003109"; batch="240923-MPM0003109" },
  @{ suffix="MPM0003363"; batch="260206-MPM0003363" }
)

$qSelect = "QualityOrderNumber,ItemBatchNumber,ItemNumber,QualityOrderStatus,ValidatedDateTime,ValidatingPersonnelNumber,PassedBatchDispositionCode,FailedBatchDispositionCode,WarehouseId,WarehouseLocationId,QMSAssignedToPersonnelNumber"
$transSelect = "inventDimId,StatusIssue,StatusReceipt,DatePhysical,DateFinancial,DateInvent,DateStatus,DateClosed,Qty,ItemId,Voucher,VoucherPhysical,InventTransOrigin,PackingSlipId,InvoiceId,ReceiptId,TransChildType,TransChildRefId,RecordId"

$personnel = New-Object System.Collections.Generic.HashSet[string]
$summaries = @()

foreach ($L in $lots) {
  $tag = $L.suffix
  $lote = $L.batch
  Write-Host "`n===== $tag / $lote ====="

  $bR = OData "ItemBatches" "BatchNumber eq '$lote'" "ItemNumber,BatchNumber,BatchExpirationDate,BatchDispositionCode" 5
  Save-Json "$tag-01-ItemBatches.json" $(if($bR.Ok){$bR.Body}else{$bR.Error})
  $batch = $null
  if ($bR.Ok -and $bR.Body.value -and $bR.Body.value.Count -gt 0) { $batch = $bR.Body.value[0] }
  if (-not $batch) { Write-Host "NO BATCH"; continue }
  $item = [string]$batch.ItemNumber
  $disp = [string]$batch.BatchDispositionCode

  $dR = OData "InventDimBiEntities" "inventBatchId eq '$lote'" "inventDimId,inventBatchId,InventLocationId,wMSLocationId,InventSiteId" 50
  Save-Json "$tag-02-InventDim.json" $(if($dR.Ok){$dR.Body}else{$dR.Error})
  $dims = @(); if ($dR.Ok -and $dR.Body.value) { $dims = @($dR.Body.value) }
  $locs = @($dims | ForEach-Object { $_.InventLocationId } | Where-Object { $_ } | Select-Object -Unique)
  Write-Host ("  locs=" + ($locs -join ","))

  $qR = OData "QualityOrderHeaders" "ItemBatchNumber eq '$lote'" $qSelect 20
  Save-Json "$tag-03-Quality.json" $(if($qR.Ok){$qR.Body}else{$qR.Error})
  $orders = @(); if ($qR.Ok -and $qR.Body.value) { $orders = @($qR.Body.value) }
  Write-Host ("  QO=" + $orders.Count)
  foreach ($o in $orders) {
    if ($o.ValidatingPersonnelNumber) { [void]$personnel.Add([string]$o.ValidatingPersonnelNumber.Trim()) }
    if ($o.QMSAssignedToPersonnelNumber) { [void]$personnel.Add([string]$o.QMSAssignedToPersonnelNumber.Trim()) }
  }

  $qWh = if ($orders.Count -gt 0) { [string]$orders[0].WarehouseId } else { $null }
  $op = Resolve-Op $locs $qWh $disp
  Write-Host ("  OP=" + $op.status + " / " + $op.rule)

  # InventTrans for REM/RES/CUARENTENA dims + also MPM/MEM for baseline entry
  $focus = @($dims | Where-Object { (NormWh $_.InventLocationId) -in @("REM","RES","CUARENTENA") })
  $also = @($dims | Where-Object { (NormWh $_.InventLocationId) -notin @("REM","RES","CUARENTENA") } | Select-Object -First 2)
  $toQuery = @($focus + $also | Select-Object -Unique)

  $transByLoc = @{}
  $ti = 0
  foreach ($d in $toQuery) {
    $ti++
    $dimId = [string]$d.inventDimId
    $loc = [string]$d.InventLocationId
    $tR = OData "InventTransCDSEntities" "inventDimId eq '$dimId'" $transSelect 100 "DatePhysical asc"
    Save-Json ("$tag-04-Trans-$ti-$loc.json") $(if($tR.Ok){$tR.Body}else{$tR.Error})
    $rows = @(); if ($tR.Ok -and $tR.Body.value) { $rows = @($tR.Body.value) }
    $transByLoc[$loc + "|" + $dimId] = $rows
    Write-Host ("  Trans $loc dim=$dimId rows=" + $rows.Count)
  }

  # Candidate movement to REM/RES/CUARENTENA = earliest DatePhysical on that dim with Qty != 0
  $movementCandidates = @()
  foreach ($d in $focus) {
    $loc = [string]$d.InventLocationId
    $dimId = [string]$d.inventDimId
    $key = $loc + "|" + $dimId
    $rows = @($transByLoc[$key])
    $sorted = @($rows | Sort-Object { $_.DatePhysical })
    $first = $sorted | Select-Object -First 1
    $firstNonNull = $sorted | Where-Object { $_.DatePhysical -and $_.DatePhysical -ne "1900-01-01T00:00:00Z" } | Select-Object -First 1
    $withQty = $sorted | Where-Object { $_.Qty -ne 0 -and $_.DatePhysical -and $_.DatePhysical -ne "1900-01-01T00:00:00Z" } | Select-Object -First 1
    $movementCandidates += [pscustomobject]@{
      location = $loc
      inventDimId = $dimId
      wMSLocationId = [string]$d.wMSLocationId
      rowCount = $rows.Count
      firstDatePhysical = if ($firstNonNull) { $firstNonNull.DatePhysical } else { $null }
      firstDateFinancial = if ($firstNonNull) { $firstNonNull.DateFinancial } else { $null }
      firstDateInvent = if ($firstNonNull) { $firstNonNull.DateInvent } else { $null }
      firstDateStatus = if ($firstNonNull) { $firstNonNull.DateStatus } else { $null }
      firstDateClosed = if ($firstNonNull) { $firstNonNull.DateClosed } else { $null }
      firstStatusIssue = if ($firstNonNull) { $firstNonNull.StatusIssue } else { $null }
      firstStatusReceipt = if ($firstNonNull) { $firstNonNull.StatusReceipt } else { $null }
      firstQty = if ($firstNonNull) { $firstNonNull.Qty } else { $null }
      firstVoucher = if ($firstNonNull) { $firstNonNull.Voucher } else { $null }
      firstOrigin = if ($firstNonNull) { $firstNonNull.InventTransOrigin } else { $null }
      qtyRowDatePhysical = if ($withQty) { $withQty.DatePhysical } else { $null }
      qtyRowStatusReceipt = if ($withQty) { $withQty.StatusReceipt } else { $null }
      qtyRowStatusIssue = if ($withQty) { $withQty.StatusIssue } else { $null }
      qtyRowQty = if ($withQty) { $withQty.Qty } else { $null }
      sampleRows = @($sorted | Select-Object -First 5 | ForEach-Object {
        [pscustomobject]@{
          DatePhysical=$_.DatePhysical; DateFinancial=$_.DateFinancial; DateInvent=$_.DateInvent
          StatusIssue=$_.StatusIssue; StatusReceipt=$_.StatusReceipt; Qty=$_.Qty
          Voucher=$_.Voucher; InventTransOrigin=$_.InventTransOrigin; PackingSlipId=$_.PackingSlipId
        }
      })
    }
  }

  $sum = [pscustomobject]@{
    suffix = $tag
    batchNumber = $lote
    itemNumber = $item
    batchDispositionCode = $disp
    batchExpirationDate = [string]$batch.BatchExpirationDate
    inventLocations = $locs
    operationalStatus = $op.status
    operationalRule = $op.rule
    operationalWarehouse = $op.wh
    qualityOrderCount = $orders.Count
    qualityOrders = @($orders | ForEach-Object {
      [pscustomobject]@{
        QualityOrderNumber = $_.QualityOrderNumber
        ItemBatchNumber = $_.ItemBatchNumber
        ItemNumber = $_.ItemNumber
        QualityOrderStatus = $_.QualityOrderStatus
        ValidationStatus = $null  # campo NO publicado en QualityOrderHeaders
        InventoryBatchId = $null  # campo NO publicado
        ValidatedDateTime = $_.ValidatedDateTime
        ValidatingPersonnelNumber = $_.ValidatingPersonnelNumber
        QMSAssignedToPersonnelNumber = $_.QMSAssignedToPersonnelNumber
        PassedBatchDispositionCode = $_.PassedBatchDispositionCode
        FailedBatchDispositionCode = $_.FailedBatchDispositionCode
        WarehouseId = $_.WarehouseId
        WarehouseLocationId = $_.WarehouseLocationId
      }
    })
    inventDims = @($dims | ForEach-Object {
      [pscustomobject]@{ inventDimId=$_.inventDimId; InventLocationId=$_.InventLocationId; wMSLocationId=$_.wMSLocationId; InventSiteId=$_.InventSiteId }
    })
    remResCuarentenaMovements = $movementCandidates
  }
  Save-Json "$tag-00-summary.json" $sum
  $summaries += $sum
}

# Workers
Write-Host "`n=== BaseWorkers ==="
$workers = @()
foreach ($pn in ($personnel | Sort-Object)) {
  Write-Host "  PN=$pn"
  $wR = OData "BaseWorkers" "PersonnelNumber eq '$pn'" $null 5
  Save-Json "worker-$pn.json" $(if($wR.Ok){$wR.Body}else{$wR.Error})
  if ($wR.Ok -and $wR.Body.value -and $wR.Body.value.Count -gt 0) {
    $row = $wR.Body.value[0]
    $workers += [pscustomobject]@{
      queryPersonnelNumber=$pn; found=$true; entity="BaseWorkers"
      PersonnelNumber=$row.PersonnelNumber; Name=$row.Name; FirstName=$row.FirstName; LastName=$row.LastName
      MiddleName=$row.MiddleName; PartyNumber=$row.PartyNumber
    }
  } else {
    $workers += [pscustomobject]@{
      queryPersonnelNumber=$pn; found=$false; entity="BaseWorkers"
      PersonnelNumber=$null; Name=$null; FirstName=$null; LastName=$null; MiddleName=$null; PartyNumber=$null
    }
  }
}

Save-Json "00-lot-summaries.json" $summaries
Save-Json "00-workers.json" $workers

$fieldAvail = @{
  QualityOrderNumber = "SI"
  InventoryBatchId = "NO"
  ValidationStatus = "NO"
  QualityOrderStatus = "SI"
  ValidatedDateTime = "SI"
  ValidatingPersonnelNumber = "SI"
  PassedBatchDispositionCode = "SI"
  FailedBatchDispositionCode = "SI"
  BaseWorkers_PersonnelNumber = "SI"
  BaseWorkers_Name = "SI"
  BaseWorkers_FirstName = "SI"
  BaseWorkers_LastName = "SI"
  InventTrans_DatePhysical = "SI"
  InventTrans_DateFinancial = "SI"
  InventTrans_DateInvent = "SI"
  InventTrans_DateStatus = "SI"
  InventTrans_DateClosed = "SI"
  InventTrans_ModifiedDateTime = "NO"
  InventTrans_CreatedDateTime = "NO"
  InventTrans_ModifiedBy = "NO"
  InventTrans_CreatedBy = "NO"
  InventTrans_Worker = "NO"
  InventTrans_PersonnelNumber = "NO"
  InventTrans_UserId = "NO"
  InventTrans_InventLocationId = "NO"
  InventTrans_InventSiteId = "NO"
  InventTrans_inventDimId = "SI"
  InventTrans_ReferenceId = "NO"
  InventTrans_ReferenceCategory = "NO"
  InventTrans_ReferenceType = "NO"
  InventTrans_InventTransType = "NO"
  InventTrans_StatusIssue = "SI"
  InventTrans_StatusReceipt = "SI"
  InventTrans_InventTransOrigin = "SI"
  InventTrans_Voucher = "SI"
  generatedAt = (Get-Date).ToString("o")
}
Save-Json "00-field-availability.json" $fieldAvail
Write-Host ("DONE lots=" + $summaries.Count + " workers=" + $workers.Count)
