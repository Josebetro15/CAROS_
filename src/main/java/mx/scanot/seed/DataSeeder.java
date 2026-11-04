package mx.scanot.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import mx.scanot.domain.Cliente;
import mx.scanot.domain.Encargado;
import mx.scanot.domain.Mecanico;
import mx.scanot.domain.Part;
import mx.scanot.domain.Rol;
import mx.scanot.domain.Signal;
import mx.scanot.domain.Supplier;
import mx.scanot.domain.SupplierQuote;
import mx.scanot.domain.Usuario;
import mx.scanot.domain.Vehicle;
import mx.scanot.domain.WorkOrder;
import mx.scanot.repo.PartRepo;
import mx.scanot.repo.SignalRepo;
import mx.scanot.repo.SupplierQuoteRepo;
import mx.scanot.repo.SupplierRepo;
import mx.scanot.repo.UsuarioRepo;
import mx.scanot.repo.VehicleRepo;
import mx.scanot.repo.WorkOrderRepo;
import mx.scanot.service.FleetService;
import mx.scanot.service.MailboxService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {
  private final UsuarioRepo usuarios;
  private final VehicleRepo vehicles;
  private final PartRepo parts;
  private final SignalRepo signals;
  private final WorkOrderRepo orders;
  private final SupplierRepo suppliers;
  private final SupplierQuoteRepo quotes;
  private final FleetService fleet;
  private final MailboxService mailbox;
  private final HerenciaSchema herencia;
  private final PasswordEncoder encoder;

  public DataSeeder(UsuarioRepo usuarios, VehicleRepo vehicles, PartRepo parts,
                    SignalRepo signals, WorkOrderRepo orders, SupplierRepo suppliers,
                    SupplierQuoteRepo quotes, FleetService fleet, MailboxService mailbox,
                    HerenciaSchema herencia, PasswordEncoder encoder) {
    this.usuarios = usuarios;
    this.vehicles = vehicles;
    this.parts = parts;
    this.signals = signals;
    this.orders = orders;
    this.suppliers = suppliers;
    this.quotes = quotes;
    this.fleet = fleet;
    this.mailbox = mailbox;
    this.herencia = herencia;
    this.encoder = encoder;
  }

  @Override
  public void run(String... args) {
    herencia.aplicar();
    seedUsers();
    herencia.aplicar();
    boolean fresh = vehicles.count() == 0;
    if (fresh) {
      Vehicle versa = veh("NVX-18-42", "Nissan", "Versa", 2021, 64210, "3N1CN7AP8ML800011", 72);
      Vehicle beat = veh("CHT-09-77", "Chevrolet", "Beat", 2020, 88120, "3G1TA5AF8LL123456", 58);
      Vehicle hilux = veh("HLX-44-01", "Toyota", "Hilux", 2019, 124500, "8AJFZ29G5K0123456", 81);
      Vehicle jetta = veh("JTA-22-08", "Volkswagen", "Jetta", 2022, 31200, "3VW2B7AJ8NM045678", 91);
      usuarios.findByEmail("cliente@scanot.mx").ifPresent(u -> {
        if (u instanceof Cliente cliente) versa.setOwner(cliente);
        vehicles.save(versa);
      });
      part("IGN-04", "Bobina de encendido", "Encendido", "1860", 6, "Chevrolet Beat, Nissan Versa");
      part("O2S-21", "Sensor de oxigeno banco 1", "Emisiones", "1240", 4, "Nissan Versa");
      part("CAT-09", "Convertidor catalitico", "Emisiones", "8900", 0, "Toyota Hilux");
      part("ABS-12", "Sensor ABS rueda delantera", "Frenos", "980", 3, "Volkswagen Jetta");
      part("THR-03", "Termostato de refrigerante", "Enfriamiento", "540", 8, "Universal");
      part("OIL-5W30", "Aceite sintetico 5W-30 4L", "Lubricacion", "620", 18, "Universal");
      sig(beat, "P0300", "Falla de encendido aleatoria / multiple", "alta", "Revisar bobinas y bujias.", "open");
      sig(versa, "P0171", "Sistema demasiado pobre (banco 1)", "media", "Inspeccionar sensor O2.", "open");
      sig(hilux, "P0420", "Eficiencia del catalizador por debajo del umbral", "media", "Cotizar catalizador.", "open");
      sig(jetta, "C0035", "Circuito sensor ABS rueda delantera izquierda", "baja", "Diagnosticar arnes ABS.", "open");
      ot("OT-2026-0138", hilux, "Servicio 120 mil km cerrado", "Saul", "done", 3);
    }
    
    seedQuotes();
    Usuario encargado = usuarios.findByEmail("encargado@scanot.mx").orElse(null);
    for (Signal s : signals.findByStatusOrderByIdDesc("open")) {
      fleet.plan(s.getVehicle(), List.of(s.getCode()), encargado);
    }
    mailbox.syncPreorders();
    herencia.aplicar();
  }

  private void seedQuotes() {
    if (quotes.count() > 0) return;
    Supplier norte = supplier("RefaNorte", "Ecatepec");
    Supplier mx = supplier("AutoPartes MX", "Tlalnepantla");
    Supplier express = supplier("TallerExpress", "CDMX");
    quote("IGN-04", norte, "2100", 2);
    quote("IGN-04", mx, "1980", 4);
    quote("IGN-04", express, "2240", 1);
    quote("O2S-21", norte, "1490", 3);
    quote("O2S-21", mx, "1320", 5);
    quote("O2S-21", express, "1580", 1);
    quote("CAT-09", norte, "8200", 4);
    quote("CAT-09", mx, "9100", 2);
    quote("CAT-09", express, "8600", 6);
    quote("ABS-12", norte, "1150", 2);
    quote("ABS-12", mx, "1090", 3);
    quote("ABS-12", express, "1280", 1);
    quote("THR-03", norte, "690", 2);
    quote("THR-03", mx, "610", 4);
    quote("THR-03", express, "740", 1);
  }

  private Supplier supplier(String name, String city) {
    return suppliers.findByName(name).orElseGet(() -> {
      Supplier s = new Supplier();
      s.setName(name);
      s.setCity(city);
      return suppliers.save(s);
    });
  }

  private void quote(String sku, Supplier supplier, String price, int days) {
    Part part = parts.findBySku(sku).orElse(null);
    if (part == null) return;
    SupplierQuote q = new SupplierQuote();
    q.setPart(part);
    q.setSupplier(supplier);
    q.setPrice(new BigDecimal(price));
    q.setLeadDays(days);
    quotes.save(q);
  }

  private void seedUsers() {
    user("cliente@scanot.mx", "Cliente demo", Rol.CLIENTE);
    user("mecanico@scanot.mx", "Mecanico demo", Rol.MECANICO);
    user("encargado@scanot.mx", "Encargado demo", Rol.ENCARGADO);
    herencia.aplicar();
  }

  private void user(String email, String name, Rol rol) {
    if (usuarios.existsByEmail(email)) return;
    Usuario u = switch (rol) {
      case CLIENTE -> new Cliente();
      case MECANICO -> new Mecanico();
      case ENCARGADO -> new Encargado();
    };
    u.setEmail(email);
    u.setPassword(encoder.encode("scanot123"));
    u.setDisplayName(name);
    u.setRol(rol);
    usuarios.save(u);
  }

  private Vehicle veh(String plate, String brand, String model, int year, int km, String vin, int health) {
    Vehicle v = new Vehicle();
    v.setPlate(plate); v.setBrand(brand); v.setModel(model);
    v.setYear(year); v.setKm(km); v.setVin(vin); v.setHealth(health);
    return vehicles.save(v);
  }

  private void part(String sku, String name, String cat, String price, int stock, String compat) {
    Part p = new Part();
    p.setSku(sku); p.setName(name); p.setCategory(cat);
    p.setPrice(new BigDecimal(price)); p.setStock(stock); p.setCompatibility(compat);
    parts.save(p);
  }

  private void sig(Vehicle v, String code, String desc, String sev, String sug, String status) {
    Signal s = new Signal();
    s.setVehicle(v); s.setCode(code); s.setDescription(desc);
    s.setSeverity(sev); s.setSuggestion(sug); s.setStatus(status);
    signals.save(s);
  }

  private void ot(String folio, Vehicle v, String desc, String tech, String status, double hours) {
    WorkOrder o = new WorkOrder();
    o.setFolio(folio); o.setVehicle(v); o.setWorkDesc(desc); o.setTechnician(tech);
    o.setStatus(status); o.setHours(BigDecimal.valueOf(hours));
    o.setScheduledAt(Instant.now().minus(2, ChronoUnit.DAYS));
    orders.save(o);
  }
}
