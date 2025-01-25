package app.fixture;

import app.application.apikey.KeyGenerator;
import app.application.client.MetadataReader;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.security.NoSuchAlgorithmException;

@TestConfiguration
public class SecurityFakeStubConfig {

    @Bean
    @Primary
    public MetadataReader fakeMetadataReader(KeyGenerator apiKeyGenerator) throws NoSuchAlgorithmException {
        return new FakeClientMetadataReader(apiKeyGenerator);
    }

}
