package app.presentation;

import app.application.EmailService;
import app.application.client.ClientMetadataReader;
import app.application.client.ClientsCommandService;
import app.application.client.TemporaryApiKeyService;
import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.client.TemporaryApiKey;
import app.exception.GlobalExceptionHandler;
import app.presentation.ClientsController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TemporaryApiKeyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClientsCommandService clientsCommandService;

    @Mock
    private ClientMetadataReader clientMetadataReader;

    @Mock
    private EmailService emailService;

    @Mock
    private TemporaryApiKeyService temporaryApiKeyService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ClientsController controller = new ClientsController(
                clientsCommandService,
                clientMetadataReader,
                emailService,
                temporaryApiKeyService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("임시 API 키 발급 성공")
    void issueTemporaryKey_Success() throws Exception {
        // Given
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        TemporaryApiKey temporaryApiKey = TemporaryApiKey.builder()
                .apiKey("temp_abc123def456")
                .ipAddress("127.0.0.1")
                .remainingCount(10)
                .issuedAt(now)
                .expiredAt(now.plusHours(24))
                .build();

        when(temporaryApiKeyService.issueTemporaryKey(anyString()))
                .thenReturn(temporaryApiKey);

        // When & Then
        mockMvc.perform(post("/api/v1/clients/temporary-key")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(200))
                .andExpect(jsonPath("$.data.apiKey").value("temp_abc123def456"))
                .andExpect(jsonPath("$.data.remainingCount").value(10))
                .andExpect(jsonPath("$.data.message").exists());
    }

    @Test
    @DisplayName("임시 API 키 발급 실패 - IP 발급 제한 초과")
    void issueTemporaryKey_ExceedLimit() throws Exception {
        // Given
        when(temporaryApiKeyService.issueTemporaryKey(anyString()))
                .thenThrow(new BusinessException(
                        StatusCode.TOO_MANY_REQUESTS,
                        "일일 임시 키 발급 제한을 초과했습니다. 내일 다시 시도해주세요."
                ));

        // When & Then
        mockMvc.perform(post("/api/v1/clients/temporary-key")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status.code").value(429))
                .andExpect(jsonPath("$.status.message").value("일일 임시 키 발급 제한을 초과했습니다. 내일 다시 시도해주세요."));
    }
}
