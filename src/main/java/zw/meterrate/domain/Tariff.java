package zw.meterrate.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Entity
@Table(name = "tariffs")
public class Tariff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "tariff", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<TariffTier> tiers = new ArrayList<>();

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public Instant getCreatedAt() { return createdAt; }
    public List<TariffTier> getTiers() { return tiers; }
    public void setTiers(List<TariffTier> tiers) {
        this.tiers = tiers;
        tiers.forEach(t -> t.setTariff(this));
    }

    /** Tiers ordered by sequence; the tier with a null upper bound is the top band. */
    public List<TariffTier> orderedTiers() {
        return tiers.stream().sorted(Comparator.comparingInt(TariffTier::getSequence)).toList();
    }
}
