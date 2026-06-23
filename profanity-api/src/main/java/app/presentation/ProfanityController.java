package app.presentation;

import static app.application.HttpClient.getClientIP;
import static app.application.HttpClient.getReferrer;

import app.application.filter.ProfanityHandler;
import app.core.data.response.FilterApiResponse;
import app.core.util.ApiKeys;
import app.dto.request.ApiRequest;
import app.dto.request.FilterRequest;
import app.security.annotation.VerifiedClientOnly;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/v1/filter", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Profanity Filter", description = "비속어 검출 및 필터링 API")
public class ProfanityController {

  private final ProfanityHandler profanityHandler;

  @VerifiedClientOnly
  @Cacheable(value = "request_filter", key = "#request.text + '_' + #request.mode")
  @Operation(
      summary = "비속어 필터링 요청",
      description =
          """
          클라이언트 등록 후 발급받은 API Key로 비속어 검사를 요청합니다.
          QUICK은 원색적인 표현을 간략히 검증하고, NORMAL은 데이터베이스의 모든 비속어를 검증하며,
          FILTER는 검출된 단어를 마스킹해 반환합니다.
          """,
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              description = "비속어 필터링 요청 본문입니다. JSON과 form 요청은 같은 필드 계약을 사용합니다.",
              content = {
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiRequest.class)),
                @Content(
                    mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                    schema = @Schema(implementation = ApiRequest.class))
              }),
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FilterApiResponse> basicProfanity(
      HttpServletRequest httpRequest,
      @Parameter(description = "클라이언트 등록 후 발급받은 API Key", required = true)
          @RequestHeader(value = "x-api-key")
          String apiKey,
      @RequestBody @Valid ApiRequest request) {
    final String clientIp = getClientIP(httpRequest);
    final String referrer = getReferrer(httpRequest);

    log.info(
        "[FILTER] 요청 수신 host={} clientIp={} apiKey={} mode={} textLen={} async={}",
        httpRequest.getServerName(),
        clientIp,
        ApiKeys.mask(apiKey),
        request.mode(),
        request.text() == null ? 0 : request.text().length(),
        request.isAsync());

    final FilterRequest filterRequest =
        FilterRequest.create(request.text(), request.mode(), apiKey, clientIp, referrer);

    if (request.isAsync()) {
      FilterApiResponse response =
          profanityHandler.requestAsyncFilter(filterRequest, request.callbackUrl());
      return ResponseEntity.ok(response);
    }
    FilterApiResponse response = profanityHandler.requestFacadeFilter(filterRequest, null);
    return ResponseEntity.ok(response);
  }

  @VerifiedClientOnly
  @Cacheable(value = "request_filter", key = "#request.text + '_' + #request.mode")
  @Hidden
  @Operation(
      summary = "비속어 필터링 요청 form",
      description = "application/x-www-form-urlencoded 형식으로 비속어 검사를 요청합니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<FilterApiResponse> basicProfanityByUrlencodedValue(
      HttpServletRequest httpRequest,
      @Parameter(description = "클라이언트 등록 후 발급받은 API Key", required = true)
          @RequestHeader(value = "x-api-key")
          String apiKey,
      @ModelAttribute @Valid ApiRequest request) {
    String clientIp = getClientIP(httpRequest);
    String referrer = getReferrer(httpRequest);

    log.info(
        "[FILTER] 요청 수신(form) host={} clientIp={} apiKey={} mode={} textLen={}",
        httpRequest.getServerName(),
        clientIp,
        ApiKeys.mask(apiKey),
        request.mode(),
        request.text() == null ? 0 : request.text().length());

    final FilterRequest filterRequest =
        FilterRequest.create(request.text(), request.mode(), apiKey, clientIp, referrer);
    FilterApiResponse response = profanityHandler.requestFacadeFilter(filterRequest, null);
    return ResponseEntity.ok(response);
  }

  @VerifiedClientOnly
  @Cacheable(value = "request_filter", key = "{#word}")
  @Operation(
      summary = "고급 비속어 필터링 요청",
      description = "word 쿼리 파라미터로 전달한 단어를 고급 필터링합니다.",
      security = @SecurityRequirement(name = "ApiKeyAuth"))
  @PostMapping("/advanced")
  public ResponseEntity<FilterApiResponse> advancedProfanity(
      @Parameter(description = "클라이언트 등록 후 발급받은 API Key", required = true)
          @RequestHeader(value = "x-api-key")
          String apiKey,
      @Parameter(description = "검사할 단어", required = true) @RequestParam("word") String word) {
    log.info("[FILTER] 요청 수신(advanced) word={} apiKey={}", word, ApiKeys.mask(apiKey));
    Objects.requireNonNull(word, "단어는 필수 입니다.");
    return ResponseEntity.ok(profanityHandler.advancedFilter(word, null));
  }
}
