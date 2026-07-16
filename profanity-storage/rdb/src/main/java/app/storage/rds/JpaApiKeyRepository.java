package app.storage.rds;

import app.domain.apikey.ApiKey;
import app.domain.apikey.ApiKeyRepository;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaApiKeyRepository extends ApiKeyRepository, JpaRepository<ApiKey, UUID> {

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
