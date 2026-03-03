# Single command to run project 
`docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build` run from root directory of project.

# Read below for further information
# Capitec – Transactions Dispute Portal

# Functional Requirements
Full‑stack system (frontend + backend) that allows customers to:
- View their transactions
- Dispute a transaction
- View the historic timeline of a dispute

# Tech Notes:
- **Java 25**, **Spring Boot 4 (WebFlux)**, **R2DBC + Postgres** (fully reactive)
- **JWT** auth (roles: `USER`, `SUPPORT`)
- **Argon2 password hashing** (one‑way + no plaintext storage)
- **Docker Compose** for a one‑command local demo which seeds some test data
- **React + Vite** for the UI
- **Redis** for cache services
- **Postres** Non-ransient data store. Schemes managed via Flyway
- **Modular Monolith** Main modules are transactions, disputes and security
- **Architest** used to ensure modularisation of project.
- **Sonarqube** used for static code analysis
- **OWASP** used for security analysis of dependencies.
- **API Documentation** via spring docs openia/swagger

## Quickstart (Dev / Demo)
This starts Postgres, the backend API, Redis and the frontend UI.
```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build
```

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Api docs: `http://localhost:8080/swagger-ui/index.html`

### Demo users (dev profile seed)

| username | password | roles |
|---|---|---|
| user1 | user1-pass | USER |
| user2 | user2-pass | USER |
| support1 | support1-pass | SUPPORT |

> Seed data lives in `backend/src/main/resources/application-dev.yaml`.

## Example API usage (curl)

Login:

```bash
curl -sS -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user1","password":"user1-pass"}'
```

List transactions (use the `accessToken` from login):
```bash
curl -sS http://localhost:8080/v1/transactions \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

Create a dispute:
```bash
curl -sS -X POST http://localhost:8080/v1/disputes \
  -H "Authorization: Bearer <ACCESS_TOKEN>" \
  -H 'Content-Type: application/json' \
  -d '{"transactionId":"10000000-0000-0000-0000-000000000001","reason":"CARD_NOT_PRESENT"}'
```

Get dispute history:
```bash
curl -sS http://localhost:8080/v1/disputes/<DISPUTE_ID>/history \
  -H "Authorization: Bearer <ACCESS_TOKEN>"
```

## Build & Test
### Backend tests:
```bash
cd backend
./gradlew test
```

### SonarQube
  - To perform a scan ensure you have sonarqube installed and have a project setup. To run a scan use the following command, ensure to update host.url and token
  ```bash
  cd backend
  ./gradlew clean test jacocoTestReport sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=sqp_f7c63032fd3957346567e684d71789011a95be1d
  ```    

### OWASP
  - Used to scan for vulnerabilities in dependencies 
  - The suppression file is here `capitec-transaction-dispute-root/backend/config/dependency-check/dependency-check-suppressions.xml`.
      - Add justified suppressions in this file.
  ```bash
  cd backend
  ./gradlew clean dependencyCheckAnalyze
  ```
  - Reports(html+json) will be produced in: capitec-transaction-dispute-root/backend/build/reports/dependency-check

  ### Documentation
  - capitec-transaction-dispute/documents contains use case, system overview and sequence diagrams.


# Other useful Docker commands
 - ```docker compose down -v```
 - ```docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d --build```
 - ```docker compose logs -f backend```
