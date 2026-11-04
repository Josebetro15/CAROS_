package mx.scanot.repo;

import java.util.List;
import mx.scanot.domain.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkOrderRepo extends JpaRepository<WorkOrder, Long> {
  List<WorkOrder> findAllByOrderByIdDesc();
  long countByStatus(String status);
}
