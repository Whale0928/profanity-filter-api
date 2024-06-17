package app.ui;

import app.application.ProfanityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RequestMapping("/api/v1/filter")
@RestController
public class ProfanityController {

    private final ProfanityService profanityService;

    public ProfanityController(ProfanityService profanityService) {
        this.profanityService = profanityService;
    }

    @GetMapping("/basic")
    public ResponseEntity<?> basicProfanity(
            @RequestParam("word") String word
    ) {
        Objects.requireNonNull(word, "단어는 필수 입니다.");
        return ResponseEntity.ok(
                profanityService.basicFilter(word)
        );
    }

    @GetMapping("/advanced")
    public ResponseEntity<?> advancedProfanity(
            @RequestParam("word") String word
    ) {
        Objects.requireNonNull(word, "단어는 필수 입니다.");
        return ResponseEntity.ok(
                profanityService.advancedFilter(word)
        );
    }
}
