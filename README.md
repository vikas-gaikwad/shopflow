# ShopFlow

A production-style e-commerce and order-management learning project built incrementally with a backend-first approach.

## Current implementation

- Product CRUD, search, filtering, pagination and sorting
- Product stock adjustment
- Category CRUD and product-category association
- Order creation with multiple items
- Transactional inventory reservation and order cancellation
- Purchase-time SKU, product-name and unit-price snapshots
- PostgreSQL persistence and Flyway schema migrations
- Maven compile CI using Java 25

## Technology baseline

- Java 25
- Spring Boot 4.1.1
- Maven
- Spring Data JPA / Hibernate
- PostgreSQL
- Flyway
- Jakarta Bean Validation
- Git and GitHub Actions

## Repository layout

```text
backend/shopflow-api/    Spring Boot API
docs/api/                API contracts and examples
docs/architecture/       Architecture and database documentation
docs/decisions/          Architecture decision records
docs/runbooks/           Operational and Git procedures
docs/versions/           Feature milestone notes
infra/                   Infrastructure configuration as it is introduced
frontend/                Angular frontend planned for a later phase
```

## Run locally

From `backend/shopflow-api`:

```powershell
$env:SHOPFLOW_DB_PASSWORD = Read-Host "Enter PostgreSQL password"
.\mvnw.cmd spring-boot:run
```

The default datasource URL is `jdbc:postgresql://localhost:5432/shopflow`, username `postgres`, and port `8080`. Environment variables can override the URL and username:

- `SHOPFLOW_DB_URL`
- `SHOPFLOW_DB_USERNAME`
- `SHOPFLOW_DB_PASSWORD`

Ensure PostgreSQL is running and the `shopflow` database exists before starting the application. Flyway applies migrations from `src/main/resources/db/migration`.

Compile:

```powershell
.\mvnw.cmd -DskipTests compile
```

Automated tests are intentionally scheduled for the later testing phase; manual verification is being used during current feature development.

## Documentation index

- [API Reference](docs/api/API_REFERENCE.md)
- [Project Guide](docs/architecture/PROJECT_GUIDE.md)
- [Database Guide](docs/architecture/DATABASE.md)
- [Git Workflow](docs/runbooks/GIT_WORKFLOW.md)
- [Roadmap](docs/ROADMAP.md)
- [Order Management Milestone](docs/versions/V1-ORDER-MANAGEMENT.md)

## Architecture direction

The current backend is a modular monolith. The intended evolution is to establish clear module boundaries before extracting services. Security, messaging, resilience, caching, observability, containers, deployment automation, automated testing, and the Angular frontend are planned phases, not all current capabilities.
