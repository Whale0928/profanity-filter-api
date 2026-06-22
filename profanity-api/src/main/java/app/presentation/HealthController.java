package app.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Health", description = "서버 상태 확인 API")
public class HealthController {

  @Operation(summary = "헬스 체크", description = "서버 상태를 확인합니다. 정상 상태이면 OK를 반환합니다.")
  @GetMapping("/health")
  public ResponseEntity<String> health() {
    return ResponseEntity.ok("OK");
  }

  @Operation(summary = "핑 체크", description = "서버 응답 상태를 확인합니다. 정상 상태이면 PONG을 반환합니다.")
  @GetMapping("/ping")
  public ResponseEntity<String> ping() {
    return ResponseEntity.ok("PONG");
  }
}
