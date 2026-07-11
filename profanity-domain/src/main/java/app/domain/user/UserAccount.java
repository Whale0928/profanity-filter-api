package app.domain.user;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder(access = PRIVATE)
@ToString(of = {"id", "displayName", "status"})
@EqualsAndHashCode(of = "id")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
@Entity(name = "users")
@Table(name = "users")
public class UserAccount {

  @Id
  @Builder.Default
  @Column(columnDefinition = "BINARY(16)")
  private UUID id = UUID.randomUUID();

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "primary_email", nullable = false, unique = true, length = 255)
  private String primaryEmail;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Builder.Default
  @Convert(converter = UserStatusConverter.class)
  @Column(nullable = false, length = 30)
  private UserStatus status = UserStatus.ACTIVE;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public static UserAccount create(
      String displayName, String primaryEmail, String avatarUrl, Instant now) {
    String requiredDisplayName = requireDisplayName(displayName);
    Instant requiredNow = Objects.requireNonNull(now, "now must not be null");
    return UserAccount.builder()
        .displayName(requiredDisplayName)
        .primaryEmail(requirePrimaryEmail(primaryEmail))
        .avatarUrl(blankToNull(avatarUrl))
        .createdAt(requiredNow)
        .updatedAt(requiredNow)
        .build();
  }

  public void synchronizeProfile(
      String displayName, String primaryEmail, String avatarUrl, Instant now) {
    String requiredDisplayName = requireDisplayName(displayName);
    String requiredPrimaryEmail = requirePrimaryEmail(primaryEmail);
    String synchronizedAvatarUrl = blankToNull(avatarUrl);
    Instant requiredNow = Objects.requireNonNull(now, "now must not be null");

    this.displayName = requiredDisplayName;
    this.primaryEmail = requiredPrimaryEmail;
    if (synchronizedAvatarUrl != null) {
      this.avatarUrl = synchronizedAvatarUrl;
    }
    this.updatedAt = requiredNow;
  }

  public boolean isActive() {
    return status == UserStatus.ACTIVE;
  }

  public void disable(Instant now) {
    this.status = UserStatus.DISABLED;
    this.updatedAt = Objects.requireNonNull(now, "now must not be null");
  }

  private static String requireDisplayName(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("displayName must not be blank");
    }
    return value.trim();
  }

  private static String requirePrimaryEmail(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("primaryEmail must not be blank");
    }
    return value.trim().toLowerCase(Locale.ROOT);
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
