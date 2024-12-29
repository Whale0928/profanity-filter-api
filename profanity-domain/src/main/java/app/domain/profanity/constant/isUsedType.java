package app.domain.profanity.constant;

import lombok.Getter;

@Getter
public enum isUsedType {
    Y("used"),
    N("not used");

    private final String description;

    isUsedType(String description) {
        this.description = description;
    }

}
