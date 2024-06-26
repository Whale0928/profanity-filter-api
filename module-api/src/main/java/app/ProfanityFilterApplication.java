package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "app", scanBasePackageClasses = ProfanityFilterApplication.class)
public class ProfanityFilterApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProfanityFilterApplication.class, args);
    }
}
