package com.example.restaurant.support;

import java.net.URI;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.mysql.MySQLContainer;

/** One container per JVM, deliberately independent of the per-class JUnit lifecycle. */
public final class SharedMySql {
    private static final String EXTERNAL_URL = System.getenv("TEST_DATABASE_URL");

    private SharedMySql() {}

    private static final class ContainerHolder {
        private static final MySQLContainer MYSQL = start();

        private static MySQLContainer start() {
            MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                    .withDatabaseName("restaurant_test")
                    .withUsername("restaurant_test")
                    .withPassword("restaurant_test")
                    .withEnv("MYSQL_ROOT_HOST", "%")
                    .withCommand("--innodb-buffer-pool-size=64M", "--max-connections=40")
                    .withCreateContainerCmdModifier(command -> command.getHostConfig()
                            .withMemory(512L * 1024 * 1024).withNanoCPUs(1_000_000_000L));
            mysql.start();
            return mysql;
        }
    }

    public static String url() {
        String url = external() ? EXTERNAL_URL : ContainerHolder.MYSQL.getJdbcUrl();
        if (!URI.create(url.substring(5)).getPath().endsWith("_test")) {
            throw new IllegalArgumentException("Integration database name must end with _test.");
        }
        return url;
    }

    public static String username() {
        return external() ? env("TEST_DATABASE_USERNAME", "restaurant_test")
                : ContainerHolder.MYSQL.getUsername();
    }

    public static String password() {
        return external() ? env("TEST_DATABASE_PASSWORD", "restaurant_test")
                : ContainerHolder.MYSQL.getPassword();
    }

    public static DriverManagerDataSource migrationAdmin() {
        if (external()) {
            String adminUrl = System.getenv("TEST_DATABASE_ADMIN_URL");
            if (adminUrl == null || adminUrl.isBlank()) {
                throw new IllegalArgumentException("Migration tests need TEST_DATABASE_ADMIN_URL "
                        + "when using external MySQL. Use -Dtest='*IntegrationTest' without root.");
            }
            return new DriverManagerDataSource(adminUrl, "root",
                    env("TEST_DATABASE_ADMIN_PASSWORD", "restaurant_test_root"));
        }
        URI uri = URI.create(url().substring(5));
        return new DriverManagerDataSource("jdbc:" + uri.resolve("/mysql"), "root", password());
    }

    private static boolean external() {
        return EXTERNAL_URL != null && !EXTERNAL_URL.isBlank();
    }

    private static String env(String key, String fallback) {
        return System.getenv().getOrDefault(key, fallback);
    }
}
