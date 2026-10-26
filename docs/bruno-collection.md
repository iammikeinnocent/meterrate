# Bruno API collection

`bruno/meterrate` is a runnable API collection covering the core flows:
create an account, meter and tariff, submit a reading with an idempotency
key, replay the same reading, run a billing period, re-run it and fetch
the resulting invoices.

```bash
npm install -g @usebruno/cli
docker compose down -v && docker compose up -d postgres
mvn -DskipTests package && java -jar target/meterrate-0.1.0.jar &
cd bruno/meterrate && bru run . --env local
```

The collection expects a clean database, so reset the Postgres volume
(`docker compose down -v`) before running so meter serials and idempotency
keys don't collide with leftover data.
