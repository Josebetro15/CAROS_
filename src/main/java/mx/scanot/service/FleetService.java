package mx.scanot.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mx.scanot.domain.Part;
import mx.scanot.domain.PartPreorder;
import mx.scanot.domain.SupplierQuote;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.Vehicle;
import mx.scanot.domain.WorkOrder;
import mx.scanot.repo.PartPreorderRepo;
import mx.scanot.repo.PartRepo;
import mx.scanot.repo.SupplierQuoteRepo;
import mx.scanot.repo.WorkOrderRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FleetService {
  public record Planned(String code, String sku, String part, String supplier, BigDecimal price,
                        int leadDays, String folio, Instant eta, boolean fromStock) {}

  private static final String WAREHOUSE = "Almacen CarOs";

  private final PartRepo parts;
  private final SupplierQuoteRepo quotes;
  private final PartPreorderRepo preorders;
  private final WorkOrderRepo orders;
  private final MailboxService mailbox;
  private final Personas personas;
  private final Folios folios;

  public FleetService(PartRepo parts, SupplierQuoteRepo quotes, PartPreorderRepo preorders, WorkOrderRepo orders,
                      MailboxService mailbox, Personas personas, Folios folios) {
    this.parts = parts;
    this.quotes = quotes;
    this.preorders = preorders;
    this.orders = orders;
    this.mailbox = mailbox;
    this.personas = personas;
    this.folios = folios;
  }

  @Transactional
  public List<Planned> plan(Vehicle vehicle, List<String> codes, Usuario actor) {
    List<Planned> out = new ArrayList<>();
    for (String code : codes) {
      if (preorders.existsByVehicleAndCode(vehicle, code)) continue;
      DtcCatalog.Info info = DtcCatalog.of(code);
      Part part = info.sku().isBlank() ? null : parts.findBySku(info.sku()).orElse(null);
      if (part == null) {
        out.add(pendiente(vehicle, code, info.sku().isBlank() ? "PENDIENTE" : info.sku(),
            info.sku().isBlank()
                ? "El código " + code + " no está en el catálogo. Hay que cotizar la pieza."
                : "El código " + code + " pide " + info.sku() + ", pero esa pieza no está en el inventario.",
            actor));
        continue;
      }
      Offer best = bestOffer(part);
      if (best == null) {
        out.add(pendiente(vehicle, code, part.getSku(),
            "Hay pieza " + part.getSku() + ", pero no tiene existencia ni cotización de proveedor.", actor));
        continue;
      }
      if (best.fromStock) {
        part.setStock(Math.max(0, part.getStock() - 1));
        parts.save(part);
      }
      Instant eta = Instant.now().plus(best.leadDays, ChronoUnit.DAYS);
      String folio = folios.nuevo();
      WorkOrder ot = nueva(vehicle, code, part.getName(), best.supplier, best.price, best.leadDays, eta, folio, actor);
      ot.setStatus(best.leadDays == 0 ? "pending" : "esperando_refaccion");
      ot.setWorkDesc(code + " · " + part.getName()
          + " · " + best.supplier + " $" + best.price
          + (best.leadDays == 0 ? " · entra hoy" : " · entra en " + best.leadDays + " días"));
      orders.save(ot);
      guardarPreorden(vehicle, code, part.getSku(), part.getName(), best.supplier, best.price, best.leadDays, eta, folio, best.fromStock);
      mailbox.notifyPreorder(vehicle, code, part.getName(), best.supplier, best.price.toPlainString(), best.leadDays, folio);
      out.add(new Planned(code, part.getSku(), part.getName(), best.supplier, best.price,
          best.leadDays, folio, eta, best.fromStock));
    }
    return out;
  }

  private Planned pendiente(Vehicle vehicle, String code, String sku, String texto, Usuario actor) {
    Instant eta = Instant.now();
    String folio = folios.nuevo();
    WorkOrder ot = nueva(vehicle, code, "Por confirmar", "Por confirmar", BigDecimal.ZERO, 0, eta, folio, actor);
    ot.setStatus("pending");
    ot.setWorkDesc(texto);
    orders.save(ot);
    guardarPreorden(vehicle, code, sku, "Por confirmar", "Por confirmar", BigDecimal.ZERO, 0, eta, folio, false);
    mailbox.notifyPreorder(vehicle, code, "pieza por confirmar (" + code + ")", "el taller", "0", 0, folio);
    return new Planned(code, sku, "Por confirmar", "Por confirmar", BigDecimal.ZERO, 0, folio, eta, false);
  }

  private WorkOrder nueva(Vehicle vehicle, String code, String part, String supplier, BigDecimal price,
                          int leadDays, Instant eta, String folio, Usuario actor) {
    WorkOrder ot = new WorkOrder();
    ot.setFolio(folio);
    ot.setVehicle(vehicle);
    ot.setTechnician(techFor(code));
    ot.setHours(new BigDecimal(leadDays == 0 ? "1.5" : "2.0"));
    ot.setPartName(part);
    ot.setSupplierName(supplier);
    ot.setLeadDays(leadDays);
    ot.setScheduledAt(eta);
    personas.vigila(ot, actor);
    if (actor != null && actor.getRol() == mx.scanot.domain.Rol.MECANICO) {
      ot.setTechnician(actor.getDisplayName());
    }
    return ot;
  }

  private void guardarPreorden(Vehicle vehicle, String code, String sku, String part, String supplier,
                               BigDecimal price, int leadDays, Instant eta, String folio, boolean fromStock) {
    PartPreorder pre = new PartPreorder();
    pre.setVehicle(vehicle);
    pre.setCode(code);
    pre.setSku(sku);
    pre.setPartName(part);
    pre.setSupplierName(supplier);
    pre.setPrice(price);
    pre.setLeadDays(leadDays);
    pre.setEta(eta);
    pre.setFolio(folio);
    pre.setFromStock(fromStock);
    preorders.save(pre);
  }

  private Offer bestOffer(Part part) {
    List<Offer> options = new ArrayList<>();
    if (part.getStock() != null && part.getStock() > 0) {
      options.add(new Offer(WAREHOUSE, part.getPrice(), 0, true));
    }
    for (SupplierQuote q : quotes.findByPartIdOrderByPriceAsc(part.getId())) {
      options.add(new Offer(q.getSupplier().getName(), q.getPrice(), q.getLeadDays(), false));
    }
    return options.stream()
        .min(Comparator.comparing(Offer::price).thenComparingInt(Offer::leadDays))
        .orElse(null);
  }

  private static String techFor(String code) {
    String[] techs = {"Rafael", "Guillermo", "Saul", "Alberto"};
    return techs[Math.floorMod(code.hashCode(), techs.length)];
  }

  private record Offer(String supplier, BigDecimal price, int leadDays, boolean fromStock) {}
}
