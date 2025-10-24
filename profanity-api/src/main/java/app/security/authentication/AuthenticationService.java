package app.security.authentication;

import app.application.client.MetadataReader;
import app.application.client.TemporaryApiKeyService;
import app.core.data.response.constant.StatusCode;
import app.domain.client.ClientMetadata;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationService {
    private static final String AUTH_TOKEN_HEADER_NAME = "X-API-KEY";
    private static final String ROLE_PREFIX = "ROLE_";
    private final MetadataReader clientMetadataReader;
    private final TemporaryApiKeyService temporaryApiKeyService;

    public Authentication getAuthentication(HttpServletRequest request) {
        String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);

        if (apiKey == null || apiKey.isEmpty()) {
            StatusCode unauthorized = StatusCode.UNAUTHORIZED;
            throw new BadCredentialsException(String.valueOf(unauthorized.code()));
        }

        // 임시 키 체크
        if (apiKey.startsWith("temp_")) {
            return handleTemporaryKey(apiKey);
        }

        // 정규 클라이언트 키 처리
        ClientMetadata metadata = clientMetadataReader.read(apiKey);

        CustomPrincipal principal = CustomPrincipal.builder()
                .apiKey(apiKey)
                .id(metadata.id())
                .email(metadata.email())
                .issuerInfo(metadata.issuerInfo())
                .permissions(metadata.permissions())
                .issuedAt(metadata.issuedAt())
                .build();

        final List<String> authorityList = principal.permissions().stream().map(permission -> ROLE_PREFIX + permission).toList();

        return new CustomAuthentication(
                apiKey,
                AuthorityUtils.createAuthorityList(authorityList)
                , principal
        );
    }

    /**
     * 임시 API 키 처리
     */
    private Authentication handleTemporaryKey(String apiKey) {
        boolean isValid = temporaryApiKeyService.validateAndUse(apiKey);
        
        if (!isValid) {
            log.warn("유효하지 않은 임시 API 키: {}", apiKey);
            throw new BadCredentialsException("유효하지 않거나 만료된 임시 API 키입니다.");
        }

        // 임시 키용 Principal 생성
        CustomPrincipal principal = CustomPrincipal.builder()
                .apiKey(apiKey)
                .id(UUID.randomUUID()) // 임시 ID
                .email("temporary@test.com")
                .issuerInfo("Temporary Key User")
                .permissions(List.of("TEMPORARY_USER"))
                .issuedAt(LocalDateTime.now().toString())
                .build();

        return new CustomAuthentication(
                apiKey,
                AuthorityUtils.createAuthorityList(ROLE_PREFIX + "TEMPORARY_USER"),
                principal
        );
    }
}
