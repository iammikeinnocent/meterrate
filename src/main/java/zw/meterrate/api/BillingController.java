package zw.meterrate.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zw.meterrate.rating.BillingService;

@RestController
public class BillingController {

    private final BillingService billingService;

    public BillingController(BillingService billingService) {
        this.billingService = billingService;
    }

    public record RunBillingRequest(@NotBlank @Pattern(regexp = "\\d{4}-\\d{2}") String period) {}

    @PostMapping("/billing/run")
    public ResponseEntity<?> run(@Valid @RequestBody RunBillingRequest request) {
        try {
            var run = billingService.run(request.period());
            return ResponseEntity.status(201).body(run);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(400).body(java.util.Map.of("error", e.getMessage()));
        }
    }
}
