package app.domain.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class UserRoleConverter implements AttributeConverter<UserRole, String> {
  @Override
  public String convertToDatabaseColumn(UserRole attribute) {
    return attribute == null ? null : attribute.name();
  }

  @Override
  public UserRole convertToEntityAttribute(String dbData) {
    return dbData == null ? null : UserRole.valueOf(dbData);
  }
}
