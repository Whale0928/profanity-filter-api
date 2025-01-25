package app.restdocs;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;

@TestConfiguration
public class AbstractRestDocsConfig {
    /**
     * Write rest documentation result handler.
     * rest docs 결과 출력 관련 설정
     */
    @Bean
    public RestDocumentationResultHandler write() {
        return MockMvcRestDocumentation.document(
                "{class-name}/{method-name}",
                Preprocessors.preprocessRequest(Preprocessors.prettyPrint(),
                Preprocessors.modifyUris().scheme("https").host("api.profanity-filter.run").removePort()
                ),
                Preprocessors.preprocessResponse(Preprocessors.prettyPrint())
        );
    }
}
