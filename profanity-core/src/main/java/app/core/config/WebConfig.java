package app.core.config;

import app.core.convert.StringToModeConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final StringToModeConverter stringToModeConverter;

    public WebConfig(StringToModeConverter stringToModeConverter) {
        this.stringToModeConverter = stringToModeConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToModeConverter);
    }
}
