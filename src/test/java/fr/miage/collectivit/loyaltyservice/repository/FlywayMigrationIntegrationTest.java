package fr.miage.collectivit.loyaltyservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

class FlywayMigrationIntegrationTest {

  @Test
  @DisplayName("La migration Flyway V1 doit s'exécuter avec succès")
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

    int applied = flyway.migrate().migrationsExecuted;

    assertThat(applied).isGreaterThanOrEqualTo(1);
    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
  }
}
