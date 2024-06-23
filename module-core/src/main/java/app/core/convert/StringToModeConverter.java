package app.core.convert;

import app.core.data.constant.Mode;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToModeConverter implements Converter<String, Mode> {
    @Override
    public Mode convert(String source) {
        try {
            return Mode.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Mode 값이 잘못되었습니다.");
        }
    }
}
