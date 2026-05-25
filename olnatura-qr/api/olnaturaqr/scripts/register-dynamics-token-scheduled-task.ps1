param(
    [int]$IntervalMinutes = 65,
    [string]$TaskName = "OlnaturaQR-DynamicsTokenRefresh"
)

$scriptPath = Join-Path $PSScriptRoot "refresh-dynamics-token.ps1"
if (-not (Test-Path $scriptPath)) {
    Write-Error "No existe $scriptPath"
    exit 1
}

$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$scriptPath`""
$trigger = New-ScheduledTaskTrigger -Once -At (Get-Date).AddMinutes(1) -RepetitionInterval (New-TimeSpan -Minutes $IntervalMinutes) -RepetitionDuration ([TimeSpan]::MaxValue)
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable

Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Settings $settings -Force | Out-Null

Write-Host "Tarea registrada: $TaskName"
Write-Host "Intervalo: cada $IntervalMinutes minutos"
Write-Host "Script: $scriptPath"
Write-Host ""
Write-Host "Requisito: variables de usuario APP_DYNAMICS_TENANT_ID, APP_DYNAMICS_CLIENT_ID, APP_DYNAMICS_CLIENT_SECRET"
Write-Host "Ver tarea: Get-ScheduledTask -TaskName $TaskName"
Write-Host "Quitar:   Unregister-ScheduledTask -TaskName $TaskName -Confirm:`$false"
