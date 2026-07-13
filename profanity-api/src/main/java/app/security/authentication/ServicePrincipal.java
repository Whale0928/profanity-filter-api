package app.security.authentication;

import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

/** 서비스 내부에서 사용하는 타입 안전한 인증 주체입니다. */
public sealed interface ServicePrincipal extends Principal, Serializable
    permits ApiKeyPrincipal, LoginUserPrincipal {

  UUID id();

  String email();

  AuthenticationType authenticationType();

  @Override
  default String getName() {
    return id().toString();
  }
}
