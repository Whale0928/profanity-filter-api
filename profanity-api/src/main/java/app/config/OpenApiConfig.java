package app.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
@SecurityScheme(
    name = "ApiKeyAuth",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "x-api-key",
    description = "클라이언트 등록 후 발급받은 API Key")
public class OpenApiConfig {

  @Bean
  OpenAPI openApi(
      @Value("classpath:openapi/overview.md") Resource overview,
      @Value("classpath:openapi/error-model.md") Resource errorModel,
      @Value("classpath:openapi/authentication.md") Resource authentication) {
    return new OpenAPI()
        .info(
            new Info()
                .title("Profanity Filter API")
                .version("v1")
                .description(readMarkdownInOrder(List.of(overview, errorModel, authentication))));
  }

  private static String readMarkdownInOrder(List<Resource> resources) {
    return resources.stream()
        .map(OpenApiConfig::readMarkdown)
        .reduce((left, right) -> left + "\n\n" + right)
        .orElse("");
  }

  private static String readMarkdown(Resource resource) {
    try (var inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("OpenAPI Markdown 문서를 읽을 수 없습니다.", exception);
    }
  }
}
