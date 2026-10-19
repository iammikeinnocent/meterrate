package zw.meterrate.repository;

import zw.meterrate.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TariffRepository extends JpaRepository<Tariff, Long> {
    /** The tariff effective on or before the given date, latest effectiveFrom wins. */
    Optional<Tariff> findFirstByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(LocalDate date);
}

