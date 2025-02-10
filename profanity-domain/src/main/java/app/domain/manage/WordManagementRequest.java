package app.domain.manage;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity(name = "word_management")
@Table(name = "word_management")
@Getter
@Builder
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class WordManagementRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("요청 유저 아이디")
    @Column(name = "request_user_id", nullable = false)
    private UUID requestUserId;

    @Comment("단어")
    @Column(nullable = false)
    private String word;

    @Comment("사유")
    @Column(nullable = false)
    private String reason;

    @Comment("심각도")
    @Column(nullable = false)
    private String severity;

    @Comment("요청 타입")
    @Column(nullable = false)
    private String requestType;

    @Comment("상태")
    @Builder.Default
    @Column(nullable = false)
    private String status = "REQUEST";

    @Comment("요청일시")
    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
}
