package app.application.apikey;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.client.Clients;
import app.domain.client.ClientsRepository;
import app.dto.request.ClientRegistRequest;
import app.dto.response.ClientsRegistResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientsRegistrationService {
    private final ClientsRepository clientRepository;
    private final APIKeyGenerator apiKeyGenerator;

    @Transactional
    public ClientsRegistResponse registerNewClient(ClientRegistRequest request) {
        validateEmail(request.email());
        String apiKey = generateUniqueApiKey();

        Clients client = Clients.builder()
                .name(request.name())
                .email(request.email())
                .apiKey(apiKey)
                .issuerInfo(request.issuerInfo())
                .note(request.note())
                .build();

        Clients savedClient = clientRepository.save(client);

        return ClientsRegistResponse.builder()
                .name(savedClient.getName())
                .email(savedClient.getEmail())
                .apiKey(savedClient.getApiKey())
                .note(savedClient.getNote())
                .build();
    }

    private void validateEmail(String email) {
        if (clientRepository.existsByEmail(email)) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "이미 등록된 이메일입니다.");
        }
    }

    private String generateUniqueApiKey() {
        try {
            String apiKey = apiKeyGenerator.generateApiKey();
            if (clientRepository.existsByApiKey(apiKey)) {
                throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "API 키 생성 중 중복이 발생했습니다.");
            }
            return apiKey;
        } catch (Exception e) {
            throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "API 키 생성 중 오류가 발생했습니다.");
        }
    }
}
