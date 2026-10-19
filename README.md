# MeterRate

A water metering and billing rating engine — Java 24, Spring Boot 3.5,
PostgreSQL with Flyway migrations, Testcontainers integration tests and a
Bruno API collection.

## What it does

- **Tiered rating engine** — consumption is charged band-by-band against
  effective-dated tariffs (HALF_UP to cents); unit-tested at band
  boundaries.
- **Idempotent ingestion** — `POST /readings` with an `Idempotency-Key`:
  replays return the stored response (`Idempotency-Replayed: true`), a
  reused key with a different body is a `409`.
- **Batch CSV import with row-level validation** — valid rows import, each
  bad row reports its own reason (unknown serial, bad date format,
  duplicate, negative kwh).
- **Idempotent billing runs** — re-running a period replaces its invoices
  inside one transaction; retried runs never double-charge.
- **Invoices by period** — `GET /invoices?period=YYYY-MM`.

## API surface

| Method | Path | Notes |
|---|------|-------|
| POST/GET | `/accounts` | household accounts |
| POST/GET | `/meters` | unique serials, linked to accounts |
| POST/GET | `/tariffs` | tiered blocks, effective-dated |
| POST | `/readings` | single reading, idempotency-key aware |
| POST | `/readings/csv` | batch import, row-level errors |
| POST | `/billing/run` | recompute invoices for a period |
| GET | `/invoices?period=` | invoice listing |

OpenAPI UI at `/swagger-ui.html` when the app runs.

## Running

```bash
docker compose up -d postgres
mvn spring-boot:run          # Flyway migrates on boot
```

## Testing

```bash
mvn test                     # unit + Testcontainers PostgreSQL integration
```

The integration suite boots the real app against a containerized Postgres:
seeds reference data, bills a period, asserts tiered invoice amounts,
re-runs the period to prove no double-charging, and verifies idempotent
reading submission end-to-end.

The Bruno collection (`bruno/`) is an executable API walkthrough — see
`docs/bruno-collection.md`.

## License

MIT
