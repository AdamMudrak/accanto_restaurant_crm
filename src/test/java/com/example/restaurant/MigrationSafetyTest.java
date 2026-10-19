package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.restaurant.support.SharedMySql;
import java.net.URI;
import java.util.UUID;
import liquibase.integration.spring.SpringLiquibase;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class MigrationSafetyTest {
    @Test
    void upgradesLegacySchemaAndSecondStartupPreservesData() throws Exception {
        withDatabase(source -> {
            migrate(source, "classpath:db/changelog/legacy-master.xml", "demo");
            JdbcTemplate jdbc = new JdbcTemplate(source);
            jdbc.update("UPDATE orders SET uuid = 'migration-preservation-marker' WHERE id = 1");
            int itemCount = jdbc.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class);
            migrate(source, "classpath:db/changelog/db.changelog-master.xml", "production");
            assertThat(jdbc.queryForObject("SELECT uuid FROM orders WHERE id = 1", String.class))
                    .isEqualTo("migration-preservation-marker");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_items", Integer.class))
                    .isEqualTo(itemCount);
            assertThat(jdbc.queryForObject("SELECT currency FROM payments WHERE id = 1", String.class))
                    .isEqualTo("UAH");
            assertThat(jdbc.queryForObject("SELECT currency FROM payments WHERE id = 2", String.class))
                    .isNull();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE method = 'CASH' "
                    + "AND reconstructed = 1 AND completed_at IS NULL", Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT time_zone FROM restaurants LIMIT 1", String.class)).isEqualTo("Europe/Kyiv");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM order_items WHERE status IN ('PAID_CARD', 'PAID_CASH') AND fulfillment_status <> 'CONFIRMED'", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_refunds", Integer.class)).isZero();
            int paymentCount = jdbc.queryForObject("SELECT COUNT(*) FROM payments", Integer.class);
            migrate(source, "classpath:db/changelog/db.changelog-master.xml", "production");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payments", Integer.class))
                    .isEqualTo(paymentCount);
            assertThat(jdbc.queryForObject("SELECT uuid FROM orders WHERE id = 1", String.class))
                    .isEqualTo("migration-preservation-marker");
        });
    }

    @Test
    void productionBootstrapDoesNotInsertDemoOrdersOrEmployees() throws Exception {
        withDatabase(source -> {
            migrate(source, "classpath:db/changelog/db.changelog-master.xml", "production");
            JdbcTemplate jdbc = new JdbcTemplate(source);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM orders", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM employees", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM restaurants", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM roles", Integer.class)).isEqualTo(2);
        });
    }

    private void migrate(DriverManagerDataSource source, String changelog, String contexts)
            throws Exception {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(source);
        liquibase.setChangeLog(changelog);
        liquibase.setContexts(contexts);
        liquibase.afterPropertiesSet();
    }

    private void withDatabase(DatabaseTest test) throws Exception {
        String schema = "restaurant_migration_test_" + UUID.randomUUID().toString().replace("-", "");
        DriverManagerDataSource admin = SharedMySql.migrationAdmin();
        JdbcTemplate jdbc = new JdbcTemplate(admin);
        jdbc.execute("CREATE DATABASE " + schema);
        try {
            URI uri = URI.create(admin.getUrl().substring(5));
            DriverManagerDataSource source = new DriverManagerDataSource(
                    "jdbc:" + uri.resolve("/" + schema), admin.getUsername(), admin.getPassword());
            test.run(source);
        } finally {
            if (!schema.matches("restaurant_migration_test_[a-f0-9]{32}")) {
                throw new IllegalStateException("Unexpected test schema name");
            }
            jdbc.execute("DROP DATABASE " + schema);
        }
    }

    @FunctionalInterface
    private interface DatabaseTest {
        void run(DriverManagerDataSource source) throws Exception;
    }
}
