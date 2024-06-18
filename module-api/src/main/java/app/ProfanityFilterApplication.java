package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "app")
public class ProfanityFilterApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProfanityFilterApplication.class, args);
    }
}
