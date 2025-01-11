package app.domain.client;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum PermissionsType {
    READ("읽기", "READ", 0),
    WRITE("쓰기", "WRITE", 1),
    DELETE("삭제", "DELETE", 2);

    private final String description;
    private final String value;
    private final int code;

    public static List<PermissionsType> defaultPermissions() {
        return List.of(READ);
    }

    public static List<PermissionsType> allPermissions() {
        return List.of(READ, WRITE, DELETE);
    }

    public static List<PermissionsType> readOnlyPermissions() {
        return List.of(READ);
    }

    public static List<PermissionsType> permissionsByTokenType(String type) {
        return switch (type) {
            case "FULL" -> allPermissions();
            case "LIMITED", "TEMPORARY" -> readOnlyPermissions();
            default -> defaultPermissions();
        };
    }
}
