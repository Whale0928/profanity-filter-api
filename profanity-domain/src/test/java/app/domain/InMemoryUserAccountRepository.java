package app.domain;

import app.domain.support.PageResult;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import app.domain.user.UserRole;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class InMemoryUserAccountRepository implements UserAccountRepository {

  private final Map<UUID, UserAccount> values = new LinkedHashMap<>();

  @Override
  public Optional<UserAccount> findById(UUID id) {
    return Optional.ofNullable(values.get(id));
  }

  @Override
  public Optional<UserAccount> findByIdForUpdate(UUID id) {
    return findById(id);
  }

  @Override
  public Optional<UserAccount> findByPrimaryEmailForUpdate(String primaryEmail) {
    return values.values().stream()
        .filter(user -> user.getPrimaryEmail().equalsIgnoreCase(primaryEmail.trim()))
        .findFirst();
  }

  @Override
  public long countCreatedSince(Instant from) {
    return values.values().stream().filter(user -> !user.getCreatedAt().isBefore(from)).count();
  }

  @Override
  public UserAccount save(UserAccount userAccount) {
    values.put(userAccount.getId(), userAccount);
    return userAccount;
  }

  @Override
  public List<UserAccount> findAllByIdIn(Collection<UUID> ids) {
    return ids.stream().map(values::get).filter(Objects::nonNull).toList();
  }

  @Override
  public PageResult<UserAccount> searchForAdmin(String query, UserRole role, int page, int size) {
    List<UserAccount> matched =
        values.values().stream()
            .filter(user -> role == null || user.getRole() == role)
            .filter(user -> query == null || matches(user, query))
            .toList();
    int from = Math.min(page * size, matched.size());
    int to = Math.min(from + size, matched.size());
    return PageResult.of(matched.subList(from, to), page, to < matched.size());
  }

  private static boolean matches(UserAccount user, String query) {
    String keyword = query.toLowerCase(Locale.ROOT);
    return user.getDisplayName().toLowerCase(Locale.ROOT).contains(keyword)
        || user.getPrimaryEmail().toLowerCase(Locale.ROOT).contains(keyword);
  }

  /**
   * 테스트에서만 관리자 계정을 만듭니다.
   *
   * <p>역할 변경 API를 만들지 않는 계약을 지키려고 운영 코드에 승격 수단을 두지 않았기 때문에, 테스트에서는 저장된 값을 직접 바꿉니다.
   */
  public static UserAccount asAdmin(UserAccount userAccount) {
    try {
      Field field = UserAccount.class.getDeclaredField("role");
      field.setAccessible(true);
      field.set(userAccount, UserRole.ADMIN);
      return userAccount;
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException(exception);
    }
  }
}
