package app.presentation;

import app.application.ProfanityFilterService;
import app.dto.request.ApiRequest;
import app.dto.request.FilterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/v1/filter")
@RestController
public class ProfanityController {

    private static final Logger log = LogManager.getLogger(ProfanityController.class);
    private final ProfanityFilterService profanityService;

    public ProfanityController(ProfanityFilterService profanityService) {
        this.profanityService = profanityService;
    }

    /**
     * APPLICATION_JSON_VALUE 미디어 타입으로 요청을 받는다.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> basicProfanity(
            HttpServletRequest httpRequest,
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @RequestBody @Valid ApiRequest request
    ) {
        String clientIp = getClientIP(httpRequest);
        String referrer = getReferrer(httpRequest);

        log.info("[API-JSON] Client IP : {} / Referer : {} / Request : {}", clientIp, referrer, request);

        FilterRequest filterRequest = FilterRequest.create(request.text(), request.mode(), apiKey, clientIp, referrer);

        return ResponseEntity.ok(
                profanityService.basicFilter(filterRequest)
        );
    }

    /**
     * APPLICATION_FORM_URLENCODED_VALUE 미디어 타입으로 요청을 받는다.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> basicProfanityByUrlencodedValue(
            HttpServletRequest httpRequest,
            @RequestHeader(value = "x-api-key", required = false) String apiKey,
            @ModelAttribute @Valid ApiRequest request
    ) {
        String clientIp = getClientIP(httpRequest);
        String referrer = getReferrer(httpRequest);

        log.info("[API-URLENCODED]  Client IP : {} / Referer : {} / Request : {}", clientIp, referrer, request);

        FilterRequest filterRequest = FilterRequest.create(request.text(), request.mode(), apiKey, clientIp, referrer);

        return ResponseEntity.ok(
                profanityService.basicFilter(filterRequest)
        );
    }

    @GetMapping("/advanced")
    public ResponseEntity<?> advancedProfanity(@RequestParam("word") String word) {
        Objects.requireNonNull(word, "단어는 필수 입니다.");
        return ResponseEntity.ok(
                profanityService.advancedFilter(word)
        );
    }

    @PostMapping("/health")
    public ResponseEntity<?> healthCheck(@RequestBody @Valid ApiRequest request) {
        return ResponseEntity.ok("OK");
    }
}
