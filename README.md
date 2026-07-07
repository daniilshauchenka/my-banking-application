# My Bank Services

## Structure

- `config-repo` - external configuration files.
- `config-server` - Spring Cloud Config Server, port `6088`.
- `discovery-server` - Eureka Server, port `6071`.
- `api-gateway` - Spring Cloud Gateway, port `6080`.
- `account-service` - account API, port `6081`, own PostgreSQL database.
- `transfer-service` - transfer API, port `6082`, own PostgreSQL database.
- `front-ui` - Thymeleaf UI, port `6085`.
- `keycloak` - OAuth 2.0 authorization server, port `6060`.

## Databases

Each business service has its own PostgreSQL database:

- `account-service` uses `account-postgres`, database `account_service`, port `6432`.
- `transfer-service` uses `transfer-postgres`, database `transfer_service`, port `6433`.

Schema migrations are managed by Liquibase:

- `account-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- `account-service/src/main/resources/db/changelog/schema`
- `account-service/src/main/resources/db/changelog/mock`
- `transfer-service/src/main/resources/db/changelog/db.changelog-master.yaml`
- `transfer-service/src/main/resources/db/changelog/schema`
- `transfer-service/src/main/resources/db/changelog/mock`

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

OAuth clients are imported from `keycloak/realm-export.json`:

- `front-ui` uses Authorization Code Flow with PKCE.
- `transfer-service` uses Client Credentials Flow with secret `transfer-secret`.

## Checks

```powershell
docker compose config
docker compose build
```

Run module tests:

```powershell
.\mvnw.cmd test
```

from each service directory.
