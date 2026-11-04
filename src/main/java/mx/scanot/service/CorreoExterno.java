package mx.scanot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class CorreoExterno {
  private final JavaMailSender mail;

  @Value("${spring.mail.username:}")
  private String usuario;

  @Value("${spring.mail.password:}")
  private String contrasena;

  @Value("${caros.mail.from:}")
  private String desde;

  public CorreoExterno(JavaMailSender mail) {
    this.mail = mail;
  }

  public boolean listo() {
    return usuario != null && !usuario.isBlank() && contrasena != null && !contrasena.isBlank();
  }

  public void enviar(String destino, String asunto, String cuerpo) {
    if (!listo()) {
      throw new IllegalStateException(
          "Falta la contraseña de aplicación de Gmail en application.properties (spring.mail.password).");
    }
    try {
      SimpleMailMessage mensaje = new SimpleMailMessage();
      mensaje.setFrom(desde == null || desde.isBlank() ? usuario : desde);
      mensaje.setTo(destino);
      mensaje.setSubject(asunto);
      mensaje.setText(cuerpo);
      mail.send(mensaje);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "No se pudo enviar el correo a " + destino + ". Revisa la contraseña de aplicación de Gmail.");
    }
  }
}
