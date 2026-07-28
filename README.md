# My Bank Services

## Structure

- `api-gateway` - Spring Cloud Gateway, port `6080`.
- `account-service` - account API, port `6081`, own PostgreSQL database.
- `transfer-service` - transfer API, port `6082`, own PostgreSQL database.
- `cash-service` - cash deposit and withdraw API, port `6083`, own PostgreSQL database.
- `notification-service` - notification API, port `6084`, own PostgreSQL database, scheduled notification processing.
- `front-ui` - Thymeleaf UI, port `6085`.
- `keycloak` - OAuth 2.0 authorization server, port `6060`.

## Databases

Each business service has its own PostgreSQL database:

- `account-service` uses `account-postgres`, database `account_service`, port `6432`.
- `transfer-service` uses `transfer-postgres`, database `transfer_service`, port `6433`.
- `cash-service` uses `cash-postgres`, database `cash_service`, port `6434`.
- `notification-service` uses `notification-postgres`, database `notification_service`, port `6435`.

Schema migrations are managed by Liquibase:

- `account-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- `account-service/src/main/resources/db/changelog/schema`
- `account-service/src/main/resources/db/changelog/mock`
- `transfer-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- `transfer-service/src/main/resources/db/changelog/schema`
- `transfer-service/src/main/resources/db/changelog/mock`
- `cash-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- `cash-service/src/main/resources/db/changelog/schema`
- `cash-service/src/main/resources/db/changelog/mock`
- `notification-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- `notification-service/src/main/resources/db/changelog/schema`
- `notification-service/src/main/resources/db/changelog/mock`

## Run

```powershell
docker compose up -d
```

UI:

```text
http://localhost:6085
```

Keycloak admin console:

```text
http://localhost:6060
```

Admin credentials: `admin / admin`.

Demo users:

- `user1 / password`
- `user2 / password`
- `user3 / password`


## Checks

```powershell
docker compose config
docker compose build
```

Run module tests:

```powershell
.\mvnw.cmd test
```

## Kubernetes

Raw Kubernetes manifests are stored in `k8s/`. The Kubernetes deployment uses Kubernetes `Service` DNS for service discovery and `ConfigMap`/`Secret` for runtime configuration.


Apply raw manifests:

```powershell
kubectl apply -k k8s
kubectl -n my-bank get pods
```

## Helm

The Helm chart is stored in `helm/my-bank`. It is an umbrella chart with subcharts for shared configuration, PostgreSQL databases, application services, Keycloak, Front UI and Ingress.

Validate and install:

```powershell
helm lint .\helm\my-bank
helm template my-bank .\helm\my-bank
helm upgrade --install my-bank .\helm\my-bank --namespace my-bank --create-namespace
```

Run Helm chart tests after deployment:

```powershell
& $helm test my-bank --namespace my-bank
```

```text
http://my-bank.local
```