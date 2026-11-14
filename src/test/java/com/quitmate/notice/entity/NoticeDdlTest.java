package com.quitmate.notice.entity;

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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 관리자 서버는 ddl-auto: update다. 엔티티가 docs/db/KAN-109-deferred-event-push.sql 과 다르면
 * Hibernate가 다른 모양으로 칸을 만든다. DB 없이 MySQL 방언으로 Hibernate가 만들 DDL을 뽑아 새 칸 3개를 확인한다.
 */
class NoticeDdlTest {

    private StandardServiceRegistry registry;

    @AfterEach
    void close() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @DisplayName("Hibernate가 MySQL에서 만들 notice 푸시 칸이 KAN-109 DDL과 같다(push_status는 ENUM이 아니라 VARCHAR, 모두 NULL 허용)")
    @Test
    void mysqlDdlMatchesKan109(@TempDir Path dir) throws Exception {
        // given
        Path out = dir.resolve("notice.sql");
        registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, MySQLDialect.class.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName())
                .applySetting(AvailableSettings.IMPLICIT_NAMING_STRATEGY, SpringImplicitNamingStrategy.class.getName())
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "none")
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create")
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, out.toString())
                .build();
        Metadata metadata = new MetadataSources(registry).addAnnotatedClass(Notice.class).buildMetadata();

        // when
        SchemaManagementToolCoordinator.process(metadata, registry,
                registry.requireService(ConfigurationService.class).getSettings(),
                DelayedDropRegistryNotAvailableImpl.INSTANCE);
        String ddl = Files.readString(out).toLowerCase().replaceAll("\\s+", " ");
        System.out.println("[notice DDL by Hibernate/MySQL] " + ddl);

        // then
        assertThat(ddl).contains("create table notice (");
        assertThat(ddl).containsPattern("push_status varchar\\(20\\)[,)]");
        assertThat(ddl).containsPattern("push_scheduled_at datetime\\(6\\)[,)]");
        assertThat(ddl).containsPattern("push_sent_at datetime\\(6\\)[,)]");
        assertThat(ddl).doesNotContain("push_status enum");
    }
}
