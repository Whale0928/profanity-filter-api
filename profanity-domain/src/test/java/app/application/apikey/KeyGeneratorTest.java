package app.application.apikey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyGeneratorTest {

    private KeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        keyGenerator = new APIKeyGenerator(
                "test-salt",
                "SHA-256"
        );
    }

    @Nested
    @DisplayName("API 키 생성할 수 있다.")
    class GenerateKey {

        @Test
        @DisplayName("새로운 API 키가 정상적으로 생성되는지 확인할 수 있다")
        void generateNewKey() throws NoSuchAlgorithmException {
            String apiKey = keyGenerator.generateApiKey();

            assertNotNull(apiKey);
            assertFalse(apiKey.isEmpty());
            assertTrue(keyGenerator.validateApiKey(apiKey));
        }

        @Test
        @DisplayName("서로 다른 API 키가 생성되는지 확인할 수 있다.")
        void generateDifferentKeys() throws NoSuchAlgorithmException {
            String firstKey = keyGenerator.generateApiKey();
            String secondKey = keyGenerator.generateApiKey();

            assertNotEquals(firstKey, secondKey);
        }

        @Test
        @DisplayName("생성된 API 키가 Base64 URL 인코딩 형식을 따른다")
        void checkBase64URLFormat() throws NoSuchAlgorithmException {
            String apiKey = keyGenerator.generateApiKey();
            assertTrue(apiKey.matches("^[A-Za-z0-9_-]+$"));
        }

        @Test
        @DisplayName("여러 개의 유효한 키를 생성한다")
        void generateMultipleValidKeys() {
            List<String> keys = IntStream.range(0, 10)
                    .mapToObj(i -> {
                        try {
                            return keyGenerator.generateApiKey();
                        } catch (NoSuchAlgorithmException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            assertTrue(keys.stream().allMatch(keyGenerator::validateApiKey));
        }
    }

    @Nested
    @DisplayName("API 키 검증")
    class ValidateKey {

        @Test
        @DisplayName("유효한 API 키 검증이 성공하는지 확인할 수 있다")
        void validateValidKey() throws NoSuchAlgorithmException {
            String validKey = keyGenerator.generateApiKey();

            boolean isValid = keyGenerator.validateApiKey(validKey);

            assertTrue(isValid);
        }

        @Test
        @DisplayName("잘못된 형식의 API 키 검증이 실패하는지 확인할 수 있다")
        void validateInvalidKey() {
            String invalidKey = "invalid-key";

            boolean isValid = keyGenerator.validateApiKey(invalidKey);

            assertFalse(isValid);
        }

        @Test
        @DisplayName("다른 salt로 생성된 키는 검증에 실패한다")
        void validateKeyWithDifferentSalt() throws NoSuchAlgorithmException {
            String validKey = keyGenerator.generateApiKey();
            KeyGenerator differentGenerator = new APIKeyGenerator("different-salt", "SHA-256");

            assertFalse(differentGenerator.validateApiKey(validKey));
        }

        @Test
        @DisplayName("null 키 검증에 실패한다")
        void validateNullKey() {
            assertFalse(keyGenerator.validateApiKey(null));
        }

        @Test
        @DisplayName("빈 문자열 키 검증에 실패한다")
        void validateEmptyKey() {
            assertFalse(keyGenerator.validateApiKey(""));
        }
    }

}
