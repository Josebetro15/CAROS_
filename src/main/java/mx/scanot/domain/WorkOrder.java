package mx.scanot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ordenes")
public class WorkOrder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "folio", nullable = false, unique = true) private String folio;
  @ManyToOne(optional = false) @JoinColumn(name = "vehiculo_id") private Vehicle vehicle;
  @Column(name = "descripcion", nullable = false) private String workDesc;
  @Column(name = "tecnico", nullable = false) private String technician;
  @Column(name = "programada_en") private Instant scheduledAt = Instant.now();
  @Column(name = "horas", nullable = false) private BigDecimal hours = new BigDecimal("1.0");
  @Column(name = "estado", nullable = false) private String status = "pending";
  @Column(name = "nombre_refaccion") private String partName;
  @Column(name = "nombre_proveedor") private String supplierName;
  @Column(name = "dias_entrega") private Integer leadDays;
  @ManyToOne @JoinColumn(name = "mecanico_id") private Mecanico mecanico;
  @ManyToOne @JoinColumn(name = "encargado_id") private Encargado encargado;

  public Long getId() { return id; }
  public String getFolio() { return folio; }
  public void setFolio(String folio) { this.folio = folio; }
  public Vehicle getVehicle() { return vehicle; }
  public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
  public String getWorkDesc() { return workDesc; }
  public void setWorkDesc(String workDesc) { this.workDesc = workDesc; }
  public String getTechnician() { return technician; }
  public void setTechnician(String technician) { this.technician = technician; }
  public Instant getScheduledAt() { return scheduledAt; }
  public void setScheduledAt(Instant scheduledAt) { this.scheduledAt = scheduledAt; }
  public BigDecimal getHours() { return hours; }
  public void setHours(BigDecimal hours) { this.hours = hours; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getPartName() { return partName; }
  public void setPartName(String partName) { this.partName = partName; }
  public String getSupplierName() { return supplierName; }
  public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
  public Integer getLeadDays() { return leadDays; }
  public void setLeadDays(Integer leadDays) { this.leadDays = leadDays; }
  public Mecanico getMecanico() { return mecanico; }
  public void setMecanico(Mecanico mecanico) { this.mecanico = mecanico; }
  public Encargado getEncargado() { return encargado; }
  public void setEncargado(Encargado encargado) { this.encargado = encargado; }
}
