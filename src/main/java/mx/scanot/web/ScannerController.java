package mx.scanot.web;

import mx.scanot.domain.Usuario;
import mx.scanot.repo.ScanReadingRepo;
import mx.scanot.service.ScanService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app/scanner")
public class ScannerController {
  private final ScanService scans;
  private final ScanReadingRepo readings;

  public ScannerController(ScanService scans, ScanReadingRepo readings) {
    this.scans = scans;
    this.readings = readings;
  }

  @GetMapping
  public String page(@AuthenticationPrincipal Usuario user, Model model) {
    model.addAttribute("user", user);
    model.addAttribute("logs", readings.findTop20ByOrderByIdDesc());
    return "app/scanner";
  }

  @PostMapping("/archivo")
  public String archivo(@AuthenticationPrincipal Usuario user, @RequestParam("file") MultipartFile file, RedirectAttributes ra) {
    if (file == null || file.isEmpty()) {
      ra.addFlashAttribute("error", "Selecciona un archivo.");
      return "redirect:/app/scanner";
    }
    try {
      ScanService.Result r = scans.ingestFile(file, user);
      String planes = r.plans().isEmpty()
          ? ""
          : r.plans().stream()
              .map(p -> p.code() + " → " + p.part() + " · " + p.supplier() + " · " + p.folio())
              .reduce((a, b) -> a + " | " + b).orElse("");
      String avisos = r.avisos().isEmpty() ? "" : String.join(" ", r.avisos());
      String extra = !planes.isBlank() ? planes : (!avisos.isBlank() ? avisos : "Esas fallas ya tenían orden. No se duplicó.");
      ra.addFlashAttribute("ok", (r.createdNew()
          ? "Unidad " + r.vehicle().getPlate() + " dada de alta. "
          : "Lectura de " + r.vehicle().getPlate() + ". ") + extra);
      return "redirect:/app/preordenes";
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    } catch (Exception e) {
      ra.addFlashAttribute("error", "No se pudo guardar la lectura. Revisa el VIN, la placa y que el archivo sea el reporte del escáner.");
    }
    return "redirect:/app/scanner";
  }

  @PostMapping("/foto")
  public String foto(@AuthenticationPrincipal Usuario user, @RequestParam("file") MultipartFile file, RedirectAttributes ra) {
    return archivo(user, file, ra);
  }
}
