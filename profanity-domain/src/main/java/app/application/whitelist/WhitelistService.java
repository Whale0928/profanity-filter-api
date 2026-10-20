package app.application.whitelist;

import app.core.data.response.constant.StatusCode;
import app.core.exception.BusinessException;
import app.domain.user.UserAccountRepository;
import app.domain.whitelist.Whitelist;
import app.domain.whitelist.WhitelistRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로그인 사용자가 자기 계정의 허용 단어 그룹을 관리합니다. */
@Service
@RequiredArgsConstructor
public class WhitelistService {

  public static final int MAX_GROUPS_PER_USER = 10;

  private final WhitelistRepository whitelistRepository;
  private final UserAccountRepository userAccountRepository;
  private final WhitelistReader whitelistReader;
  private final Clock loginAuthClock;

  @Transactional(readOnly = true)
  public List<WhitelistView> findAll(UUID userId) {
    return whitelistRepository.findAllByUserIdOrderByCreatedAtAscIdAsc(userId).stream()
        .map(WhitelistView::from)
        .toList();
  }

  /**
   * 허용 단어 그룹을 만듭니다.
   *
   * <p>동시에 들어온 생성 요청이 상한을 함께 통과하지 못하도록 계정 행을 잠근 뒤 개수를 셉니다.
   *
   * @throws BusinessException 계정당 상한을 넘거나 이름, 단어가 규칙에 맞지 않는 경우
   */
  @Transactional
  public WhitelistView create(UUID userId, String name, List<String> words) {
    userAccountRepository
        .findByIdForUpdate(userId)
        .orElseThrow(() -> new BusinessException(StatusCode.USER_NOT_FOUND));
    if (whitelistRepository.countByUserId(userId) >= MAX_GROUPS_PER_USER) {
      throw new BusinessException(
          StatusCode.WHITELIST_LIMIT_EXCEEDED,
          "허용 단어 그룹은 계정당 최대 " + MAX_GROUPS_PER_USER + "개까지 만들 수 있습니다.");
    }
    Instant now = loginAuthClock.instant();
    Whitelist saved = whitelistRepository.save(Whitelist.create(userId, name, words, now));
    return WhitelistView.from(saved);
  }

  /**
   * 이름과 단어 목록을 바꿉니다.
   *
   * @throws BusinessException 그룹이 없거나 다른 계정의 것인 경우
   */
  @Transactional
  public WhitelistView update(UUID userId, UUID whitelistId, String name, List<String> words) {
    Whitelist whitelist = ownedBy(userId, whitelistId);
    whitelist.update(name, words, loginAuthClock.instant());
    Whitelist saved = whitelistRepository.save(whitelist);
    whitelistReader.evict(whitelistId);
    return WhitelistView.from(saved);
  }

  /**
   * 그룹을 지웁니다. 지운 뒤 이 ID를 지정한 필터 요청은 오류로 응답합니다.
   *
   * @throws BusinessException 그룹이 없거나 다른 계정의 것인 경우
   */
  @Transactional
  public void delete(UUID userId, UUID whitelistId) {
    Whitelist whitelist = ownedBy(userId, whitelistId);
    whitelistRepository.delete(whitelist);
    whitelistReader.evict(whitelistId);
  }

  /** 없는 그룹과 남의 그룹을 같은 코드로 응답해 다른 계정의 그룹이 존재하는지 알 수 없게 합니다. */
  private Whitelist ownedBy(UUID userId, UUID whitelistId) {
    return whitelistRepository
        .findById(whitelistId)
        .filter(whitelist -> whitelist.isOwnedBy(userId))
        .orElseThrow(() -> new BusinessException(StatusCode.WHITELIST_NOT_FOUND));
  }

  /**
   * 대시보드에 보여 주는 허용 단어 그룹입니다.
   *
   * @param id 필터 요청의 whitelistIds에 넣는 값
   * @param words 고객이 입력한 그대로의 허용 단어
   */
  public record WhitelistView(
      UUID id,
      String name,
      List<String> words,
      int wordCount,
      Instant createdAt,
      Instant updatedAt) {

    public static WhitelistView from(Whitelist whitelist) {
      return new WhitelistView(
          whitelist.getId(),
          whitelist.getName(),
          whitelist.getWords(),
          whitelist.getWords().size(),
          whitelist.getCreatedAt(),
          whitelist.getUpdatedAt());
    }
  }
}
