package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

@DisplayName("Persistence Adapter - Flyway Migration Integration Test")
class FlywayMigrationIntegrationTest {

  @Test
  @DisplayName("Flyway migrations should execute successfully on the database")
  void shouldExecuteFlywayMigrationSuccessfully() {
    DataSource dataSource =
        DataSourceBuilder.create()
            .url("jdbc:h2:mem:flywaydb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
            .driverClassName("org.h2.Driver")
            .username("sa")
            .password("")
            .build();

    Flyway flyway =
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load();

    MigrateResult result = flyway.migrate();

    assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(2);
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
  }
}
