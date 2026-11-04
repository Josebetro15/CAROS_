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
@Table(name = "lecturas")
public class ScanReading {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(optional = false) @JoinColumn(name = "vehiculo_id") private Vehicle vehicle;
  @Column(name = "vin", nullable = false) private String vin;
  @Column(name = "km", nullable = false) private Integer km;
  @Column(name = "origen", nullable = false) private String source;
  @Column(name = "codigos_dtc", nullable = false) private String dtcCodes = "";
  @Column(name = "unidad_nueva", nullable = false) private boolean createdNew;
  @Column(name = "creado_en") private Instant createdAt = Instant.now();
  @ManyToOne @JoinColumn(name = "mecanico_id") private Mecanico mecanico;
  @ManyToOne @JoinColumn(name = "encargado_id") private Encargado encargado;

  public Long getId() { return id; }
  public Vehicle getVehicle() { return vehicle; }
  public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
  public String getVin() { return vin; }
  public void setVin(String vin) { this.vin = vin; }
  public Integer getKm() { return km; }
  public void setKm(Integer km) { this.km = km; }
  public String getSource() { return source; }
  public void setSource(String source) { this.source = source; }
  public String getDtcCodes() { return dtcCodes; }
  public void setDtcCodes(String dtcCodes) { this.dtcCodes = dtcCodes; }
  public boolean isCreatedNew() { return createdNew; }
  public void setCreatedNew(boolean createdNew) { this.createdNew = createdNew; }
  public Instant getCreatedAt() { return createdAt; }
  public Mecanico getMecanico() { return mecanico; }
  public void setMecanico(Mecanico mecanico) { this.mecanico = mecanico; }
  public Encargado getEncargado() { return encargado; }
  public void setEncargado(Encargado encargado) { this.encargado = encargado; }
}
