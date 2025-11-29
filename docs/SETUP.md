## Environment Setup

### 1. Prerequisites

- Docker 24+
- Docker Compose v2+
- Node.js 20+, pnpm 9+ (for BFF and Angular UI development)
- Java 21 (Temurin recommended) and Maven 3.9+

### 2. Keycloak & Database

`docker-compose.yml` provisions:

- `postgres-keycloak`: Stores Keycloak data.
- `keycloak`: Imports `infra/keycloak/realm-export.json` on the first run.

Create a Keycloak admin user:

```bash
docker compose exec keycloak /opt/keycloak/bin/kc.sh add-user \
  --realm master --user admin --password admin
```

Import the realm:

```bash
docker compose exec keycloak /opt/keycloak/bin/kc.sh import --file /opt/keycloak/data/import/realm-export.json
```

The realm defines:

- `bff-client` (public PKCE client)
- `gateway-client` (confidential service client)
- Roles: `ROLE_USER`, `ROLE_ADMIN`

### 3. Backend Microservices

`backend/api-gateway` and `backend/user-service` are standalone Spring Boot projects.

Build locally:

```bash
mvn -pl backend/api-gateway -am clean package
mvn -pl backend/user-service -am clean package
```

Each module exposes health checks at `/actuator/health`.

### 4. BFF (Node/TypeScript)

```
cd services/bff
pnpm install
pnpm dev
```

Environment variables (`.env`):

```
PORT=8081
KEYCLOAK_BASE_URL=http://localhost:8080/realms/poc
KEYCLOAK_CLIENT_ID=bff-client
GATEWAY_URL=http://localhost:8082
SESSION_SECRET=change-me
```

### 5. Angular UI

```
cd frontend/auth-ui
pnpm install
pnpm start
```

Environment file (`src/environments/environment.ts`):

```ts
export const environment = {
  production: false,
  bffBaseUrl: 'http://localhost:8081'
};
```

### 6. Docker Compose

```
docker compose up --build
```

Services:

- `keycloak`: http://localhost:8080
- `bff`: http://localhost:8081
- `api-gateway`: http://localhost:8082
- `user-service`: http://localhost:8083
- `auth-ui`: http://localhost:4200

### 7. RBAC Testing

1. Create users and assign roles via Keycloak admin.
2. Log in through the Angular UI.
3. UI calls BFF → BFF obtains access token → Gateway forwards to microservices.
4. Check logs (`docker compose logs`) to verify role enforcement.

