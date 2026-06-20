package app.test.support.fixture;

public final class IntegrationClients {

  public static final IntegrationClient READ_CLIENT =
      new IntegrationClient(
          "E2E Read Client", "e2e-read@example.com", "HmikqfE546l5lP4R5UbETsfROP8go0Kq-9cZqNw-nDU");

  public static final IntegrationClient WRITE_CLIENT =
      new IntegrationClient(
          "E2E Write Client",
          "e2e-write@example.com",
          "u6N_yQZAPfyrLheRXi7V0tZkvqe5Mno__vV0BlxpCjk");

  private IntegrationClients() {}
}
