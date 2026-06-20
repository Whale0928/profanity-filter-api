package app.test.support.fixture;

public final class IntegrationWords {

  public static final IntegrationWord ACTIVE_PROFANITY_SAMPLE = new IntegrationWord("나쁜말샘플", true);
  public static final IntegrationWord ACTIVE_SLANG_SAMPLE = new IntegrationWord("비속어샘플", true);
  public static final IntegrationWord INACTIVE_FORBIDDEN_SAMPLE =
      new IntegrationWord("금칙어샘플", false);

  private IntegrationWords() {}
}
