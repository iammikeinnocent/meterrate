package zw.meterrate;

import org.junit.jupiter.api.Test;
import zw.meterrate.domain.Tariff;
import zw.meterrate.domain.TariffTier;
import zw.meterrate.rating.RatingEngine;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RatingEngineTest {

    private final RatingEngine engine = new RatingEngine();

    private Tariff tariff() {
        var tariff = new Tariff();
        tariff.setName("Residential 2026");
        var tiers = new ArrayList<TariffTier>();
        tiers.add(tier(0, "6.00", "0.4200", 1));   // first 6 kWh @ 0.42
        tiers.add(tier(6, "20.00", "0.7500", 2));  // 6..20 @ 0.75
        tiers.add(tier(20, null, "1.1000", 3));    // above 20 @ 1.10
        tariff.setTiers(tiers);
        return tariff;
    }

    private TariffTier tier(int seq, String upTo, String rate, int sequence) {
        var t = new TariffTier();
        t.setSequence(sequence);
        t.setUpToKwh(upTo == null ? null : new BigDecimal(upTo));
        t.setRatePerKwh(new BigDecimal(rate));
        return t;
    }

    @Test
    void consumptionInsideTheFirstBandChargesAtTheFirstRate() {
        assertEquals(0, new BigDecimal("2.52").compareTo(engine.charge(new BigDecimal("6"), tariff())));
    }

    @Test
    void consumptionCrossingBandsIsChargedMarginal() {
        // 10 kWh: 6 @ 0.42 = 2.52 + 4 @ 0.75 = 3.00 -> 5.52
        assertEquals(0, new BigDecimal("5.52").compareTo(engine.charge(new BigDecimal("10"), tariff())));
    }

    @Test
    void consumptionAboveAllBoundsUsesTheTopBand() {
        // 30 kWh: 2.52 + 14*0.75 + 10*1.10 = 2.52 + 10.50 + 11.00 = 24.02
        assertEquals(0, new BigDecimal("24.02").compareTo(engine.charge(new BigDecimal("30"), tariff())));
    }

    @Test
    void zeroConsumptionChargesNothing() {
        assertEquals(0, BigDecimal.ZERO.compareTo(engine.charge(BigDecimal.ZERO, tariff())));
    }
}
