package mx.scanot;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import mx.scanot.config.GoogleProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GoogleProps.class)
public class ScanotApplication {
  public static void main(String[] args) {
    prepararBase();
    SpringApplication.run(ScanotApplication.class, args);
  }

  /** Railway entrega postgres://usuario:clave@host/base. Spring necesita jdbc. */
  private static void prepararBase() {
    String raw = System.getenv("DATABASE_URL");
    if (raw == null || raw.isBlank()) return;
    if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) return;
    String sinEsquema = raw.replaceFirst("^postgres(ql)?://", "");
    int at = sinEsquema.lastIndexOf('@');
    if (at < 1) return;
    String credenciales = sinEsquema.substring(0, at);
    String host = sinEsquema.substring(at + 1);
    int sep = credenciales.indexOf(':');
    if (sep < 1) return;
    String usuario = url(credenciales.substring(0, sep));
    String clave = url(credenciales.substring(sep + 1));
    String jdbc = "jdbc:postgresql://" + host;
    if (!host.contains("railway.internal") && !jdbc.contains("sslmode=")) {
      jdbc += jdbc.contains("?") ? "&" : "?";
      jdbc += "sslmode=require";
    }
    System.setProperty("spring.datasource.url", jdbc);
    System.setProperty("spring.datasource.username", usuario);
    System.setProperty("spring.datasource.password", clave);
  }

  private static String url(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }
}
