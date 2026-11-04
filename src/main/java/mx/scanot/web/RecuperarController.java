package mx.scanot.web;

import mx.scanot.service.PasswordResetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RecuperarController {
  private final PasswordResetService resets;

  public RecuperarController(PasswordResetService resets) {
    this.resets = resets;
  }

  @GetMapping("/recuperar")
  public String formulario() {
    return "recuperar";
  }

  @PostMapping("/recuperar")
  public String pedir(@RequestParam String correo, RedirectAttributes ra) {
    try {
      resets.pedir(correo);
      ra.addFlashAttribute("ok", "Si ese correo tiene cuenta, te enviamos un enlace. Revisa también el spam.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
    }
    return "redirect:/recuperar";
  }

  @GetMapping("/recuperar/{token}")
  public String nueva(@PathVariable String token, Model model, RedirectAttributes ra) {
    if (!resets.vigente(token)) {
      ra.addFlashAttribute("error", "El enlace no es válido o ya caducó.");
      return "redirect:/recuperar";
    }
    model.addAttribute("token", token);
    return "recuperar-nueva";
  }

  @PostMapping("/recuperar/{token}")
  public String cambiar(@PathVariable String token,
                        @RequestParam String password,
                        @RequestParam String password2,
                        RedirectAttributes ra) {
    try {
      resets.cambiar(token, password, password2);
      ra.addFlashAttribute("ok", "Contraseña actualizada. Ya puedes entrar.");
      return "redirect:/login";
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("error", e.getMessage());
      return "redirect:/recuperar/" + token;
    }
  }
}
