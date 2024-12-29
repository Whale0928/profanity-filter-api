package app.application.apikey;

import java.security.NoSuchAlgorithmException;

public interface KeyGenerator {
    String generateApiKey() throws NoSuchAlgorithmException;

    boolean validateApiKey(String apiKey);
}
