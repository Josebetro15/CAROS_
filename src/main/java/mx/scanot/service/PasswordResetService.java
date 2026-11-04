package mx.scanot.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import mx.scanot.domain.Recuperacion;
import mx.scanot.domain.Usuario;
import mx.scanot.repo.RecuperacionRepo;
import mx.scanot.repo.UsuarioRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {
  private final UsuarioRepo usuarios;
  private final RecuperacionRepo tokens;
  private final PasswordEncoder encoder;
  private final MailboxService mailbox;

  @Value("${caros.public-url:http://localhost:8080}")
  private String sitio;

  public PasswordResetService(UsuarioRepo usuarios, RecuperacionRepo tokens, PasswordEncoder encoder, MailboxService mailbox) {
    this.usuarios = usuarios;
    this.tokens = tokens;
    this.encoder = encoder;
    this.mailbox = mailbox;
  }

  @Transactional
  public void pedir(String correo) {
    if (correo == null || correo.isBlank()) throw new IllegalArgumentException("Escribe tu correo.");
    Usuario user = usuarios.findByEmail(correo.trim().toLowerCase()).orElse(null);
    if (user == null) return;
    tokens.deleteByUsuarioId(user.getId());
    tokens.flush();
    Recuperacion r = new Recuperacion();
    r.setUsuarioId(user.getId());
    r.setToken(UUID.randomUUID().toString().replace("-", ""));
    r.setCreadoEn(Instant.now());
    r.setExpiraEn(Instant.now().plus(30, ChronoUnit.MINUTES));
    tokens.save(r);
    String base = sitio.endsWith("/") ? sitio.substring(0, sitio.length() - 1) : sitio;
    mailbox.notifyReset(user, base + "/recuperar/" + r.getToken());
  }

  public boolean vigente(String token) {
    return tokens.findByToken(token == null ? "" : token).filter(r -> r.getExpiraEn().isAfter(Instant.now())).isPresent();
  }

  @Transactional
  public void cambiar(String token, String pass, String pass2) {
    Recuperacion r = tokens.findByToken(token == null ? "" : token)
        .orElseThrow(() -> new IllegalArgumentException("El enlace no es válido o ya se usó."));
    if (r.getExpiraEn().isBefore(Instant.now())) {
      tokens.delete(r);
      throw new IllegalArgumentException("El enlace caducó. Pide otro.");
    }
    if (pass == null || pass.length() < 6) throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
    if (pass.length() > 72) throw new IllegalArgumentException("La contraseña es demasiado larga.");
    if (!pass.equals(pass2)) throw new IllegalArgumentException("Las contraseñas no coinciden.");
    Usuario user = usuarios.findById(r.getUsuarioId())
        .orElseThrow(() -> new IllegalArgumentException("Esa cuenta ya no existe."));
    user.setPassword(encoder.encode(pass));
    usuarios.save(user);
    tokens.delete(r);
  }
}
