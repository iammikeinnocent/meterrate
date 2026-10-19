package zw.meterrate.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zw.meterrate.domain.Invoice;
import zw.meterrate.repository.InvoiceRepository;

import java.util.List;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceRepository invoiceRepository;

    public InvoiceController(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam(required = false) String period) {
        if (period != null && !period.matches("\\d{4}-\\d{2}")) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", "period must be YYYY-MM"));
        }
        List<Invoice> invoices = period == null
            ? invoiceRepository.findAll()
            : invoiceRepository.findByPeriodOrderByMeterId(period);
        return ResponseEntity.ok(invoices);
    }
}
