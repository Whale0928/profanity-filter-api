package app.application;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpClient {

    private static final Logger log = LogManager.getLogger(HttpClient.class);
    private static final String[] HEADER_TYPES = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
    };

    public static String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");

        for (String headerType : HEADER_TYPES) {
            ip = request.getHeader(headerType);
            if (!Objects.isNull(ip) && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                break;
            }
        }

        if (Objects.isNull(ip) || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        log.info("실제 원격(클라이언트) IP 주소: {}", ip);
        return ip;
    }

    public static String getReferrer(HttpServletRequest request) {
        String referrer = request.getHeader("Referer");
        log.info("Referer : {}", referrer);
        return referrer;
    }

}
