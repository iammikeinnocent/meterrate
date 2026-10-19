package zw.meterrate.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "tariff_tiers")
public class TariffTier {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tariff_id")
    private Tariff tariff;

    /** Upper bound of this band in kWh; null = top band with no upper bound. */
    @Column(name = "up_to_kwh", precision = 10, scale = 2)
    private BigDecimal upToKwh;

    @Column(name = "rate_per_kwh", nullable = false, precision = 10, scale = 4)
    private BigDecimal ratePerKwh;

    @Column(nullable = false)
    private Integer sequence;

    public Long getId() { return id; }
    public Tariff getTariff() { return tariff; }
    public void setTariff(Tariff tariff) { this.tariff = tariff; }
    public BigDecimal getUpToKwh() { return upToKwh; }
    public void setUpToKwh(BigDecimal upToKwh) { this.upToKwh = upToKwh; }
    public BigDecimal getRatePerKwh() { return ratePerKwh; }
    public void setRatePerKwh(BigDecimal ratePerKwh) { this.ratePerKwh = ratePerKwh; }
    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }
}
