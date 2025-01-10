package app.presentation;

import app.application.filter.ProfanityHandler;
import app.core.data.response.FilterApiResponse;
import app.dto.request.ApiRequest;
import app.dto.request.FilterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static app.application.HttpClient.getClientIP;
import static app.application.HttpClient.getReferrer;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/filter")
public class ProfanityController {

    private final ProfanityHandler profanityHandler;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> basicProfanity(
            HttpServletRequest httpRequest,
            @RequestHeader(value = "x-api-key") String apiKey,
            @RequestBody @Valid ApiRequest request
    ) {
        final String clientIp = getClientIP(httpRequest);
        final String referrer = getReferrer(httpRequest);

        log.info("api key : {}", apiKey);
        log.info("[API-JSON] Client IP : {} / Referer : {} / Request : {}", clientIp, referrer, request);
        final FilterRequest filterRequest = FilterRequest.create(request.text(), request.mode(), apiKey, clientIp, referrer);
        FilterApiResponse response = profanityHandler.requestFacadeFilter(filterRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> basicProfanityByUrlencodedValue(
            HttpServletRequest httpRequest,
            @RequestHeader(value = "x-api-key") String apiKey,
            @ModelAttribute @Valid ApiRequest request
    ) {
        String clientIp = getClientIP(httpRequest);
        String referrer = getReferrer(httpRequest);

        log.info("[API-URLENCODED]  Client IP : {} / Referer : {} / Request : {}", clientIp, referrer, request);

        final FilterRequest filterRequest = FilterRequest.create(request.text(), request.mode(), apiKey, clientIp, referrer);
        FilterApiResponse response = profanityHandler.requestFacadeFilter(filterRequest);
        return ResponseEntity.ok(response
        );
    }

    @PostMapping("/advanced")
    public ResponseEntity<?> advancedProfanity(
            @RequestHeader(value = "x-api-key") String apiKey,
            @RequestParam("word") String word
    ) {
        log.info("[API-Advanced] Request : {} , key :{}", word, apiKey);
        Objects.requireNonNull(word, "단어는 필수 입니다.");
        return ResponseEntity.ok(profanityHandler.advancedFilter(word)
        );
    }
}
