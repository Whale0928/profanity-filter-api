package app.test.support.fixture;

public final class SeedWords {

  public static final SeedWord ACTIVE_PROFANITY_SAMPLE = new SeedWord("나쁜말샘플", true);
  public static final SeedWord ACTIVE_SLANG_SAMPLE = new SeedWord("비속어샘플", true);
  public static final SeedWord INACTIVE_FORBIDDEN_SAMPLE = new SeedWord("금칙어샘플", false);

  private SeedWords() {}
}
