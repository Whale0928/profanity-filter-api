package app.application.auth;

import app.domain.user.OAuthLoginProfile;

@FunctionalInterface
public interface SsoLoginCompletionService {
  String issueExchangeCode(OAuthLoginProfile profile);
}
