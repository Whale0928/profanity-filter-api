package app.test.support.fixture;

public final class SeedManageAccounts {

  public static final SeedManageAccount ADMIN =
      new SeedManageAccount("e2e-admin", "{noop}e2e-admin-password");

  private SeedManageAccounts() {}
}
