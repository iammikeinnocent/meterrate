package zw.meterrate.rating;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zw.meterrate.domain.Invoice;
import zw.meterrate.domain.Meter;
import zw.meterrate.domain.Reading;
import zw.meterrate.domain.Tariff;
import zw.meterrate.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
public class BillingService {

    private final MeterRepository meterRepository;
    private final ReadingRepository readingRepository;
    private final TariffRepository tariffRepository;
    private final InvoiceRepository invoiceRepository;
    private final RatingEngine ratingEngine;

    public BillingService(MeterRepository meterRepository,
                          ReadingRepository readingRepository,
                          TariffRepository tariffRepository,
                          InvoiceRepository invoiceRepository,
                          RatingEngine ratingEngine) {
        this.meterRepository = meterRepository;
        this.readingRepository = readingRepository;
        this.tariffRepository = tariffRepository;
        this.invoiceRepository = invoiceRepository;
        this.ratingEngine = ratingEngine;
    }

    /**
     * Recomputes every invoice for the period. Re-running a period REPLACES
     * its invoices inside one transaction — billing is idempotent by
     * construction, so retried runs never double-charge.
     */
    @Transactional
    public BillingRun run(String period) {
        var yearMonth = YearMonth.parse(period);
        var from = yearMonth.atDay(1);
        var to = yearMonth.atEndOfMonth();

        var tariff = tariffRepository
            .findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(from)
            .orElseThrow(() -> new IllegalStateException("no tariff effective on or before " + from));

        invoiceRepository.deleteByPeriod(period);

        var invoices = new ArrayList<Invoice>();
        var skipped = new ArrayList<SkippedMeter>();
        for (Meter meter : meterRepository.findAll()) {
            var readings = readingRepository.findByMeterIdAndReadOnBetweenOrderByReadOnAsc(meter.getId(), from, to);
            if (readings.isEmpty()) {
                skipped.add(new SkippedMeter(meter.getSerial(), "no readings in period"));
                continue;
            }
            var consumption = readings.stream()
                .map(Reading::getKwh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            var amount = ratingEngine.charge(consumption, tariff);

            var invoice = new Invoice();
            invoice.setMeterId(meter.getId());
            invoice.setPeriod(period);
            invoice.setKwh(consumption);
            invoice.setAmount(amount);
            invoices.add(invoiceRepository.save(invoice));
        }
        return new BillingRun(period, tariff.getId(), invoices, skipped);
    }

    public record BillingRun(String period, Long tariffId, List<Invoice> invoices, List<SkippedMeter> skipped) {}
    public record SkippedMeter(String serial, String reason) {}

    /** Meter consumption for a period — used by the reporting endpoint. */
    public BigDecimal consumptionForPeriod(Long meterId, String period) {
        var yearMonth = YearMonth.parse(period);
        var from = LocalDate.from(yearMonth.atDay(1));
        var to = LocalDate.from(yearMonth.atEndOfMonth());
        var readings = readingRepository.findByMeterIdAndReadOnBetweenOrderByReadOnAsc(meterId, from, to);
        return readings.stream().map(Reading::getKwh).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}