package app.restdocs;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.restdocs.operation.preprocess.Preprocessors;

@TestConfiguration
public class RestDocsConfiguration {
	/**
	 * Write rest documentation result handler.
	 * rest docs 결과 출력 관련 설정
	 */
	@Bean
	public RestDocumentationResultHandler write() {
		return MockMvcRestDocumentation.document(
			"{class-name}/{method-name}", // 문서의 경로와 파일명을 지정
			Preprocessors.preprocessRequest(Preprocessors.prettyPrint()),// 문서의 request 부분을 예쁘게 출력
			Preprocessors.preprocessResponse(Preprocessors.prettyPrint())// 문서의 response 부분을 예쁘게 출력
		);
	}
}
