package app.core.data.constant;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum Mode {
    QUICK,
    NORMAL,
    FILTER;

    @JsonCreator
    public static Mode fromString(String value) {
        return Arrays.stream(Mode.values()).findFirst()
                .filter(mode -> mode.name().equalsIgnoreCase(value))
                .orElse(null);
    }
}
