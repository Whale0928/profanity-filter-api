package app.fixture;

import app.application.client.MetadataReader;
import app.core.data.response.constant.StatusCode;
import app.domain.client.ClientMetadata;
import app.domain.client.PermissionsType;

import java.util.NoSuchElementException;
import java.util.UUID;

public class FakeClientMetadataReader implements MetadataReader {

    @Override
    public ClientMetadata read(String apiKey) {

        if (apiKey.equals("valid-api-key")) {
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
}
