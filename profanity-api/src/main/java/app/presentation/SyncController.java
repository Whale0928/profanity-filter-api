package app.presentation;

import static app.application.HttpClient.getClientIP;
import static app.application.HttpClient.getReferrer;

import app.application.manage.SyncHandler;
import app.core.data.manage.response.ResultMessage;
import app.openapi.SyncOpenApi;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RequestMapping("/api/v1/sync")
@RestController
@SyncOpenApi.ApiTag
public class SyncController {

  private final SyncHandler syncHandler;

  @SyncOpenApi.DoSync
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<ResultMessage> doSync(
      HttpServletRequest httpRequest, @RequestParam("password") String password) {
    final String clientIp = getClientIP(httpRequest);
    final String referrer = getReferrer(httpRequest);

    Objects.requireNonNull(password, "비밀번호를 제공해야 합니다.");

    log.info(
        "[API] <<do Sync>> Client IP : {} / Referer : {} / password : {}",
        clientIp,
        referrer,
        password);
    ResultMessage resultMessage = syncHandler.doSync(password);
    return ResponseEntity.ok(resultMessage);
  }
}
