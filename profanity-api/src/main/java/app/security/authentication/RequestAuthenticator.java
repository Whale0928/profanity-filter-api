package app.security.authentication;

import app.security.filter.RequestCredential;
import org.springframework.security.core.Authentication;

/** 한 종류의 자격 증명만 검증하는 인증기입니다. */
public interface RequestAuthenticator {

  AuthenticationType supports();

  Authentication authenticate(RequestCredential credential);
}
