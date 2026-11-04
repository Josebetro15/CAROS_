package mx.scanot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caros.google")
public class GoogleProps {
  private boolean enabled;
  private String clientId = "";
  private String clientSecret = "";

  public boolean isEnabled() { return enabled && clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank(); }
  public boolean isFlagOn() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public String getClientId() { return clientId; }
  public void setClientId(String clientId) { this.clientId = clientId; }
  public String getClientSecret() { return clientSecret; }
  public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
}
