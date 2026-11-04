package mx.scanot.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApiRootController {
  @GetMapping("/api")
  public Map<String, Object> index() {
    Map<String, String> endpoints = new LinkedHashMap<>();
    endpoints.put("GET /api/me", "Usuario actual");
    endpoints.put("GET /swagger-ui.html", "Probar las APIs");
    endpoints.put("GET /api/vehiculos", "Lista de unidades");
    endpoints.put("GET /api/vehiculos/{id}", "Ficha");
    endpoints.put("GET /api/vehiculos/{id}/actualizaciones", "Historial de avance");
    endpoints.put("POST /api/vehiculos/{id}/avance", "Publicar status (mecanico)");
    endpoints.put("POST /api/vehiculos/vincular", "Cliente vincula placa+VIN");
    endpoints.put("POST /api/scanner", "Alta/update por JSON del scanner");
    endpoints.put("POST /api/scanner/archivo", "Alta/update subiendo archivo");
    endpoints.put("GET /api/scanner/lecturas", "Ultimas lecturas");
    endpoints.put("GET /api/scanner/estado", "Conectividad del escaner y alertas abiertas");
    endpoints.put("GET /api/diagnosticos", "DTCs");
    endpoints.put("POST /api/diagnosticos/{id}/cerrar", "Cerrar senal");
    endpoints.put("GET /api/partes", "Inventario y cotizaciones");
    endpoints.put("GET /api/preordenes", "Preordenes al mejor precio");
    endpoints.put("GET /api/ordenes", "Ordenes de trabajo");
    endpoints.put("POST /api/ordenes", "Crear OT");
    endpoints.put("POST /api/ordenes/{id}/status", "Cambiar status de OT");
    endpoints.put("GET /api/correos", "Avisos al cliente");
    endpoints.put("GET /api/flujo", "Flujo del encargado");
    return Map.of(
        "app", "CarOs",
        "auth", "HTTP Basic  (ej. mecanico@scanot.mx / scanot123)",
        "endpoints", endpoints);
  }
}
