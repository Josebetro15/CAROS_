package mx.scanot.service;

import java.util.List;
import mx.scanot.domain.MailMessage;
import mx.scanot.domain.PartPreorder;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.Vehicle;
import mx.scanot.repo.MailMessageRepo;
import mx.scanot.repo.PartPreorderRepo;
import mx.scanot.repo.UsuarioRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailboxService {
  public static final String TALLER = "taller@caros.mx";

  private final MailMessageRepo mails;
  private final UsuarioRepo usuarios;
  private final PartPreorderRepo preorders;
  private final Personas personas;
  private final CorreoExterno correo;

  public MailboxService(MailMessageRepo mails, UsuarioRepo usuarios, PartPreorderRepo preorders, Personas personas,
                        CorreoExterno correo) {
    this.mails = mails;
    this.usuarios = usuarios;
    this.preorders = preorders;
    this.personas = personas;
    this.correo = correo;
  }

  @Transactional
  public void notifyPreorder(Vehicle vehicle, String code, String part, String supplier, String price, int leadDays, String folio) {
    String when = leadDays == 0 ? "hoy, porque la pieza ya está en almacén" : "en " + leadDays + " días, cuando llegue la refacción";
    String body = "Hola. En la revisión de " + vehicle.getPlate() + " (" + vehicle.getBrand() + " " + vehicle.getModel()
        + ") el escáner marcó " + code + ". Pedimos " + part + " con " + supplier + " por $" + price
        + ". La unidad entra al taller " + when + ". Orden " + folio + ". Así no se queda parada esperando.";
    send("ot-" + folio, ownerEmail(vehicle), ownerName(vehicle),
        "CarOs · " + vehicle.getPlate() + " entra " + (leadDays == 0 ? "hoy" : "en " + leadDays + " días"),
        body, "preorden", vehicle.getPlate(), null);
  }

  @Transactional
  public void notifyAvance(Vehicle vehicle, String kind, String message) {
    String titulo = switch (kind == null ? "" : kind) {
      case "ingreso" -> "Unidad ingresada";
      case "diagnostico" -> "Diagnóstico en curso";
      case "piezas_pedidas" -> "Piezas encargadas";
      case "en_reparacion" -> "En reparación";
      case "listo" -> "Lista para entrega";
      case "entregado" -> "Unidad entregada";
      default -> "Avance del taller";
    };
    String body = "Hola. La unidad " + vehicle.getPlate() + " (" + vehicle.getBrand() + " " + vehicle.getModel()
        + ") cambió a: " + titulo + ".\n\n" + message;
    send("avance-" + vehicle.getId() + "-" + System.nanoTime(), ownerEmail(vehicle), ownerName(vehicle),
        "CarOs · " + vehicle.getPlate() + " · " + titulo, body, "aviso", vehicle.getPlate(), null);
  }

  @Transactional
  public void notifyReset(Usuario user, String link) {
    send("reset-" + user.getId() + "-" + System.nanoTime(), user.getEmail(), user.getEmail(),
        "CarOs · restablece tu contraseña",
        "Pide una contraseña nueva en este enlace. Caduca en 30 minutos:\n" + link,
        "recuperacion", null, null);
  }

  @Transactional
  public MailMessage compose(Usuario author, String toEmail, String subject, String body, String plate) {
    String to = toEmail.trim().toLowerCase();
    return send("manual-" + System.nanoTime(), to, to, subject.trim(), body.trim(), "aviso", plate, author);
  }

  @Transactional
  public void syncPreorders() {
    for (PartPreorder p : preorders.findAll()) {
      if (mails.existsByRefKey("ot-" + p.getFolio())) continue;
      notifyPreorder(p.getVehicle(), p.getCode(), p.getPartName(), p.getSupplierName(),
          p.getPrice().toPlainString(), p.getLeadDays(), p.getFolio());
    }
  }

  public List<MailMessage> inbox(Usuario user) {
    if (user.getRol() == mx.scanot.domain.Rol.CLIENTE) {
      return mails.findByToEmailOrderByIdDesc(user.getEmail());
    }
    return mails.findAllByOrderByIdDesc();
  }

  private MailMessage send(String ref, String toEmail, String toName, String subject, String body, String kind, String plate, Usuario author) {
    if (mails.existsByRefKey(ref)) return mails.findAll().stream().filter(m -> ref.equals(m.getRefKey())).findFirst().orElseThrow();
    MailMessage m = new MailMessage();
    m.setRefKey(ref);
    m.setToEmail(toEmail);
    m.setToName(toName == null || toName.isBlank() ? toEmail : toName);
    m.setSubject(subject);
    m.setBody(body.length() > 1900 ? body.substring(0, 1900) : body);
    m.setKind(kind);
    m.setPlate(plate);
    personas.recibe(m);
    personas.firma(m, author);
    mails.save(m);
    if (correo.listo() && toEmail != null && toEmail.contains("@") && !TALLER.equalsIgnoreCase(toEmail)) {
      try {
        correo.enviar(toEmail, subject, body);
      } catch (RuntimeException ignored) {

      }
    }
    return m;
  }

  private String ownerEmail(Vehicle vehicle) {
    if (vehicle.getOwner() == null) return TALLER;
    return vehicle.getOwner().getEmail();
  }

  private String ownerName(Vehicle vehicle) {
    if (vehicle.getOwner() == null) return "Taller CarOs";
    return vehicle.getOwner().getDisplayName();
  }
}
