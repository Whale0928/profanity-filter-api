package app.presentation;

import app.openapi.HealthOpenApi;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "서버 상태 확인 API")
public class HealthController {

  @HealthOpenApi.Health
  @GetMapping(value = "/health", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("OK");
  }

  @HealthOpenApi.Ping
  @GetMapping(value = "/ping", produces = MediaType.TEXT_PLAIN_VALUE)
  public ResponseEntity<String> ping() {
    return ResponseEntity.ok("PONG");
  }
}
