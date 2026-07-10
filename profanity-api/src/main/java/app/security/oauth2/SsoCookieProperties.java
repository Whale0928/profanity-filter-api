package app.security.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sso.cookie")
public record SsoCookieProperties(String name, long ttlSeconds, String signingKey) {}
