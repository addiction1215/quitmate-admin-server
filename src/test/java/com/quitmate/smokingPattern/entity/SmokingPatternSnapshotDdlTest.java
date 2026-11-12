package com.quitmate.smokingPattern.entity;

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
 * 운영 관리자 서버는 ddl-auto: update다. 엔티티가 KAN-133 DDL(앱 서버 addiction#94 본문)과 다르면
 * Hibernate가 다른 모양으로 테이블을 만들거나 컬럼을 덧붙인다. DB 없이 MySQL 방언으로 Hibernate가 만들 DDL을 뽑아
 * 컬럼·타입·NULL 여부·유니크·인덱스가 같은지 확인한다.
 */
class SmokingPatternSnapshotDdlTest {

    private StandardServiceRegistry registry;

    @AfterEach
    void close() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @DisplayName("Hibernate가 MySQL에서 만들 smoking_pattern_snapshot DDL이 KAN-133 DDL과 같다")
    @Test
    void mysqlDdlMatchesKan133(@TempDir Path dir) throws Exception {
        // given
        Path out = dir.resolve("smoking_pattern_snapshot.sql");
        registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, MySQLDialect.class.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName())
                .applySetting(AvailableSettings.IMPLICIT_NAMING_STRATEGY, SpringImplicitNamingStrategy.class.getName())
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "none")
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create")
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, out.toString())
                .build();
        Metadata metadata = new MetadataSources(registry).addAnnotatedClass(SmokingPatternSnapshot.class).buildMetadata();

        // when
        SchemaManagementToolCoordinator.process(metadata, registry,
                registry.requireService(ConfigurationService.class).getSettings(),
                DelayedDropRegistryNotAvailableImpl.INSTANCE);
        String ddl = Files.readString(out).toLowerCase().replaceAll("\\s+", " ");
        System.out.println("[smoking_pattern_snapshot DDL by Hibernate/MySQL] " + ddl);

        // then
        assertThat(ddl).contains("create table smoking_pattern_snapshot (");
        assertThat(ddl).contains("id bigint not null auto_increment");
        assertThat(ddl).contains("user_id bigint not null");
        assertThat(ddl).contains("period_start date not null");
        assertThat(ddl).contains("period_end date not null");
        assertThat(ddl).contains("algorithm_version varchar(20) not null");
        assertThat(ddl).contains("data_sufficiency varchar(20) not null");
        assertThat(ddl).contains("observed_days integer not null");
        assertThat(ddl).contains("smoke_count integer not null");
        assertThat(ddl).containsPattern("peak_hour integer[,)]");
        assertThat(ddl).containsPattern("routine_type varchar\\(20\\)[,)]");
        assertThat(ddl).containsPattern("trend_direction varchar\\(20\\)[,)]");
        assertThat(ddl).contains("result_json text not null");
        assertThat(ddl).containsPattern("created_date datetime\\(6\\)[,)]");
        assertThat(ddl).containsPattern("updated_date datetime\\(6\\)[,)]");
        assertThat(ddl).contains("primary key (id)");
        assertThat(ddl).contains("create index idx_smoking_pattern_snapshot_period on smoking_pattern_snapshot (period_start, data_sufficiency)");
        assertThat(ddl).contains("constraint uk_smoking_pattern_snapshot_user_period unique (user_id, period_start)");
        // MySQL ENUM이나 FK로 만들지 않는다
        assertThat(ddl).doesNotContain("enum(");
        assertThat(ddl).doesNotContain("foreign key");
    }
}
