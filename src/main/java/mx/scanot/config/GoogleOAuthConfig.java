package mx.scanot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
@ConditionalOnProperty(prefix = "caros.google", name = "enabled", havingValue = "true")
public class GoogleOAuthConfig {

  @Bean
  ClientRegistrationRepository clientRegistrationRepository(GoogleProps props) {
    if (!props.isEnabled()) {
      throw new IllegalStateException("Google está activado, pero faltan el client-id o el client-secret.");
    }
    ClientRegistration google = CommonOAuth2Provider.GOOGLE.getBuilder("google")
        .clientId(props.getClientId().trim())
        .clientSecret(props.getClientSecret().trim())
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .scope("email", "profile")
        .build();
    return new InMemoryClientRegistrationRepository(google);
  }

  @Bean
  OAuth2AuthorizedClientService authorizedClientService(ClientRegistrationRepository clients) {
    return new InMemoryOAuth2AuthorizedClientService(clients);
  }

  @Bean
  OAuth2UserService<OAuth2UserRequest, OAuth2User> googleUserService() {
    DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
    return request -> {
      OAuth2User google = delegate.loadUser(request);
      Object emailAttr = google.getAttributes().get("email");
      if (emailAttr == null || emailAttr.toString().isBlank()) {
        throw new OAuth2AuthenticationException(new OAuth2Error("email"), "Google no compartió el correo.");
      }
      return google;
    };
  }
}
