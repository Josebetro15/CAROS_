package mx.scanot.web;

import java.util.List;
import mx.scanot.domain.Rol;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.Vehicle;
import mx.scanot.repo.SignalRepo;
import mx.scanot.repo.VehicleRepo;
import mx.scanot.repo.VehicleUpdateRepo;
import mx.scanot.repo.WorkOrderRepo;
import mx.scanot.service.Personas;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app")
public class AppController {
  private final VehicleRepo vehicles;
  private final SignalRepo signals;
  private final WorkOrderRepo orders;
  private final VehicleUpdateRepo updates;
  private final Personas personas;

  public AppController(VehicleRepo vehicles, SignalRepo signals, WorkOrderRepo orders, VehicleUpdateRepo updates,
                       Personas personas) {
    this.vehicles = vehicles;
    this.signals = signals;
    this.orders = orders;
    this.updates = updates;
    this.personas = personas;
  }

  @GetMapping
  public String dashboard(@AuthenticationPrincipal Usuario user, Model model) {
    model.addAttribute("user", user);
    if (user.getRol() == Rol.CLIENTE) {
      List<Vehicle> mine = vehicles.findByOwnerId(user.getId());
      model.addAttribute("vehicles", mine);
      model.addAttribute("updates", mine.stream()
          .flatMap(v -> updates.findByVehicleOrderByIdDesc(v).stream())
          .limit(8)
          .toList());
      return "app/cliente";
    }
    model.addAttribute("vehiculos", vehicles.count());
    model.addAttribute("abiertas", signals.countByStatus("open"));
    model.addAttribute("enCurso", orders.countByStatus("in_progress"));
    model.addAttribute("esperando", orders.countByStatus("esperando_refaccion"));
    model.addAttribute("signals", signals.findByStatusOrderByIdDesc("open"));
    model.addAttribute("orders", orders.findAllByOrderByIdDesc());
    return "app/dashboard";
  }

  @PostMapping("/vincular")
  public String claim(@AuthenticationPrincipal Usuario user,
                      @RequestParam String plate,
                      @RequestParam String vin,
                      RedirectAttributes ra) {
    if (plate == null || plate.isBlank() || vin == null || vin.isBlank()) {
      ra.addFlashAttribute("error", "Escribe la placa y el VIN.");
      return "redirect:/app";
    }
    vehicles.findByPlate(plate.trim().toUpperCase()).filter(v -> v.getVin().equalsIgnoreCase(vin.trim()))
        .ifPresentOrElse(v -> {
          if (v.getOwner() != null && (user.getRol() != Rol.CLIENTE || !v.getOwner().getId().equals(user.getId()))) {
            ra.addFlashAttribute("error", "Esa unidad ya tiene dueño. Pídele al taller que te la asigne.");
            return;
          }
          if (v.getOwner() != null) {
            ra.addFlashAttribute("ok", "Esa unidad ya está en tu cuenta.");
            return;
          }
          v.setOwner(personas.clienteDe(user));
          vehicles.save(v);
          ra.addFlashAttribute("ok", "Unidad vinculada.");
        }, () -> ra.addFlashAttribute("error", "La placa y el VIN no coinciden con ninguna unidad."));
    return "redirect:/app";
  }
}
