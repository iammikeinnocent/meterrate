package zw.meterrate.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zw.meterrate.domain.Reading;
import zw.meterrate.repository.MeterRepository;
import zw.meterrate.repository.ReadingRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Batch CSV reading import with row-level validation: valid rows are
 * imported, invalid rows are reported individually, so one bad line
 * never fails the whole batch.
 *
 * Expected row format: meterSerial,YYYY-MM-DD,kwh
 */
@RestController
@RequestMapping("/readings/csv")
public class CsvImportController {

    private static final Pattern DATE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final MeterRepository meterRepository;
    private final ReadingRepository readingRepository;

    public CsvImportController(MeterRepository meterRepository, ReadingRepository readingRepository) {
        this.meterRepository = meterRepository;
        this.readingRepository = readingRepository;
    }

    public record CsvImportResult(int imported, List<RowError> errors) {}
    public record RowError(int row, String message) {}

    @PostMapping
    public ResponseEntity<CsvImportResult> importCsv(@RequestBody String csv) {
        var imported = 0;
        var errors = new ArrayList<RowError>();
        var lines = csv.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {
            var rowNumber = i + 1;
            var line = lines[i].trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            var parts = line.split(",");
            if (parts.length != 3) {
                errors.add(new RowError(rowNumber, "expected 3 comma-separated fields: meterSerial,YYYY-MM-DD,kwh"));
                continue;
            }
            var serial = parts[0].trim();
            var dayText = parts[1].trim();
            var kwhText = parts[2].trim();

            var meter = meterRepository.findBySerial(serial).orElse(null);
            if (meter == null) {
                errors.add(new RowError(rowNumber, "unknown meter serial \"" + serial + "\""));
                continue;
            }
            if (!DATE_PATTERN.matcher(dayText).matches()) {
                errors.add(new RowError(rowNumber, "date \"" + dayText + "\" must be YYYY-MM-DD"));
                continue;
            }
            LocalDate day;
            try {
                day = LocalDate.parse(dayText);
            } catch (DateTimeParseException e) {
                errors.add(new RowError(rowNumber, "invalid date \"" + dayText + "\""));
                continue;
            }
            BigDecimal kwh;
            try {
                kwh = new BigDecimal(kwhText);
                if (kwh.signum() < 0) {
                    errors.add(new RowError(rowNumber, "kwh must be non-negative"));
                    continue;
                }
            } catch (NumberFormatException e) {
                errors.add(new RowError(rowNumber, "invalid kwh \"" + kwhText + "\""));
                continue;
            }
            if (readingRepository.existsByMeterIdAndReadOn(meter.getId(), day)) {
                errors.add(new RowError(rowNumber, "reading already exists for " + serial + " on " + day));
                continue;
            }

            var reading = new Reading();
            reading.setMeterId(meter.getId());
            reading.setReadOn(day);
            reading.setKwh(kwh);
            readingRepository.save(reading);
            imported += 1;
        }

        return ResponseEntity.ok(new CsvImportResult(imported, errors));
    }
}
