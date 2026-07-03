package app.security.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sso.frontend")
public record SsoFrontendProperties(String redirectUri) {}
