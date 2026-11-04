package mx.scanot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "solicitudes_cuenta")
public class SolicitudCuenta {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "correo", nullable = false, unique = true)
  private String correo;

  @Column(name = "nombre", nullable = false)
  private String nombre;

  @Column(name = "contrasena", nullable = false)
  private String contrasena;

  @Enumerated(EnumType.STRING)
  @Column(name = "rol", nullable = false)
  private Rol rol;

  @Column(name = "codigo_correo", nullable = false, length = 6)
  private String codigoCorreo;

  @Column(name = "token_correo", nullable = false, unique = true)
  private String tokenCorreo;

  @Column(name = "codigo_acceso", length = 6)
  private String codigoAcceso;

  @Column(name = "correo_ok", nullable = false)
  private boolean correoOk;

  @Column(name = "intentos")
  private Integer intentos = 0;

  @Column(name = "reenvios")
  private Integer reenvios = 0;

  @Column(name = "creado_en", nullable = false)
  private Instant creadoEn = Instant.now();

  @Column(name = "expira_en", nullable = false)
  private Instant expiraEn;

  public Long getId() { return id; }
  public String getCorreo() { return correo; }
  public void setCorreo(String correo) { this.correo = correo; }
  public String getNombre() { return nombre; }
  public void setNombre(String nombre) { this.nombre = nombre; }
  public String getContrasena() { return contrasena; }
  public void setContrasena(String contrasena) { this.contrasena = contrasena; }
  public Rol getRol() { return rol; }
  public void setRol(Rol rol) { this.rol = rol; }
  public String getCodigoCorreo() { return codigoCorreo; }
  public void setCodigoCorreo(String codigoCorreo) { this.codigoCorreo = codigoCorreo; }
  public String getTokenCorreo() { return tokenCorreo; }
  public void setTokenCorreo(String tokenCorreo) { this.tokenCorreo = tokenCorreo; }
  public String getCodigoAcceso() { return codigoAcceso; }
  public void setCodigoAcceso(String codigoAcceso) { this.codigoAcceso = codigoAcceso; }
  public boolean isCorreoOk() { return correoOk; }
  public void setCorreoOk(boolean correoOk) { this.correoOk = correoOk; }
  public Integer getIntentos() { return intentos == null ? 0 : intentos; }
  public void setIntentos(Integer intentos) { this.intentos = intentos; }
  public Integer getReenvios() { return reenvios == null ? 0 : reenvios; }
  public void setReenvios(Integer reenvios) { this.reenvios = reenvios; }
  public Instant getCreadoEn() { return creadoEn; }
  public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
  public Instant getExpiraEn() { return expiraEn; }
  public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }

  public boolean pideAcceso() {
    return rol == Rol.MECANICO || rol == Rol.ENCARGADO;
  }
}
