# Tasks — PostgreSQL Database Integration

## Checklist

| #  | Task                                              | Status |
|----|---------------------------------------------------|--------|
| 1  | Create `docker-compose.yml` at project root       | ✅     |
| 2  | Update `application.properties` port 5432 → 5441  | ✅     |
| 3  | Start Postgres container (`docker compose up -d`)  | 🔲     |
| 4  | Run Spring Boot app (`./mvnw spring-boot:run`)     | 🔲     |
| 5  | Verify all 8 tables are created in `orlandodb`     | 🔲     |
| 6  | Smoke-test an endpoint (e.g. POST /api/v1/auth/register) | 🔲 |

---

## Task Details

### Task 1 — Create `docker-compose.yml`
- Add `docker-compose.yml` to project root
- Use `postgres:16-alpine` image
- Map port `5441:5432`
- Set env: `POSTGRES_DB=orlandodb`, `POSTGRES_USER=postgres`, `POSTGRES_PASSWORD=postgres`
- Named volume `orlando_pgdata` for data persistence
- Add health check with `pg_isready`

### Task 2 — Update `application.properties`
- Change `spring.datasource.url` from port `5432` to `5440`
- No other property changes required

### Task 3 — Start Postgres Container
```bash
docker compose up -d
```
Verify with:
```bash
docker compose ps
docker compose logs orlando-postgres
```

### Task 4 — Run Spring Boot Application
```bash
./mvnw spring-boot:run
```
Watch logs for Hibernate `create table` / `alter table` statements.

### Task 5 — Verify Tables
```bash
docker exec -it orlando-postgres psql -U postgres -d orlandodb -c "\dt"
```
Expected tables: `staff`, `products`, `customers`, `addresses`, `shopping_cart`, `cart_item`, `orders`, `order_item`.

### Task 6 — Smoke Test
```bash
# Register a customer
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john@test.com","password":"P@ssw0rd1"}'

# Check the customer was persisted
docker exec -it orlando-postgres psql -U postgres -d orlandodb -c "SELECT * FROM customers;"
```
