package zw.meterrate.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoices", uniqueConstraints = @UniqueConstraint(columnNames = {"meter_id", "period"}))
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meter_id", nullable = false)
    private Long meterId;

    @Column(nullable = false, length = 7)
    private String period;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal kwh;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "computed_at", nullable = false, updatable = false)
    private Instant computedAt = Instant.now();

    public Long getId() { return id; }
    public Long getMeterId() { return meterId; }
    public void setMeterId(Long meterId) { this.meterId = meterId; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public BigDecimal getKwh() { return kwh; }
    public void setKwh(BigDecimal kwh) { this.kwh = kwh; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Instant getComputedAt() { return computedAt; }
}
