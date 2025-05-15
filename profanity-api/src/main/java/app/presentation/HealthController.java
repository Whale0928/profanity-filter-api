package app.presentation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    @RequestMapping("/health")
    public String health() {
        return "OK";
    }

    @RequestMapping("/ping")
    public String ping() {
        return "PONG";
    }
}
