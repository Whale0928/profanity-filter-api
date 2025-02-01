package app.security.annotation;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * 차단되거나 폐기된 클라이언트의 접근을 제한하는 어노테이션입니다.
 * Method 레벨에서 사용되며, AOP를 통해 권한을 체크합니다.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VerifiedClientOnly {
    /**
     * 차단된 클라이언트 체크 여부
     */
    boolean checkBlocked() default true;

    /**
     * 폐기된 클라이언트 체크 여부
     */
    boolean checkDiscarded() default true;
}
