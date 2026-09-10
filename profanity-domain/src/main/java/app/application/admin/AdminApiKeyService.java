package app.application.admin;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import app.domain.support.PageQuery;
import app.domain.support.PageResult;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 관리자 API Key 조회와 폐기 기능입니다. 키 해시와 원문은 어떤 응답에도 포함하지 않습니다. */
@Service
@RequiredArgsConstructor
public class AdminApiKeyService {

  private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

  private final ApiKeyRepository apiKeyRepository;
  private final UserAccountRepository userAccountRepository;
  private final AdminAuditService adminAuditService;
  private final Clock loginAuthClock;

  /**
   * API Key 목록을 조회합니다.
   *
   * @param status ACTIVE 또는 EXPIRED. null이면 전체입니다.
   */
  @Transactional(readOnly = true)
  public PageResult<AdminApiKeyView> search(
      String query, ApiKeyStatus status, PageQuery pageQuery) {
    PageResult<ApiKey> page =
        apiKeyRepository.searchForAdmin(
            PageQuery.normalizeQuery(query),
            status == null ? null : status == ApiKeyStatus.ACTIVE,
            pageQuery.page(),
            pageQuery.size());
    Map<UUID, String> ownerNames = ownerNames(page);
    return page.map(apiKey -> AdminApiKeyView.from(apiKey, ownerNames.get(apiKey.getUserId())));
  }

  /**
   * API Key를 폐기합니다.
   *
   * @throws BusinessException 대상이 없거나 이미 만료 또는 폐기된 경우
   */
  @Transactional
  public AdminApiKeyView revoke(UUID actorId, UUID apiKeyId, String reason) {
    ApiKey apiKey =
        apiKeyRepository
            .findByIdForUpdate(apiKeyId)
            .orElseThrow(() -> new BusinessException(StatusCode.API_KEY_NOT_FOUND));
    if (!apiKey.isActive()) {
      throw new BusinessException(StatusCode.API_KEY_ALREADY_REVOKED);
    }

    Instant now = loginAuthClock.instant();
    apiKey.revokeByAdmin(
        actorId, reason, LocalDateTime.ofInstant(now, SERVICE_ZONE).truncatedTo(ChronoUnit.MICROS));
    ApiKey saved = apiKeyRepository.save(apiKey);
    adminAuditService.record(
        actorId,
        AdminAuditAction.API_KEY_REVOKED,
        AdminAuditTargetType.API_KEY,
        apiKeyId.toString(),
        reason,
        now);
    return AdminApiKeyView.from(saved, ownerName(saved.getUserId()));
  }

  private String ownerName(UUID userId) {
    if (userId == null) {
      return null;
    }
    return userAccountRepository.findById(userId).map(UserAccount::getDisplayName).orElse(null);
  }

  private Map<UUID, String> ownerNames(PageResult<ApiKey> page) {
    Set<UUID> ownerIds =
        page.items().stream()
            .map(ApiKey::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    // 소유자가 없는 과거 키의 userId가 null이므로 null 키 조회를 허용하는 HashMap을 돌려준다.
    Map<UUID, String> names = new HashMap<>();
    if (ownerIds.isEmpty()) {
      return names;
    }
    userAccountRepository
        .findAllByIdIn(ownerIds)
        .forEach(owner -> names.putIfAbsent(owner.getId(), owner.getDisplayName()));
    return names;
  }

  /** 목록 필터에 사용하는 API Key 상태입니다. */
  public enum ApiKeyStatus {
    ACTIVE,
    EXPIRED
  }

  /**
   * 관리자 API Key 항목입니다. 키 원문과 해시는 포함하지 않습니다.
   *
   * @param ownerName 소유 사용자 표시 이름. 소유자가 없으면 null입니다.
   * @param lastUsedAt 마지막 사용 시각. 인증 경로 부하를 줄이려고 60초 간격으로 기록하므로 그만큼 지연될 수 있습니다.
   */
  public record AdminApiKeyView(
      UUID id,
      String name,
      String keyHint,
      String ownerName,
      String email,
      boolean active,
      LocalDateTime issuedAt,
      LocalDateTime expiredAt,
      LocalDateTime lastUsedAt) {

    public static AdminApiKeyView from(ApiKey apiKey, String ownerName) {
      return new AdminApiKeyView(
          apiKey.getId(),
          apiKey.getName(),
          apiKey.getKeyHint(),
          ownerName,
          apiKey.getEmail(),
          apiKey.isActive(),
          apiKey.getIssuedAt(),
          apiKey.getExpiredAt(),
          apiKey.getLastUsedAt());
    }
  }
}
