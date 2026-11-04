package mx.scanot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import mx.scanot.domain.ScanReading;
import mx.scanot.domain.Signal;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.Vehicle;
import mx.scanot.repo.ScanReadingRepo;
import mx.scanot.repo.SignalRepo;
import mx.scanot.repo.VehicleRepo;
import mx.scanot.service.FleetService.Planned;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ScanService {
  public record Result(Vehicle vehicle, boolean createdNew, List<String> codes, List<Planned> plans, List<String> avisos) {}

  private static final Pattern VIN = Pattern.compile("(?i)vin\\s*[:#]?\\s*([A-HJ-NPR-Z0-9]{11,17})\\b");
  private static final Pattern VIN_SUELTO = Pattern.compile("^[A-HJ-NPR-Z0-9]{11,17}$");
  private static final Pattern DTC = Pattern.compile("\\b([PCBU][0-9A-F]{4})\\b", Pattern.CASE_INSENSITIVE);

  private final VehicleRepo vehicles;
  private final SignalRepo signals;
  private final ScanReadingRepo readings;
  private final FleetService fleet;
  private final Personas personas;
  private final ObjectMapper json;

  public ScanService(VehicleRepo vehicles, SignalRepo signals, ScanReadingRepo readings,
                     FleetService fleet, Personas personas, ObjectMapper json) {
    this.vehicles = vehicles;
    this.signals = signals;
    this.readings = readings;
    this.fleet = fleet;
    this.personas = personas;
    this.json = json;
  }

  @Transactional
  public Result ingestText(String raw, String source, Usuario actor) {
    Parsed p = parse(raw);
    return apply(p, source, actor);
  }

  @Transactional
  public Result ingestFile(MultipartFile file, Usuario actor) throws Exception {
    String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
    String type = file.getContentType() == null ? "" : file.getContentType();
    if (type.startsWith("image/") || name.matches(".*\\.(png|jpe?g|webp)$")) {
      String qr = readQr(file.getInputStream());
      if (qr == null || qr.isBlank()) {
        throw new IllegalArgumentException("No se leyó un código QR. Sube el archivo JSON del escáner.");
      }
      return ingestText(qr, "escaneo-qr:" + name, actor);
    }
    String text = new String(file.getBytes());
    return ingestText(text, "archivo:" + name, actor);
  }

  private String readQr(InputStream in) {
    try {
      BufferedImage img = ImageIO.read(in);
      if (img == null) return null;
      BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));
      return new MultiFormatReader().decode(bitmap).getText();
    } catch (Exception e) {
      return null;
    }
  }

  private Result apply(Parsed p, String source, Usuario actor) {
    boolean created = false;
    Vehicle v = vehicles.findByVin(p.vin).orElse(null);
    if (v == null) {
      String placa = placaLibre(p.plate, p.vin);
      v = new Vehicle();
      v.setVin(p.vin);
      v.setPlate(placa);
      v.setBrand(p.brand);
      v.setModel(p.model);
      v.setYear(p.year);
      v.setKm(p.km);
      v.setHealth(100);
      v = vehicles.save(v);
      created = true;
    } else {
      if (p.km >= 0) v.setKm(p.km);
      if (p.plate != null && !p.plate.isBlank()) {
        String placa = p.plate.trim().toUpperCase(Locale.ROOT);
        final Long id = v.getId();
        vehicles.findByPlate(placa)
            .filter(otro -> !otro.getId().equals(id))
            .ifPresent(otro -> {
              throw new IllegalArgumentException("Esa placa ya pertenece a otra unidad.");
            });
        v.setPlate(placa);
      }
      v = vehicles.save(v);
    }
    List<String> opened = new ArrayList<>();
    List<String> avisos = new ArrayList<>();
    for (String code : p.dtcs) {
      if (signals.existsByVehicleAndCodeAndStatus(v, code, "open")) continue;
      DtcCatalog.Info info = DtcCatalog.of(code);
      Signal s = new Signal();
      s.setVehicle(v);
      s.setCode(code);
      s.setDescription(info.description());
      s.setSeverity(info.severity());
      s.setSuggestion(info.suggestion());
      s.setStatus("open");
      signals.save(s);
      opened.add(code);
      if (info.sku().isBlank()) {
        avisos.add(code + " no está en el catálogo: se abrió la falla y una orden para cotizar la pieza.");
      }
    }
    if (created) v.setHealth(Math.max(40, 100 - opened.size() * 8));
    else if (!opened.isEmpty()) v.setHealth(Math.max(40, v.getHealth() - opened.size() * 2));
    vehicles.save(v);
    ScanReading log = new ScanReading();
    log.setVehicle(v);
    log.setVin(v.getVin());
    log.setKm(v.getKm());
    log.setSource(source);
    log.setDtcCodes(String.join(",", p.dtcs));
    log.setCreatedNew(created);
    personas.escanea(log, actor);
    readings.save(log);
    List<Planned> plans = fleet.plan(v, opened, actor);
    for (Planned plan : plans) {
      if ("Por confirmar".equals(plan.supplier()) && avisos.stream().noneMatch(a -> a.startsWith(plan.code()))) {
        avisos.add(plan.code() + ": no había pieza lista. Se abrió la orden " + plan.folio() + " para cotizarla.");
      }
    }
    return new Result(v, created, opened, plans, avisos);
  }

  private String placaLibre(String plate, String vin) {
    String base = plate == null ? "" : plate.trim().toUpperCase(Locale.ROOT);
    if (base.isBlank()) base = "TMP-" + vin.substring(Math.max(0, vin.length() - 6));
    if (!base.matches("[A-Z0-9-]{5,12}")) base = "TMP-" + vin.substring(Math.max(0, vin.length() - 6));
    String candidata = base.length() > 12 ? base.substring(0, 12) : base;
    int n = 1;
    while (vehicles.findByPlate(candidata).isPresent()) {
      String sufijo = "-" + n++;
      String recorte = base.length() + sufijo.length() > 12 ? base.substring(0, 12 - sufijo.length()) : base;
      candidata = recorte + sufijo;
    }
    return candidata;
  }

  private Parsed parse(String raw) {
    String trimmed = raw.trim();
    if (trimmed.startsWith("{")) return parseJson(trimmed);
    if (trimmed.contains(",") && trimmed.toLowerCase(Locale.ROOT).contains("vin")) return parseCsv(trimmed);
    return parsePlain(trimmed);
  }

  private Parsed parseJson(String raw) {
    try {
      JsonNode n = json.readTree(raw);
      List<String> dtcs = new ArrayList<>();
      JsonNode arr = n.get("dtcs");
      if (arr != null && arr.isArray()) {
        for (JsonNode x : arr) dtcs.add(x.asText().toUpperCase(Locale.ROOT));
      }
      String vin = text(n, "vin").toUpperCase(Locale.ROOT);
      if (!VIN_SUELTO.matcher(vin).matches()) throw new IllegalArgumentException("El VIN debe tener de 11 a 17 caracteres, sin I, O ni Q.");
      int year = n.has("year") ? n.get("year").asInt(0) : 2020;
      int km = n.has("km") ? n.get("km").asInt(-1) : 0;
      if (year < 1980 || year > 2035) throw new IllegalArgumentException("El año del reporte debe estar entre 1980 y 2035.");
      if (km < 0) throw new IllegalArgumentException("El kilometraje del reporte no puede ser negativo.");
      return new Parsed(
          vin,
          text(n, "plate"),
          or(text(n, "brand"), "Marca"),
          or(text(n, "model"), "Modelo"),
          year,
          km,
          codigos(dtcs));
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("El JSON no es válido.");
    }
  }

  private Parsed parseCsv(String raw) {
    String[] lines = raw.split("\\R");
    if (lines.length < 2) return parsePlain(raw);
    String[] headers = lines[0].split(",");
    String[] values = lines[1].split(",");
    java.util.Map<String, String> map = new java.util.HashMap<>();
    for (int i = 0; i < Math.min(headers.length, values.length); i++) {
      map.put(headers[i].trim().toLowerCase(Locale.ROOT), values[i].trim());
    }
    String vin = map.getOrDefault("vin", "").toUpperCase(Locale.ROOT);
    if (!VIN_SUELTO.matcher(vin).matches()) throw new IllegalArgumentException("El CSV no trae un VIN válido.");
    List<String> dtcs = new ArrayList<>();
    String d = map.getOrDefault("dtcs", map.getOrDefault("dtc", ""));
    for (String x : d.split("[;| ]+")) if (!x.isBlank()) dtcs.add(x.toUpperCase(Locale.ROOT));
    int year = parseInt(map.get("year"), 2020);
    int km = parseInt(map.get("km"), 0);
    if (year < 1980 || year > 2035) throw new IllegalArgumentException("El año del CSV debe estar entre 1980 y 2035.");
    if (km < 0) throw new IllegalArgumentException("El kilometraje del CSV no puede ser negativo.");
    return new Parsed(vin, map.getOrDefault("plate", ""),
        or(map.get("brand"), "Marca"), or(map.get("model"), "Modelo"),
        year, km, codigos(dtcs));
  }

  private Parsed parsePlain(String raw) {
    Matcher vm = VIN.matcher(raw);
    String vin;
    if (vm.find()) vin = vm.group(1).toUpperCase(Locale.ROOT);
    else if (VIN_SUELTO.matcher(raw.trim().toUpperCase(Locale.ROOT)).matches()) vin = raw.trim().toUpperCase(Locale.ROOT);
    else throw new IllegalArgumentException("No se encontró el VIN. El reporte debe decir VIN: y los 17 caracteres.");
    List<String> dtcs = new ArrayList<>();
    Matcher dm = DTC.matcher(raw);
    while (dm.find()) dtcs.add(dm.group(1).toUpperCase(Locale.ROOT));
    Matcher km = Pattern.compile("(\\d{3,})\\s*km", Pattern.CASE_INSENSITIVE).matcher(raw);
    int kilometers = km.find() ? Integer.parseInt(km.group(1)) : 0;
    return new Parsed(vin, "", guessBrand(vin), guessModel(vin), 2020, kilometers, dtcs);
  }

  private static List<String> codigos(List<String> crudos) {
    List<String> ok = new ArrayList<>();
    for (String code : crudos) {
      if (code != null && code.toUpperCase(Locale.ROOT).matches("[PCBU][0-9A-F]{4}")) ok.add(code.toUpperCase(Locale.ROOT));
    }
    return ok;
  }

  private static String text(JsonNode n, String k) {
    return n.has(k) && !n.get(k).isNull() ? n.get(k).asText("") : "";
  }
  private static String or(String v, String d) { return v == null || v.isBlank() ? d : v; }
  private static int parseInt(String v, int d) {
    try { return v == null || v.isBlank() ? d : Integer.parseInt(v.replaceAll("[^0-9]", "")); }
    catch (NumberFormatException e) { return d; }
  }
  private static String guessBrand(String vin) {
    if (vin.startsWith("1HG")) return "Honda";
    if (vin.startsWith("3N1")) return "Nissan";
    if (vin.startsWith("3G1")) return "Chevrolet";
    if (vin.startsWith("8AJ")) return "Toyota";
    if (vin.startsWith("3VW") || vin.startsWith("WVW")) return "Volkswagen";
    return "Marca";
  }
  private static String guessModel(String vin) {
    if (vin.startsWith("1HG")) return "Civic";
    if (vin.startsWith("3N1")) return "Versa";
    if (vin.startsWith("3G1")) return "Beat";
    if (vin.startsWith("8AJ")) return "Hilux";
    if (vin.startsWith("3VW")) return "Jetta";
    return "Modelo";
  }

  private record Parsed(String vin, String plate, String brand, String model, int year, int km, List<String> dtcs) {}
}
