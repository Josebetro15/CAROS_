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

@Entity
@Table(name = "cotizaciones")
public class SupplierQuote {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(optional = false) @JoinColumn(name = "refaccion_id") private Part part;
  @ManyToOne(optional = false) @JoinColumn(name = "proveedor_id") private Supplier supplier;
  @Column(name = "precio", nullable = false) private BigDecimal price;
  @Column(name = "dias_entrega", nullable = false) private Integer leadDays;

  public Long getId() { return id; }
  public Part getPart() { return part; }
  public void setPart(Part part) { this.part = part; }
  public Supplier getSupplier() { return supplier; }
  public void setSupplier(Supplier supplier) { this.supplier = supplier; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public Integer getLeadDays() { return leadDays; }
  public void setLeadDays(Integer leadDays) { this.leadDays = leadDays; }
}
