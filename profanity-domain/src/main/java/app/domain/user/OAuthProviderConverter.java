package app.domain.user;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class OAuthProviderConverter implements AttributeConverter<OAuthProvider, String> {

  @Override
  public String convertToDatabaseColumn(OAuthProvider attribute) {
    return attribute == null ? null : attribute.value();
  }

  @Override
  public OAuthProvider convertToEntityAttribute(String dbData) {
    return dbData == null ? null : OAuthProvider.from(dbData);
  }
}
