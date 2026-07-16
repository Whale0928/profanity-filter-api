package app.domain.user;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@Builder
@ToString(of = {"id", "userId", "provider"})
@EqualsAndHashCode(of = "id")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
@Entity(name = "oauth_accounts")
@Table(name = "oauth_accounts")
public class OAuthAccount {

  @Id
  @Builder.Default
  @Column(columnDefinition = "BINARY(16)")
  private UUID id = UUID.randomUUID();

  @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
  private UUID userId;

  @Convert(converter = OAuthProviderConverter.class)
  @Column(nullable = false, length = 30)
  private OAuthProvider provider;

  @Column(name = "provider_user_id", nullable = false, length = 255)
  private String providerUserId;

  @Column(name = "provider_email", length = 255)
  private String providerEmail;

  @Column(name = "email_verified", nullable = false, columnDefinition = "TINYINT")
  private boolean emailVerified;

  @Column(name = "provider_username", length = 100)
  private String providerUsername;

  @Column(name = "display_name", length = 100)
  private String displayName;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Column(name = "linked_at", nullable = false, updatable = false)
  private Instant linkedAt;

  public static OAuthAccount link(UUID userId, OAuthLoginProfile profile, Instant now) {
    return OAuthAccount.builder()
        .userId(userId)
        .provider(profile.provider())
        .providerUserId(profile.providerUserId())
        .providerEmail(profile.providerEmail())
        .emailVerified(profile.emailVerified())
        .providerUsername(profile.providerUsername())
        .displayName(profile.displayName())
        .avatarUrl(profile.avatarUrl())
        .linkedAt(now)
        .build();
  }

  public void synchronizeProfile(OAuthLoginProfile profile) {
    if (provider != profile.provider() || !providerUserId.equals(profile.providerUserId())) {
      throw new IllegalArgumentException("OAuth identity cannot be changed");
    }
    this.providerEmail = profile.providerEmail();
    this.emailVerified = profile.emailVerified();
    this.providerUsername = profile.providerUsername();
    this.displayName = profile.displayName();
    this.avatarUrl = profile.avatarUrl();
  }
}
