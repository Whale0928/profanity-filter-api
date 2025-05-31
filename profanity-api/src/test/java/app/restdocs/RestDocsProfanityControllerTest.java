package app.restdocs;

import app.application.filter.ProfanityHandler;
import app.core.data.constant.Mode;
import app.core.data.elapsed.Elapsed;
import app.core.data.elapsed.ElapsedStartAt;
import app.core.data.response.Detected;
import app.core.data.response.FilterApiResponse;
import app.core.data.response.Status;
import app.core.data.response.constant.StatusCode;
import app.dto.request.ApiRequest;
import app.dto.request.FilterRequest;
import app.presentation.ProfanityController;
import app.security.SecurityContextUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("restdocs")
class RestDocsProfanityControllerTest extends AbstractRestDocs {
    private final ProfanityHandler profanityHandler = mock(ProfanityHandler.class);

    @Override
    protected Object initController() {
        return new ProfanityController(profanityHandler);
    }

    @Test
    @DisplayName("필터링 요청 API(APPLICATION_JSON_VALUE)")
    void step_1() throws Exception {
        final String key = apiKeyGenerator.generateApiKey();

        Elapsed end = Elapsed.end(ElapsedStartAt.now());
        var request = ApiRequest.builder().text("욕설을 사용하지 ㅅㅂ 마세요.").mode(Mode.FILTER).build();
        var response = FilterApiResponse.builder()
                .trackingId(UUID.randomUUID())
                .status(Status.of(StatusCode.OK))
                .detected(Set.of(Detected.of(2, "ㅅㅂ")))
                .filtered("욕설을 사용하지 ** 마세요.")
                .elapsed(end)
                .build();

        securityUtil.when(SecurityContextUtil::getCurrentApikey).thenReturn(key);
        when(profanityHandler.requestFacadeFilter(any(FilterRequest.class),any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/filter")
                        .header("x-api-key", key)
                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andDo(
                        document("api/filter/request",
                                requestHeaders(
                                        headerWithName("x-api-key").description("클라이언트 API 키")
                                ),
                                requestFields(
                                        fieldWithPath("text").description("필터링할 텍스트"),
                                        fieldWithPath("mode").description("필터링 모드 (FILTER, DETECT)"),
                                        fieldWithPath("callbackUrl").optional().description("콜백 URL (비동기 처리시 사용)")
                                ),
                                responseFields(
                                        fieldWithPath("trackingId").description("요청 식별자"),
                                        fieldWithPath("status").description("응답 상태 정보"),
                                        fieldWithPath("status.code").description("상태 코드"),
                                        fieldWithPath("status.message").description("상태 메시지"),
                                        fieldWithPath("status.description").description("상태 설명"),
                                        fieldWithPath("status.DetailDescription").description("상세 설명"),
                                        fieldWithPath("detected[].length").description("필터링된 단어 길이"),
                                        fieldWithPath("detected[].filteredWord").description("필터링된 단어"),
                                        fieldWithPath("filtered").description("필터링된 결과 텍스트"),
                                        fieldWithPath("elapsed").description("처리 소요 시간")
                                )
                        )
                );
    }
}
