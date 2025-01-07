package app.application.client;

import app.domain.client.ClientMetadata;

public interface MetadataReader {
    ClientMetadata read(String apiKey);
}
