package mx.scanot.repo;

import java.util.Optional;
import mx.scanot.domain.SolicitudCuenta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudRepo extends JpaRepository<SolicitudCuenta, Long> {
  Optional<SolicitudCuenta> findByCorreo(String correo);
  Optional<SolicitudCuenta> findByTokenCorreo(String token);
  void deleteByCorreo(String correo);
}
