# Milestone: Order Management

## Scope

This milestone adds multi-item order creation, order retrieval, paginated listing and cancellation to the existing Product and Category modules.

## Endpoints

- `POST /api/v1/orders`
- `GET /api/v1/orders/{id}`
- `GET /api/v1/orders?page=0&size=10`
- `PATCH /api/v1/orders/{id}/cancel`

See [API Reference](../api/API_REFERENCE.md) for request/response contracts.

## Business rules

- An order requires a non-blank customer name and at least one item.
- Product IDs and quantities must be positive.
- Duplicate product IDs within the same order are rejected.
- Products must exist, be active, and have sufficient stock.
- Product rows are locked in sorted ID order.
- Inventory reservation and order persistence occur in one transaction.
- Order items snapshot product SKU, name, unit price, quantity and line total.
- Only `PLACED` orders can be cancelled.
- Cancellation restores inventory and changes the status to `CANCELLED`.
- A repeated cancellation returns `409 Conflict`.
- A product referenced by an order item cannot be deleted due to the database foreign key.

## Manual verification performed

- Order created with two products and total amount `219000.00`.
- Order retrieval returned item details and price snapshots.
- Cancellation returned status `CANCELLED` and advanced `updatedAt`.
- Repeated cancellation returned `409 Conflict`.
- After cancelling order #2, stock was 8 for product ID 3 and 9 for product ID 4 because order #1 remained `PLACED`.
- An order requesting 999 units of product ID 4 returned `409 Conflict`; stock remained 8 and 9.

These are manual checks in the development database, not automated test coverage.

## Implementation concepts

- Spring transaction boundaries and rollback
- Pessimistic row locking
- DTO validation
- JPA entity relationships and cascade behavior
- Price/name/SKU snapshots for historical order details
- Flyway migration V3
- Reusable generic pagination response for orders

## Follow-up considerations

- Add automated unit and integration tests in the planned testing phase.
- Consider inventory movement/audit records.
- Consider idempotency keys for retry-safe order creation.
- Add security before exposing customer orders beyond local development.
- Review order listing query behavior and performance with realistic data volumes.
