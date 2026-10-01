package app.test.support.fake;

import app.application.apikey.ApiKeyMetadataReader;
import app.application.client.KeyGenerator;
import app.core.data.response.constant.StatusCode;
import app.domain.apikey.ApiKeyMetadata;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

public class FakeApiKeyMetadataReader implements ApiKeyMetadataReader {
  public static final List<String> validKeys = new ArrayList<>();

  /** 유효하지만 로그인 계정에 연결되지 않은 키. 실제 ApiKeyReader의 미연결 거절 규칙을 흉내낸다. */
  public static final List<String> unlinkedKeys = new ArrayList<>();

  private final KeyGenerator apiKeyGenerator;

  public FakeApiKeyMetadataReader(KeyGenerator apiKeyGenerator) throws NoSuchAlgorithmException {
    this.apiKeyGenerator = apiKeyGenerator;
    validKeys.clear();
    unlinkedKeys.clear();
    validKeys.add(apiKeyGenerator.generateApiKey());
    validKeys.add(apiKeyGenerator.generateApiKey());
  }

  @Override
  public ApiKeyMetadata read(String apiKey) {
    if (!apiKeyGenerator.validateApiKey(apiKey)) {
      throw new IllegalArgumentException(StatusCode.INVALID_API_KEY.stringCode());
    }
    if (!validKeys.contains(apiKey)) {
      throw new NoSuchElementException(StatusCode.NOT_FOUND_CLIENT.stringCode());
    }
    if (unlinkedKeys.contains(apiKey)) {
      throw new NoSuchElementException(StatusCode.API_KEY_NOT_LINKED.stringCode());
    }
    return new ApiKeyMetadata(
        UUID.nameUUIDFromBytes(apiKey.getBytes()),
        "tester@example.com",
        "test",
        List.of("READ"),
        "2026-07-17T00:00:00",
        apiKeyGenerator.hashApiKey(apiKey));
  }
}
