# ShopFlow Roadmap

Status is recorded as of the current Product, Category and Order Management implementation.

## Completed

- [x] Repository and Spring Boot backend foundation
- [x] Product CRUD
- [x] Product search, filters, pagination and sorting
- [x] Product stock adjustment
- [x] Category CRUD and product-category relationship
- [x] Flyway migrations V1–V3
- [x] Order creation with multiple items
- [x] Transactional inventory reservation
- [x] Purchase-time order-item snapshots
- [x] Order retrieval and pagination
- [x] Order cancellation and stock restoration
- [x] Manual verification of order creation, cancellation, repeated cancellation conflict and insufficient-stock rejection
- [x] Maven compile CI using Java 25

## In progress / immediate

- [ ] Complete and review Order Management documentation
- [ ] Review staged code and documentation diff
- [ ] Commit, push and merge Order Management feature

## Planned

- [ ] Complete remaining backend features
- [ ] Automated testing phase (JUnit 5 / Mockito and integration testing)
- [ ] Authentication and authorization
- [ ] Messaging/event-driven integration (Kafka)
- [ ] Resilience patterns and external API integration
- [ ] Caching
- [ ] Observability, metrics, logs and tracing
- [ ] Containers and deployment pipeline
- [ ] Angular frontend
- [ ] Final end-to-end verification and production-readiness review

Items in the planned section are not yet implemented unless moved to Completed with evidence.
- [x] Customer profile CRUD endpoints (create, retrieve, list and update)
- [x] Flyway V4 customer table and case-insensitive email uniqueness
- [x] Manual verification of customer API validation and duplicate-email handling
