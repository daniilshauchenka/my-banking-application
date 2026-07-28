$ErrorActionPreference = "Stop"

$services = @(
    "api-gateway"
    "account-service"
    "transfer-service"
    "cash-service"
    "notification-service"
    "front-ui"
)

foreach ($service in $services) {
    if ([string]::IsNullOrWhiteSpace($service)) {
        throw "Service name is empty"
    }

    $context = Join-Path $PSScriptRoot "..\$service"
    Write-Host "Building my-bank/${service}:latest from $context"
    docker build -t "my-bank/${service}:latest" $context
}
