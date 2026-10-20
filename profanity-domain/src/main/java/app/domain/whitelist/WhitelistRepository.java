package app.domain.whitelist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WhitelistRepository {
  Whitelist save(Whitelist whitelist);

  Optional<Whitelist> findById(UUID id);

  /** 계정의 허용 단어 그룹을 만든 순서대로 조회합니다. */
  List<Whitelist> findAllByUserIdOrderByCreatedAtAscIdAsc(UUID userId);

  long countByUserId(UUID userId);

  void delete(Whitelist whitelist);
}
