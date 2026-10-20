package app.test.support.fixture;

public final class SeedApiKeys {

  public static final SeedApiKey READ_CLIENT =
      new SeedApiKey(
          "00000000-0000-0000-0000-000000000001",
          "E2E Read Client",
          "e2e-read@example.com",
          "HmikqfE546l5lP4R5UbETsfROP8go0Kq-9cZqNw-nDU");

  public static final SeedApiKey WRITE_CLIENT =
      new SeedApiKey(
          "00000000-0000-0000-0000-000000000002",
          "E2E Write Client",
          "e2e-write@example.com",
          "u6N_yQZAPfyrLheRXi7V0tZkvqe5Mno__vV0BlxpCjk");

  /** 로그인 계정에 연결되지 않은 기존 키. 인증 거절과 최초 로그인 시 자동 연결 검증에 쓴다. */
  public static final SeedApiKey LEGACY_CLIENT =
      new SeedApiKey(
          "00000000-0000-0000-0000-000000000003",
          "E2E Legacy Client",
          "e2e-legacy@example.com",
          "GFx8znIDWePH3M_MOPd00aLwXp0C7LGPCKsQb13FZzI");

  private SeedApiKeys() {}
}
