package mx.scanot.api;

import java.util.List;
import mx.scanot.api.dto.Dtos.AvanceInDto;
import mx.scanot.api.dto.Dtos.UpdateDto;
import mx.scanot.api.dto.Dtos.VehicleDto;
import mx.scanot.domain.Rol;
import mx.scanot.domain.UpdateMedia;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.Vehicle;
import mx.scanot.domain.VehicleUpdate;
import mx.scanot.repo.VehicleRepo;
import mx.scanot.repo.VehicleUpdateRepo;
import mx.scanot.service.MediaService;
import mx.scanot.service.MailboxService;
import mx.scanot.service.Personas;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ApiVehicleController {
  private final VehicleRepo vehicles;
  private final VehicleUpdateRepo updates;
  private final MediaService media;
  private final Personas personas;
  private final MailboxService mailbox;

  public ApiVehicleController(VehicleRepo vehicles, VehicleUpdateRepo updates, MediaService media, Personas personas,
                              MailboxService mailbox) {
    this.vehicles = vehicles;
    this.updates = updates;
    this.media = media;
    this.personas = personas;
    this.mailbox = mailbox;
  }

  @GetMapping("/me")
  public mx.scanot.api.dto.Dtos.MeDto me(@AuthenticationPrincipal Usuario user) {
    return mx.scanot.api.dto.Dtos.MeDto.of(user);
  }

  @GetMapping("/vehiculos")
  public List<VehicleDto> list(@AuthenticationPrincipal Usuario user) {
    List<Vehicle> list = user.getRol() == Rol.CLIENTE
        ? vehicles.findByOwnerId(user.getId())
        : vehicles.findAll();
    return list.stream().map(VehicleDto::of).toList();
  }

  @GetMapping("/vehiculos/{id}")
  public VehicleDto one(@PathVariable Long id, @AuthenticationPrincipal Usuario user) {
    return VehicleDto.of(owned(id, user));
  }

  @GetMapping("/vehiculos/{id}/actualizaciones")
  public List<UpdateDto> history(@PathVariable Long id, @AuthenticationPrincipal Usuario user) {
    Vehicle v = owned(id, user);
    return updates.findByVehicleOrderByIdDesc(v).stream().map(UpdateDto::of).toList();
  }

  @PostMapping("/vehiculos/{id}/avance")
  public UpdateDto avanceJson(@PathVariable Long id,
                              @AuthenticationPrincipal Usuario user,
                              @RequestBody AvanceInDto body) {
    staff(user);
    return saveAvance(owned(id, user), user, body.kind(), body.message(), List.of());
  }

  @PostMapping(value = "/vehiculos/{id}/avance-media", consumes = "multipart/form-data")
  public UpdateDto avanceFiles(@PathVariable Long id,
                               @AuthenticationPrincipal Usuario user,
                               @RequestParam String kind,
                               @RequestParam String message,
                               @RequestParam(value = "files", required = false) List<MultipartFile> files) {
    staff(user);
    return saveAvance(owned(id, user), user, kind, message, files == null ? List.of() : files);
  }

  @PostMapping("/vehiculos/vincular")
  public VehicleDto claim(@AuthenticationPrincipal Usuario user,
                          @RequestParam String plate,
                          @RequestParam String vin) {
    Vehicle v = vehicles.findByPlate(plate.trim().toUpperCase())
        .filter(x -> x.getVin().equalsIgnoreCase(vin.trim()))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La placa y el VIN no coinciden"));
    if (user.getRol() != Rol.CLIENTE) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo un cliente puede vincular");
    if (v.getOwnerUserId() != null && !v.getOwnerUserId().equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Esa unidad ya tiene dueño");
    }
    if (v.getOwnerUserId() != null) return VehicleDto.of(v);
    v.setOwner(personas.clienteDe(user));
    return VehicleDto.of(vehicles.save(v));
  }

  private UpdateDto saveAvance(Vehicle v, Usuario user, String kind, String message, List<MultipartFile> files) {
    if (kind == null || message == null || message.isBlank()
        || !List.of("ingreso", "diagnostico", "piezas_pedidas", "en_reparacion", "listo", "entregado").contains(kind)) {
      throw new IllegalArgumentException("Elige un estado válido y escribe el avance.");
    }
    VehicleUpdate u = new VehicleUpdate();
    u.setVehicle(v);
    personas.publica(u, user);
    u.setKind(kind);
    u.setMessage(message);
    try {
      int n = 0;
      for (MultipartFile f : files) {
        if (f == null || f.isEmpty() || n >= 3) continue;
        UpdateMedia m = new UpdateMedia();
        m.setUpdate(u);
        m.setUrl(media.save(f));
        m.setMediaType(MediaService.kindOf(f));
        u.getMedia().add(m);
        n++;
      }
    } catch (Exception e) {
      throw new IllegalArgumentException("No se pudo guardar el archivo.");
    }
    VehicleUpdate guardado = updates.save(u);
    mailbox.notifyAvance(v, kind, message);
    return UpdateDto.of(guardado);
  }

  private Vehicle owned(Long id, Usuario user) {
    Vehicle v = vehicles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe"));
    if (user.getRol() == Rol.CLIENTE && (v.getOwnerUserId() == null || !v.getOwnerUserId().equals(user.getId()))) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No es tu unidad");
    }
    return v;
  }

  private static void staff(Usuario user) {
    if (user.getRol() == Rol.CLIENTE) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo taller");
    }
  }
}
