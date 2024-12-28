package app.presentation;

import app.TestConfig;
import app.core.data.response.FilterApiResponse;
import app.dto.request.ApiRequest;
import app.fixture.ApiTestFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Import(TestConfig.class)
@WebMvcTest(ProfanityController.class)
class ProfanityControllerTest {
    private static final String REQUEST_URL = "/api/v1/filter";
    private ApiTestFixture fixture;
    @Autowired
    private ObjectMapper mapper;
    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        fixture = new ApiTestFixture();
    }

    @Nested
    @DisplayName("Quick 타입의 요청을 할 수 있다.")
    class QuickRequestTest {

        @Test
        @DisplayName("Applicaion/json 형식으로 요청을 보낼 수 있다.")
        void test1() throws Exception {
            // given
            ApiRequest quickRequest = fixture.createQuickRequest("안녕하세요. 비속어를 검증합니다.");

            // when
            var response = mockMvc.perform(post(REQUEST_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(quickRequest)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();

            var responseEntity = mapper.readValue(response.getContentAsString(), FilterApiResponse.class);

            assertNotNull(responseEntity.trackingId());
            assertTrue(responseEntity.detected().stream().anyMatch(d -> quickRequest.text().contains(d.filteredWord())));
            assertTrue(responseEntity.filtered().isEmpty());
        }

        @Test
        @DisplayName("Urlencoded 형식으로 요청을 보낼 수 있다.")
        void test2() throws Exception {
            // given
            ApiRequest quickRequest = fixture.createQuickRequest("안녕하세요. 비속어를 검증합니다.");

            // when
            var response = mockMvc.perform(post(REQUEST_URL)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("text", quickRequest.text())
                            .param("mode", quickRequest.mode().name()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();

            var responseEntity = mapper.readValue(response.getContentAsString(), FilterApiResponse.class);

            assertNotNull(responseEntity.trackingId());
            assertTrue(responseEntity.detected().stream().anyMatch(d -> quickRequest.text().contains(d.filteredWord())));
            assertTrue(responseEntity.filtered().isEmpty());
        }

        @Test
        @DisplayName("필터링된 단어가 없는 경우 빈 리스트를 반환한다.")
        void test3() throws Exception {
            // given
            ApiRequest quickRequest = fixture.createQuickRequest("안녕하세요. 검증합니다.");

            // when
            var response = mockMvc.perform(post(REQUEST_URL)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("text", quickRequest.text())
                            .param("mode", quickRequest.mode().name()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();

            var responseEntity = mapper.readValue(response.getContentAsString(), FilterApiResponse.class);

            assertNotNull(responseEntity.trackingId());
            assertTrue(responseEntity.detected().isEmpty());
            assertTrue(responseEntity.filtered().isEmpty());
        }

        @Test
        @DisplayName("필수 파라미터가 없는 경우 4000 에러를 반환한다.")
        void test4() throws Exception {
            // given
            // when
            mockMvc.perform(post(REQUEST_URL)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("text", "")
                            .param("mode", ""))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status.code").value(4000))
                    .andExpect(jsonPath("$.status.message").value("Bad_request"));
        }
    }

    @Nested
    @DisplayName("Normal 타입의 요청을 할 수 있다.")
    class NormalRequestTest {

        @Test
        @DisplayName("Applicaion/json 형식으로 요청을 보낼 수 있다.")
        void test1() throws Exception {
            // given
            ApiRequest normalRequest = fixture.createNormalRequest("안녕하세요. 비속어를 검증합니다.");

            // when
            var response = mockMvc.perform(post(REQUEST_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(normalRequest)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();

            var responseEntity = mapper.readValue(response.getContentAsString(), FilterApiResponse.class);

            assertNotNull(responseEntity.trackingId());
            assertTrue(responseEntity.detected().stream().anyMatch(d -> normalRequest.text().contains(d.filteredWord())));
            assertTrue(responseEntity.filtered().isEmpty());
        }

        @Test
        @DisplayName("Urlencoded 형식으로 요청을 보낼 수 있다.")
        void test2() throws Exception {
            // given
            ApiRequest normalRequest = fixture.createNormalRequest("안녕하세요. 비속어를 검증합니다.");

            // when
            var response = mockMvc.perform(post(REQUEST_URL)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("text", normalRequest.text())
                            .param("mode", normalRequest.mode().name()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();

            var responseEntity = mapper.readValue(response.getContentAsString(), FilterApiResponse.class);

            assertNotNull(responseEntity.trackingId());
            assertTrue(responseEntity.detected().stream().anyMatch(d -> normalRequest.text().contains(d.filteredWord())));
            assertTrue(responseEntity.filtered().isEmpty());
        }
    }

    @Nested
    @DisplayName("Sanitize 타입의 요청을 할 수 있다.")
    class SanitizeRequestTest {

        @Test
        @DisplayName("Applicaion/json 형식으로 요청을 보낼 수 있다.")
        void test1() throws Exception {
            // given
            final String text = "안녕하세요. 비속어를 검증합니다.";
            final String filteredText = "안녕하세요. ***를 검증합니다.";
            final ApiRequest sanitizeRequest = fixture.createSanitizeRequest(text);

            // when
            var response = mockMvc.perform(post(REQUEST_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(sanitizeRequest)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();

            var responseEntity = mapper.readValue(response.getContentAsString(), FilterApiResponse.class);

            assertNotNull(responseEntity.trackingId());
            assertTrue(responseEntity.detected().stream().anyMatch(d -> sanitizeRequest.text().contains(d.filteredWord())));
            assertEquals(filteredText, responseEntity.filtered());
        }

        @Test
        @DisplayName("Urlencoded 형식으로 요청을 보낼 수 있다.")
        void test2() throws Exception {
            // given
            final String text = "안녕하세요. 비속어를 검증합니다.";
            final String filteredText = "안녕하세요. ***를 검증합니다.";
            final ApiRequest sanitizeRequest = fixture.createSanitizeRequest(text);

            // when
            var response = mockMvc.perform(post(REQUEST_URL)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .param("text", sanitizeRequest.text())
                            .param("mode", sanitizeRequest.mode().name()))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse();

            var responseEntity = mapper.readValue(response.getContentAsString(), FilterApiResponse.class);

            assertNotNull(responseEntity.trackingId());
            assertTrue(responseEntity.detected().stream().anyMatch(d -> sanitizeRequest.text().contains(d.filteredWord())));
            assertEquals(filteredText, responseEntity.filtered());
        }

    }
}
