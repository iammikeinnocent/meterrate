package zw.meterrate.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import zw.meterrate.domain.*;
import zw.meterrate.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;

/** Accounts, meters and tiered tariffs — the rating engine's reference data. */
@RestController
public class CatalogController {

    private final AccountRepository accountRepository;
    private final MeterRepository meterRepository;
    private final TariffRepository tariffRepository;

    public CatalogController(AccountRepository accountRepository,
                             MeterRepository meterRepository,
                             TariffRepository tariffRepository) {
        this.accountRepository = accountRepository;
        this.meterRepository = meterRepository;
        this.tariffRepository = tariffRepository;
    }

    public record CreateAccountRequest(@NotBlank String name, String address) {}

    @PostMapping("/accounts")
    public ResponseEntity<Account> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        var account = new Account();
        account.setName(request.name());
        account.setAddress(request.address());
        return ResponseEntity.status(201).body(accountRepository.save(account));
    }

    @GetMapping("/accounts")
    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }

    public record CreateMeterRequest(@NotBlank String serial, @NotNull Long accountId) {}

    @PostMapping("/meters")
    public ResponseEntity<?> createMeter(@Valid @RequestBody CreateMeterRequest request) {
        if (!accountRepository.existsById(request.accountId())) {
            return ResponseEntity.status(400).body(Map.of("error", "unknown_account"));
        }
        if (meterRepository.findBySerial(request.serial()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "duplicate_serial"));
        }
        var meter = new Meter();
        meter.setSerial(request.serial());
        meter.setAccountId(request.accountId());
        return ResponseEntity.status(201).body(meterRepository.save(meter));
    }

    @GetMapping("/meters")
    public List<Meter> listMeters() {
        return meterRepository.findAll();
    }

    public record TierRequest(BigDecimal upToKwh, @NotNull @DecimalMin("0.0001") BigDecimal ratePerKwh) {}
    public record CreateTariffRequest(
        @NotBlank String name,
        @NotNull LocalDate effectiveFrom,
        @NotEmpty List<@Valid TierRequest> tiers
    ) {}

    @PostMapping("/tariffs")
    public ResponseEntity<Tariff> createTariff(@Valid @RequestBody CreateTariffRequest request) {
        var tariff = new Tariff();
        tariff.setName(request.name());
        tariff.setEffectiveFrom(request.effectiveFrom());
        var tiers = request.tiers().stream().map(tier -> {
            var entity = new TariffTier();
            entity.setUpToKwh(tier.upToKwh());
            entity.setRatePerKwh(tier.ratePerKwh());
            return entity;
        }).toList();
        tariff.setTiers(new java.util.ArrayList<>(tiers));
        // enforce: exactly one top band (null upTo), bands ascending
        var topBands = tariff.orderedTiers().stream().filter(t -> t.getUpToKwh() == null).count();
        if (topBands != 1) {
            return ResponseEntity.badRequest().build();
        }
        var saved = tariffRepository.save(tariff);
        return ResponseEntity.status(201).body(saved);
    }

    @GetMapping("/tariffs")
    public List<Tariff> listTariffs() {
        return tariffRepository.findAll();
    }
}
