package mx.scanot.repo;

import java.util.Optional;
import mx.scanot.domain.Recuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecuperacionRepo extends JpaRepository<Recuperacion, Long> {
  Optional<Recuperacion> findByToken(String token);
  void deleteByUsuarioId(Long usuarioId);
}
