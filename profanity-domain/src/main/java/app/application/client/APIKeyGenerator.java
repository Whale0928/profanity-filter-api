package app.application.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
public class APIKeyGenerator implements KeyGenerator {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
    private final String salt;
    private final String algorithm;

    public APIKeyGenerator(
            @Value("${api-key.keycode}") String salt,
            @Value("${api-key.algorithm}") String algorithm
    ) {
        this.salt = salt;
        this.algorithm = algorithm;
    }

    @Override
    public String generateApiKey() throws NoSuchAlgorithmException {
        // 랜덤 바이트 생성
        byte[] randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);

        // 솔트를 적용한 해시 생성
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.update(randomBytes);
        digest.update(salt.getBytes());
        byte[] hash = digest.digest();

        // 랜덤 바이트와 해시를 조합
        byte[] combined = new byte[32];
        System.arraycopy(randomBytes, 0, combined, 0, randomBytes.length);
        System.arraycopy(hash, 0, combined, randomBytes.length, 16);

        return encoder.encodeToString(combined);
    }

    @Override
    public boolean validateApiKey(String apiKey) {
        try {
            // Base64 디코딩
            byte[] decoded = Base64.getUrlDecoder().decode(apiKey);
            if (decoded.length != 32) return false;

            // 랜덤 부분과 해시 부분 분리
            byte[] randomPart = new byte[16];
            System.arraycopy(decoded, 0, randomPart, 0, 16);

            // 해시 재생성
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(randomPart);
            digest.update(salt.getBytes());
            byte[] expectedHash = digest.digest();

            // 해시 비교
            byte[] actualHash = new byte[16];
            System.arraycopy(decoded, 16, actualHash, 0, 16);

            return MessageDigest.isEqual(actualHash, java.util.Arrays.copyOf(expectedHash, 16));
        } catch (Exception e) {
            return false;
        }
    }
}
