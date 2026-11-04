package mx.scanot.repo;

import java.util.Optional;
import mx.scanot.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepo extends JpaRepository<Supplier, Long> {
  Optional<Supplier> findByName(String name);
}
