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
@Table(name = "senales")
public class Signal {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(optional = false) @JoinColumn(name = "vehiculo_id") private Vehicle vehicle;
  @Column(name = "codigo", nullable = false) private String code;
  @Column(name = "descripcion", nullable = false) private String description;
  @Column(name = "gravedad", nullable = false) private String severity;
  @Column(name = "sugerencia", nullable = false) private String suggestion;
  @Column(name = "estado", nullable = false) private String status = "open";
  @Column(name = "creado_en") private Instant createdAt = Instant.now();
  @Column(name = "cerrado_en") private Instant closedAt;

  public Long getId() { return id; }
  public Vehicle getVehicle() { return vehicle; }
  public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  public String getSeverity() { return severity; }
  public void setSeverity(String severity) { this.severity = severity; }
  public String getSuggestion() { return suggestion; }
  public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getClosedAt() { return closedAt; }
  public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
