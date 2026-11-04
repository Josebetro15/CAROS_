package mx.scanot.web;

import mx.scanot.domain.Rol;
import mx.scanot.domain.Usuario;
import mx.scanot.service.MailboxService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/app")
public class AssistantController {
  private final MailboxService mailbox;

  public AssistantController(MailboxService mailbox) {
    this.mailbox = mailbox;
  }

  @GetMapping("/correo")
  public String correo(@AuthenticationPrincipal Usuario user, Model model) {
    model.addAttribute("user", user);
    model.addAttribute("mails", mailbox.inbox(user));
    model.addAttribute("staff", user.getRol() != Rol.CLIENTE);
    return "app/correo";
  }
}
