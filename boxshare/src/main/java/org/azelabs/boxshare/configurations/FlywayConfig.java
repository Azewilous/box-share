package org.azelabs.boxshare.configurations;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class FlywayConfig {

    @Bean
    public Flyway flyway(DataSource dataSource,
                         @Value("${spring.flyway.baseline-on-migrate:false}") boolean baselineOnMigrate,
                         @Value("${spring.flyway.baseline-version:1}") String baselineVersion) {
        return Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(baselineOnMigrate)
                .baselineVersion(baselineVersion)
                .load();
    }

    // Runs after the full application context is ready, avoiding BeanFactoryPostProcessor
    // bootstrap ordering conflicts with Spring Boot DevTools.
    @Bean
    public ApplicationRunner flywayMigrate(Flyway flyway) {
        return args -> flyway.migrate();
    }
}
