# Design — PostgreSQL Database Integration

## Architecture

```
┌──────────────────────────────────┐
│        Spring Boot App           │
│   (host — localhost:8080)        │
│                                  │
│  ┌────────────┐ ┌─────────────┐  │
│  │  JPA/Hib.  │→│ JDBC Driver │──┼──→ localhost:5441
│  └────────────┘ └─────────────┘  │
└──────────────────────────────────┘
                                       │
                                       ▼
                          ┌────────────────────────┐
                          │  Docker: PostgreSQL 16  │
                          │  Container port: 5432   │
                          │  Host port: 5441        │
                          │  DB: orlandodb          │
                          │  Volume: orlando_pgdata │
                          └────────────────────────┘
```

## Components

### 1. Docker Compose (`docker-compose.yml`)
- **Image:** `postgres:16-alpine` (lightweight)
- **Port mapping:** `5441:5432`
- **Environment:**
  - `POSTGRES_DB=orlandodb`
  - `POSTGRES_USER=postgres`
  - `POSTGRES_PASSWORD=postgres`
- **Volume:** Named volume `orlando_pgdata` mounted at `/var/lib/postgresql/data`
- **Health check:** `pg_isready` to ensure DB readiness before the app connects

### 2. Application Properties Update
- Change `spring.datasource.url` from `jdbc:postgresql://localhost:5432/orlandodb`
  to `jdbc:postgresql://localhost:5441/orlandodb`
- All other JPA/Hibernate settings remain unchanged

### 3. Existing Entity → Table Mapping

The project already has full JPA annotations on all entities:

| Entity        | `@Table`          | Soft Delete           | Relationships              |
|---------------|-------------------|-----------------------|----------------------------|
| Staff         | `staff`           | `@SQLDelete` / filter | —                          |
| Product       | `products`        | `@SQLDelete` / filter | —                          |
| Customer      | `customers`       | `@SQLDelete` / filter | `@OneToMany → Address`     |
| Address       | `addresses`       | No                    | `@ManyToOne → Customer`    |
| ShoppingCart  | `shopping_cart`   | No                    | `@OneToMany → CartItem`    |
| CartItem      | `cart_item`       | No                    | `@ManyToOne → ShoppingCart`|
| Order         | `orders`          | `@SQLDelete` / filter | `@OneToMany → OrderItem`   |
| OrderItem     | `order_item`      | No                    | `@ManyToOne → Order`       |

> No code changes to entities or repositories are needed — they are already fully annotated.

### 4. Data Flow

1. Developer runs `docker compose up -d` → Postgres container starts on port 5441
2. Developer runs `./mvnw spring-boot:run` → Spring Boot connects to `localhost:5440`
3. Hibernate `ddl-auto=update` creates/updates all 8 tables automatically
4. REST API endpoints work with real persistence
