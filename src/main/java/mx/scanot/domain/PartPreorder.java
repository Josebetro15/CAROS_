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
@Table(name = "preordenes")
public class PartPreorder {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(optional = false) @JoinColumn(name = "vehiculo_id") private Vehicle vehicle;
  @Column(name = "codigo", nullable = false) private String code;
  @Column(name = "sku", nullable = false) private String sku;
  @Column(name = "nombre_refaccion", nullable = false) private String partName;
  @Column(name = "nombre_proveedor", nullable = false) private String supplierName;
  @Column(name = "precio", nullable = false) private BigDecimal price;
  @Column(name = "dias_entrega", nullable = false) private Integer leadDays;
  @Column(name = "llegada", nullable = false) private Instant eta;
  @Column(name = "folio", nullable = false) private String folio;
  @Column(name = "de_almacen", nullable = false) private boolean fromStock;
  @Column(name = "creado_en", nullable = false) private Instant createdAt = Instant.now();

  public Long getId() { return id; }
  public Vehicle getVehicle() { return vehicle; }
  public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public String getPartName() { return partName; }
  public void setPartName(String partName) { this.partName = partName; }
  public String getSupplierName() { return supplierName; }
  public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public Integer getLeadDays() { return leadDays; }
  public void setLeadDays(Integer leadDays) { this.leadDays = leadDays; }
  public Instant getEta() { return eta; }
  public void setEta(Instant eta) { this.eta = eta; }
  public String getFolio() { return folio; }
  public void setFolio(String folio) { this.folio = folio; }
  public boolean isFromStock() { return fromStock; }
  public void setFromStock(boolean fromStock) { this.fromStock = fromStock; }
  public Instant getCreatedAt() { return createdAt; }
}
