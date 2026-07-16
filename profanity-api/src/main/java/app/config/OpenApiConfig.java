package app.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecuritySchemes({
  @SecurityScheme(
      name = "ApiKeyAuth",
      type = SecuritySchemeType.APIKEY,
      in = SecuritySchemeIn.HEADER,
      paramName = "x-api-key",
      description = "SSO 로그인 후 개발자 포털에서 발급받은 API Key"),
  @SecurityScheme(
      name = "LoginJwtAuth",
      type = SecuritySchemeType.HTTP,
      scheme = "bearer",
      bearerFormat = "JWT",
      description = "Google/GitHub SSO 로그인 후 발급된 대시보드 access token")
})
public class OpenApiConfig {

  @Bean
  OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Profanity Filter API")
                .version("v1")
                .description("한국어와 영어 비속어를 검출하고 필터링하는 API입니다."));
  }
}
