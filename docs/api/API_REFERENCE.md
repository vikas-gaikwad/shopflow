# ShopFlow API Reference

Base URL for local development: `http://localhost:8080`

All examples assume the API is running and the database is populated as needed. JSON examples are illustrative; use IDs that exist in your database.

## Common behavior

- Product and category creation return `201 Created`.
- Product and category deletion return `204 No Content`.
- Order creation returns `201 Created`.
- Validation errors are handled by `GlobalExceptionHandler`; the current response includes a timestamp, HTTP status, error, message, and field errors.
- Not-found errors return `404 Not Found`.
- Business conflicts and database constraint conflicts generally return `409 Conflict`.
- The API currently has no authentication layer.

## Products

### Create product

`POST /api/v1/products`

Request fields:

| Field | Rules |
|---|---|
| `sku` | Required, non-blank, max 50 chars; letters, digits, `_` and `-` only |
| `name` | Required, non-blank, max 150 chars |
| `description` | Optional, max 1000 chars |
| `price` | Required and greater than zero |
| `stockQuantity` | Required, zero or greater |
| `categoryId` | Optional; if supplied, must be at least 1 and refer to an existing category |

Example:

```json
{
  "sku": "LAPTOP-003",
  "name": "Engineering Laptop",
  "description": "Laptop for development",
  "price": 68000.00,
  "stockQuantity": 12,
  "categoryId": 1
}
```

Returns a `ProductResponse`.

### List/search/filter/sort products

`GET /api/v1/products`

Query parameters:

| Parameter | Default | Purpose |
|---|---|---|
| `search` | none | Search term |
| `active` | none | Filter by active flag |
| `categoryId` | none | Filter by category |
| `page` | `0` | Zero-based page index |
| `size` | `10` | Page size |
| `sortBy` | `id` | Sort field |
| `sortDir` | `asc` | Sort direction |

The service validates pagination and supported sort fields/directions. See `ProductService` for the current allowlist and exact search matching behavior.

Example:

`GET /api/v1/products?search=laptop&active=true&page=0&size=10&sortBy=name&sortDir=asc`

Response shape: `ProductPageResponse` with `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, and `last`.

### Get product

`GET /api/v1/products/{id}`

Returns a `ProductResponse`, or `404` if no product exists.

### Update product

`PUT /api/v1/products/{id}`

Request fields: `name` (required, max 150), `description` (optional, max 1000), `price` (required, greater than zero), `stockQuantity` (required, zero or greater), `active` (boolean), and optional `categoryId` (if supplied, at least 1).

Example:

```json
{
  "name": "Engineering Laptop",
  "description": "Updated description",
  "price": 69000.00,
  "stockQuantity": 12,
  "active": true,
  "categoryId": 1
}
```

Returns the updated `ProductResponse`.

### Delete product

`DELETE /api/v1/products/{id}`

Returns `204 No Content` on success. A product referenced by order history cannot be deleted because `order_items.product_id` has an `ON DELETE RESTRICT` foreign key. Database integrity conflicts return `409`.

### Adjust stock

`PATCH /api/v1/products/{id}/stock`

Request:

```json
{ "adjustment": -2 }
```

`adjustment` is required. Positive values increase stock; negative values decrease stock. A zero adjustment is rejected by the service. Stock cannot become negative. Returns the updated `ProductResponse`.

## Categories

### Create category

`POST /api/v1/categories`

Request fields: `name` (required, non-blank, max 100) and optional `description` (max 500).

```json
{
  "name": "Accessories",
  "description": "Computer accessories"
}
```

Returns `201 Created` and a `CategoryResponse`. Category names are unique case-insensitively.

### List categories

`GET /api/v1/categories`

Returns a JSON array of `CategoryResponse` objects.

### Get category

`GET /api/v1/categories/{id}`

Returns one category or `404` if it does not exist.

### Update category

`PUT /api/v1/categories/{id}`

Uses the same fields and validation as category creation. Returns the updated category.

### Delete category

`DELETE /api/v1/categories/{id}`

Returns `204 No Content` on success. A category assigned to products cannot be deleted; this business conflict returns `409`.

## Orders

### Create order

`POST /api/v1/orders`

Returns `201 Created`.

Request:

```json
{
  "customerName": "Demo Customer",
  "customerEmail": "demo@example.com",
  "items": [
    { "productId": 3, "quantity": 2 },
    { "productId": 4, "quantity": 1 }
  ]
}
```

Validation:
- `customerName` is required, non-blank, max 150 characters.
- `customerEmail` is optional; if provided, it must be a valid email and no longer than 254 characters.
- `items` must not be empty.
- Each item requires a positive `productId` and positive `quantity`.
- A product may appear only once in an order.
- All products must exist and be active.
- Stock must be sufficient for every requested line.
- Inventory is reserved in the same transaction as order persistence.
- Product rows are locked in sorted ID order to reduce deadlock risk.

Possible errors include `400` for invalid requests, `404` for an unknown product, and `409` for inactive products or insufficient stock.

Successful response shape:

```json
{
  "id": 2,
  "customerName": "Demo Customer",
  "customerEmail": "demo@example.com",
  "status": "PLACED",
  "totalAmount": 219000.00,
  "items": [
    {
      "productId": 3,
      "productSku": "LAPTOP-002",
      "productName": "Dell Laptop Pro",
      "quantity": 2,
      "unitPrice": 72000.00,
      "lineTotal": 144000.00
    },
    {
      "productId": 4,
      "productSku": "LAPTOP-001",
      "productName": "Business Laptop",
      "quantity": 1,
      "unitPrice": 75000.00,
      "lineTotal": 75000.00
    }
  ],
  "createdAt": "2026-10-16T10:56:39Z",
  "updatedAt": "2026-10-16T10:56:39Z"
}
```

The timestamps and IDs above are examples. Each order item stores SKU, product name, unit price and line total as purchase-time snapshots.

### Get order

`GET /api/v1/orders/{id}`

Returns the order with its item details, or `404` if the order does not exist.

### List orders

`GET /api/v1/orders?page=0&size=10`

- `page` is zero-based and defaults to `0`.
- `size` defaults to `10` and must be between `1` and `100`.
- Orders are sorted by creation time descending.
- Returns the shared `PageResponse<OrderResponse>` structure: `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, and `last`.

### Cancel order

`PATCH /api/v1/orders/{id}/cancel`

A placed order can be cancelled. The service locks the order and related product rows, restores the quantities, and changes the status to `CANCELLED` within a transaction.

- Unknown order: `404`.
- Already-cancelled order: `409`.
- Stock restoration failures cause the transaction to fail rather than intentionally completing a partial cancellation.

Cancellation does not delete the order or its item history.

## Error response format

The global handler produces a common error structure for handled exceptions:

```json
{
  "timestamp": "2026-10-16T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Example conflict message"
}
```

Validation failures additionally include `fieldErrors`, mapping field names to validation messages. Exact messages depend on the exception and validation rule.


## Customers

### Create customer
`POST /api/v1/customers`

Request:
```json
{
  "name": "Demo Customer",
  "email": "demo@example.com",
  "phone": "9876543210"
}
```

Returns `201 Created` with a `CustomerResponse`.

Validation:
- Name is required and limited to 150 characters.
- Email is required, must be valid, and is limited to 254 characters.
- Phone is optional and limited to 20 characters.
- Email addresses are normalized to lowercase.
- Duplicate email addresses return `409 Conflict`.

### Get customer
`GET /api/v1/customers/{id}`

Returns a customer or `404 Not Found`.

### List customers
`GET /api/v1/customers?page=0&size=10`

- Page is zero-based and defaults to 0.
- Size defaults to 10 and must be between 1 and 100.
- Results are sorted by customer ID ascending.
- Returns the shared `PageResponse<CustomerResponse>` structure.

### Update customer
`PUT /api/v1/customers/{id}`

Request:
```json
{
  "name": "Updated Customer",
  "email": "updated@example.com",
  "phone": "9876543210"
}
```

Returns the updated customer.
An email already used by another customer returns `409 Conflict`.
An unknown customer returns `404 Not Found`.

Security note: Authentication and authorization are planned for a later milestone. These endpoints must not be considered production-ready for access to personal data until appropriate access controls are implemented.

