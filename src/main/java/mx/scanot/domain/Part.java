package mx.scanot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "refacciones")
public class Part {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @Column(name = "sku", nullable = false, unique = true) private String sku;
  @Column(name = "nombre", nullable = false) private String name;
  @Column(name = "categoria", nullable = false) private String category;
  @Column(name = "precio", nullable = false) private BigDecimal price;
  @Column(name = "existencia", nullable = false) private Integer stock = 0;
  @Column(name = "compatibilidad", nullable = false) private String compatibility = "";

  public Long getId() { return id; }
  public String getSku() { return sku; }
  public void setSku(String sku) { this.sku = sku; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getCategory() { return category; }
  public void setCategory(String category) { this.category = category; }
  public BigDecimal getPrice() { return price; }
  public void setPrice(BigDecimal price) { this.price = price; }
  public Integer getStock() { return stock; }
  public void setStock(Integer stock) { this.stock = stock; }
  public String getCompatibility() { return compatibility; }
  public void setCompatibility(String compatibility) { this.compatibility = compatibility; }
}
