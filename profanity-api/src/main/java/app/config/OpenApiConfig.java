package app.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Profanity Filter API",
            version = "v1",
            description =
                """
                한국어와 영어 비속어를 검출하고 필터링하는 API입니다.
                정규식, 비속어 데이터베이스, 아호코라식 알고리즘을 사용해 QUICK, NORMAL, FILTER 모드를 제공합니다.
                """))
@SecurityScheme(
    name = "ApiKeyAuth",
    type = SecuritySchemeType.APIKEY,
    in = SecuritySchemeIn.HEADER,
    paramName = "x-api-key",
    description = "클라이언트 등록 후 발급받은 API Key")
public class OpenApiConfig {}
