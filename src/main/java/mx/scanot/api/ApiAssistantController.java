package mx.scanot.api;

import java.util.List;
import java.util.Map;
import mx.scanot.domain.MailMessage;
import mx.scanot.domain.Usuario;
import mx.scanot.service.MailboxService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiAssistantController {
  private final MailboxService mailbox;

  public ApiAssistantController(MailboxService mailbox) {
    this.mailbox = mailbox;
  }

  @GetMapping("/correos")
  public List<Map<String, String>> correos(@AuthenticationPrincipal Usuario user) {
    return mailbox.inbox(user).stream().map(this::mail).toList();
  }

  private Map<String, String> mail(MailMessage m) {
    return Map.of(
        "para", m.getToEmail(),
        "asunto", m.getSubject(),
        "cuerpo", m.getBody(),
        "tipo", m.getKind());
  }
}
