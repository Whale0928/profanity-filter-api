package app.domain.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @deprecated 기존 일일 집계 저장소이며 수집 중단을 검토 중입니다.
 */
@Deprecated(forRemoval = true)
public interface ReportRepository {
  Optional<Report> findById(UUID id);

  Report save(Report Report);

  <S extends Report> List<S> saveAll(Iterable<S> entities);

  List<Report> findAll();

  List<Report> findAllByClientId(UUID clientId);

  void deleteById(UUID id);

  boolean existsById(UUID id);
}
