package app.security.authentication;

import app.application.client.MetadataReader;
import app.core.data.response.constant.StatusCode;
import app.domain.client.ClientMetadata;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthenticationService {
    private static final String AUTH_TOKEN_HEADER_NAME = "X-API-KEY";
    private static final String ROLE_PREFIX = "ROLE_";
    private final MetadataReader clientMetadataReader;

    public Authentication getAuthentication(HttpServletRequest request) {
        String apiKey = request.getHeader(AUTH_TOKEN_HEADER_NAME);

        if (apiKey == null || apiKey.isEmpty()) {
            StatusCode unauthorized = StatusCode.UNAUTHORIZED;
            throw new BadCredentialsException(String.valueOf(unauthorized.code()));
        }

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
}
