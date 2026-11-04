package mx.scanot.service;

import java.time.Year;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class Folios {
  public String nuevo() {
    String corto = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    return "OT-" + Year.now().getValue() + "-" + corto;
  }
}
