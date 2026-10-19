package zw.meterrate.rating;

import org.springframework.stereotype.Component;
import zw.meterrate.domain.Tariff;
import zw.meterrate.domain.TariffTier;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tiered rating: consumption is charged band-by-band against the tariff's
 * ordered tiers. A band with a null upper bound is the top band. Rounding is
 * HALF_UP to cents — the same convention the invoice table stores.
 */
@Component
public class RatingEngine {

    public BigDecimal charge(BigDecimal consumptionKwh, Tariff tariff) {
        var tiers = tariff.orderedTiers();
        if (tiers.isEmpty()) {
            throw new IllegalStateException("tariff " + tariff.getName() + " has no tiers");
        }

        var remaining = consumptionKwh;
        var previousBound = BigDecimal.ZERO;
        var charge = BigDecimal.ZERO;

        for (TariffTier tier : tiers) {
            var upper = tier.getUpToKwh() == null
                ? BigDecimal.valueOf(Double.MAX_VALUE)
                : tier.getUpToKwh();
            var span = remaining.min(upper.subtract(previousBound));
            if (span.signum() > 0) {
                charge = charge.add(span.multiply(tier.getRatePerKwh()));
                remaining = remaining.subtract(span);
            }
            previousBound = upper;
            if (remaining.signum() <= 0) {
                break;
            }
        }
        return charge.setScale(2, RoundingMode.HALF_UP);
    }
}
