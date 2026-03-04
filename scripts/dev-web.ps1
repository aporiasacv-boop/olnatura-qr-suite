$ErrorActionPreference = "Stop"
$repoRoot = (Get-Item $PSScriptRoot).Parent.FullName
Set-Location (Join-Path $repoRoot "olnatura-qr\web\qr-enterprise-frontend")
npm run dev @args
