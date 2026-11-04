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
@Table(name = "archivos")
public class UpdateMedia {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(optional = false) @JoinColumn(name = "avance_id") private VehicleUpdate update;
  @Column(name = "tipo_archivo", nullable = false) private String mediaType;
  @Column(name = "url", nullable = false) private String url;

  public Long getId() { return id; }
  public VehicleUpdate getUpdate() { return update; }
  public void setUpdate(VehicleUpdate update) { this.update = update; }
  public String getMediaType() { return mediaType; }
  public void setMediaType(String mediaType) { this.mediaType = mediaType; }
  public String getUrl() { return url; }
  public void setUrl(String url) { this.url = url; }
}
