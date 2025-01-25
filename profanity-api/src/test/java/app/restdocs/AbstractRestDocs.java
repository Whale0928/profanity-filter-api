package app.restdocs;


import app.application.apikey.APIKeyGenerator;
import app.exception.GlobalExceptionHandler;
import app.security.SecurityContextUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.operation.preprocess.Preprocessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.CharacterEncodingFilter;

import static org.mockito.Mockito.mockStatic;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@Import(AbstractRestDocsConfig.class)
@ExtendWith(RestDocumentationExtension.class)
public abstract class AbstractRestDocs {

    protected final MockedStatic<SecurityContextUtil> securityUtil = mockStatic(SecurityContextUtil.class);
    protected final APIKeyGenerator apiKeyGenerator = new APIKeyGenerator("solt", "SHA-256");
    protected MockMvc mockMvc;
    protected ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp(RestDocumentationContextProvider provider) {
        this.mockMvc = MockMvcBuilders.standaloneSetup(initController())

                .apply(documentationConfiguration(provider)
                        .operationPreprocessors()
                        .withRequestDefaults(Preprocessors.prettyPrint())
                        .withResponseDefaults(Preprocessors.prettyPrint())
                )
                .alwaysDo(print())
                .setControllerAdvice(GlobalExceptionHandler.class)
                .addFilters(new CharacterEncodingFilter("UTF-8", true))
                .build();
    }

    @AfterEach
    void tearDown() {
        securityUtil.close();
    }

    protected abstract Object initController();
}
