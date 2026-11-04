package mx.scanot.web;

import java.util.Map;
import org.springframework.stereotype.Component;

/** Textos que ve el usuario. Los códigos de la base no cambian. */
@Component("etiquetas")
public class Etiquetas {
  private static final Map<String, String> TEXTO = Map.ofEntries(
      Map.entry("open", "Abierta"),
      Map.entry("closed", "Cerrada"),
      Map.entry("pending", "Pendiente"),
      Map.entry("in_progress", "En taller"),
      Map.entry("calidad", "Calidad"),
      Map.entry("facturado", "Facturado"),
      Map.entry("done", "Entregada"),
      Map.entry("esperando_refaccion", "Esperando refacción"),
      Map.entry("alta", "Alta"),
      Map.entry("media", "Media"),
      Map.entry("baja", "Baja"),
      Map.entry("ingreso", "Ingreso"),
      Map.entry("diagnostico", "Diagnóstico"),
      Map.entry("piezas_pedidas", "Piezas encargadas"),
      Map.entry("en_reparacion", "En reparación"),
      Map.entry("listo", "Listo para entrega"),
      Map.entry("entregado", "Unidad entregada"),
      Map.entry("preorden", "Preorden"),
      Map.entry("aviso", "Aviso"),
      Map.entry("recuperacion", "Recuperación"),
      Map.entry("CLIENTE", "Cliente"),
      Map.entry("MECANICO", "Mecánico"),
      Map.entry("ENCARGADO", "Supervisor")
  );

  public String de(String codigo) {
    if (codigo == null || codigo.isBlank()) return "";
    return TEXTO.getOrDefault(codigo, codigo);
  }
}
