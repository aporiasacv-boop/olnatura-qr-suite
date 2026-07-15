param(
    [string]$BaseUrl = "https://olnatura-produccion.operations.dynamics.com",
    [string]$TenantId = $env:APP_DYNAMICS_TENANT_ID,
    [string]$ClientId = $env:APP_DYNAMICS_CLIENT_ID,
    [string]$ClientSecret = $env:APP_DYNAMICS_CLIENT_SECRET,
    [int]$RefreshMinutes = 0,
    [string]$CacheFile = "",
    [switch]$ShowToken
)

$ErrorActionPreference = "Stop"

if (-not $TenantId) { $TenantId = [Environment]::GetEnvironmentVariable("APP_DYNAMICS_TENANT_ID", "User") }
if (-not $ClientId) { $ClientId = [Environment]::GetEnvironmentVariable("APP_DYNAMICS_CLIENT_ID", "User") }
if (-not $ClientSecret) { $ClientSecret = [Environment]::GetEnvironmentVariable("APP_DYNAMICS_CLIENT_SECRET", "User") }

if (-not $TenantId -or -not $ClientId -or -not $ClientSecret) {
    Write-Error "Define APP_DYNAMICS_TENANT_ID, APP_DYNAMICS_CLIENT_ID y APP_DYNAMICS_CLIENT_SECRET"
    exit 1
}

if (-not $CacheFile) {
    $CacheFile = Join-Path $PSScriptRoot ".dynamics-token-cache.json"
}

function Request-DynamicsToken {
    $scope = "$BaseUrl/.default"
    $tokenUrl = "https://login.microsoftonline.com/$TenantId/oauth2/v2.0/token"
    $body = @{
        grant_type    = "client_credentials"
        client_id     = $ClientId
        client_secret = $ClientSecret
        scope         = $scope
    }
    $resp = Invoke-RestMethod -Method Post -Uri $tokenUrl -Body $body -ContentType "application/x-www-form-urlencoded"
    $expiresIn = [int]$resp.expires_in
    if ($expiresIn -le 0) { $expiresIn = 3599 }
    $fetchedAt = [DateTimeOffset]::UtcNow
    $expiresAt = $fetchedAt.AddSeconds($expiresIn)
    $cache = @{
        fetchedAtUtc = $fetchedAt.ToString("o")
        expiresAtUtc = $expiresAt.ToString("o")
        expiresInSec   = $expiresIn
        tokenType      = $resp.token_type
        accessToken    = $resp.access_token
        resource       = $BaseUrl
    }
    $cache | ConvertTo-Json -Depth 3 | Set-Content -Path $CacheFile -Encoding UTF8
    Write-Host "Token renovado. Valido hasta $expiresAt UTC (expires_in=${expiresIn}s)"
    Write-Host "Cache: $CacheFile"
    if ($ShowToken) {
        Write-Host "access_token=$($resp.access_token)"
    }
    return $cache
}

if ($RefreshMinutes -le 0) {
    Request-DynamicsToken | Out-Null
    exit 0
}

Write-Host "Renovacion cada $RefreshMinutes minutos. Ctrl+C para detener."
while ($true) {
    try {
        Request-DynamicsToken | Out-Null
    } catch {
        Write-Host "ERROR renovacion: $($_.Exception.Message)"
    }
    Start-Sleep -Seconds ($RefreshMinutes * 60)
}
