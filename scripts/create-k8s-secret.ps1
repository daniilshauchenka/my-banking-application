param(
    [string]$EnvFile = ".env",
    [string]$Namespace = "my-bank",
    [string]$SecretName = "my-bank-app-secrets"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $EnvFile)) {
    throw "Env file '$EnvFile' was not found. Create it from .env.example first."
}

kubectl create namespace $Namespace --dry-run=client -o yaml | kubectl apply -f -
kubectl create secret generic $SecretName `
    --namespace $Namespace `
    --from-env-file $EnvFile `
    --dry-run=client `
    -o yaml | kubectl apply -f -
