package app.domain.constant;

public enum isUsed {
    Y("used"),
    N("not used");

    private final String description;

    isUsed(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
