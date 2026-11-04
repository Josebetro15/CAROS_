package mx.scanot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  OpenAPI carosApi() {
    return new OpenAPI().info(new Info()
        .title("CarOs API")
        .version("1.0")
        .description("Scanner, cotizaciones, preordenes, ordenes y recuperacion de contrasena. "
            + "En Authorize usa Basic: mecanico@scanot.mx / scanot123"));
  }
}
