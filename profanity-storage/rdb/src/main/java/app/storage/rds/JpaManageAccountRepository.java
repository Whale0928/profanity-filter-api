package app.storage.rds;

import app.domain.manage.ManageAccount;
import app.domain.manage.ManageAccountRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaManageAccountRepository
    extends ManageAccountRepository, JpaRepository<ManageAccount, Long> {

  @Query("select m from manage_account m where m.password = :password")
  @Override
  Optional<ManageAccount> findByPassword(@Param("password") String password);
}
