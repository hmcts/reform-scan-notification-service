package uk.gov.hmcts.reform.notificationservice.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

@AutoConfigureAfter(FlywayAutoConfiguration.FlywayConfiguration.class)
@Configuration
@DependsOn({"flyway", "flywayInitializer"})
@EnableSchedulerLock(defaultLockAtMostFor = "${scheduling.lock_at_most_for}")
public class ShedlockConfiguration {

    /**
     * The function creates a LockProvider bean using a DataSource to provide locking functionality.
     *
     * @param dataSource The `dataSource` parameter in the `lockProvider` method is an instance of the
     *                   `DataSource` class. It is used to provide the necessary database connection
     *                   information for creating a `JdbcTemplateLockProvider` instance, which is responsible
     *                   for managing locks in the application.
     * @return An instance of `JdbcTemplateLockProvider` is being returned.
     */
    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(dataSource);
    }
}
