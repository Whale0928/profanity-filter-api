package app.ui;

import app.application.SyncService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static app.application.HttpClient.getClientIP;
import static app.application.HttpClient.getReferrer;

@RequestMapping("/api/v1/sync")
@RestController
public class SyncController {

    private static final Logger log = LogManager.getLogger(SyncController.class);
    private final SyncService syncService;

    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping
    public ResponseEntity<?> doSync(
            HttpServletRequest httpRequest,
            @RequestParam("password") String password
    ) {
        String clientIp = getClientIP(httpRequest);
        String referrer = getReferrer(httpRequest);

        log.info("[API] <<do SyncService>> Client IP : {} / Referer : {} / password : {}", clientIp, referrer, password);

        return ResponseEntity.ok(syncService.doSync(password));
    }
}
