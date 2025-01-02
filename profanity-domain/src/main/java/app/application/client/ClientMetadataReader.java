package app.application.client;

import app.domain.client.ClientMetadata;
import app.domain.client.Clients;
import app.domain.client.ClientsRepository;
import app.domain.client.PermissionsType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientMetadataReader {
    private final ClientsRepository clientsRepository;

    public ClientMetadata read(String apiKey) {

        Clients clients = clientsRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new IllegalArgumentException("not found client key:" + apiKey));
        final List<String> permissions = clients.getPermissions().stream().map(PermissionsType::getValue).toList();

        return ClientMetadata.builder()
                .email(clients.getEmail())
                .issuerInfo(clients.getIssuerInfo())
                .note(clients.getNote())
                .permissions(permissions)
                .issuedAt(clients.getIssuedAt().toString())
                .build();
    }
}
