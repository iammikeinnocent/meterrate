package zw.meterrate.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "readings", uniqueConstraints = @UniqueConstraint(columnNames = {"meter_id", "read_on"}))
public class Reading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meter_id", nullable = false)
    private Long meterId;

    @Column(name = "read_on", nullable = false)
    private LocalDate readOn;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal kwh;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public Long getMeterId() { return meterId; }
    public void setMeterId(Long meterId) { this.meterId = meterId; }
    public LocalDate getReadOn() { return readOn; }
    public void setReadOn(LocalDate readOn) { this.readOn = readOn; }
    public BigDecimal getKwh() { return kwh; }
    public void setKwh(BigDecimal kwh) { this.kwh = kwh; }
    public Instant getCreatedAt() { return createdAt; }
}
