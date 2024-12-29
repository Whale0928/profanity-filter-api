package app.fixture;

import app.application.apikey.KeyGenerator;
import app.domain.client.Clients;
import app.dto.request.ClientRegistCommand;

public class ClientTestFixture {

    public static ClientRegistCommand createRequest(String name, String email) {
        return new ClientRegistCommand(
                name,
                email,
                "테스트 발급자",
                "테스트 노트"
        );
    }

    public static Clients createClient(String name, String email, String apiKey) {
        return Clients.builder()
                .name(name)
                .email(email)
                .apiKey(apiKey)
                .issuerInfo("기존 발급자")
                .build();
    }

    public static KeyGenerator createKeyGenerator(String fixedApiKey) {
        return new KeyGenerator() {
            @Override
            public String generateApiKey() {
                return fixedApiKey;
            }

            @Override
            public boolean validateApiKey(String apiKey) {
                return true;
            }
        };
    }
}
