package app.application.apikey;

import app.application.client.APIKeyGenerator;
import app.application.client.ClientsCommandService;
import app.application.client.KeyGenerator;
import app.core.exception.BusinessException;
import app.domain.InmemoryClientsRepository;
import app.dto.request.ClientRegistCommand;
import app.dto.response.ClientsRegistResponse;
import app.fixture.ClientTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


class ClientsCommandServiceTest {

    private ClientsCommandService clientsCommandService;
    private InmemoryClientsRepository clientsRepository;
    private KeyGenerator keyGenerator;

    @BeforeEach
    void setUp() {
        clientsRepository = new InmemoryClientsRepository();
        keyGenerator = new APIKeyGenerator("test-salt", "SHA-256");
        clientsCommandService = new ClientsCommandService(clientsRepository, keyGenerator);
    }

    @Nested
    @DisplayName("신규 클라이언트를 등록할 때")
    class RegisterNewClient {

        @Test
        @DisplayName("유효한 요청이면 성공한다")
        void validRequest() {
            // given
            ClientRegistCommand request = ClientTestFixture.createRequest("테스트 클라이언트", "test@example.com");

            // when
            ClientsRegistResponse response = clientsCommandService.registerNewClient(request);

            // then
            assertNotNull(response);
            assertEquals(request.name(), response.name());
            assertEquals(request.email(), response.email());
            assertNotNull(response.apiKey());
            assertEquals(request.note(), response.note());
        }

        @Test
        @DisplayName("이메일이 중복되면 실패한다")
        void duplicateEmail() {
            // given
            String duplicateEmail = "test@example.com";
            clientsRepository.save(ClientTestFixture.createClient(
                    "기존 클라이언트",
                    duplicateEmail,
                    "existing-key"
            ));

            ClientRegistCommand request = ClientTestFixture.createRequest("신규 클라이언트", duplicateEmail);

            // when & then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> clientsCommandService.registerNewClient(request));
            assertEquals("이미 등록된 이메일입니다.", exception.getStatus().DetailDescription());
        }

        @Test
        @DisplayName("API 키가 중복되면 실패한다")
        void duplicateApiKey() {
            // given
            String duplicateApiKey = "duplicate-api-key";
            clientsRepository.save(ClientTestFixture.createClient(
                    "기존 클라이언트",
                    "existing@example.com",
                    duplicateApiKey
            ));

            ClientsCommandService service = new ClientsCommandService(
                    clientsRepository,
                    ClientTestFixture.createKeyGenerator(duplicateApiKey)
            );

            ClientRegistCommand request = ClientTestFixture.createRequest(
                    "신규 클라이언트",
                    "new@example.com"
            );

            // when & then
            BusinessException exception = assertThrows(BusinessException.class,
                    () -> service.registerNewClient(request));
            assertEquals("API 키 생성 중 중복이 발생했습니다.", exception.getStatus().DetailDescription());
        }
    }
}
