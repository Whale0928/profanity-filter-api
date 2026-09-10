package app.application.inquiry;

import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import app.domain.inquiry.Inquiry;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 문의 목록과 상세에 보여 줄 요청자 정보를 모읍니다. 사용자 계정은 한 번에 조회하고 API Key는 사용자 계정이 없는 문의에만 조회합니다. */
@Component
@RequiredArgsConstructor
public class InquiryRequesterResolver {

  private final UserAccountRepository userAccountRepository;
  private final ApiKeyRepository apiKeyRepository;

  public InquiryRequester resolve(Inquiry inquiry) {
    return resolveAll(List.of(inquiry)).getOrDefault(inquiry.getId(), InquiryRequester.unknown());
  }

  /** 문의 식별자별 요청자 정보를 반환합니다. */
  public Map<Long, InquiryRequester> resolveAll(Collection<Inquiry> inquiries) {
    Map<UUID, UserAccount> users = findUsers(inquiries);
    Map<Long, InquiryRequester> resolved = new HashMap<>();
    for (Inquiry inquiry : inquiries) {
      resolved.put(inquiry.getId(), resolveOne(inquiry, users));
    }
    return resolved;
  }

  /** 답변 작성자 이름을 한 번에 조회합니다. */
  public Map<UUID, String> resolveAuthorNames(Collection<UUID> authorIds) {
    List<UUID> ids = authorIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) {
      return Map.of();
    }
    return userAccountRepository.findAllByIdIn(ids).stream()
        .collect(Collectors.toMap(UserAccount::getId, UserAccount::getDisplayName));
  }

  private InquiryRequester resolveOne(Inquiry inquiry, Map<UUID, UserAccount> users) {
    UUID requesterUserId = inquiry.getRequesterUserId();
    UserAccount user = requesterUserId == null ? null : users.get(requesterUserId);
    if (user != null) {
      return new InquiryRequester(user.getDisplayName(), user.getPrimaryEmail());
    }
    if (inquiry.getRequesterApiKeyId() == null) {
      return InquiryRequester.unknown();
    }
    Optional<ApiKey> apiKey = apiKeyRepository.findById(inquiry.getRequesterApiKeyId());
    return apiKey
        .map(key -> new InquiryRequester(key.getName(), key.getEmail()))
        .orElseGet(InquiryRequester::unknown);
  }

  private Map<UUID, UserAccount> findUsers(Collection<Inquiry> inquiries) {
    List<UUID> userIds =
        inquiries.stream()
            .map(Inquiry::getRequesterUserId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return userAccountRepository.findAllByIdIn(userIds).stream()
        .collect(Collectors.toMap(UserAccount::getId, Function.identity()));
  }
}
