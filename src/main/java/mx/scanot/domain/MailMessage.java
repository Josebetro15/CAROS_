package mx.scanot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "correos")
public class MailMessage {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "clave", nullable = false, unique = true) private String refKey;
  @Column(name = "correo_destino", nullable = false) private String toEmail;
  @Column(name = "nombre_destino", nullable = false) private String toName;
  @Column(name = "asunto", nullable = false) private String subject;
  @Column(name = "cuerpo", nullable = false, length = 2000) private String body;
  @Column(name = "tipo", nullable = false) private String kind;
  @Column(name = "placa") private String plate;
  @Column(name = "creado_en") private Instant createdAt = Instant.now();
  @ManyToOne @JoinColumn(name = "cliente_id") private Cliente cliente;
  @ManyToOne @JoinColumn(name = "mecanico_id") private Mecanico mecanico;
  @ManyToOne @JoinColumn(name = "encargado_id") private Encargado encargado;

  public Long getId() { return id; }
  public String getRefKey() { return refKey; }
  public void setRefKey(String refKey) { this.refKey = refKey; }
  public String getToEmail() { return toEmail; }
  public void setToEmail(String toEmail) { this.toEmail = toEmail; }
  public String getToName() { return toName; }
  public void setToName(String toName) { this.toName = toName; }
  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }
  public String getBody() { return body; }
  public void setBody(String body) { this.body = body; }
  public String getKind() { return kind; }
  public void setKind(String kind) { this.kind = kind; }
  public String getPlate() { return plate; }
  public void setPlate(String plate) { this.plate = plate; }
  public Instant getCreatedAt() { return createdAt; }
  public Cliente getCliente() { return cliente; }
  public void setCliente(Cliente cliente) { this.cliente = cliente; }
  public Mecanico getMecanico() { return mecanico; }
  public void setMecanico(Mecanico mecanico) { this.mecanico = mecanico; }
  public Encargado getEncargado() { return encargado; }
  public void setEncargado(Encargado encargado) { this.encargado = encargado; }
}
