package app.application.client;

import app.domain.client.ClientMetadata;

public interface MetadataReader {
    ClientMetadata read(String apiKey);

    String getApiKeyByEmail(String email);

    boolean verifyClientByEmail(String email);
}
