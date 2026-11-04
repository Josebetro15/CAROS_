package mx.scanot.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import mx.scanot.api.dto.Dtos.PlanDto;
import mx.scanot.api.dto.Dtos.ScanInDto;
import mx.scanot.api.dto.Dtos.ScanLogDto;
import mx.scanot.api.dto.Dtos.ScanOutDto;
import mx.scanot.api.dto.Dtos.VehicleDto;
import mx.scanot.repo.ScanReadingRepo;
import mx.scanot.service.ScanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.LinkedHashMap;
import java.util.Map;
import mx.scanot.domain.Usuario;
import mx.scanot.repo.SignalRepo;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/scanner")
public class ApiScanController {
  private final ScanService scans;
  private final ScanReadingRepo readings;
  private final SignalRepo signals;
  private final ObjectMapper mapper;

  public ApiScanController(ScanService scans, ScanReadingRepo readings, SignalRepo signals, ObjectMapper mapper) {
    this.scans = scans;
    this.readings = readings;
    this.signals = signals;
    this.mapper = mapper;
  }

  @GetMapping("/estado")
  public Map<String, Object> estado() {
    var logs = readings.findTop20ByOrderByIdDesc();
    var last = logs.isEmpty() ? null : logs.get(0);
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("escaner", "en línea");
    body.put("mensaje", "Canal listo para el reporte del escáner.");
    body.put("ultimaLectura", last == null ? "sin lecturas" : last.getVin() + " · " + last.getKm() + " km");
    body.put("senalesAbiertas", signals.countByStatus("open"));
    body.put("alertas", signals.findByStatusOrderByIdDesc("open").stream().limit(5)
        .map(s -> s.getCode() + " · " + s.getVehicle().getPlate() + " · " + s.getSeverity())
        .toList());
    return body;
  }

  @GetMapping("/lecturas")
  public List<ScanLogDto> logs() {
    return readings.findTop20ByOrderByIdDesc().stream().map(ScanLogDto::of).toList();
  }

  @PostMapping
  public ScanOutDto json(@AuthenticationPrincipal Usuario user, @RequestBody ScanInDto body) throws Exception {
    if (body.vin() == null || body.vin().isBlank()) {
      throw new IllegalArgumentException("El reporte no trae VIN");
    }
    ScanService.Result r = scans.ingestText(mapper.writeValueAsString(body), "api-json", user);
    return out(r);
  }

  @PostMapping("/archivo")
  public ScanOutDto archivo(@AuthenticationPrincipal Usuario user, @RequestParam("file") MultipartFile file) throws Exception {
    ScanService.Result r = scans.ingestFile(file, user);
    return out(r);
  }

  private static ScanOutDto out(ScanService.Result r) {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault());
    List<PlanDto> planes = r.plans().stream()
        .map(p -> new PlanDto(p.code(), p.sku(), p.part(), p.supplier(), p.price().toPlainString(),
            p.leadDays(), p.folio(), fmt.format(p.eta()), p.fromStock()))
        .toList();
    return new ScanOutDto(VehicleDto.of(r.vehicle()), r.createdNew(), r.codes(), planes, r.avisos());
  }
}
