package mx.scanot.repo;

import java.util.List;
import java.util.Optional;
import mx.scanot.domain.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepo extends JpaRepository<Vehicle, Long> {
  Optional<Vehicle> findByVin(String vin);
  Optional<Vehicle> findByPlate(String plate);
  List<Vehicle> findByOwnerId(Long ownerId);
}
