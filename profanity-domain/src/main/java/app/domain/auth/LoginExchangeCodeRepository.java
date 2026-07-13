package app.domain.auth;

import java.util.Optional;

public interface LoginExchangeCodeRepository {
  Optional<LoginExchangeCode> findByCodeHashForUpdate(String codeHash);

  LoginExchangeCode save(LoginExchangeCode exchangeCode);
}
