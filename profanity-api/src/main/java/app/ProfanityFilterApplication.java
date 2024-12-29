package app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class ProfanityFilterApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProfanityFilterApplication.class, args);
    }
}
