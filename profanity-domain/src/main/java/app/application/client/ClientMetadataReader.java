package app.application.client;

import app.application.apikey.APIKeyGenerator;
import app.core.data.response.constant.StatusCode;
import app.domain.client.ClientMetadata;
import app.domain.client.Clients;
import app.domain.client.ClientsRepository;
import app.domain.client.PermissionsType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientMetadataReader implements MetadataReader {
    private final ClientsRepository clientsRepository;
    private final APIKeyGenerator keyGenerator;

    @Override
    @Transactional(readOnly = true)
    public ClientMetadata read(String apiKey) {

        if (Boolean.FALSE.equals(keyGenerator.validateApiKey(apiKey))) {
            throw new IllegalArgumentException(StatusCode.INVALID_API_KEY.stringCode());
        }

        Clients clients = clientsRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new NoSuchElementException(StatusCode.NOT_FOUND_CLIENT.stringCode()));

        final List<String> permissions = clients.getPermissions().stream().map(PermissionsType::getValue).toList();

        return ClientMetadata.builder()
                .id(clients.getId())
                .email(clients.getEmail())
                .issuerInfo(clients.getIssuerInfo())
                .note(clients.getNote())
                .permissions(permissions)
                .issuedAt(clients.getIssuedAt().toString())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyClientByEmail(String email) {
        return clientsRepository.findByEmail(email).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public String getApiKeyByEmail(String email) {
        Clients clients = clientsRepository.findByEmail(email)
                .orElseThrow(() -> new NoSuchElementException(StatusCode.NOT_FOUND_CLIENT.stringCode()));
        return clients.getApiKey();
    }
}
