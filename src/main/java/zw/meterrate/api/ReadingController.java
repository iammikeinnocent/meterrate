package zw.meterrate.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zw.meterrate.domain.Meter;
import zw.meterrate.domain.Reading;
import zw.meterrate.repository.MeterRepository;
import zw.meterrate.repository.ReadingRepository;
import zw.meterrate.web.IdempotencyService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/readings")
public class ReadingController {

    private final MeterRepository meterRepository;
    private final ReadingRepository readingRepository;
    private final IdempotencyService idempotency;

    public ReadingController(MeterRepository meterRepository,
                             ReadingRepository readingRepository,
                             IdempotencyService idempotency) {
        this.meterRepository = meterRepository;
        this.readingRepository = readingRepository;
        this.idempotency = idempotency;
    }

    public record CreateReadingRequest(
        @NotNull String meterSerial,
        @NotNull LocalDate readOn,
        @NotNull @DecimalMin("0.0") BigDecimal kwh
    ) {}

    @PostMapping
    public ResponseEntity<String> create(
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Valid @RequestBody CreateReadingRequest request
    ) {
        var body = """
            {"meterSerial":"%s","readOn":"%s","kwh":%s}""".formatted(
            request.meterSerial(), request.readOn(), request.kwh());

        var replay = idempotency.replay(idempotencyKey, body);
        if (replay.isPresent()) {
            var outcome = replay.get();
            return ResponseEntity.status(outcome.status())
                .header("Idempotency-Replayed", "true")
                .body(outcome.body());
        }

        var meter = meterRepository.findBySerial(request.meterSerial()).orElse(null);
        if (meter == null) {
            return ResponseEntity.status(400).body("{\"error\":\"unknown_meter\"}");
        }
        if (readingRepository.existsByMeterIdAndReadOn(meter.getId(), request.readOn())) {
            return ResponseEntity.status(409).body("{\"error\":\"duplicate_reading\"}");
        }

        var reading = new Reading();
        reading.setMeterId(meter.getId());
        reading.setReadOn(request.readOn());
        reading.setKwh(request.kwh());
        readingRepository.save(reading);

        var response = "{\"meterSerial\":\"%s\",\"readOn\":\"%s\",\"kwh\":%s}".formatted(
            request.meterSerial(), request.readOn(), request.kwh());
        idempotency.store(idempotencyKey, body, 201, response);
        return ResponseEntity.status(201).body(response);
    }
}
