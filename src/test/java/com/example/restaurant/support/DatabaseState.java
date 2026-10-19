package com.example.restaurant.support;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Restores data in the existing schema, preserving Liquibase state and the connection pool. */
public final class DatabaseState {
    private final DataSource dataSource;
    private final List<String> tables;

    public DatabaseState(DataSource dataSource, PasswordEncoder encoder) throws Exception {
        this.dataSource = dataSource;
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        tables = jdbc.queryForList("SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' "
                + "AND LOWER(table_name) NOT LIKE 'databasechangelog%' "
                + "AND LEFT(table_name, 9) <> '_fixture_'", String.class);
        List<String> existing = jdbc.queryForList("SELECT table_name FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND LEFT(table_name, 9) = '_fixture_'", String.class);
        if (!existing.isEmpty()) {
            for (String table : tables) {
                checkName(table);
                List<String> columns = columns(jdbc, table);
                if (!existing.contains("_fixture_" + table)
                        || !columns.equals(columns(jdbc, "_fixture_" + table))) {
                    throw new IllegalStateException("Fixture schema changed; use a fresh disposable test database.");
                }
            }
            restore();
            System.out.println("Shared integration infrastructure: existing data baseline restored once.");
            return;
        }
        for (String table : List.of("reviews", "payment_refunds", "payment_items", "order_items", "payments",
                "guests", "orders", "uuid_tokens", "employee_shifts", "monobank_keys")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("UPDATE employees SET password = ?, is_deleted = false", encoder.encode(
                AbstractIntegrationTest.PASSWORD));
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            for (String table : tables) {
                checkName(table);
                statement.execute("DROP TABLE IF EXISTS `_fixture_" + table + "`");
                statement.execute("CREATE TABLE `_fixture_" + table + "` AS SELECT * FROM `"
                        + table + "`");
            }
        }
        System.out.println("Shared integration infrastructure: baseline created once for "
                + tables.size() + " tables.");
    }

    public void restore() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            connection.setAutoCommit(false);
            statement.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String table : tables) {
                    statement.executeUpdate("DELETE FROM `" + table + "`");
                    statement.executeUpdate("INSERT INTO `" + table
                            + "` SELECT * FROM `_fixture_" + table + "`");
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                statement.execute("SET FOREIGN_KEY_CHECKS = 1");
                connection.setAutoCommit(true);
            }
        }
    }

    private List<String> columns(JdbcTemplate jdbc, String table) {
        return jdbc.queryForList("SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position",
                String.class, table);
    }

    private static void checkName(String name) {
        if (!name.matches("[a-z_]+")) {
            throw new IllegalArgumentException("Unexpected fixture table: " + name);
        }
    }
}
