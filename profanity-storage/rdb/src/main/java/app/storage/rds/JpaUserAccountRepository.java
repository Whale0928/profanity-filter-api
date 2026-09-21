package app.storage.rds;

import app.domain.support.PageResult;
import app.domain.user.UserAccount;
import app.domain.user.UserAccountRepository;
import app.domain.user.UserRole;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaUserAccountRepository
    extends UserAccountRepository, JpaRepository<UserAccount, UUID> {

  @Override
  @Query("select count(u) from users u where u.createdAt >= :from")
  long countCreatedSince(@Param("from") Instant from);

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from users u where u.id = :id")
  Optional<UserAccount> findByIdForUpdate(@Param("id") UUID id);

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from users u where u.primaryEmail = :primaryEmail")
  Optional<UserAccount> findByPrimaryEmailForUpdate(@Param("primaryEmail") String primaryEmail);

  /**
   * 역할 파라미터를 JPQL에서 null 비교하지 않도록 분기합니다. AttributeConverter로 매핑한 enum을 {@code :param is null} 형태로
   * 비교하면 타입 추론이 불안정하기 때문입니다.
   */
  @Override
  default PageResult<UserAccount> searchForAdmin(String query, UserRole role, int page, int size) {
    Pageable pageable =
        PageResults.request(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    Slice<UserAccount> slice =
        role == null ? searchSlice(query, pageable) : searchSlice(query, role, pageable);
    return PageResults.from(slice);
  }

  @Query(
      """
      select u from users u
      where :query is null
         or lower(u.displayName) like lower(concat('%', :query, '%'))
         or lower(u.primaryEmail) like lower(concat('%', :query, '%'))
      """)
  Slice<UserAccount> searchSlice(@Param("query") String query, Pageable pageable);

  @Query(
      """
      select u from users u
      where u.role = :role
        and (:query is null
             or lower(u.displayName) like lower(concat('%', :query, '%'))
             or lower(u.primaryEmail) like lower(concat('%', :query, '%')))
      """)
  Slice<UserAccount> searchSlice(
      @Param("query") String query, @Param("role") UserRole role, Pageable pageable);
}
