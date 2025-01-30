package app.fixture;

import app.application.apikey.KeyGenerator;
import app.application.client.MetadataReader;
import app.core.data.response.constant.StatusCode;
import app.domain.client.ClientMetadata;
import app.domain.client.PermissionsType;

import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class FakeClientMetadataReader implements MetadataReader {
    public static final CopyOnWriteArrayList<String> validKeys = new CopyOnWriteArrayList<>();
    private final KeyGenerator apiKeyGenerator;

    public FakeClientMetadataReader(KeyGenerator apiKeyGenerator) throws NoSuchAlgorithmException {
        this.apiKeyGenerator = apiKeyGenerator;
        validKeys.add(apiKeyGenerator.generateApiKey());
        validKeys.add(apiKeyGenerator.generateApiKey());
    }

    @Override
    public ClientMetadata read(String apiKey) {

        if (Boolean.FALSE.equals(apiKeyGenerator.validateApiKey(apiKey))) {
            throw new IllegalArgumentException(StatusCode.INVALID_API_KEY.stringCode());
        }

        if (validKeys.contains(apiKey)) {
            return ClientMetadata.builder()
                    .id(UUID.randomUUID())
                    .email("fake@mail.com")
                    .issuerInfo("issuer info")
                    .note("note")
                    .permissions(PermissionsType.allPermissions().stream().map(PermissionsType::getValue).toList())
                    .issuedAt("2025-09-01T00:00:00Z")
                    .build();
        } else {
            throw new NoSuchElementException(StatusCode.NOT_FOUND_CLIENT.stringCode());
        }
    }

    @Override
    public String getApiKeyByEmail(String email) {
        return validKeys.stream().findFirst().orElseThrow();
    }
}
