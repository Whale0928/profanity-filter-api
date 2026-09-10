package app.domain.manage;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity(name = "word_management")
@Table(name = "word_management")
@Getter
@Builder
@AllArgsConstructor(access = PRIVATE)
@NoArgsConstructor(access = PROTECTED)
public class WordManagementRequest {

  public static final String STATUS_REQUEST = "REQUEST";
  public static final String STATUS_APPROVED = "APPROVED";
  public static final String STATUS_REJECTED = "REJECTED";

  /** V5 이전에 승인 처리된 요청이 사용하던 상태 값입니다. */
  public static final String STATUS_LEGACY_APPROVED = "OK";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Comment("요청 API Key 아이디. 로그인 사용자가 대시보드에서 등록하면 비어 있습니다.")
  @Column(name = "request_user_id")
  private UUID requestUserId;

  @Comment("연결된 문의 아이디")
  @Column(name = "inquiry_id", unique = true)
  private Long inquiryId;

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
  private String status = STATUS_REQUEST;

  @Comment("요청일시")
  @Builder.Default
  @Column(nullable = false)
  private LocalDateTime requestedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

  /** 아직 승인도 거절도 되지 않은 요청인지 확인합니다. */
  public boolean isPending() {
    return STATUS_REQUEST.equals(status);
  }

  /** 승인 결과를 기록합니다. 사전 반영 여부와 무관하게 상태만 바꿉니다. */
  public void approve() {
    this.status = STATUS_APPROVED;
  }

  /** 거절 결과를 기록합니다. 사전은 변경하지 않습니다. */
  public void reject() {
    this.status = STATUS_REJECTED;
  }

  /** 외부 API가 노출하는 요청 유형입니다. 저장된 값이 알려진 유형이 아니면 null입니다. */
  public WordRequestType exposedRequestType() {
    return WordRequestType.fromStored(requestType);
  }

  /** 외부에 노출하는 상태 값입니다. 과거에 승인 처리된 OK도 APPROVED로 통일해 보여 줍니다. */
  public String exposedStatus() {
    return STATUS_LEGACY_APPROVED.equals(status) ? STATUS_APPROVED : status;
  }
}
