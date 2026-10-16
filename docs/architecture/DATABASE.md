# ShopFlow Database Guide

Database: PostgreSQL. Schema changes are applied by Flyway from `backend/shopflow-api/src/main/resources/db/migration`. Hibernate uses schema validation; migrations should be added rather than editing previously applied migrations.

## Tables

### `products` — V1

| Column | Meaning |
|---|---|
| `id` | Identity primary key |
| `sku` | Required unique SKU, max 50 |
| `name` | Required name, max 150 |
| `description` | Optional description, max 1000 |
| `price` | `NUMERIC(12,2)`, greater than zero |
| `stock_quantity` | Integer, zero or greater |
| `active` | Boolean |
| `created_at`, `updated_at` | Time-zone-aware timestamps |

Indexes: `idx_products_name`, `idx_products_active`. The SKU is unique.

### `categories` — V2

| Column | Meaning |
|---|---|
| `id` | Identity primary key |
| `name` | Required category name, max 100 |
| `description` | Optional description, max 500 |
| `created_at`, `updated_at` | Time-zone-aware timestamps |

`ux_categories_name_lower` enforces case-insensitive uniqueness for category names.

### Product/category relationship — V2

`products.category_id` is nullable so pre-existing products remain valid. It references `categories.id` with `ON DELETE RESTRICT`. `idx_products_category_id` supports category lookups.

### `orders` — V3

| Column | Meaning |
|---|---|
| `id` | Identity primary key |
| `customer_name` | Required customer name, max 150 |
| `customer_email` | Optional email, max 254 |
| `status` | `PLACED` or `CANCELLED` |
| `total_amount` | `NUMERIC(14,2)`, zero or greater |
| `created_at`, `updated_at` | Time-zone-aware timestamps |

Constraints: `chk_orders_status`, `chk_orders_total_amount`. Indexes: `idx_orders_created_at`, `idx_orders_status`.

### `order_items` — V3

| Column | Meaning |
|---|---|
| `id` | Identity primary key |
| `order_id` | Required FK to `orders.id` |
| `product_id` | Required FK to `products.id` |
| `product_sku`, `product_name` | Purchase-time snapshots |
| `quantity` | Positive integer |
| `unit_price` | `NUMERIC(12,2)`, positive |
| `line_total` | `NUMERIC(14,2)`, positive |

Constraints: order FK uses `ON DELETE CASCADE`; product FK uses `ON DELETE RESTRICT`; `(order_id, product_id)` is unique; quantity, unit price and line total must be positive. Indexes: `idx_order_items_order_id`, `idx_order_items_product_id`.

The product foreign key prevents deleting a product that appears in order history. Order item snapshots preserve the purchase-time name, SKU and price even if the current product information changes.

## Migration history

| Version | File | Purpose |
|---|---|---|
| V1 | `V1__create_products_table.sql` | Create products table and indexes |
| V2 | `V2__create_categories_and_link_products.sql` | Create categories, add nullable product category FK and index |
| V3 | `V3__create_orders_and_order_items.sql` | Create orders/order_items, constraints and indexes |

Never modify an already-applied migration in a shared database. Add a new versioned migration for subsequent schema changes.

## Transactional inventory behavior

Order creation locks product rows, validates all requested quantities, decreases stock, and persists the order in a single transaction. Cancellation locks the order and its associated products, restores quantities, and changes the order status. A failure should roll back the transaction.

The database currently stores a current stock quantity, not a separate inventory movement ledger. A future enhancement could add auditable stock movements.
