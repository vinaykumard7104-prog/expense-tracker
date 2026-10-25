![CI](https://github.com/vinaykumard7104-prog/expense-tracker/actions/workflows/ci.yml/badge.svg)
# ledgerly. - Expense Tracker

Full-stack personal finance app: Spring Boot 4 REST API (JWT, MySQL, Flyway) + a vanilla JS dashboard served from the same app.

# ledgerly. – Expense Tracker

A full-stack personal finance app. Users register, log in, track income and expenses, set category budgets, and get alerts when a budget is nearly used up.

**Tech:** Java 21 · Spring Boot 4 · Spring Security + JWT · Spring Data JPA · MySQL · Flyway · Vanilla JS

## Features
- Secure sign-up and login with JWT and BCrypt-hashed passwords
- Every user sees only their own data
- Budget alerts at 80% and 100% of a category limit
- Monthly analytics: balance, spending by category, 6-month trend
- Versioned database schema with Flyway migrations

## Screenshots
![Login](docs/login.png)
![Dashboard](docs/dashboard.png)
![Budgets](docs/budgets.png)
![Database](docs/database.png)

## Run locally
1. Start MySQL; the database is created automatically.
2. Set the env var `DB_PASSWORD` (and optionally `JWT_SECRET`, 32+ chars).
3. `./mvnw spring-boot:run`  then open http://localhost:8080 and create an account.

## API (all `/api/v1/**` need `Authorization: Bearer <token>`)
| Method | Path | Purpose |
|---|---|---|
| POST | /api/auth/register, /api/auth/login | Get a JWT |
| GET | /api/v1/categories | List categories |
| GET/POST/DELETE | /api/v1/transactions | Paged list, create (returns budget alert), delete |
| GET/PUT | /api/v1/budgets | Budgets with spent and percent; upsert a limit |
| GET | /api/v1/reports/summary, /by-category, /trend | Analytics |

## Design notes
- Stateless JWT auth, BCrypt password hashing, every query scoped to the logged-in user
- Flyway migrations; Hibernate runs in `validate` mode
- `BigDecimal`/`DECIMAL(12,2)` for money, DTO records, global exception handler
- `@EntityGraph` to avoid N+1 queries, composite index on (user_id, txn_date)
