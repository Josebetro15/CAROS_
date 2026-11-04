package mx.scanot.web;

import java.math.BigDecimal;
import java.time.Instant;
import mx.scanot.domain.Part;
import mx.scanot.domain.Rol;
import mx.scanot.domain.Signal;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.WorkOrder;
import mx.scanot.repo.PartPreorderRepo;
import mx.scanot.repo.PartRepo;
import mx.scanot.repo.SignalRepo;
import mx.scanot.domain.Supplier;
import mx.scanot.domain.SupplierQuote;
import mx.scanot.repo.SupplierQuoteRepo;
import mx.scanot.repo.SupplierRepo;
import mx.scanot.repo.VehicleRepo;
import mx.scanot.repo.WorkOrderRepo;
import mx.scanot.domain.Mecanico;
import mx.scanot.repo.MecanicoRepo;
import mx.scanot.service.Folios;
import mx.scanot.service.Personas;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/app")
public class TallerController {
  private final SignalRepo signals;
  private final PartRepo parts;
  private final WorkOrderRepo orders;
  private final VehicleRepo vehicles;
  private final SupplierQuoteRepo quotes;
  private final PartPreorderRepo preorders;
  private final SupplierRepo suppliers;
  private final Personas personas;
  private final Folios folios;
  private final MecanicoRepo mecanicos;

  public TallerController(SignalRepo signals, PartRepo parts, WorkOrderRepo orders, VehicleRepo vehicles,
                          SupplierQuoteRepo quotes, PartPreorderRepo preorders, SupplierRepo suppliers, Personas personas,
                          Folios folios, MecanicoRepo mecanicos) {
    this.signals = signals;
    this.parts = parts;
    this.orders = orders;
    this.vehicles = vehicles;
    this.quotes = quotes;
    this.preorders = preorders;
    this.suppliers = suppliers;
    this.personas = personas;
    this.folios = folios;
    this.mecanicos = mecanicos;
  }

  @GetMapping("/diagnosticos")
  public String diagnosticos(@AuthenticationPrincipal Usuario user, Model model) {
    staff(user);
    model.addAttribute("user", user);
    model.addAttribute("signals", signals.findAllByOrderByIdDesc());
    return "app/diagnosticos";
  }

  @PostMapping("/diagnosticos/{id}/cerrar")
  public String cerrar(@PathVariable Long id, @AuthenticationPrincipal Usuario user) {
    staff(user);
    Signal s = signals.findById(id).orElseThrow();
    s.setStatus("closed");
    s.setClosedAt(Instant.now());
    signals.save(s);
    return "redirect:/app/diagnosticos";
  }

  @GetMapping("/partes")
  public String partes(@AuthenticationPrincipal Usuario user, Model model) {
    model.addAttribute("user", user);
    model.addAttribute("parts", parts.findAll());
    model.addAttribute("quotes", quotes.findAll());
    model.addAttribute("orders", orders.findAllByOrderByIdDesc());
    model.addAttribute("suppliers", suppliers.findAll());
    return "app/partes";
  }

  @PostMapping("/partes")
  public String altaParte(@AuthenticationPrincipal Usuario user,
                          @RequestParam String sku,
                          @RequestParam String name,
                          @RequestParam String category,
                          @RequestParam String price,
                          @RequestParam Integer stock,
                          @RequestParam String compatibility,
                          @RequestParam(required = false) String supplierName,
                          @RequestParam(required = false) String supplierCity,
                          @RequestParam(required = false) String quotePrice,
                          @RequestParam(required = false) Integer leadDays,
                          RedirectAttributes ra) {
    staff(user);
    try {
      String clave = sku == null ? "" : sku.trim().toUpperCase();
      String nombre = limpio(name);
      String categoria = limpio(category);
      if (!clave.matches("[A-Z0-9-]{2,30}")) {
        throw new IllegalArgumentException("El SKU debe tener de 2 a 30 letras, números o guiones.");
      }
      if (parts.findBySku(clave).isPresent()) {
        throw new IllegalArgumentException("Ese SKU ya está en el inventario.");
      }
      if (nombre.length() < 2) throw new IllegalArgumentException("Escribe el nombre de la pieza.");
      if (categoria.length() < 2) throw new IllegalArgumentException("Escribe la categoría.");
      if (stock == null || stock < 0) throw new IllegalArgumentException("La existencia no puede ser negativa.");
      Part part = new Part();
      part.setSku(clave);
      part.setName(nombre);
      part.setCategory(categoria);
      part.setPrice(dinero(price));
      part.setStock(stock);
      part.setCompatibility(compatibility == null ? "" : compatibility.trim());
      parts.save(part);
      ligarProveedor(part, supplierName, supplierCity, quotePrice, leadDays);
      ra.addFlashAttribute("ok", "Pieza " + part.getSku() + " agregada al inventario.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/app/partes";
  }

  @PostMapping("/partes/proveedor")
  public String editarProveedor(@AuthenticationPrincipal Usuario user,
                                @RequestParam Long supplierId,
                                @RequestParam String supplierName,
                                @RequestParam String supplierCity,
                                RedirectAttributes ra) {
    staff(user);
    try {
      Supplier s = suppliers.findById(supplierId)
          .orElseThrow(() -> new IllegalArgumentException("Ese proveedor no existe."));
      guardarProveedor(s, supplierName, supplierCity);
      ra.addFlashAttribute("ok", "Proveedor actualizado.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/app/partes";
  }

  @PostMapping("/partes/cotizacion")
  public String editarCotizacion(@AuthenticationPrincipal Usuario user,
                                 @RequestParam Long quoteId,
                                 @RequestParam String supplierName,
                                 @RequestParam String supplierCity,
                                 @RequestParam String price,
                                 @RequestParam Integer leadDays,
                                 RedirectAttributes ra) {
    staff(user);
    try {
      SupplierQuote q = quotes.findById(quoteId)
          .orElseThrow(() -> new IllegalArgumentException("Esa cotización no existe."));
      if (leadDays == null || leadDays < 0) {
        throw new IllegalArgumentException("Los días de entrega no pueden ser negativos.");
      }
      guardarProveedor(q.getSupplier(), supplierName, supplierCity);
      q.setPrice(dinero(price));
      q.setLeadDays(leadDays);
      quotes.save(q);
      ra.addFlashAttribute("ok", "Proveedor y cotización actualizados.");
      return "redirect:/app/partes/" + q.getPart().getId();
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/app/partes";
    }
  }

  @GetMapping("/partes/{id}")
  public String verParte(@PathVariable Long id, @AuthenticationPrincipal Usuario user, Model model) {
    staff(user);
    Part part = parts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    model.addAttribute("user", user);
    model.addAttribute("part", part);
    model.addAttribute("quotes", quotes.findByPartIdOrderByPriceAsc(id));
    return "app/parte";
  }

  @PostMapping("/partes/{id}")
  public String actualizarParte(@PathVariable Long id,
                                @AuthenticationPrincipal Usuario user,
                                @RequestParam String sku,
                                @RequestParam String name,
                                @RequestParam String category,
                                @RequestParam String price,
                                @RequestParam Integer stock,
                                @RequestParam String compatibility,
                                @RequestParam(required = false) String supplierName,
                                @RequestParam(required = false) String supplierCity,
                                @RequestParam(required = false) String quotePrice,
                                @RequestParam(required = false) Integer leadDays,
                                RedirectAttributes ra) {
    staff(user);
    try {
      Part part = parts.findById(id).orElseThrow(() -> new IllegalArgumentException("Esa pieza no existe."));
      String clave = sku == null ? "" : sku.trim().toUpperCase();
      String nombre = limpio(name);
      String categoria = limpio(category);
      if (!clave.matches("[A-Z0-9-]{2,30}")) {
        throw new IllegalArgumentException("El SKU debe tener de 2 a 30 letras, números o guiones.");
      }
      parts.findBySku(clave).filter(otro -> !otro.getId().equals(part.getId()))
          .ifPresent(otro -> {
            throw new IllegalArgumentException("Ese SKU ya está en el inventario.");
          });
      if (nombre.length() < 2) throw new IllegalArgumentException("Escribe el nombre de la pieza.");
      if (categoria.length() < 2) throw new IllegalArgumentException("Escribe la categoría.");
      if (stock == null || stock < 0) throw new IllegalArgumentException("La existencia no puede ser negativa.");
      part.setSku(clave);
      part.setName(nombre);
      part.setCategory(categoria);
      part.setPrice(dinero(price));
      part.setStock(stock);
      part.setCompatibility(compatibility == null ? "" : compatibility.trim());
      parts.save(part);
      ligarProveedor(part, supplierName, supplierCity, quotePrice, leadDays);
      ra.addFlashAttribute("ok", "Pieza " + part.getSku() + " actualizada.");
      return "redirect:/app/partes";
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/app/partes/" + id;
    }
  }

  @Transactional
  @PostMapping("/partes/{id}/eliminar")
  public String eliminarParte(@PathVariable Long id, @AuthenticationPrincipal Usuario user, RedirectAttributes ra) {
    staff(user);
    try {
      Part part = parts.findById(id).orElseThrow(() -> new IllegalArgumentException("Esa pieza no existe."));
      quotes.deleteAll(quotes.findByPartIdOrderByPriceAsc(id));
      parts.delete(part);
      ra.addFlashAttribute("ok", "Pieza " + part.getSku() + " eliminada.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    } catch (DataIntegrityViolationException e) {
      TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
      ra.addFlashAttribute("error", "No se puede eliminar: la pieza sigue ligada a otra tabla.");
      return "redirect:/app/partes/" + id;
    }
    return "redirect:/app/partes";
  }

  @GetMapping("/preordenes")
  public String preordenes(@AuthenticationPrincipal Usuario user, Model model) {
    staff(user);
    model.addAttribute("user", user);
    model.addAttribute("preorders", preorders.findAllByOrderByIdDesc());
    return "app/preordenes";
  }

  @GetMapping("/ordenes")
  public String ordenes(@AuthenticationPrincipal Usuario user, Model model) {
    staff(user);
    model.addAttribute("user", user);
    model.addAttribute("orders", orders.findAllByOrderByIdDesc());
    model.addAttribute("vehicles", vehicles.findAll());
    model.addAttribute("parts", parts.findAll());
    model.addAttribute("mecanicos", mecanicos.findAll());
    model.addAttribute("soyMecanico", user.getRol() == Rol.MECANICO);
    return "app/ordenes";
  }

  @PostMapping("/ordenes")
  public String crearOt(@AuthenticationPrincipal Usuario user,
                        @RequestParam Long vehicleId,
                        @RequestParam String workDesc,
                        @RequestParam(required = false) Long partId,
                        @RequestParam(required = false) Long mechanicId,
                        @RequestParam(required = false) String hours,
                        RedirectAttributes ra) {
    staff(user);
    try {
      if (workDesc == null || workDesc.trim().length() < 3) {
        throw new IllegalArgumentException("Describe el trabajo, mínimo 3 letras.");
      }
      if (workDesc.trim().length() > 255) {
        throw new IllegalArgumentException("La descripción no puede pasar de 255 letras.");
      }
      BigDecimal horas = horasDe(hours);
      WorkOrder o = new WorkOrder();
      o.setFolio(folios.nuevo());
      o.setVehicle(vehicles.findById(vehicleId).orElseThrow(() -> new IllegalArgumentException("Elige un vehículo.")));
      o.setWorkDesc(workDesc.trim());
      o.setHours(horas);
      o.setStatus("pending");
      if (user.getRol() == Rol.MECANICO) {
        o.setTechnician(user.getDisplayName());
        o.setMecanico(mecanicos.getReferenceById(user.getId()));
      } else {
        Mecanico m = mecanicos.findById(mechanicId == null ? -1 : mechanicId)
            .orElseThrow(() -> new IllegalArgumentException("Elige al mecánico."));
        o.setTechnician(m.getDisplayName());
        o.setMecanico(m);
      }
      if (partId != null) {
        parts.findById(partId).ifPresent(p -> o.setPartName(p.getSku() + " · " + p.getName()));
      }
      personas.vigila(o, user);
      orders.save(o);
      ra.addFlashAttribute("ok", "Orden " + o.getFolio() + " creada.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/app/ordenes";
  }

  @GetMapping("/flujo")
  public String flujo(@AuthenticationPrincipal Usuario user, Model model) {
    if (user.getRol() != Rol.ENCARGADO) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    model.addAttribute("user", user);
    model.addAttribute("orders", orders.findAllByOrderByIdDesc());
    return "app/flujo";
  }

  @PostMapping("/flujo/{id}/avanzar")
  public String avanzar(@PathVariable Long id, @AuthenticationPrincipal Usuario user) {
    if (user.getRol() != Rol.ENCARGADO) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    WorkOrder o = orders.findById(id).orElseThrow();
    if ("esperando_refaccion".equals(o.getStatus())) o.setStatus("pending");
    else if ("pending".equals(o.getStatus())) o.setStatus("in_progress");
    else if ("in_progress".equals(o.getStatus())) o.setStatus("calidad");
    else if ("calidad".equals(o.getStatus())) o.setStatus("facturado");
    else if ("facturado".equals(o.getStatus())) o.setStatus("done");
    personas.vigila(o, user);
    orders.save(o);
    return "redirect:/app/flujo";
  }

  private void ligarProveedor(Part part, String supplierName, String supplierCity, String quotePrice, Integer leadDays) {
    if (supplierName == null || supplierName.isBlank()) return;
    String ciudad = supplierCity == null ? "" : supplierCity.trim();
    if (ciudad.length() < 2) throw new IllegalArgumentException("Si pones proveedor, también escribe la ciudad.");
    Supplier s = suppliers.findByName(limpio(supplierName)).orElseGet(Supplier::new);
    guardarProveedor(s, supplierName, ciudad);
    if (quotePrice != null && !quotePrice.isBlank()) {
      if (leadDays == null || leadDays < 0) {
        throw new IllegalArgumentException("Los días de entrega no pueden ser negativos.");
      }
      BigDecimal precio = dinero(quotePrice);
      int dias = leadDays;
      SupplierQuote existente = quotes.findByPartIdOrderByPriceAsc(part.getId()).stream()
          .filter(q -> q.getSupplier().getId().equals(s.getId()))
          .findFirst()
          .orElse(null);
      if (existente == null) {
        existente = new SupplierQuote();
        existente.setPart(part);
        existente.setSupplier(s);
      }
      existente.setPrice(precio);
      existente.setLeadDays(dias);
      quotes.save(existente);
    }
  }

  private void guardarProveedor(Supplier s, String name, String city) {
    String nombre = limpio(name);
    String ciudad = limpio(city);
    if (nombre.length() < 2) throw new IllegalArgumentException("Escribe el nombre del proveedor.");
    if (ciudad.length() < 2) throw new IllegalArgumentException("Escribe la ciudad del proveedor.");
    suppliers.findByName(nombre)
        .filter(otro -> s.getId() == null || !otro.getId().equals(s.getId()))
        .ifPresent(otro -> {
          throw new IllegalArgumentException("Ese proveedor ya existe.");
        });
    s.setName(nombre);
    s.setCity(ciudad);
    suppliers.save(s);
  }

  private static BigDecimal horasDe(String raw) {
    if (raw == null || raw.isBlank()) return new BigDecimal("1.5");
    try {
      BigDecimal n = new BigDecimal(raw.trim().replace(",", "."));
      if (n.compareTo(new BigDecimal("0.5")) < 0 || n.compareTo(new BigDecimal("40")) > 0) {
        throw new IllegalArgumentException("Las horas deben estar entre 0.5 y 40.");
      }
      return n;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Las horas deben ser un número.");
    }
  }

  private static String limpio(String value) {
    return value == null ? "" : value.trim();
  }

  private static BigDecimal dinero(String raw) {
    if (raw == null || raw.isBlank()) throw new IllegalArgumentException("Escribe el precio.");
    try {
      BigDecimal n = new BigDecimal(raw.trim().replace(",", "."));
      if (n.signum() < 0) throw new IllegalArgumentException("El precio no puede ser negativo.");
      return n;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("El precio debe ser un número.");
    }
  }

  private static void staff(Usuario user) {
    if (user.getRol() == Rol.CLIENTE) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
  }
}
