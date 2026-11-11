package com.quitmate.user.userPass.entity;

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
 * 운영 관리자 서버는 ddl-auto: update다. 엔티티가 KAN-145 DDL과 다르면 Hibernate가 다른 모양으로 테이블을 만든다.
 * DB 없이 MySQL 방언으로 Hibernate가 만들 DDL을 뽑아, KAN-145 DDL의 컬럼·타입·NULL 여부·인덱스와 맞는지 확인한다.
 */
class UserPassDdlTest {

    private StandardServiceRegistry registry;

    @AfterEach
    void close() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @DisplayName("Hibernate가 MySQL에서 만들 user_pass DDL이 KAN-145 DDL과 같다")
    @Test
    void mysqlDdlMatchesKan145(@TempDir Path dir) throws Exception {
        // given
        Path out = dir.resolve("user_pass.sql");
        registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, MySQLDialect.class.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName())
                .applySetting(AvailableSettings.IMPLICIT_NAMING_STRATEGY, SpringImplicitNamingStrategy.class.getName())
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "none")
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create")
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, out.toString())
                .build();
        Metadata metadata = new MetadataSources(registry).addAnnotatedClass(UserPass.class).buildMetadata();

        // when
        SchemaManagementToolCoordinator.process(metadata, registry,
                registry.requireService(ConfigurationService.class).getSettings(),
                DelayedDropRegistryNotAvailableImpl.INSTANCE);
        String ddl = Files.readString(out).toLowerCase().replaceAll("\\s+", " ");
        System.out.println("[user_pass DDL by Hibernate/MySQL] " + ddl);

        // then
        assertThat(ddl).contains("create table user_pass (");
        assertThat(ddl).contains("id bigint not null auto_increment");
        assertThat(ddl).contains("user_id bigint not null");
        assertThat(ddl).contains("product varchar(30) not null");
        assertThat(ddl).contains("source varchar(20) not null");
        assertThat(ddl).contains("starts_at datetime(6) not null");
        assertThat(ddl).contains("ends_at datetime(6) not null");
        assertThat(ddl).containsPattern("revoked_at datetime\\(6\\)[,)]");
        assertThat(ddl).containsPattern("memo varchar\\(255\\)[,)]");
        assertThat(ddl).containsPattern("created_date datetime\\(6\\)[,)]");
        assertThat(ddl).containsPattern("updated_date datetime\\(6\\)[,)]");
        assertThat(ddl).contains("primary key (id)");
        assertThat(ddl).contains("create index idx_user_pass_user_ends on user_pass (user_id, ends_at)");
        // MySQL ENUM이나 FK로 만들지 않는다
        assertThat(ddl).doesNotContain("enum(");
        assertThat(ddl).doesNotContain("foreign key");
    }
}
