package mx.scanot.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {
  @Value("${scanot.upload-dir:uploads}")
  private String uploadDir;

  public String save(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty()) return null;
    String t = file.getContentType() == null ? "" : file.getContentType();
    String orig = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    String ext = orig.contains(".") ? orig.substring(orig.lastIndexOf('.')).toLowerCase(Locale.ROOT) : "";
    boolean imagen = ext.equals(".png") || ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".webp") || ext.equals(".gif");
    boolean video = ext.equals(".mp4") || ext.equals(".webm") || ext.equals(".mov");
    if (!imagen && !video) {
      throw new IllegalArgumentException("Solo se aceptan fotos JPG, PNG, WEBP o GIF, y video MP4, WEBM o MOV.");
    }
    if (!t.isBlank()) {
      if (imagen && !t.startsWith("image/") && !t.equals("application/octet-stream")) {
        throw new IllegalArgumentException("El archivo no parece una foto.");
      }
      if (video && !t.startsWith("video/") && !t.equals("application/octet-stream")) {
        throw new IllegalArgumentException("El archivo no parece un video.");
      }
    }
    String name = UUID.randomUUID() + ext;
    Path dir = Path.of(uploadDir).toAbsolutePath();
    Files.createDirectories(dir);
    Path dest = dir.resolve(name);
    file.transferTo(dest);
    return "/uploads/" + name;
  }

  public static String kindOf(MultipartFile file) {
    String orig = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    if (orig.endsWith(".mp4") || orig.endsWith(".webm") || orig.endsWith(".mov")) return "video";
    return "image";
  }
}
