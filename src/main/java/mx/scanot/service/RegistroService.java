package mx.scanot.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import mx.scanot.domain.Cliente;
import mx.scanot.domain.Encargado;
import mx.scanot.domain.Mecanico;
import mx.scanot.domain.Rol;
import mx.scanot.domain.SolicitudCuenta;
import mx.scanot.domain.Usuario;
import mx.scanot.repo.SolicitudRepo;
import mx.scanot.repo.UsuarioRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistroService {
  private final SolicitudRepo solicitudes;
  private final UsuarioRepo usuarios;
  private final PasswordEncoder encoder;
  private final CorreoExterno correo;
  private final SolicitudIntentos intentos;
  private final SecureRandom random = new SecureRandom();

  @Value("${caros.supervisor.email:jh6681891@gmail.com}")
  private String supervisor;

  @Value("${caros.public-url:http://localhost:8080}")
  private String sitio;

  public RegistroService(SolicitudRepo solicitudes, UsuarioRepo usuarios, PasswordEncoder encoder, CorreoExterno correo,
                        SolicitudIntentos intentos) {
    this.solicitudes = solicitudes;
    this.usuarios = usuarios;
    this.encoder = encoder;
    this.correo = correo;
    this.intentos = intentos;
  }

  public String supervisor() {
    return supervisor;
  }

  @Transactional
  public SolicitudCuenta solicitar(String nombre, String correoUsuario, String pass, String pass2, String rolTexto) {
    if (!correo.listo()) {
      throw new IllegalStateException(
          "Falta la contraseña de aplicación de Gmail en application.properties (spring.mail.password).");
    }
    String nombreLimpio = limpio(nombre);
    String email = limpio(correoUsuario).toLowerCase();
    Rol rol = rolDe(rolTexto);
    if (nombreLimpio.length() < 2) throw new IllegalArgumentException("Escribe tu nombre.");
    if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw new IllegalArgumentException("El correo no es válido.");
    if (pass == null || pass.length() < 6) throw new IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.");
    if (pass.length() > 72) throw new IllegalArgumentException("La contraseña es demasiado larga.");
    if (!pass.equals(pass2)) throw new IllegalArgumentException("Las contraseñas no coinciden.");
    if (usuarios.existsByEmail(email)) throw new IllegalArgumentException("Ese correo ya tiene cuenta. Entra con él.");

    solicitudes.deleteByCorreo(email);
    solicitudes.flush();

    SolicitudCuenta s = new SolicitudCuenta();
    s.setCorreo(email);
    s.setNombre(nombreLimpio);
    s.setContrasena(encoder.encode(pass));
    s.setRol(rol);
    s.setCodigoCorreo(codigo());
    s.setTokenCorreo(UUID.randomUUID().toString().replace("-", ""));
    s.setCorreoOk(false);
    s.setCreadoEn(Instant.now());
    s.setExpiraEn(Instant.now().plus(30, ChronoUnit.MINUTES));
    if (s.pideAcceso()) s.setCodigoAcceso(codigo());
    solicitudes.save(s);
    enviarCodigos(s);
    return s;
  }

  @Transactional
  public void reenviar(String correoUsuario) {
    if (!correo.listo()) {
      throw new IllegalStateException(
          "Falta la contraseña de aplicación de Gmail en application.properties (spring.mail.password).");
    }
    SolicitudCuenta s = buscar(correoUsuario);
    if (s.getReenvios() >= 3) {
      throw new IllegalArgumentException("Ya reenviamos el código 3 veces. Espera a que caduque o crea la cuenta de nuevo.");
    }
    s.setCodigoCorreo(codigo());
    s.setTokenCorreo(UUID.randomUUID().toString().replace("-", ""));
    s.setCorreoOk(false);
    s.setReenvios(s.getReenvios() + 1);
    s.setExpiraEn(Instant.now().plus(30, ChronoUnit.MINUTES));
    if (s.pideAcceso()) s.setCodigoAcceso(codigo());
    solicitudes.save(s);
    enviarCodigos(s);
  }

  private void enviarCodigos(SolicitudCuenta s) {
    String base = sitio.endsWith("/") ? sitio.substring(0, sitio.length() - 1) : sitio;
    String enlace = base + "/registro/verificar?token=" + s.getTokenCorreo();
    correo.enviar(s.getCorreo(), "CarOs · confirma tu correo",
        "Hola " + s.getNombre() + ".\n\n"
            + "Para confirmar tu correo en CarOs usa este código: " + s.getCodigoCorreo() + "\n\n"
            + "O abre este enlace:\n" + enlace + "\n\n"
            + "Caduca en 30 minutos. Si no pediste la cuenta, ignora este mensaje.");
    if (s.pideAcceso()) {
      correo.enviar(supervisor, "CarOs · código de acceso para " + etiqueta(s.getRol()),
          "Hola.\n\n"
              + s.getNombre() + " (" + s.getCorreo() + ") pidió una cuenta de " + etiqueta(s.getRol()) + " en CarOs.\n\n"
              + "Código de acceso: " + s.getCodigoAcceso() + "\n\n"
              + "Pásaselo solo si autorizas ese rol. Caduca en 30 minutos.");
    }
  }

  @Transactional
  public SolicitudCuenta porToken(String token) {
    SolicitudCuenta s = solicitudes.findByTokenCorreo(token == null ? "" : token.trim())
        .orElseThrow(() -> new IllegalArgumentException("El enlace no es válido o ya se usó."));
    vigente(s);
    s.setCorreoOk(true);
    return solicitudes.save(s);
  }

  @Transactional
  public boolean confirmar(String correoUsuario, String codigoCorreo, String codigoAcceso) {
    SolicitudCuenta s = buscar(correoUsuario);
    if (!s.isCorreoOk()) {
      if (codigoCorreo == null || !s.getCodigoCorreo().equals(codigoCorreo.trim())) {
        int n = intentos.sumar(s.getId());
        if (n >= 5) throw new IllegalArgumentException("Demasiados intentos. Vuelve a crear la cuenta.");
        throw new IllegalArgumentException("El código de tu correo no coincide. Te quedan " + (5 - n) + " intentos.");
      }
      s.setCorreoOk(true);
    }
    if (s.pideAcceso()) {
      if (codigoAcceso == null || s.getCodigoAcceso() == null || !s.getCodigoAcceso().equals(codigoAcceso.trim())) {
        int n = intentos.sumar(s.getId());
        if (n >= 5) throw new IllegalArgumentException("Demasiados intentos. Vuelve a crear la cuenta.");
        throw new IllegalArgumentException("El código de acceso no coincide. Te quedan " + (5 - n) + " intentos.");
      }
    }
    crearUsuario(s);
    solicitudes.delete(s);
    return true;
  }

  @Transactional
  public SolicitudCuenta solicitarGoogle(String nombre, String email, Rol rol) {
    if (!correo.listo()) {
      throw new IllegalStateException(
          "Falta la contraseña de aplicación de Gmail en application.properties (spring.mail.password).");
    }
    if (rol != Rol.MECANICO && rol != Rol.ENCARGADO) {
      throw new IllegalArgumentException("Ese rol no necesita código de supervisor.");
    }
    if (usuarios.existsByEmail(email)) throw new IllegalArgumentException("Ese correo ya tiene cuenta. Entra con Google.");
    solicitudes.deleteByCorreo(email);
    solicitudes.flush();
    SolicitudCuenta s = new SolicitudCuenta();
    s.setCorreo(email);
    s.setNombre(nombre);
    s.setContrasena(encoder.encode(UUID.randomUUID().toString()));
    s.setRol(rol);
    s.setCodigoCorreo(codigo());
    s.setTokenCorreo(UUID.randomUUID().toString().replace("-", ""));
    s.setCorreoOk(true);
    s.setCodigoAcceso(codigo());
    s.setCreadoEn(Instant.now());
    s.setExpiraEn(Instant.now().plus(30, ChronoUnit.MINUTES));
    solicitudes.save(s);
    correo.enviar(supervisor, "CarOs · código de acceso para " + etiqueta(rol),
        "Hola.\n\n"
            + nombre + " (" + email + ") quiere una cuenta de " + etiqueta(rol) + " con Google.\n\n"
            + "Código de acceso: " + s.getCodigoAcceso() + "\n\n"
            + "Pásaselo solo si autorizas ese rol. Caduca en 30 minutos.");
    return s;
  }

  public SolicitudCuenta buscar(String correoUsuario) {
    String email = limpio(correoUsuario).toLowerCase();
    SolicitudCuenta s = solicitudes.findByCorreo(email)
        .orElseThrow(() -> new IllegalArgumentException("No hay un registro pendiente con ese correo."));
    vigente(s);
    return s;
  }

  private void vigente(SolicitudCuenta s) {
    if (s.getExpiraEn().isBefore(Instant.now())) {
      solicitudes.delete(s);
      throw new IllegalArgumentException("El código caducó. Vuelve a crear la cuenta.");
    }
  }

  private void crearUsuario(SolicitudCuenta s) {
    if (usuarios.existsByEmail(s.getCorreo())) {
      throw new IllegalArgumentException("Ese correo ya tiene cuenta. Entra con él.");
    }
    Usuario u = switch (s.getRol()) {
      case CLIENTE -> new Cliente();
      case MECANICO -> new Mecanico();
      case ENCARGADO -> new Encargado();
    };
    u.setEmail(s.getCorreo());
    u.setDisplayName(s.getNombre());
    u.setPassword(s.getContrasena());
    u.setRol(s.getRol());
    usuarios.save(u);
  }

  private static Rol rolDe(String texto) {
    if (texto == null) throw new IllegalArgumentException("Elige un rol.");
    return switch (texto.trim().toUpperCase()) {
      case "CLIENTE" -> Rol.CLIENTE;
      case "MECANICO" -> Rol.MECANICO;
      case "ENCARGADO", "SUPERVISOR" -> Rol.ENCARGADO;
      default -> throw new IllegalArgumentException("Elige cliente, mecánico o supervisor.");
    };
  }

  public static String etiqueta(Rol rol) {
    return switch (rol) {
      case CLIENTE -> "cliente";
      case MECANICO -> "mecánico";
      case ENCARGADO -> "supervisor";
    };
  }

  private String codigo() {
    return String.format("%06d", random.nextInt(1_000_000));
  }

  private static String limpio(String value) {
    return value == null ? "" : value.trim();
  }
}
