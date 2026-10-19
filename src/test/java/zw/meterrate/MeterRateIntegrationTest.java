package zw.meterrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MeterRateIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    private String base() {
        return "http://localhost:" + port;
    }

    @BeforeEach
    void seed() {
        // tariff effective 2026-01-01: 6 @ 0.42, 6-20 @ 0.75, 20+ @ 1.10
        rest.postForEntity(base() + "/tariffs", Map.of(
            "name", "Residential 2026",
            "effectiveFrom", "2026-01-01",
            "tiers", java.util.List.of(
                Map.of("upToKwh", 6, "ratePerKwh", 0.42, "sequence", 1),
                Map.of("upToKwh", 20, "ratePerKwh", 0.75, "sequence", 2),
                Map.of("upToKwh", null, "ratePerKwh", 1.10, "sequence", 3)
            )), Map.class);
        var account = rest.postForEntity(base() + "/accounts", Map.of("name", "Moyo household", "address", "12 Josphat Rd"), Map.class);
        Long accountId = ((Number) account.getBody().get("id")).longValue();
        rest.postForEntity(base() + "/meters", Map.of("serial", "WM-001", "accountId", accountId), Map.class);
        rest.postForEntity(base() + "/meters", Map.of("serial", "WM-002", "accountId", accountId), Map.class);
    }

    @Test
    void endToEndBillingRunProducesTieredInvoicesAndIsIdempotent() {
        // two readings for WM-001 (8 + 4 = 12 kWh -> 2.52 + 6*0.75 = 7.02)
        assertEquals(201, postReading("WM-001", "2026-03-05", 8).getStatusCode().value());
        assertEquals(201, postReading("WM-001", "2026-03-20", 4).getStatusCode().value());
        // one reading for WM-002 (3 kWh -> 1.26)
        assertEquals(201, postReading("WM-002", "2026-03-10", 3).getStatusCode().value());

        var run = rest.postForEntity(base() + "/billing/run", Map.of("period", "2026-03"), Map.class);
        assertEquals(201, run.getStatusCode().value());

        var invoices = rest.getForEntity(base() + "/invoices?period=2026-03", Map.class).getBody();
        assertNotNull(invoices);
        assertTrue(invoices.toString().contains("7.02"), "WM-001 invoice amount: " + invoices);
        assertTrue(invoices.toString().contains("1.26"), "WM-002 invoice amount: " + invoices);

        // re-running the period replaces invoices — never double-charges
        rest.postForEntity(base() + "/billing/run", Map.of("period", "2026-03"), Map.class);
        var after = rest.getForEntity(base() + "/invoices?period=2026-03", Map.class).getBody();
        assertEquals(invoices.toString(), after.toString());
    }

    @Test
    void idempotencyKeyReplaysReturnTheSameResponseWithoutDuplicateRows() {
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("Idempotency-Key", "reading-2026-04-01-WM-001");
        var request = new org.springframework.http.HttpEntity<>(Map.of(
            "meterSerial", "WM-001", "readOn", "2026-04-01", "kwh", 2), headers);

        var first = rest.postForEntity(base() + "/readings", request, String.class);
        assertEquals(201, first.getStatusCode().value());
        assertFalse(first.getHeaders().getFirst("Idempotency-Replayed") != null);

        var replay = rest.postForEntity(base() + "/readings", request, String.class);
        assertEquals(201, replay.getStatusCode().value());
        assertEquals("true", replay.getHeaders().getFirst("Idempotency-Replayed"));
        assertEquals(first.getBody(), replay.getBody());
    }

    @Test
    void csvImportReportsRowLevelErrorsAndImportsValidRows() {
        var csv = String.join("\n",
            "# comment line",
            "WM-001,2026-05-01,5",
            "WM-XXX,2026-05-02,5",
            "WM-001,2026/05/03,5",
            "WM-002,2026-05-04,-1",
            "WM-002,2026-05-05,7");
        var result = rest.postForEntity(base() + "/readings/csv", csv, Map.class);
        assertEquals(200, result.getStatusCode().value());
        var body = result.getBody();
        assertEquals(2, ((Number) body.get("imported")).intValue());
        assertTrue(body.toString().contains("unknown meter serial"));
        assertTrue(body.toString().contains("must be YYYY-MM-DD"));
    }

    private org.springframework.http.ResponseEntity<String> postReading(String serial, String day, double kwh) {
        return rest.postForEntity(base() + "/readings",
            Map.of("meterSerial", serial, "readOn", day, "kwh", kwh), String.class);
    }
}
