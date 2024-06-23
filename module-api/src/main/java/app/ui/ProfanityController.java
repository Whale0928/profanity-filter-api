package app.ui;

import app.application.ProfanityFilter;
import app.request.ApiRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping("/api/v1/filter")
@RestController
public class ProfanityController {

    private final ProfanityFilter profanityService;

    public ProfanityController(ProfanityFilter profanityService) {
        this.profanityService = profanityService;
    }

    /**
     * APPLICATION_JSON_VALUE 미디어 타입으로 요청을 받는다.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> basicProfanity(@RequestBody @Valid ApiRequest request) {
        return ResponseEntity.ok(
                profanityService.basicFilter(request.text(), request.mode())
        );
    }

    /**
     * APPLICATION_FORM_URLENCODED_VALUE 미디어 타입으로 요청을 받는다.
     *
     * @param request the request
     * @return the response entity
     */
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<?> basicProfanityByUrlencodedValue(@ModelAttribute @Valid ApiRequest request) {
        return ResponseEntity.ok(
                profanityService.basicFilter(request.text(), request.mode())
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
        return ResponseEntity.ok(profanityService.healthCheck(request));
    }
}
