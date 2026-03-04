$ErrorActionPreference = "Stop"
$repoRoot = (Get-Item $PSScriptRoot).Parent.FullName
Set-Location $repoRoot
docker compose -f olnatura-qr/infra/docker/docker-compose.demo.yml up --build @args
