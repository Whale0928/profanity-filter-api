package app.presentation;

import static app.application.HttpClient.getClientIP;
import static app.application.HttpClient.getReferrer;

import app.application.filter.ProfanityHandler;
import app.core.data.response.FilterApiResponse;
import app.core.util.ApiKeys;
import app.dto.request.ApiRequest;
import app.dto.request.FilterRequest;
import app.openapi.ProfanityOpenApi;
import app.security.SecurityContextUtil;
import app.security.annotation.VerifiedClientOnly;
import io.swagger.v3.oas.annotations.Hidden;
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
@ProfanityOpenApi.ApiTag
public class ProfanityController {

  private final ProfanityHandler profanityHandler;

  @VerifiedClientOnly
  @Cacheable(value = "request_filter", key = "#request.text + '_' + #request.mode")
  @ProfanityOpenApi.BasicProfanity
  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<FilterApiResponse> basicProfanity(
      HttpServletRequest httpRequest,
      @RequestHeader(value = "x-api-key") String apiKey,
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
        FilterRequest.create(
            request.text(),
            request.mode(),
            SecurityContextUtil.getCurrentApiKeyHash(),
            clientIp,
            referrer);

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
  @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public ResponseEntity<FilterApiResponse> basicProfanityByUrlencodedValue(
      HttpServletRequest httpRequest,
      @RequestHeader(value = "x-api-key") String apiKey,
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
        FilterRequest.create(
            request.text(),
            request.mode(),
            SecurityContextUtil.getCurrentApiKeyHash(),
            clientIp,
            referrer);
    FilterApiResponse response = profanityHandler.requestFacadeFilter(filterRequest, null);
    return ResponseEntity.ok(response);
  }

  @VerifiedClientOnly
  @Cacheable(value = "request_filter", key = "{#word}")
  @ProfanityOpenApi.AdvancedProfanity
  @PostMapping("/advanced")
  public ResponseEntity<FilterApiResponse> advancedProfanity(
      @RequestHeader(value = "x-api-key") String apiKey, @RequestParam("word") String word) {
    log.info("[FILTER] 요청 수신(advanced) word={} apiKey={}", word, ApiKeys.mask(apiKey));
    Objects.requireNonNull(word, "단어는 필수 입니다.");
    return ResponseEntity.ok(profanityHandler.advancedFilter(word, null));
  }
}
