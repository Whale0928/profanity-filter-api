package app.application;

import app.core.data.constant.Mode;
import app.core.data.response.ApiResponse;
import jakarta.validation.constraints.NotNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class ProfanityService {

    private static final Logger log = LogManager.getLogger(ProfanityService.class);
    private final ProfanityFilterService filterService;


    public ProfanityService(ProfanityFilterService profanityFilterService) {
        this.filterService = profanityFilterService;
    }

    public ApiResponse basicFilter(
            @NotNull(message = "검사할 대상은 필수입니다.") String text,
            @NotNull(message = "검사 방식은 필수입니다.") Mode mode
    ) {
        log.info("[API] : request basicFilter: text={}, mode={}", text, mode);
        return filterService.requestFacadeFilter(text, mode);
    }

    public ApiResponse advancedFilter(String word) {
        return null;
    }
}
