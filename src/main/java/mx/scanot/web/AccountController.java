package mx.scanot.web;

import mx.scanot.config.GoogleProps;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {
  private final GoogleProps google;

  public AccountController(GoogleProps google) {
    this.google = google;
  }

  @GetMapping("/login")
  public String login(Model model) {
    model.addAttribute("google", google.isEnabled());
    return "login";
  }
}
