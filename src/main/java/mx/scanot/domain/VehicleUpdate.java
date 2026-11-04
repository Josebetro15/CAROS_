package mx.scanot.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "avances")
public class VehicleUpdate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(optional = false) @JoinColumn(name = "vehiculo_id") private Vehicle vehicle;
  @ManyToOne @JoinColumn(name = "mecanico_id") private Mecanico mecanico;
  @ManyToOne @JoinColumn(name = "encargado_id") private Encargado encargado;
  @Column(name = "tipo", nullable = false) private String kind;
  @Column(name = "mensaje", nullable = false, length = 2000) private String message;
  @Column(name = "creado_en") private Instant createdAt = Instant.now();
  @OneToMany(mappedBy = "update", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UpdateMedia> media = new ArrayList<>();

  public Long getId() { return id; }
  public Vehicle getVehicle() { return vehicle; }
  public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
  public Mecanico getMecanico() { return mecanico; }
  public void setMecanico(Mecanico mecanico) { this.mecanico = mecanico; }
  public Encargado getEncargado() { return encargado; }
  public void setEncargado(Encargado encargado) { this.encargado = encargado; }
  public Long getAuthorUserId() {
    if (mecanico != null) return mecanico.getId();
    return encargado == null ? null : encargado.getId();
  }
  public String getKind() { return kind; }
  public void setKind(String kind) { this.kind = kind; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public Instant getCreatedAt() { return createdAt; }
  public List<UpdateMedia> getMedia() { return media; }
}
