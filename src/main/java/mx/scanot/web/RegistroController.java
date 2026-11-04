package mx.scanot.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import mx.scanot.domain.Rol;
import mx.scanot.domain.SolicitudCuenta;
import mx.scanot.domain.Usuario;
import mx.scanot.service.GoogleAccounts;
import mx.scanot.service.RegistroService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

@Controller
@RequestMapping("/registro")
public class RegistroController {
  private final RegistroService registro;
  private final GoogleAccounts cuentas;

  public RegistroController(RegistroService registro, GoogleAccounts cuentas) {
    this.registro = registro;
    this.cuentas = cuentas;
  }

  @GetMapping
  public String formulario(Model model) {
    model.addAttribute("supervisor", registro.supervisor());
    return "registro";
  }

  @PostMapping
  public String crear(@RequestParam String nombre,
                      @RequestParam String correo,
                      @RequestParam String password,
                      @RequestParam String password2,
                      @RequestParam String rol,
                      RedirectAttributes ra) {
    try {
      SolicitudCuenta s = registro.solicitar(nombre, correo, password, password2, rol);
      String aviso = s.pideAcceso()
          ? "Te enviamos un código a tu correo. El código de acceso se envió al supervisor."
          : "Te enviamos un código y un enlace a tu correo.";
      ra.addFlashAttribute("ok", aviso);
      return "redirect:/registro/confirmar?correo=" + UriUtils.encodeQueryParam(s.getCorreo(), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/registro";
    }
  }

  @GetMapping("/confirmar")
  public String confirmar(@RequestParam(required = false) String correo, Model model, RedirectAttributes ra) {
    if (correo == null || correo.isBlank()) return "redirect:/registro";
    try {
      SolicitudCuenta s = registro.buscar(correo);
      model.addAttribute("solicitud", s);
      model.addAttribute("supervisor", registro.supervisor());
      model.addAttribute("rolTexto", RegistroService.etiqueta(s.getRol()));
      return "confirmar";
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/registro";
    }
  }

  @PostMapping("/confirmar")
  public String confirmarPost(@RequestParam String correo,
                              @RequestParam(required = false) String codigoCorreo,
                              @RequestParam(required = false) String codigoAcceso,
                              RedirectAttributes ra) {
    try {
      registro.confirmar(correo, codigoCorreo, codigoAcceso);
      ra.addFlashAttribute("ok", "Cuenta creada. Ya puedes entrar.");
      return "redirect:/login";
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/registro/confirmar?correo=" + UriUtils.encodeQueryParam(correo.trim().toLowerCase(), StandardCharsets.UTF_8);
    }
  }

  @PostMapping("/reenviar")
  public String reenviar(@RequestParam String correo, RedirectAttributes ra) {
    String destino = "redirect:/registro/confirmar?correo=" + UriUtils.encodeQueryParam(correo.trim().toLowerCase(), StandardCharsets.UTF_8);
    try {
      registro.reenviar(correo);
      ra.addFlashAttribute("ok", "Te reenviamos un código nuevo. El anterior ya no sirve. Revisa también el spam.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return destino;
  }

  @GetMapping("/google")
  public String google(HttpServletRequest request, Model model) {
    String correo = (String) request.getSession().getAttribute("googleCorreo");
    if (correo == null) return "redirect:/login";
    model.addAttribute("correo", correo);
    model.addAttribute("nombre", request.getSession().getAttribute("googleNombre"));
    model.addAttribute("supervisor", registro.supervisor());
    return "registro-google";
  }

  @PostMapping("/google")
  public String googleRol(HttpServletRequest request, HttpServletResponse response, @RequestParam String rol, RedirectAttributes ra) {
    String correo = (String) request.getSession().getAttribute("googleCorreo");
    String nombre = (String) request.getSession().getAttribute("googleNombre");
    if (correo == null || nombre == null) return "redirect:/login";
    try {
      Rol elegido = switch (rol == null ? "" : rol.trim().toUpperCase()) {
        case "CLIENTE" -> Rol.CLIENTE;
        case "MECANICO" -> Rol.MECANICO;
        case "ENCARGADO", "SUPERVISOR" -> Rol.ENCARGADO;
        default -> throw new IllegalArgumentException("Elige cliente, mecánico o supervisor.");
      };
      if (elegido == Rol.CLIENTE) {
        Usuario user = cuentas.crearCliente(correo, nombre);
        request.getSession().removeAttribute("googleCorreo");
        request.getSession().removeAttribute("googleNombre");
        var token = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(token);
        new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), request, response);
        return "redirect:/app";
      }
      registro.solicitarGoogle(nombre, correo, elegido);
      request.getSession().removeAttribute("googleCorreo");
      request.getSession().removeAttribute("googleNombre");
      ra.addFlashAttribute("ok", "Google ya confirmó tu correo. El código de acceso se envió al supervisor.");
      return "redirect:/registro/confirmar?correo=" + UriUtils.encodeQueryParam(correo, StandardCharsets.UTF_8);
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/registro/google";
    }
  }

  @GetMapping("/verificar")
  public String verificar(@RequestParam String token, RedirectAttributes ra) {
    try {
      SolicitudCuenta s = registro.porToken(token);
      if (!s.pideAcceso()) {
        registro.confirmar(s.getCorreo(), null, null);
        ra.addFlashAttribute("ok", "Correo confirmado. Ya puedes entrar.");
        return "redirect:/login";
      }
      ra.addFlashAttribute("ok", "Correo confirmado. Ahora escribe el código que recibió el supervisor.");
      return "redirect:/registro/confirmar?correo=" + UriUtils.encodeQueryParam(s.getCorreo(), StandardCharsets.UTF_8);
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/registro";
    }
  }
}
