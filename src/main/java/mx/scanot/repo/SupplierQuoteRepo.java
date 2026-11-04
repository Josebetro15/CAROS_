package mx.scanot.repo;

import java.util.List;
import mx.scanot.domain.SupplierQuote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierQuoteRepo extends JpaRepository<SupplierQuote, Long> {
  List<SupplierQuote> findByPartIdOrderByPriceAsc(Long partId);
}
