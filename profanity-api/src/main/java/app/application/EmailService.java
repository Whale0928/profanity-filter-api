package app.application;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class EmailService {
    private static final String EMAIL_KEY_PREFIX = "email:";  // 상수로 분리
    private static final String EMAIL_TEMPLATE = "email-template";  // 상수로 분리

    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;
    private final RedisTemplate<String, String> redisTemplate;
    private final Random random = new SecureRandom();

    @Async
    public void sendEmailNotice(String email) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            String emailVerificationCode = createEmailVerificationCode(email);
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject("인증 번호 안내"); // 메일 제목
            mimeMessageHelper.setText(setContext(emailVerificationCode), true);
            mimeMessageHelper.setFrom("noreply@korea-profanity-filter.com", "한국어 비속어 검증 API [말조심하세욧]");
            javaMailSender.send(mimeMessage);

            log.info("Succeeded to send Email {}", email);
        } catch (Exception e) {
            log.info("Failed to send Email : {}", e.getMessage());
        }
    }

    public String setContext(String code) {
        // 현재 시간으로부터 5분 후의 시간 계산
        LocalDateTime expirationTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(5);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH시 mm분 ss초");
        String formattedTime = expirationTime.format(formatter);

        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("expirationTime", formattedTime);
        return templateEngine.process(EMAIL_TEMPLATE, context);
    }

    public String createEmailVerificationCode(String email) {
        String code = String.format("%06d", random.nextInt(1000000));
        redisTemplate.opsForValue().set(EMAIL_KEY_PREFIX + email, code, Duration.ofMinutes(5));
        return code;
    }

    public boolean verifyEmailCode(String email, String code) {
        String storedCode = redisTemplate.opsForValue().get(EMAIL_KEY_PREFIX + email);
        if (storedCode != null && storedCode.equals(code)) {
            redisTemplate.delete(EMAIL_KEY_PREFIX + email);
            return true;
        }
        return false;
    }
}
