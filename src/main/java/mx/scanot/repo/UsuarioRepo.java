package mx.scanot.repo;

import java.util.Optional;
import mx.scanot.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepo extends JpaRepository<Usuario, Long> {
  Optional<Usuario> findByEmail(String email);
  boolean existsByEmail(String email);
}
