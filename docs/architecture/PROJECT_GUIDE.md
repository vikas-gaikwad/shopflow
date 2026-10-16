# ShopFlow Project Guide

## 1. Purpose

ShopFlow is a production-style e-commerce learning project. It is being built backend-first with Java 25, Spring Boot 4.1.1, PostgreSQL and Flyway. The current backend is a modular monolith; service extraction is a future architecture decision, not a current deployment topology.

## 2. Current modules

- `product`: product lifecycle, search/filter/pagination, stock adjustment and category association.
- `category`: category lifecycle and protection against deleting categories used by products.
- `order`: order creation, item snapshots, inventory reservation and cancellation.
- `common`: shared exception handling and pagination response DTO.
- `ShopflowApiApplication`: Spring Boot application entry point.

Typical request path:

```text
HTTP request
  -> REST Controller
  -> request DTO validation
  -> Service (business rules / transaction boundary)
  -> Spring Data JPA Repository
  -> PostgreSQL
  -> Response DTO
  -> HTTP response
```

Controllers expose DTOs, not persistence entities. Service classes implement business behavior. Flyway migrations are the source of schema evolution; Hibernate is configured to validate rather than create/update the schema automatically.

## 3. Order placement flow

```mermaid
flowchart TD
    A[POST /api/v1/orders] --> B[OrderController]
    B --> C[Validate request DTO]
    C --> D[Begin service transaction]
    D --> E[Reject duplicate product IDs]
    E --> F[Lock products in sorted ID order]
    F --> G{Products exist and are active?}
    G -- No --> X[Throw exception and roll back]
    G -- Yes --> H{Enough stock for every line?}
    H -- No --> X
    H -- Yes --> I[Decrease inventory]
    I --> J[Create order and item price/name/SKU snapshots]
    J --> K[Persist order]
    K --> L[Commit transaction]
    L --> M[Return 201 Created]
```

The service validates all requested stock quantities before applying any inventory changes. Product row locks protect the inventory decision from concurrent order attempts. Database writes and stock changes share a transaction.

## 4. Order cancellation flow

```mermaid
flowchart TD
    A[PATCH /api/v1/orders/id/cancel] --> B[Lock order row]
    B --> C{Order exists?}
    C -- No --> D[404 Not Found]
    C -- Yes --> E{Status is PLACED?}
    E -- No --> F[409 Conflict]
    E -- Yes --> G[Lock associated products in sorted ID order]
    G --> H[Restore each item quantity]
    H --> I[Set status CANCELLED]
    I --> J[Commit transaction]
    J --> K[Return updated order]
```

A cancelled order retains its history. Repeated cancellation is rejected, preventing the same order from restoring inventory twice.

## 5. Product/category relationship

```mermaid
erDiagram
    CATEGORY ||--o{ PRODUCT : categorizes
    PRODUCT ||--o{ ORDER_ITEM : appears_in
    CUSTOMER_ORDER ||--|{ ORDER_ITEM : contains

    CATEGORY {
        bigint id PK
        varchar name
        varchar description
    }
    PRODUCT {
        bigint id PK
        varchar sku UK
        varchar name
        numeric price
        int stock_quantity
        boolean active
        bigint category_id FK
    }
    CUSTOMER_ORDER {
        bigint id PK
        varchar customer_name
        varchar customer_email
        varchar status
        numeric total_amount
    }
    ORDER_ITEM {
        bigint id PK
        bigint order_id FK
        bigint product_id FK
        varchar product_sku
        varchar product_name
        int quantity
        numeric unit_price
        numeric line_total
    }
```

`orders` is the physical table name; the JPA entity is `CustomerOrder`. The order item stores purchase-time product details as snapshots while retaining a foreign key to the product.

## 6. Important implementation concepts

- DTO validation uses Jakarta Bean Validation.
- `@Transactional` keeps order persistence and inventory changes atomic.
- `PESSIMISTIC_WRITE` locks rows that are being modified by order operations.
- `BigDecimal` is used for monetary calculations.
- `Instant` stores timestamps.
- `OrderStatus` is persisted as a string enum.
- Flyway owns database schema migrations.
- Global exception handling maps known failures to HTTP error responses.

## 7. Current boundaries and planned capabilities

Implemented: product/category/order REST APIs, PostgreSQL persistence, Flyway, validation, basic exception handling, stock locking, and compile CI.

Planned: automated test phase, security/authentication/authorization, messaging, resilience, caching, observability, containerization, deployment automation, and Angular frontend. Do not describe planned items as available until implemented and verified.
