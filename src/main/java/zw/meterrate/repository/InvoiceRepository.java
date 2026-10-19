package zw.meterrate.repository;

import zw.meterrate.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByPeriodOrderByMeterId(String period);
    void deleteByPeriod(String period);
    Optional<Invoice> findByMeterIdAndPeriod(Long meterId, String period);
}

