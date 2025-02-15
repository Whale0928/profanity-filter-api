package app.presentation;

import app.TestConfig;
import app.application.client.APIKeyGenerator;
import app.core.data.response.constant.StatusCode;
import app.dto.request.ApiRequest;
import app.fixture.ApiTestFixture;
import app.fixture.FakeClientMetadataReader;
import app.fixture.SecurityFakeStubConfig;
import app.security.SecurityConfig;
import app.security.authentication.AuthenticationService;
import app.security.filter.CustomAuthenticationEntryPoint;
import app.security.filter.CustomAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProfanityController.class)
@Import({
        TestConfig.class,
        SecurityFakeStubConfig.class,
        SecurityConfig.class,
        CustomAuthenticationFilter.class,
        CustomAuthenticationEntryPoint.class,
        AuthenticationService.class,
        APIKeyGenerator.class
})
class SecurityAuthenticationTest {
    private static final String REQUEST_URL = "/api/v1/filter";
    private String validApiKey;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    private ApiTestFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new ApiTestFixture();
        validApiKey = FakeClientMetadataReader.validKeys.get(0);
    }

    @Test
    @DisplayName("유효한 API 키 요청시 200 OK와 함께 성공 응답을 반환한다")
    void test_200() throws Exception {
        ApiRequest request = fixture.createQuickRequest("test text");
        mockMvc.perform(post(REQUEST_URL)
                        .header("X-API-KEY", validApiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(jsonPath("$.status.code").value(2000))
                .andExpect(jsonPath("$.status.message").value(StatusCode.OK.status()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("API 키가 비어있는 경우 4010 UNAUTHORIZED 응답을 반환한다")
    void test_4010() throws Exception {
        ApiRequest request = fixture.createQuickRequest("test text");
        mockMvc.perform(post(REQUEST_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-KEY", "")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(4010))
                .andExpect(jsonPath("$.status.message").value(StatusCode.UNAUTHORIZED.status()));
    }

    @Test
    @DisplayName("잘못된 형식의 API 키 요청시 4031 INVALID_API_KEY 응답을 반환한다")
    void test_4031() throws Exception {
        ApiRequest request = fixture.createQuickRequest("test text");

        mockMvc.perform(post(REQUEST_URL)
                        .header("X-API-KEY", "invalid-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(StatusCode.INVALID_API_KEY.code()))
                .andExpect(jsonPath("$.status.message").value(StatusCode.INVALID_API_KEY.status()));
    }

    @Test
    @DisplayName("존재하지 않는 API 키 요청시 4040 NOT_FOUND_CLIENT 응답을 반환한다")
    void test_4040() throws Exception {
        ApiRequest request = fixture.createQuickRequest("test text");
        String key = FakeClientMetadataReader.validKeys.get(0);
        FakeClientMetadataReader.validKeys.remove(key);

        mockMvc.perform(post(REQUEST_URL)
                        .header("X-API-KEY", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(StatusCode.NOT_FOUND_CLIENT.code()))
                .andExpect(jsonPath("$.status.message").value(StatusCode.NOT_FOUND_CLIENT.status()));

        FakeClientMetadataReader.validKeys.add(key);
    }
}
