package app.application;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@EnableAsync
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender javaMailSender;
    private final SpringTemplateEngine templateEngine;

    @Async
    public void sendEmailNotice(String email) {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        try {
            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "UTF-8");
            mimeMessageHelper.setTo(email);
            mimeMessageHelper.setSubject("인증 번호 안내"); // 메일 제목
            mimeMessageHelper.setText(setContext(todayDate()), true);
            mimeMessageHelper.setFrom("noreply@korea-profanity-filter.com", "한국어 비속어 검증 API [말조심하세욧]");
            javaMailSender.send(mimeMessage);

            log.info("Succeeded to send Email {}", email);
        } catch (Exception e) {
            log.info("Failed to send Email : {}", e.getMessage());
        }
    }

    public String todayDate() {
        ZonedDateTime todayDate = LocalDateTime.now(ZoneId.of("Asia/Seoul")).atZone(ZoneId.of("Asia/Seoul"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월 d일");
        return todayDate.format(formatter);
    }

    //thymeleaf를 통한 html 적용
    public String setContext(String date) {
        // 현재 시간으로부터 5분 후의 시간 계산
        LocalDateTime expirationTime = LocalDateTime.now(ZoneId.of("Asia/Seoul")).plusMinutes(5);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH시 mm분 ss초");
        String formattedTime = expirationTime.format(formatter);

        Context context = new Context();
        context.setVariable("code", date);
        context.setVariable("expirationTime", formattedTime);
        return templateEngine.process("todo", context);
    }
}
