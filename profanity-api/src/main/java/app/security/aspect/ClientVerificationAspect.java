package app.security.aspect;


import app.core.data.response.ApiResponse;
import app.core.data.response.Status;
import app.security.SecurityContextUtil;
import app.security.annotation.VerifiedClientOnly;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

import static app.core.data.response.constant.StatusCode.FORBIDDEN;

/**
 * 클라이언트 검증을 처리하는 AOP 클래스입니다.
 * VerifiedClientOnly 어노테이션이 붙은 메소드에 대해 권한을 체크합니다.
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class ClientVerificationAspect {

    /**
     * 클라이언트의 상태를 체크하고 차단되거나 폐기된 클라이언트의 접근을 제한합니다.
     */
    @Around("@annotation(app.security.annotation.VerifiedClientOnly)")
    public Object verifyClient(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        VerifiedClientOnly annotation = method.getAnnotation(VerifiedClientOnly.class);

        if (annotation.checkBlocked() && SecurityContextUtil.isBlockedClient()) {
            log.info("차단된 클라이언트의 접근이 감지되었습니다. : {}", SecurityContextUtil.getAuthentication());
            return ApiResponse.error(Status.of(FORBIDDEN, "차단된 클라이언트입니다."));
        }
        if (annotation.checkDiscarded() && SecurityContextUtil.isDiscardedClient()) {
            log.info("폐기된 클라이언트의 접근이 감지되었습니다. : {}", SecurityContextUtil.getAuthentication());
            return ApiResponse.error(Status.of(FORBIDDEN, "폐기된 클라이언트입니다."));
        }

        return joinPoint.proceed();
    }
}
