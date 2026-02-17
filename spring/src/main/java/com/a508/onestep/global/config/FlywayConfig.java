package com.a508.onestep.global.config;

import com.a508.onestep.global.logging.utils.LogUtils;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Set;

/**
 * DB 마이그레이션을 위한 Configuration
 * - local: 매 시작 시 DB 초기화 (clean + migrate)
 * - dev/prod: 증분 마이그레이션
 */
@Configuration
public class FlywayConfig {

    private static final Set<String> LOCAL_LIKE_PROFILES = Set.of("local", "dev");

    @Value("${spring.config.activate.on-profile}")
    private String activeProfile;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        LogUtils.info("Initializing Flyway - profile: {}", activeProfile);

        boolean isLocal = LOCAL_LIKE_PROFILES.contains(activeProfile);

        String[] locations = isLocal
                ? new String[]{"classpath:db/migration", "classpath:db/seed/local"}
                : new String[]{"classpath:db/migration"};

        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .validateOnMigrate(!isLocal)
                .cleanDisabled(!isLocal)
                .load();

//        if (isLocal) {
//            LogUtils.info("Local/Dev profile - cleaning database for fresh start");
//            flyway.clean();
//        }

        flyway.repair();
        flyway.migrate();
        LogUtils.info("Flyway migration completed successfully");
        return flyway;
    }
}