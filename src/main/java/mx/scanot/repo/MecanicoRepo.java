package mx.scanot.repo;

import mx.scanot.domain.Mecanico;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MecanicoRepo extends JpaRepository<Mecanico, Long> {
}
