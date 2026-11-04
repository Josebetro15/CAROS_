package mx.scanot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "vehiculos")
public class Vehicle {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "placa", nullable = false, unique = true) private String plate;
  @Column(name = "marca", nullable = false) private String brand;
  @Column(name = "modelo", nullable = false) private String model;
  @Column(name = "anio", nullable = false) private Integer year;
  @Column(name = "km", nullable = false) private Integer km = 0;
  @Column(name = "vin", nullable = false, unique = true) private String vin;
  @Column(name = "salud", nullable = false) private Integer health = 100;
  @Column(name = "inventario_inicial") private String inventarioInicial = "";
  @ManyToOne @JoinColumn(name = "cliente_id") private Cliente owner;

  public Long getId() { return id; }
  public String getPlate() { return plate; }
  public void setPlate(String plate) { this.plate = plate; }
  public String getBrand() { return brand; }
  public void setBrand(String brand) { this.brand = brand; }
  public String getModel() { return model; }
  public void setModel(String model) { this.model = model; }
  public Integer getYear() { return year; }
  public void setYear(Integer year) { this.year = year; }
  public Integer getKm() { return km; }
  public void setKm(Integer km) { this.km = km; }
  public String getVin() { return vin; }
  public void setVin(String vin) { this.vin = vin; }
  public Integer getHealth() { return health; }
  public void setHealth(Integer health) { this.health = health; }
  public String getInventarioInicial() { return inventarioInicial; }
  public void setInventarioInicial(String inventarioInicial) { this.inventarioInicial = inventarioInicial; }
  public Cliente getOwner() { return owner; }
  public void setOwner(Cliente owner) { this.owner = owner; }
  public Long getOwnerUserId() { return owner == null ? null : owner.getId(); }
}
