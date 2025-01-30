package app.application.client;

import app.application.apikey.KeyGenerator;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.client.ClientMetadata;
import app.domain.client.Clients;
import app.domain.client.ClientsRepository;
import app.dto.request.ClientRegistCommand;
import app.dto.response.ClientsRegistResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ClientsCommandService {
    private final ClientsRepository clientRepository;
    private final KeyGenerator apiKeyGenerator;

    @Transactional
    public ClientsRegistResponse registerNewClient(ClientRegistCommand request) {
        validateEmail(request.email());
        String apiKey = generateApiKey();
        validateApiKey(apiKey);

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

    @Transactional
    public ClientMetadata updateClientInfo(
            final String apiKey,
            final String issuerInfo,
            final String note
    ) {
        Clients clients = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new NoSuchElementException(StatusCode.NOT_FOUND_CLIENT.stringCode()));

        clients.updateInfo(issuerInfo, note);

        return ClientMetadata.builder()
                .id(clients.getId())
                .email(clients.getEmail())
                .issuerInfo(clients.getIssuerInfo())
                .note(clients.getNote())
                .permissions(clients.getPlainPermissions())
                .issuedAt(clients.getIssuedAt().toString())
                .build();
    }

    @Transactional
    public void discardClient(String apikey) {
        Clients clients = clientRepository.findByApiKey(apikey)
                .orElseThrow(() -> new NoSuchElementException(StatusCode.NOT_FOUND_CLIENT.stringCode()));
        clients.discarded();
    }

    @Transactional
    public String regenerateApiKey(String currentApiKey) {
        Clients clients = clientRepository.findByApiKey(currentApiKey)
                .orElseThrow(() -> new NoSuchElementException(StatusCode.NOT_FOUND_CLIENT.stringCode()));
        String newApiKey = generateApiKey();
        return clients.updateApiKey(newApiKey);
    }

    private void validateEmail(String email) {
        if (clientRepository.existsByEmail(email)) {
            throw new BusinessException(StatusCode.BAD_REQUEST, "이미 등록된 이메일입니다.");
        }
    }

    private void validateApiKey(String apiKey) {
        if (clientRepository.existsByApiKey(apiKey)) {
            throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "API 키 생성 중 중복이 발생했습니다.");
        }
    }

    private String generateApiKey() {
        try {
            return apiKeyGenerator.generateApiKey();
        } catch (Exception e) {
            throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "API 키 생성 중 오류가 발생했습니다.");
        }
    }

}
