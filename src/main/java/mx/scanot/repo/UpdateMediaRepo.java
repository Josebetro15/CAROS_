package mx.scanot.repo;

import java.util.Optional;
import mx.scanot.domain.UpdateMedia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UpdateMediaRepo extends JpaRepository<UpdateMedia, Long> {
  Optional<UpdateMedia> findByUrl(String url);
}
