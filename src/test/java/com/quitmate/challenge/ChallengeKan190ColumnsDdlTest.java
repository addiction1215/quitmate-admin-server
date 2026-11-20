package com.quitmate.challenge;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.challenge.challengehistory.entity.ChallengeHistory;
import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.user.push.entity.Push;
import com.quitmate.user.users.entity.User;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.engine.config.spi.ConfigurationService;
import org.hibernate.tool.schema.spi.DelayedDropRegistryNotAvailableImpl;
import org.hibernate.tool.schema.spi.SchemaManagementToolCoordinator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 관리자 서버는 KAN-190 칸을 만들지 않고 앱 서버 DDL(addiction#111 docs/db/KAN-190-mission-self-complete.sql 1))이 만든
 * 칸을 매핑만 한다. dev는 ddl-auto: none이라 이름이 어긋나면 Unknown column으로 500이 나고, 운영은 ddl-auto: update라
 * 타입이 어긋나면 Hibernate가 다른 칸을 덧붙인다. DB 없이 MySQL 방언으로 Hibernate가 만들 DDL을 뽑아 이름·타입·NULL 허용을 본다.
 */
class ChallengeKan190ColumnsDdlTest {

    private StandardServiceRegistry registry;

    @AfterEach
    void close() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @DisplayName("challenge_history.completed_at·cancelled_at은 datetime(6) NULL, mission.min_hold_seconds는 int NULL이다")
    @Test
    void columnsMatchKan190Ddl(@TempDir Path dir) throws Exception {
        // given
        Path out = dir.resolve("challenge.sql");
        registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, MySQLDialect.class.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName())
                .applySetting(AvailableSettings.IMPLICIT_NAMING_STRATEGY, SpringImplicitNamingStrategy.class.getName())
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "none")
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create")
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, out.toString())
                .build();
        Metadata metadata = new MetadataSources(registry)
                .addAnnotatedClass(Challenge.class)
                .addAnnotatedClass(Mission.class)
                .addAnnotatedClass(ChallengeHistory.class)
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Push.class)
                .buildMetadata();

        // when
        SchemaManagementToolCoordinator.process(metadata, registry,
                registry.requireService(ConfigurationService.class).getSettings(),
                DelayedDropRegistryNotAvailableImpl.INSTANCE);
        String ddl = Files.readString(out).toLowerCase().replaceAll("\\s+", " ");

        // then
        String challengeHistory = createTable(ddl, "challenge_history");
        assertThat(challengeHistory).containsPattern("completed_at datetime\\(6\\)[,)]");
        assertThat(challengeHistory).containsPattern("cancelled_at datetime\\(6\\)[,)]");
        String mission = createTable(ddl, "mission");
        assertThat(mission).containsPattern("min_hold_seconds integer[,)]");
    }

    private static String createTable(String ddl, String table) {
        Matcher matcher = Pattern.compile("create table " + table + " \\((.*?)\\) engine").matcher(ddl);
        assertThat(matcher.find()).as(table + " 테이블 DDL").isTrue();
        return matcher.group(1) + ")";
    }
}
