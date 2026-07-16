package app.test.support.config;

import app.application.apikey.ApiKeyMetadataReader;
import app.application.client.KeyGenerator;
import app.test.support.fake.FakeApiKeyMetadataReader;
import java.security.NoSuchAlgorithmException;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class SecurityFakeStubConfig {

  @Bean
  @Primary
  public ApiKeyMetadataReader fakeMetadataReader(KeyGenerator apiKeyGenerator)
      throws NoSuchAlgorithmException {
    return new FakeApiKeyMetadataReader(apiKeyGenerator);
  }
}
