$ErrorActionPreference = "Stop"
$repoRoot = (Get-Item $PSScriptRoot).Parent.FullName
Set-Location (Join-Path $repoRoot "olnatura-qr\api\olnaturaqr")
& .\gradlew.bat bootRun @args
