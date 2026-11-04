package mx.scanot.config;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import mx.scanot.repo.UsuarioRepo;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  private final ObjectProvider<ClientRegistrationRepository> googleClients;
  private final ObjectProvider<GoogleLoginSuccess> googleSuccess;
  private final ObjectProvider<OAuth2UserService<OAuth2UserRequest, OAuth2User>> googleUsers;

  public SecurityConfig(ObjectProvider<ClientRegistrationRepository> googleClients,
                        ObjectProvider<GoogleLoginSuccess> googleSuccess,
                        ObjectProvider<OAuth2UserService<OAuth2UserRequest, OAuth2User>> googleUsers) {
    this.googleClients = googleClients;
    this.googleSuccess = googleSuccess;
    this.googleUsers = googleUsers;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  UserDetailsService userDetailsService(UsuarioRepo usuarios) {
    return email -> usuarios.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException(email));
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.ignoringRequestMatchers((HttpServletRequest request) -> {
          String auth = request.getHeader("Authorization");
          return auth != null && (auth.startsWith("Basic ") || auth.startsWith("Bearer "));
        }))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/", "/login", "/oauth2/**", "/login/oauth2/**", "/registro", "/registro/**", "/recuperar", "/recuperar/**",
                "/css/**", "/img/**", "/samples/**").permitAll()
            .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").authenticated()
            .requestMatchers("/api", "/api/auth/**").permitAll()
            .requestMatchers("/api/scanner/**").hasAnyRole("MECANICO", "ENCARGADO")
            .requestMatchers("/api/flujo", "/api/flujo/**").hasRole("ENCARGADO")
            .requestMatchers("/api/diagnosticos", "/api/diagnosticos/**", "/api/partes", "/api/partes/**",
                "/api/preordenes", "/api/preordenes/**", "/api/ordenes", "/api/ordenes/**").hasAnyRole("MECANICO", "ENCARGADO")
            .requestMatchers("/api/**").authenticated()
            .requestMatchers("/uploads/**").authenticated()
            .requestMatchers("/app/scanner/**", "/app/diagnosticos", "/app/diagnosticos/**",
                "/app/partes", "/app/partes/**", "/app/preordenes", "/app/ordenes", "/app/ordenes/**").hasAnyRole("MECANICO", "ENCARGADO")
            .requestMatchers("/app/flujo", "/app/flujo/**").hasRole("ENCARGADO")
            .requestMatchers("/app/**").authenticated()
            .anyRequest().authenticated())
        .formLogin(form -> form
            .loginPage("/login")
            .defaultSuccessUrl("/app", true)
            .permitAll())
        .httpBasic(Customizer.withDefaults())
        .logout(logout -> logout.logoutSuccessUrl("/").permitAll());
    if (googleClients.getIfAvailable() != null && googleSuccess.getIfAvailable() != null && googleUsers.getIfAvailable() != null) {
      http.oauth2Login(oauth -> oauth
          .loginPage("/login")
          .failureHandler((request, response, ex) -> {
            String msg = ex.getMessage() == null ? "Google no dejó entrar." : ex.getMessage();
            response.sendRedirect("/login?google=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
          })
          .userInfoEndpoint(user -> user.userService(googleUsers.getObject()))
          .successHandler(googleSuccess.getObject()));
    }
    return http.build();
  }
}
