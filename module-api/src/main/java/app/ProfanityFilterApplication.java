package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories(basePackages = "app")
@EntityScan(basePackages = "app")
@SpringBootApplication(scanBasePackages = "app", scanBasePackageClasses = ProfanityFilterApplication.class)
public class ProfanityFilterApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProfanityFilterApplication.class, args);
    }
}
