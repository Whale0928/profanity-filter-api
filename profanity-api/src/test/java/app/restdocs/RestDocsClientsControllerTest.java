package app.restdocs;

import app.application.apikey.APIKeyGenerator;
import app.application.apikey.ClientsCommandService;
import app.application.client.ClientMetadataReader;
import app.domain.client.ClientMetadata;
import app.dto.request.ClientRegistRequest;
import app.dto.response.ClientsRegistResponse;
import app.presentation.ClientsController;
import app.security.SecurityContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("restdocs")
class RestDocsClientsControllerTest extends AbstractRestDocs {
    private final ClientsCommandService clientsCommandService = mock(ClientsCommandService.class);
    private final ClientMetadataReader clientReader = mock(ClientMetadataReader.class);
    private final MockedStatic<SecurityContextUtil> securityUtil = mockStatic(SecurityContextUtil.class);
    private final APIKeyGenerator apiKeyGenerator = new APIKeyGenerator("solt", "SHA-256");

    @Override
    protected Object initController() {
        return new ClientsController(clientsCommandService, clientReader);
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
    }

    @Test
    @DisplayName("클라이언트 등록 API")
    void step_1() throws Exception {
        var request = ClientRegistRequest.builder()
                .name("팀 보틀노트")
                .email("bottlenote@email.com")
                .issuerInfo("도메인(https://bottle-note.com)  ,  연락처(010-1234-5678)")
                .note("학원에서 진행하는 팀 프로젝트 입니다. 피드 작성 시 비속어 검증을 위해 신청 합니다.")
                .build();
        var response = ClientsRegistResponse.builder()
                .name(request.name())
                .email(request.email())
                .apiKey("apiKey")
                .note(request.note())
                .build();

        when(clientsCommandService.registerNewClient(request.toCommand())).thenReturn(response);

        mockMvc.perform(post("/api/v1/clients/register")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(
                        document("api/clients/register",
                                requestFields(
                                        fieldWithPath("name").description("이름 또는 조직명")
                                                .type(JsonFieldType.STRING),
                                        fieldWithPath("email").description("이메일")
                                                .type(JsonFieldType.STRING),
                                        fieldWithPath("issuerInfo").description("발급자 정보")
                                                .type(JsonFieldType.STRING),
                                        fieldWithPath("note").description("메모")
                                                .type(JsonFieldType.STRING)
                                ),
                                responseFields(
                                        fieldWithPath("status").type(JsonFieldType.OBJECT)
                                                .description("응답 상태 정보"),
                                        fieldWithPath("status.code").ignored(),
                                        fieldWithPath("status.message").ignored(),
                                        fieldWithPath("status.description").ignored(),
                                        fieldWithPath("status.DetailDescription").ignored(),
                                        fieldWithPath("data").type(JsonFieldType.OBJECT)
                                                .description("응답 데이터"),
                                        fieldWithPath("data.name").type(JsonFieldType.STRING)
                                                .description("등록된 클라이언트 이름"),
                                        fieldWithPath("data.email").type(JsonFieldType.STRING)
                                                .description("등록된 클라이언트 이메일"),
                                        fieldWithPath("data.apiKey").type(JsonFieldType.STRING)
                                                .description("발급된 API 키"),
                                        fieldWithPath("data.note").type(JsonFieldType.STRING)
                                                .description("등록된 메모")
                                )
                        )
                );
    }


    @Test
    @DisplayName("클라이언트 메타 정보 조회 API")
    void step_2() throws Exception {
        final String key = apiKeyGenerator.generateApiKey();
        var response = ClientMetadata.builder()
                .id(UUID.fromString("4e35ca95-1bdc-48dc-b38d-2db0307c24ad"))
                .email("bottlenote@email.com")
                .issuerInfo("도메인(https://bottle-note.com)  ,  연락처(010-1234-5678)")
                .note("학원에서 진행하는 팀 프로젝트 입니다. 피드 작성 시 비속어 검증을 위해 신청 합니다.")
                .permissions(List.of("READ", "WRITE"))
                .issuedAt("2021-08-01T00:00:00").build();

        securityUtil.when(SecurityContextUtil::getCurrentApikey).thenReturn(key);
        when(clientReader.read(key)).thenReturn(response);

        mockMvc.perform(get("/api/v1/clients")
                        .header("x-api-key", key))
                .andExpect(status().isOk())
                .andDo(
                        document("api/clients/info",
                                requestHeaders(
                                        headerWithName("x-api-key").description("클라이언트 API 키")
                                ),
                                responseFields(
                                        fieldWithPath("status").description("응답 상태 정보"),
                                        fieldWithPath("status.code").ignored(),
                                        fieldWithPath("status.message").ignored(),
                                        fieldWithPath("status.description").ignored(),
                                        fieldWithPath("status.DetailDescription").ignored(),
                                        fieldWithPath("data").ignored(),
                                        fieldWithPath("data.id").description("클라이언트 식별자"),
                                        fieldWithPath("data.email").description("클라이언트 이메일"),
                                        fieldWithPath("data.issuerInfo").description("클라이언트 발급 정보"),
                                        fieldWithPath("data.note").description("클라이언트 메모"),
                                        fieldWithPath("data.permissions").description("권한 목록"),
                                        fieldWithPath("data.issuedAt").description("발급일")
                                )
                        )
                );
    }
}
