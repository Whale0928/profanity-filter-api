package app.domain.client;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Getter
@Builder
@ToString(of = {"id", "name", "email", "apiKey"})
@EqualsAndHashCode(of = "id")
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
@Entity(name = "clients")
@Table(name = "clients")
public class Clients {

    @Id
    @Builder.Default
    @Comment("클라이언트 고유 식별자")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id = UUID.randomUUID();

    @Comment("클라이언트명")
    @Column(nullable = false)
    private String name;

    @Comment("이메일")
    @Column(nullable = false, unique = true)
    private String email;

    @Comment("API 키")
    @Column(name = "api_key", nullable = false, unique = true)
    private String apiKey;

    @Comment("발급자 정보")
    @Column(name = "issuer_info", nullable = false)
    private String issuerInfo;

    @Comment("비고")
    @Column(name = "note")
    private String note;

    @Builder.Default
    @Comment("권한")
    @Column(name = "permissions", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = PermissionsTypeConverter.class)
    private List<PermissionsType> permissions = PermissionsType.defaultPermissions();

    @Builder.Default
    @Comment("발급일시")
    @Column(name = "issued_at")
    private LocalDateTime issuedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

    @Comment("만료일시")
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Builder.Default
    @Comment("요청 횟수")
    @Column(name = "request_count")
    private Long requestCount = 0L;
}
