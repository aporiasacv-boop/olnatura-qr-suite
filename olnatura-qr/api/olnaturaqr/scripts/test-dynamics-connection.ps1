param(
    [string]$BaseUrl = "https://olnatura-produccion.operations.dynamics.com",
    [string]$TenantId = $env:APP_DYNAMICS_TENANT_ID,
    [string]$ClientId = $env:APP_DYNAMICS_CLIENT_ID,
    [string]$ClientSecret = $env:APP_DYNAMICS_CLIENT_SECRET,
    [string]$Lote = "",
    [string]$ItemNumber = ""
)

if (-not $TenantId -or -not $ClientId -or -not $ClientSecret) {
    Write-Error "Define APP_DYNAMICS_TENANT_ID, APP_DYNAMICS_CLIENT_ID y APP_DYNAMICS_CLIENT_SECRET"
    exit 1
}

$scope = "$BaseUrl/.default"
$tokenUrl = "https://login.microsoftonline.com/$TenantId/oauth2/v2.0/token"

Write-Host "1) Solicitando token OAuth..."
$body = @{
    grant_type    = "client_credentials"
    client_id     = $ClientId
    client_secret = $ClientSecret
    scope         = $scope
}
$tokenResp = Invoke-RestMethod -Method Post -Uri $tokenUrl -Body $body -ContentType "application/x-www-form-urlencoded"
Write-Host "   OK token_type=$($tokenResp.token_type) expires_in=$($tokenResp.expires_in)s"

$headers = @{ Authorization = "Bearer $($tokenResp.access_token)" }

function Test-Entity($name, $filter) {
    $url = "$BaseUrl/data/$name?`$top=1"
    if ($filter) {
        $url += "&`$filter=" + [uri]::EscapeDataString($filter)
    }
    Write-Host "2) $name"
    Write-Host "   GET $url"
    try {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        $r = Invoke-RestMethod -Method Get -Uri $url -Headers $headers
        $sw.Stop()
        $count = if ($r.value) { $r.value.Count } else { 0 }
        Write-Host "   OK rows=$count elapsedMs=$($sw.ElapsedMilliseconds)"
        if ($count -gt 0) { $r.value[0] | ConvertTo-Json -Compress }
    } catch {
        Write-Host "   ERROR $($_.Exception.Message)"
    }
}

$item = $ItemNumber.Trim()
$batch = $Lote.Trim()
if (-not $item -and $batch) {
    Test-Entity "ItemBatchAttributeValuesV2" "ItemBatchNumber eq '$($batch -replace "'","''")'"
}
if ($item) {
    $esc = $item -replace "'","''"
    Test-Entity "ReleasedProductMasters" "ItemNumber eq '$esc'"
    Test-Entity "WarehousesOnHandV2" "ItemNumber eq '$esc'"
}
if ($batch) {
    $esc = $batch -replace "'","''"
    Test-Entity "ItemBatchAttributeValuesV2" "ItemBatchNumber eq '$esc'"
}
Test-Entity "QualityOrderLineResults" $null

Write-Host "3) Renovando token (segunda peticion)..."
$tokenResp2 = Invoke-RestMethod -Method Post -Uri $tokenUrl -Body $body -ContentType "application/x-www-form-urlencoded"
Write-Host "   OK expires_in=$($tokenResp2.expires_in)s (nuevo access_token generado por Azure)"
