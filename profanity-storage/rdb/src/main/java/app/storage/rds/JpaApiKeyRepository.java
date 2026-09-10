package app.storage.rds;

import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import app.domain.support.PageResult;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaApiKeyRepository extends ApiKeyRepository, JpaRepository<ApiKey, UUID> {

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select k from api_keys k where k.id = :id")
  Optional<ApiKey> findByIdForUpdate(@Param("id") UUID id);

  @Override
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select k from api_keys k where k.id = :id and k.userId = :userId")
  Optional<ApiKey> findByIdAndUserIdForUpdate(@Param("id") UUID id, @Param("userId") UUID userId);

  @Override
  default PageResult<ApiKey> searchForAdmin(String query, Boolean activeOnly, int page, int size) {
    Pageable pageable =
        PageResults.request(page, size, Sort.by(Sort.Direction.DESC, "issuedAt", "id"));
    Slice<ApiKey> slice;
    if (activeOnly == null) {
      slice = searchSlice(query, pageable);
    } else if (activeOnly) {
      slice = searchActiveSlice(query, pageable);
    } else {
      slice = searchExpiredSlice(query, pageable);
    }
    return PageResults.from(slice);
  }

  @Query(
      """
      select k from api_keys k
      where :query is null
         or lower(k.name) like lower(concat('%', :query, '%'))
         or lower(k.email) like lower(concat('%', :query, '%'))
         or lower(k.keyHint) like lower(concat('%', :query, '%'))
      """)
  Slice<ApiKey> searchSlice(@Param("query") String query, Pageable pageable);

  @Query(
      """
      select k from api_keys k
      where k.expiredAt is null
        and (:query is null
             or lower(k.name) like lower(concat('%', :query, '%'))
             or lower(k.email) like lower(concat('%', :query, '%'))
             or lower(k.keyHint) like lower(concat('%', :query, '%')))
      """)
  Slice<ApiKey> searchActiveSlice(@Param("query") String query, Pageable pageable);

  @Query(
      """
      select k from api_keys k
      where k.expiredAt is not null
        and (:query is null
             or lower(k.name) like lower(concat('%', :query, '%'))
             or lower(k.email) like lower(concat('%', :query, '%'))
             or lower(k.keyHint) like lower(concat('%', :query, '%')))
      """)
  Slice<ApiKey> searchExpiredSlice(@Param("query") String query, Pageable pageable);

  @Override
  @Modifying
  @Query(
      """
      update api_keys k
      set k.userId = :userId
      where k.userId is null and lower(k.email) = lower(:email)
      """)
  int claimUnownedByEmail(@Param("userId") UUID userId, @Param("email") String email);

  @Override
  @Modifying
  @Query(
      """
      update api_keys k
      set k.requestCount = (select count(r.id)
                            from records r
                            where r.apiKeyHash = k.keyHash)
      where k.expiredAt is null
      """)
  void updateRequestCount();
}
