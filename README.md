# MeterRate

A rating engine for household water meters: submit meter readings, price
consumption against tiered tariffs, and bill a month into invoices. Java 24,
Spring Boot 3.5, PostgreSQL with Flyway migrations, Testcontainers
integration tests and a Bruno API collection.

## How it works

- Consumption is charged band-by-band against effective-dated tariffs,
  rounded HALF_UP to cents. With the seed tariff, the first 6 kWh cost 0.42
  per unit, the next 14 cost 0.75, and anything above 20 costs 1.10.
- `POST /readings` accepts an `Idempotency-Key` header: replaying the same
  key with the same body returns the stored response and an
  `Idempotency-Replayed: true` header; reusing a key with a different body
  is a `409`.
- `POST /readings/csv` imports a batch with row-level validation. Valid rows
  import; each bad row reports its own reason (unknown serial, bad date
  format, duplicate, negative kwh).
- `POST /billing/run` recomputes all invoices for a period in one
  transaction. Re-running a period replaces its invoices, so retried runs
  never double-charge.
- `GET /invoices?period=YYYY-MM` lists the invoices for a period.

## API surface

| Method   | Path                | Notes                                 |
| -------- | ------------------- | ------------------------------------- |
| POST/GET | `/accounts`         | household accounts                    |
| POST/GET | `/meters`           | unique serials, linked to accounts    |
| POST/GET | `/tariffs`          | tiered blocks, effective-dated        |
| POST     | `/readings`         | single reading, idempotency-key aware |
| POST     | `/readings/csv`     | batch import, row-level errors        |
| POST     | `/billing/run`      | recompute invoices for a period       |
| GET      | `/invoices?period=` | invoice listing                       |

OpenAPI UI at `/swagger-ui.html` when the app runs.

## Running

Requires Java 24, Maven and Docker.

```bash
docker compose up -d postgres
mvn spring-boot:run          # Flyway migrates on boot
```

The app listens on port 8080.

## Testing

```bash
mvn test                     # unit + Testcontainers PostgreSQL integration
```

The integration suite boots the app against a containerized Postgres: it
seeds reference data, bills a period, asserts the exact tiered invoice
amounts, re-runs the period to prove no double-charging, and verifies
idempotent reading submission end to end.

The Bruno collection in `bruno/meterrate` is an executable API walkthrough,
one request per step from creating an account to fetching invoices; see
`docs/bruno-collection.md`.
