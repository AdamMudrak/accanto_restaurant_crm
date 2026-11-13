package com.example.restaurant;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.restaurant.support.AbstractIntegrationTest;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestContextManager;

class SharedInfrastructureIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private ApplicationContext context;
    @Autowired
    private DataSource dataSource;

    @Test
    void domainSuitesReuseApplicationContextDatabaseAndConnectionPool() {
        for (Class<?> type : List.of(AuthIntegrationTest.class, AuthorizationIntegrationTest.class,
                AnalyticsIntegrationTest.class, DiningTableIntegrationTest.class,
                EmployeeIntegrationTest.class, EmployeeOrderIntegrationTest.class,
                GuestOrderIntegrationTest.class, GuestSessionIntegrationTest.class,
                ImageIntegrationTest.class, ManagementIntegrationTest.class, MenuIntegrationTest.class,
                PaymentIntegrationTest.class, ProviderHttpIntegrationTest.class,
                ReviewIntegrationTest.class, WebhookIntegrationTest.class,
                PaymentOperationsIntegrationTest.class, RestaurantTimeZoneIntegrationTest.class,
                OrderReliabilityIntegrationTest.class)) {
            ApplicationContext other = new TestContextManager(type).getTestContext()
                    .getApplicationContext();
            assertThat(other).as(type.getSimpleName()).isSameAs(context);
            assertThat(other.getBean(DataSource.class)).isSameAs(dataSource);
        }
        assertThat(dataSource).isInstanceOf(HikariDataSource.class);
        HikariDataSource pool = (HikariDataSource) dataSource;
        assertThat(pool.getMaximumPoolSize()).isEqualTo(6);
        assertThat(pool.getHikariPoolMXBean().getTotalConnections()).isLessThanOrEqualTo(6);
        System.out.println("Verified: all 18 domain suites share one Spring context and Hikari pool.");
    }

    @Test
    void restoringDataPreservesPoolAndResetsMutationsWithoutRunningMigrations() throws Exception {
        long changeSets = jdbc.queryForObject("SELECT COUNT(*) FROM DATABASECHANGELOG", Long.class);
        String email = employees.findById(2L).orElseThrow().getEmail();
        jdbc.update("UPDATE employees SET email = 'mutated@example.com', is_deleted = true WHERE id = 2");
        fixture(com.example.restaurant.entity.management.Restaurant.Currency.UAH);
        databaseState.restore();
        assertThat(employees.findById(2L).orElseThrow().getEmail()).isEqualTo(email);
        assertThat(employees.findById(2L).orElseThrow().isDeleted()).isFalse();
        assertThat(orders.count()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM DATABASECHANGELOG", Long.class)).isEqualTo(changeSets);
        assertThat(context.getBean(DataSource.class)).isSameAs(dataSource);
    }
}
