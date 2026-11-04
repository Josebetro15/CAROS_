package mx.scanot.repo;

import java.util.List;
import mx.scanot.domain.PartPreorder;
import mx.scanot.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartPreorderRepo extends JpaRepository<PartPreorder, Long> {
  boolean existsByVehicleAndCode(Vehicle vehicle, String code);
  List<PartPreorder> findAllByOrderByIdDesc();
  List<PartPreorder> findByVehicleOrderByIdDesc(Vehicle vehicle);
}
