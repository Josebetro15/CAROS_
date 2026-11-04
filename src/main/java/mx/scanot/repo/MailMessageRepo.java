package mx.scanot.repo;

import java.util.List;
import mx.scanot.domain.MailMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailMessageRepo extends JpaRepository<MailMessage, Long> {
  boolean existsByRefKey(String refKey);
  List<MailMessage> findAllByOrderByIdDesc();
  List<MailMessage> findByToEmailOrderByIdDesc(String toEmail);
}
