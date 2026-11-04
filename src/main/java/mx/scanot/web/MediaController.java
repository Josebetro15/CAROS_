package mx.scanot.web;

import java.nio.file.Path;
import mx.scanot.domain.Rol;
import mx.scanot.domain.UpdateMedia;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.Vehicle;
import mx.scanot.repo.UpdateMediaRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class MediaController {
  private final UpdateMediaRepo archivos;

  @Value("${scanot.upload-dir:uploads}")
  private String uploadDir;

  public MediaController(UpdateMediaRepo archivos) {
    this.archivos = archivos;
  }

  @GetMapping("/uploads/{nombre:.+}")
  public ResponseEntity<Resource> ver(@PathVariable String nombre, @AuthenticationPrincipal Usuario user) {
    if (user == null || !nombre.matches("[A-Za-z0-9._-]+")) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    UpdateMedia archivo = archivos.findByUrl("/uploads/" + nombre)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Vehicle v = archivo.getUpdate().getVehicle();
    boolean staff = user.getRol() != Rol.CLIENTE;
    boolean dueno = v.getOwnerUserId() != null && v.getOwnerUserId().equals(user.getId());
    if (!staff && !dueno) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    Path path = Path.of(uploadDir).toAbsolutePath().resolve(nombre);
    if (!path.toFile().isFile()) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return ResponseEntity.ok().contentType(tipo(nombre)).body(new FileSystemResource(path));
  }

  private static MediaType tipo(String nombre) {
    String n = nombre.toLowerCase();
    if (n.endsWith(".png")) return MediaType.IMAGE_PNG;
    if (n.endsWith(".gif")) return MediaType.IMAGE_GIF;
    if (n.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
    if (n.endsWith(".mp4")) return MediaType.parseMediaType("video/mp4");
    if (n.endsWith(".webm")) return MediaType.parseMediaType("video/webm");
    if (n.endsWith(".mov")) return MediaType.parseMediaType("video/quicktime");
    return MediaType.IMAGE_JPEG;
  }
}
