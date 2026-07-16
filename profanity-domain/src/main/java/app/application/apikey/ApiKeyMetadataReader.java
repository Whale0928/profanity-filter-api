package app.application.apikey;

import app.domain.apikey.ApiKeyMetadata;

public interface ApiKeyMetadataReader {
  ApiKeyMetadata read(String plaintextApiKey);
}
