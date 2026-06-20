package app.test.support.fixture;

public final class SeedClients {

  public static final SeedClient READ_CLIENT =
      new SeedClient(
          "E2E Read Client", "e2e-read@example.com", "HmikqfE546l5lP4R5UbETsfROP8go0Kq-9cZqNw-nDU");

  public static final SeedClient WRITE_CLIENT =
      new SeedClient(
          "E2E Write Client",
          "e2e-write@example.com",
          "u6N_yQZAPfyrLheRXi7V0tZkvqe5Mno__vV0BlxpCjk");

  private SeedClients() {}
}
