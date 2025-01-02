package app.domain.client;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

@Converter
public class PermissionsTypeConverter
        implements AttributeConverter<List<PermissionsType>, String> {

    private static final String SPLIT_CHAR = "/";

    @Override
    public String convertToDatabaseColumn(List<PermissionsType> attribute) {
        if (attribute != null) {
            return attribute.stream().map(PermissionsType::name).reduce((a, b) -> a + SPLIT_CHAR + b).orElse("");
        }
        return "";
    }

    @Override
    public List<PermissionsType> convertToEntityAttribute(String dataset) {
        if (dataset == null || dataset.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(dataset.split(SPLIT_CHAR)).map(PermissionsType::valueOf).toList();
    }

}
