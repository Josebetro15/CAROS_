package mx.scanot.repo;

import java.util.List;
import mx.scanot.domain.ScanReading;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanReadingRepo extends JpaRepository<ScanReading, Long> {
  List<ScanReading> findTop20ByOrderByIdDesc();
}
