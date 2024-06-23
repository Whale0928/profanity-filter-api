package app.domain.constant;

public enum isUsedType {
    Y("used"),
    N("not used");

    private final String description;

    isUsedType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
