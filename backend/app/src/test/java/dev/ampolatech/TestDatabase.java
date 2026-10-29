package dev.ampolatech;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * One PostgreSQL for the whole test run, set up by postgres/initdb/create-roles.sh. The app
 * connects to it as the runtime role, never as the container's superuser.
 */
final class TestDatabase {

  private static final String OWNER_PASSWORD = "owner-test";
  private static final String APP_PASSWORD = "app-test";

  // Never closed: Testcontainers' Ryuk container stops it when the test run ends, so the
  // application contexts Spring caches across test classes keep their database.
  @SuppressWarnings("resource")
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:18.6")
          .withDatabaseName("ampolatech")
          .withEnv("AMPOLATECH_OWNER_PASSWORD", OWNER_PASSWORD)
          .withEnv("AMPOLATECH_APP_PASSWORD", APP_PASSWORD)
          .withCopyFileToContainer(
              MountableFile.forClasspathResource("initdb/create-roles.sh", 0755),
              "/docker-entrypoint-initdb.d/create-roles.sh");

  static {
    POSTGRES.start();
  }

  private TestDatabase() {}

  /** The role names come from application.yml, so the tests check them too. */
  static void register(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.password", () -> APP_PASSWORD);
    registry.add("spring.liquibase.password", () -> OWNER_PASSWORD);
  }

  /** The owner role, for test data the runtime role may not write. */
  static JdbcClient owner() {
    return JdbcClient.create(
        new DriverManagerDataSource(POSTGRES.getJdbcUrl(), "ampolatech_owner", OWNER_PASSWORD));
  }
}
