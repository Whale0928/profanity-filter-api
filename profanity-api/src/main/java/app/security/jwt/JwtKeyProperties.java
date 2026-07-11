package app.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt.keys")
public record JwtKeyProperties(String privateJwk, String publicJwkSet) {}
