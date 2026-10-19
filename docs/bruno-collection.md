# Bruno API collection

`bruno/meterrate` is a runnable API collection covering the core flows:
seed reference data, submit an idempotent reading, run a billing period and
fetch invoices.

```bash
npm install -g @usebruno/cli
docker compose up -d postgres
mvn -DskipTests package && java -jar target/meterrate-0.1.0.jar &
cd bruno && bru run meterrate --env local
```
