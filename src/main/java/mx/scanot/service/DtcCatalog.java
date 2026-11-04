package mx.scanot.service;

import java.util.Map;

public final class DtcCatalog {
  public record Info(String description, String severity, String suggestion, String sku) {}

  private static final Map<String, Info> CATALOG = Map.of(
      "P0300", new Info("Falla de encendido aleatoria o múltiple", "alta", "Revisar bobinas y bujías.", "IGN-04"),
      "P0301", new Info("Falla de encendido en el cilindro 1", "alta", "Probar la bobina y la bujía del cilindro 1.", "IGN-04"),
      "P0171", new Info("Mezcla demasiado pobre (banco 1)", "media", "Revisar el sensor de oxígeno y fugas de vacío.", "O2S-21"),
      "P0172", new Info("Mezcla demasiado rica (banco 1)", "media", "Revisar el sensor MAF y la presión de combustible.", "O2S-21"),
      "P0420", new Info("Eficiencia del catalizador por debajo del umbral", "media", "Cotizar el catalizador.", "CAT-09"),
      "C0035", new Info("Circuito del sensor ABS, rueda delantera izquierda", "baja", "Revisar el arnés y el sensor.", "ABS-12"),
      "P0128", new Info("Termostato: el motor no alcanza la temperatura", "baja", "Cambiar el termostato.", "THR-03")
  );

  private DtcCatalog() {}

  public static Info of(String code) {
    return CATALOG.getOrDefault(code.toUpperCase(),
        new Info("Codigo OBD " + code, "media", "Consultar manual de servicio.", ""));
  }
}
