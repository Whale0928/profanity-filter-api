package app.ui;

import app.application.ProfanityService;
import app.request.ApiRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
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

    private final ProfanityService profanityService;

    public ProfanityController(ProfanityService profanityService) {
        this.profanityService = profanityService;
    }

    @PostMapping
    public ResponseEntity<?> basicProfanity(@RequestBody @Valid ApiRequest request) {
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
