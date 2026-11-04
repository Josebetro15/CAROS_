package mx.scanot.web;

import java.util.List;
import mx.scanot.domain.Rol;
import mx.scanot.domain.UpdateMedia;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.Vehicle;
import mx.scanot.domain.VehicleUpdate;
import mx.scanot.repo.SignalRepo;
import mx.scanot.repo.VehicleRepo;
import mx.scanot.repo.VehicleUpdateRepo;
import mx.scanot.service.MediaService;
import mx.scanot.domain.Cliente;
import mx.scanot.repo.ClienteRepo;
import mx.scanot.service.MailboxService;
import mx.scanot.service.Personas;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/vehiculos")
public class VehicleController {
  public static final List<String[]> KINDS = List.of(
      new String[]{"ingreso", "Unidad ingresada"},
      new String[]{"diagnostico", "Diagnóstico en curso"},
      new String[]{"piezas_pedidas", "Piezas encargadas"},
      new String[]{"en_reparacion", "En reparación"},
      new String[]{"listo", "Listo para entrega"},
      new String[]{"entregado", "Unidad entregada"}
  );

  private final VehicleRepo vehicles;
  private final SignalRepo signals;
  private final VehicleUpdateRepo updates;
  private final MediaService media;
  private final Personas personas;
  private final ClienteRepo clientes;
  private final MailboxService mailbox;

  public VehicleController(VehicleRepo vehicles, SignalRepo signals, VehicleUpdateRepo updates, MediaService media,
                           Personas personas, ClienteRepo clientes, MailboxService mailbox) {
    this.vehicles = vehicles;
    this.signals = signals;
    this.updates = updates;
    this.media = media;
    this.personas = personas;
    this.clientes = clientes;
    this.mailbox = mailbox;
  }

  @GetMapping
  public String list(@AuthenticationPrincipal Usuario user, Model model) {
    model.addAttribute("user", user);
    model.addAttribute("vehicles", user.getRol() == Rol.CLIENTE
        ? vehicles.findByOwnerId(user.getId())
        : vehicles.findAll());
    model.addAttribute("staff", user.getRol() != Rol.CLIENTE);
    if (user.getRol() != Rol.CLIENTE) model.addAttribute("clientes", clientes.findAll());
    return "app/vehiculos";
  }

  @GetMapping("/inspeccion")
  public String inspeccion(@RequestParam(required = false) Long id, @AuthenticationPrincipal Usuario user, Model model) {
    List<Vehicle> list = user.getRol() == Rol.CLIENTE ? vehicles.findByOwnerId(user.getId()) : vehicles.findAll();
    if (list.isEmpty()) {
      model.addAttribute("user", user);
      model.addAttribute("vacio", true);
      return "app/inspeccion";
    }
    Vehicle current = list.get(0);
    int index = 0;
    if (id != null) {
      for (int i = 0; i < list.size(); i++) {
        if (list.get(i).getId().equals(id)) {
          current = list.get(i);
          index = i;
        }
      }
    }
    Vehicle prev = list.get((index - 1 + list.size()) % list.size());
    Vehicle next = list.get((index + 1) % list.size());
    model.addAttribute("user", user);
    model.addAttribute("v", current);
    model.addAttribute("prev", prev.getId());
    model.addAttribute("next", next.getId());
    model.addAttribute("total", list.size());
    model.addAttribute("numero", index + 1);
    model.addAttribute("vacio", false);
    return "app/inspeccion";
  }

  @PostMapping
  public String alta(@AuthenticationPrincipal Usuario user,
                     @RequestParam String plate,
                     @RequestParam String vin,
                     @RequestParam String brand,
                     @RequestParam String modelName,
                     @RequestParam Integer year,
                     @RequestParam Integer km,
                     @RequestParam(required = false) String inventario,
                     @RequestParam(required = false) String ownerId,
                     RedirectAttributes ra) {
    if (user.getRol() == Rol.CLIENTE) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    try {
      String placa = plate == null ? "" : plate.trim().toUpperCase();
      String serie = vin == null ? "" : vin.trim().toUpperCase();
      if (!placa.matches("[A-Z0-9-]{5,12}")) {
        throw new IllegalArgumentException("La placa debe tener de 5 a 12 letras, números o guiones.");
      }
      if (!serie.matches("[A-HJ-NPR-Z0-9]{11,17}")) {
        throw new IllegalArgumentException("El VIN debe tener de 11 a 17 caracteres, sin I, O ni Q.");
      }
      if (year == null || year < 1980 || year > 2035) {
        throw new IllegalArgumentException("El año debe estar entre 1980 y 2035.");
      }
      if (km == null || km < 0) throw new IllegalArgumentException("El kilometraje no puede ser negativo.");
      if (brand == null || brand.trim().length() < 2) throw new IllegalArgumentException("Escribe la marca.");
      if (modelName == null || modelName.trim().length() < 1) throw new IllegalArgumentException("Escribe el modelo.");
      if (vehicles.findByPlate(placa).isPresent()) throw new IllegalArgumentException("Esa placa ya está registrada.");
      if (vehicles.findByVin(serie).isPresent()) throw new IllegalArgumentException("Ese VIN ya está registrado.");
      Vehicle v = new Vehicle();
      v.setPlate(placa);
      v.setVin(serie);
      v.setBrand(brand.trim());
      v.setModel(modelName.trim());
      v.setYear(year);
      v.setKm(km);
      v.setHealth(100);
      v.setInventarioInicial(inventario == null ? "" : inventario.trim());
      if (ownerId != null && !ownerId.isBlank()) {
        long idDueno;
        try {
          idDueno = Long.parseLong(ownerId);
        } catch (NumberFormatException e) {
          throw new IllegalArgumentException("Ese cliente no existe.");
        }
        Cliente dueno = clientes.findById(idDueno)
            .orElseThrow(() -> new IllegalArgumentException("Ese cliente no existe."));
        v.setOwner(dueno);
      }
      vehicles.save(v);
      ra.addFlashAttribute("ok", "Unidad " + v.getPlate() + " registrada en recepción.");
      return "redirect:/app/vehiculos/" + v.getId();
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/app/vehiculos";
    }
  }

  @GetMapping("/{id}")
  public String detail(@PathVariable Long id, @AuthenticationPrincipal Usuario user, Model model) {
    Vehicle v = vehicles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (user.getRol() == Rol.CLIENTE && (v.getOwnerUserId() == null || !v.getOwnerUserId().equals(user.getId()))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    model.addAttribute("user", user);
    model.addAttribute("v", v);
    model.addAttribute("signals", signals.findByVehicleOrderByIdDesc(v));
    model.addAttribute("updates", updates.findByVehicleOrderByIdDesc(v));
    model.addAttribute("kinds", KINDS);
    model.addAttribute("staff", user.getRol() != Rol.CLIENTE);
    return "app/vehiculo";
  }

  @PostMapping("/{id}/avance")
  public String avance(@PathVariable Long id,
                       @AuthenticationPrincipal Usuario user,
                       @RequestParam String kind,
                       @RequestParam String message,
                       @RequestParam(value = "files", required = false) List<MultipartFile> files,
                       RedirectAttributes ra) throws Exception {
    if (user.getRol() == Rol.CLIENTE) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    if (message == null || message.trim().length() < 3) {
      ra.addFlashAttribute("error", "Escribe el avance, mínimo 3 letras.");
      return "redirect:/app/vehiculos/" + id;
    }
    if (message.trim().length() > 2000) {
      ra.addFlashAttribute("error", "El mensaje no puede pasar de 2000 letras.");
      return "redirect:/app/vehiculos/" + id;
    }
    boolean kindOk = KINDS.stream().anyMatch(k -> k[0].equals(kind));
    if (!kindOk) {
      ra.addFlashAttribute("error", "Elige un estado válido.");
      return "redirect:/app/vehiculos/" + id;
    }
    Vehicle v = vehicles.findById(id).orElseThrow();
    VehicleUpdate u = new VehicleUpdate();
    u.setVehicle(v);
    personas.publica(u, user);
    u.setKind(kind);
    u.setMessage(message.trim());
    if (files != null) {
      int n = 0;
      for (MultipartFile f : files) {
        if (f.isEmpty() || n >= 3) continue;
        try {
          String url = media.save(f);
          UpdateMedia m = new UpdateMedia();
          m.setUpdate(u);
          m.setUrl(url);
          m.setMediaType(MediaService.kindOf(f));
          u.getMedia().add(m);
          n++;
        } catch (IllegalArgumentException e) {
          ra.addFlashAttribute("error", e.getMessage());
          return "redirect:/app/vehiculos/" + id;
        }
      }
    }
    updates.save(u);
    mailbox.notifyAvance(v, kind, u.getMessage());
    ra.addFlashAttribute("ok", "Avance publicado. El cliente recibe el aviso.");
    return "redirect:/app/vehiculos/" + id;
  }
}
