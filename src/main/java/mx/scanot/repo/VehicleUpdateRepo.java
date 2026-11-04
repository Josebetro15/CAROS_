package mx.scanot.repo;

import java.util.List;
import mx.scanot.domain.Vehicle;
import mx.scanot.domain.VehicleUpdate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleUpdateRepo extends JpaRepository<VehicleUpdate, Long> {
  List<VehicleUpdate> findByVehicleOrderByIdDesc(Vehicle vehicle);
}
