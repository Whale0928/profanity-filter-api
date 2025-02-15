package app.restdocs;

import app.application.manage.WordManagementService;
import app.dto.message.SuccessBusinessMessage;
import app.dto.request.WordRequest;
import app.dto.request.WordRequest.RequestType;
import app.dto.request.WordRequest.WordSeverity;
import app.dto.response.MessageResponse;
import app.presentation.WordManagementController;
import app.security.SecurityContextUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("restdocs")
class WordManagementControllerTest extends AbstractRestDocs {
    private final WordManagementService wordManagement = mock(WordManagementService.class);

    @Override
    protected Object initController() {
        return new WordManagementController(wordManagement);
    }

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        final String key = apiKeyGenerator.generateApiKey();
        securityUtil.when(SecurityContextUtil::getCurrentApikey).thenReturn(key);
    }


    @Test
    @DisplayName("신규 비속어 등록 요청 API")
    void step_1() throws Exception {
        var request = WordRequest.builder()
                .word("새로운 단어")
                .reason("이 단어는 욕설입니다.")
                .severity(WordSeverity.LOW)
                .type(RequestType.ADD)
                .build();

        var response = MessageResponse.of(SuccessBusinessMessage.REQUEST_SUCCESS);
        UUID currentUserId = UUID.randomUUID();

        securityUtil.when(SecurityContextUtil::getCurrentUserId).thenReturn(currentUserId);
        when(wordManagement.requestNewWord(
                currentUserId,
                request.word(),
                request.reason(),
                request.severity().name())
        ).thenReturn(response);

        mockMvc.perform(post("/api/v1/word/request")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document("api/word/request",
                                requestFields(
                                        fieldWithPath("word")
                                                .description("신규 등록할 비속어"),
                                        fieldWithPath("reason")
                                                .description("신규 등록할 비속어의 이유"),
                                        fieldWithPath("severity")
                                                .description("신규 등록할 비속어의 심각도"),
                                        fieldWithPath("type")
                                                .description("요청 타입 (ADD, REMOVE, MODIFY)")
                                ),
                                responseFields(
                                        fieldWithPath("status").description("응답 상태 정보"),
                                        fieldWithPath("status.code").ignored(),
                                        fieldWithPath("status.message").ignored(),
                                        fieldWithPath("status.description").ignored(),
                                        fieldWithPath("status.DetailDescription").ignored(),
                                        fieldWithPath("data").description("응답 데이터"),
                                        fieldWithPath("data.result").description("결과"),
                                        fieldWithPath("data.message").description("결과 코드")
                                )
                        )
                );
    }
}
