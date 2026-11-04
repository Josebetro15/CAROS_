package mx.scanot.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mx.scanot.api.dto.Dtos.OrderDto;
import mx.scanot.api.dto.Dtos.OrderInDto;
import mx.scanot.api.dto.Dtos.PartDto;
import mx.scanot.api.dto.Dtos.PreorderDto;
import mx.scanot.api.dto.Dtos.SignalDto;
import mx.scanot.api.dto.Dtos.StatusInDto;
import mx.scanot.domain.Rol;
import mx.scanot.domain.Signal;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.WorkOrder;
import mx.scanot.repo.PartPreorderRepo;
import mx.scanot.repo.PartRepo;
import mx.scanot.repo.SignalRepo;
import mx.scanot.repo.VehicleRepo;
import mx.scanot.repo.WorkOrderRepo;
import mx.scanot.domain.Mecanico;
import mx.scanot.repo.MecanicoRepo;
import mx.scanot.service.Folios;
import mx.scanot.service.Personas;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiTallerController {
  private final SignalRepo signals;
  private final PartRepo parts;
  private final WorkOrderRepo orders;
  private final VehicleRepo vehicles;
  private final PartPreorderRepo preorders;
  private final Personas personas;
  private final Folios folios;
  private final MecanicoRepo mecanicos;

  public ApiTallerController(SignalRepo signals, PartRepo parts, WorkOrderRepo orders, VehicleRepo vehicles,
                             PartPreorderRepo preorders, Personas personas, Folios folios, MecanicoRepo mecanicos) {
    this.signals = signals;
    this.parts = parts;
    this.orders = orders;
    this.vehicles = vehicles;
    this.preorders = preorders;
    this.personas = personas;
    this.folios = folios;
    this.mecanicos = mecanicos;
  }

  @GetMapping("/diagnosticos")
  public List<SignalDto> diagnosticos(@AuthenticationPrincipal Usuario user) {
    staff(user);
    return signals.findAllByOrderByIdDesc().stream().map(SignalDto::of).toList();
  }

  @PostMapping("/diagnosticos/{id}/cerrar")
  public SignalDto cerrar(@PathVariable Long id, @AuthenticationPrincipal Usuario user) {
    staff(user);
    Signal s = signals.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe"));
    s.setStatus("closed");
    s.setClosedAt(Instant.now());
    return SignalDto.of(signals.save(s));
  }

  @GetMapping("/partes")
  public List<PartDto> partes(@AuthenticationPrincipal Usuario user) {
    staff(user);
    return parts.findAll().stream().map(PartDto::of).toList();
  }

  @GetMapping("/preordenes")
  public List<PreorderDto> preordenes(@AuthenticationPrincipal Usuario user) {
    staff(user);
    return preorders.findAllByOrderByIdDesc().stream().map(PreorderDto::of).toList();
  }

  @GetMapping("/ordenes")
  public List<OrderDto> ordenes(@AuthenticationPrincipal Usuario user) {
    staff(user);
    return orders.findAllByOrderByIdDesc().stream().map(OrderDto::of).toList();
  }

  @PostMapping("/ordenes")
  public OrderDto crear(@AuthenticationPrincipal Usuario user, @RequestBody OrderInDto body) {
    staff(user);
    if (body.workDesc() == null || body.workDesc().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Describe el trabajo");
    }
    WorkOrder o = new WorkOrder();
    o.setFolio(folios.nuevo());
    o.setVehicle(vehicles.findById(body.vehicleId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no existe")));
    o.setWorkDesc(body.workDesc().trim());
    o.setHours(new BigDecimal("1.5"));
    o.setStatus("pending");
    if (user.getRol() == Rol.MECANICO) {
      o.setTechnician(user.getDisplayName());
      o.setMecanico(mecanicos.getReferenceById(user.getId()));
    } else {
      String nombre = body.technician() == null ? "" : body.technician().trim();
      Mecanico m = mecanicos.findAll().stream()
          .filter(x -> x.getDisplayName().equalsIgnoreCase(nombre))
          .findFirst()
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El técnico debe ser un mecánico registrado"));
      o.setTechnician(m.getDisplayName());
      o.setMecanico(m);
    }
    personas.vigila(o, user);
    return OrderDto.of(orders.save(o));
  }

  @PostMapping("/ordenes/{id}/status")
  public OrderDto status(@PathVariable Long id,
                         @AuthenticationPrincipal Usuario user,
                         @RequestBody StatusInDto body) {
    if (user.getRol() == Rol.CLIENTE) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo taller");
    WorkOrder o = orders.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe"));
    String st = body.status();
    if (!List.of("esperando_refaccion", "pending", "in_progress", "calidad", "facturado", "done").contains(st)) {
      throw new IllegalArgumentException("El estado debe ser esperando refacción, pendiente, en ejecución, calidad, facturado o cerrada.");
    }
    o.setStatus(st);
    if (user.getRol() == Rol.ENCARGADO) personas.vigila(o, user);
    return OrderDto.of(orders.save(o));
  }

  @GetMapping("/flujo")
  public Map<String, List<OrderDto>> flujo() {
    Map<String, List<OrderDto>> board = new LinkedHashMap<>();
    List<OrderDto> all = orders.findAllByOrderByIdDesc().stream().map(OrderDto::of).toList();
    board.put("esperando_refaccion", all.stream().filter(o -> "esperando_refaccion".equals(o.status())).toList());
    board.put("pending", all.stream().filter(o -> "pending".equals(o.status())).toList());
    board.put("in_progress", all.stream().filter(o -> "in_progress".equals(o.status())).toList());
    board.put("calidad", all.stream().filter(o -> "calidad".equals(o.status())).toList());
    board.put("facturado", all.stream().filter(o -> "facturado".equals(o.status())).toList());
    board.put("done", all.stream().filter(o -> "done".equals(o.status())).toList());
    return board;
  }

  private static void staff(Usuario user) {
    if (user.getRol() == Rol.CLIENTE) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo taller");
  }
}
