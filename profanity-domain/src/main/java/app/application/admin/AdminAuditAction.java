package app.application.admin;

/** 감사 기록에 사용하는 action 값입니다. 워커별로 새 값이 필요하면 여기에 추가합니다. */
public final class AdminAuditAction {
  public static final String USER_STATUS_CHANGED = "USER_STATUS_CHANGED";
  public static final String API_KEY_REVOKED = "API_KEY_REVOKED";
  public static final String WORD_CREATED = "WORD_CREATED";
  public static final String WORD_UPDATED = "WORD_UPDATED";

  public static final String NEWS_CREATED = "NEWS_CREATED";
  public static final String NEWS_UPDATED = "NEWS_UPDATED";
  public static final String NEWS_DELETED = "NEWS_DELETED";
  public static final String INQUIRY_STATUS_CHANGED = "INQUIRY_STATUS_CHANGED";
  public static final String INQUIRY_REPLIED = "INQUIRY_REPLIED";
  public static final String WORD_REQUEST_APPROVED = "WORD_REQUEST_APPROVED";
  public static final String WORD_REQUEST_REJECTED = "WORD_REQUEST_REJECTED";

  private AdminAuditAction() {}
}
