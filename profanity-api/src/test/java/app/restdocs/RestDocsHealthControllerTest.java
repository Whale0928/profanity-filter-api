package app.restdocs;

import app.presentation.HealthController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseBody;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("restdocs")
class RestDocsHealthControllerTest extends AbstractRestDocs {

    @Override
    protected Object initController() {
        return new HealthController();
    }

    @Test
    @DisplayName("헬스 상태 체크 API")
    void step_1() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andDo(document("api/health",
                        responseBody()
                ));
    }


    @Test
    @DisplayName("핑 체크 API")
    void step_2() throws Exception {
        mockMvc.perform(get("/api/v1/ping"))
                .andExpect(status().isOk())
                .andDo(document("api/ping",
                        responseBody()
                ));
    }
}
