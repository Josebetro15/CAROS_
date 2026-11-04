package mx.scanot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import mx.scanot.domain.Usuario;
import mx.scanot.repo.UsuarioRepo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "caros.google", name = "enabled", havingValue = "true")
public class GoogleLoginSuccess implements AuthenticationSuccessHandler {
  private final UsuarioRepo usuarios;
  private final SecurityContextRepository sesiones = new HttpSessionSecurityContextRepository();

  public GoogleLoginSuccess(UsuarioRepo usuarios) {
    this.usuarios = usuarios;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    try {
      OAuth2User google = (OAuth2User) authentication.getPrincipal();
      Object emailAttr = google.getAttributes().get("email");
      Object nameAttr = google.getAttributes().get("name");
      if (emailAttr == null || emailAttr.toString().isBlank()) {
        response.sendRedirect("/login?google=" + URLEncoder.encode("Google no compartió el correo.", StandardCharsets.UTF_8));
        return;
      }
      String email = emailAttr.toString().trim().toLowerCase();
      String name = nameAttr == null ? "" : nameAttr.toString().trim();
      Usuario user = usuarios.findByEmail(email).orElse(null);
      if (user == null) {
        request.getSession().setAttribute("googleCorreo", email);
        request.getSession().setAttribute("googleNombre", name.isBlank() ? email : name);
        response.sendRedirect("/registro/google");
        return;
      }
      UsernamePasswordAuthenticationToken token =
          new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
      SecurityContextHolder.getContext().setAuthentication(token);
      sesiones.saveContext(SecurityContextHolder.getContext(), request, response);
      response.sendRedirect("/app");
    } catch (RuntimeException e) {
      String msg = e.getMessage() == null ? "No se pudo entrar con Google." : e.getMessage();
      response.sendRedirect("/login?google=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
    }
  }
}
