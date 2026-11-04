package mx.scanot.repo;

import java.util.List;
import mx.scanot.domain.Signal;
import mx.scanot.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SignalRepo extends JpaRepository<Signal, Long> {
  List<Signal> findAllByOrderByIdDesc();
  List<Signal> findByStatusOrderByIdDesc(String status);
  List<Signal> findByVehicleOrderByIdDesc(Vehicle vehicle);
  long countByStatus(String status);
  boolean existsByVehicleAndCodeAndStatus(Vehicle vehicle, String code, String status);
}
