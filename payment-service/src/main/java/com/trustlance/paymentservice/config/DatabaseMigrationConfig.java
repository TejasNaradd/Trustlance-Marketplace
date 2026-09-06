package com.trustlance.paymentservice.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Native Spring Boot 4 hook: executes Flyway migrations right before
 * Hibernate builds the EntityManagerFactory and validates the schema.
 */
@Configuration
public class DatabaseMigrationConfig implements HibernatePropertiesCustomizer {

    private final DataSource dataSource;

    public DatabaseMigrationConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load();
        flyway.migrate();
    }
}
