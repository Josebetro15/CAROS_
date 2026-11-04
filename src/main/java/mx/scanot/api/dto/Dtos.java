package mx.scanot.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import mx.scanot.domain.Part;
import mx.scanot.domain.ScanReading;
import mx.scanot.domain.Signal;
import mx.scanot.domain.UpdateMedia;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.Vehicle;
import mx.scanot.domain.VehicleUpdate;
import mx.scanot.domain.WorkOrder;

public final class Dtos {
  private Dtos() {}

  public record MeDto(Long id, String email, String displayName, String rol) {
    public static MeDto of(Usuario u) {
      return new MeDto(u.getId(), u.getEmail(), u.getDisplayName(), u.getRol().name());
    }
  }

  public record VehicleDto(Long id, String plate, String brand, String model, Integer year,
                           Integer km, String vin, Integer health, Long ownerUserId) {
    public static VehicleDto of(Vehicle v) {
      return new VehicleDto(v.getId(), v.getPlate(), v.getBrand(), v.getModel(), v.getYear(),
          v.getKm(), v.getVin(), v.getHealth(), v.getOwnerUserId());
    }
  }

  public record SignalDto(Long id, Long vehicleId, String plate, String code, String description,
                          String severity, String suggestion, String status, Instant createdAt) {
    public static SignalDto of(Signal s) {
      return new SignalDto(s.getId(), s.getVehicle().getId(), s.getVehicle().getPlate(), s.getCode(),
          s.getDescription(), s.getSeverity(), s.getSuggestion(), s.getStatus(), s.getCreatedAt());
    }
  }

  public record PartDto(Long id, String sku, String name, String category, BigDecimal price,
                        Integer stock, String compatibility) {
    public static PartDto of(Part p) {
      return new PartDto(p.getId(), p.getSku(), p.getName(), p.getCategory(), p.getPrice(),
          p.getStock(), p.getCompatibility());
    }
  }

  public record OrderDto(Long id, String folio, Long vehicleId, String plate, String workDesc,
                         String technician, BigDecimal hours, String status, Instant scheduledAt) {
    public static OrderDto of(WorkOrder o) {
      return new OrderDto(o.getId(), o.getFolio(), o.getVehicle().getId(), o.getVehicle().getPlate(),
          o.getWorkDesc(), o.getTechnician(), o.getHours(), o.getStatus(), o.getScheduledAt());
    }
  }

  public record MediaDto(Long id, String type, String url) {
    public static MediaDto of(UpdateMedia m) {
      return new MediaDto(m.getId(), m.getMediaType(), m.getUrl());
    }
  }

  public record UpdateDto(Long id, Long vehicleId, String kind, String message, Long authorUserId,
                          Instant createdAt, List<MediaDto> media) {
    public static UpdateDto of(VehicleUpdate u) {
      return new UpdateDto(u.getId(), u.getVehicle().getId(), u.getKind(), u.getMessage(),
          u.getAuthorUserId(), u.getCreatedAt(), u.getMedia().stream().map(MediaDto::of).toList());
    }
  }

  public record ScanLogDto(Long id, String vin, String plate, Integer km, String source,
                           String dtcCodes, boolean createdNew, Instant createdAt) {
    public static ScanLogDto of(ScanReading r) {
      return new ScanLogDto(r.getId(), r.getVin(), r.getVehicle().getPlate(), r.getKm(),
          r.getSource(), r.getDtcCodes(), r.isCreatedNew(), r.getCreatedAt());
    }
  }

  public record ScanInDto(String vin, String plate, String brand, String model, Integer year,
                          Integer km, List<String> dtcs) {}

  public record PlanDto(String code, String sku, String part, String supplier, String price,
                       int leadDays, String folio, String eta, boolean fromStock) {}

  public record ScanOutDto(VehicleDto vehicle, boolean createdNew, List<String> codes, List<PlanDto> planes,
                          List<String> avisos) {}

  public record OrderInDto(Long vehicleId, String workDesc, String technician) {}

  public record AvanceInDto(String kind, String message) {}

  public record StatusInDto(String status) {}

  public record PreorderDto(Long id, String plate, String code, String sku, String part, String supplier,
                           String price, int leadDays, String folio, boolean fromStock) {
    public static PreorderDto of(mx.scanot.domain.PartPreorder p) {
      return new PreorderDto(p.getId(), p.getVehicle().getPlate(), p.getCode(), p.getSku(), p.getPartName(),
          p.getSupplierName(), p.getPrice().toPlainString(), p.getLeadDays(), p.getFolio(), p.isFromStock());
    }
  }
}
