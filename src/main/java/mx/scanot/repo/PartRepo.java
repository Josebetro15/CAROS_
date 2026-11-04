package mx.scanot.repo;

import java.util.Optional;
import mx.scanot.domain.Part;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRepo extends JpaRepository<Part, Long> {
  Optional<Part> findBySku(String sku);
}
