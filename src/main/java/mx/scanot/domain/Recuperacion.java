package mx.scanot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "recuperaciones")
public class Recuperacion {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "usuario_id", nullable = false)
  private Long usuarioId;

  @Column(name = "token", nullable = false, unique = true)
  private String token;

  @Column(name = "creado_en", nullable = false)
  private Instant creadoEn = Instant.now();

  @Column(name = "expira_en", nullable = false)
  private Instant expiraEn;

  public Long getId() { return id; }
  public Long getUsuarioId() { return usuarioId; }
  public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }
  public Instant getCreadoEn() { return creadoEn; }
  public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
  public Instant getExpiraEn() { return expiraEn; }
  public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }
}
