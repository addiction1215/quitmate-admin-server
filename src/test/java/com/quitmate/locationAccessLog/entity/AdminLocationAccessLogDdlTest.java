package com.quitmate.locationAccessLog.entity;

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
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 관리자 서버는 ddl-auto: update 다. 엔티티가 KAN-208 DDL(docs/db/KAN-208-admin-location-access-log.sql)과 다르면
 * Hibernate 가 다른 모양으로 표·인덱스를 만든다. DB 없이 MySQL 방언으로 Hibernate 가 만들 DDL 을 뽑아,
 * 칸·타입·NULL 여부·인덱스 이름이 DDL 파일과 같은지, 위치 값 칸이 없는지 확인한다.
 */
class AdminLocationAccessLogDdlTest {

    private static final Path DDL_FILE = Path.of("docs/db/KAN-208-admin-location-access-log.sql");

    /** Hibernate 가 만들 칸 정의 = DDL 파일의 칸 정의(COMMENT 앞까지) */
    private static final List<String> COLUMNS = List.of(
            "id bigint not null auto_increment",
            "admin_user_id bigint not null",
            "target_user_id bigint not null",
            "resource_type varchar(40) not null",
            "resource_id bigint not null",
            "api varchar(100) not null",
            "accessed_at datetime(6) not null"
    );

    private StandardServiceRegistry registry;

    @AfterEach
    void close() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @DisplayName("Hibernate 가 MySQL 에서 만들 admin_location_access_log DDL 이 KAN-208 DDL 파일과 같고, 위치 값 칸이 없다")
    @Test
    void mysqlDdlMatchesKan208(@TempDir Path dir) throws Exception {
        // given
        Path out = dir.resolve("admin_location_access_log.sql");
        registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DIALECT, MySQLDialect.class.getName())
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY, CamelCaseToUnderscoresNamingStrategy.class.getName())
                .applySetting(AvailableSettings.IMPLICIT_NAMING_STRATEGY, SpringImplicitNamingStrategy.class.getName())
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, "none")
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "create")
                .applySetting(AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_CREATE_TARGET, out.toString())
                .build();
        Metadata metadata = new MetadataSources(registry).addAnnotatedClass(AdminLocationAccessLog.class).buildMetadata();

        // when
        SchemaManagementToolCoordinator.process(metadata, registry,
                registry.requireService(ConfigurationService.class).getSettings(),
                DelayedDropRegistryNotAvailableImpl.INSTANCE);
        String ddl = normalize(Files.readString(out));
        String ddlFile = normalize(Files.readString(DDL_FILE));
        System.out.println("[admin_location_access_log DDL by Hibernate/MySQL] " + ddl);

        // then: 칸은 이 7개뿐이고, 정의가 DDL 파일과 같다
        String tablePrefix = "create table admin_location_access_log (";
        assertThat(ddl).contains(tablePrefix);
        String body = ddl.substring(ddl.indexOf(tablePrefix) + tablePrefix.length(), ddl.indexOf(", primary key (id)"));
        assertThat(Arrays.stream(body.split(", ")).map(String::trim).toList())
                .containsExactlyInAnyOrderElementsOf(COLUMNS);
        assertThat(ddl).contains("primary key (id)");
        assertThat(ddlFile).contains("create table admin_location_access_log (");
        assertThat(ddlFile).contains("primary key (id)");
        for (String column : COLUMNS) {
            assertThat(ddlFile).contains(column);
        }

        // 인덱스 이름·칸이 같다(운영 ddl-auto: update 는 이름으로 있는지 본다)
        assertThat(ddl).contains("create index idx_admin_location_access_log_target_accessed "
                + "on admin_location_access_log (target_user_id, accessed_at)");
        assertThat(ddl).contains("create index idx_admin_location_access_log_accessed on admin_location_access_log (accessed_at)");
        assertThat(ddlFile).contains("key idx_admin_location_access_log_target_accessed (target_user_id, accessed_at)");
        assertThat(ddlFile).contains("key idx_admin_location_access_log_accessed (accessed_at)");

        // 위치 값 칸·ENUM·FK·감사용 시각 칸(created_date 등)을 만들지 않는다
        assertThat(ddl).doesNotContain("address", "latitude", "longitude", "location ", "enum(", "foreign key",
                "created_date", "updated_date");
        assertThat(ddlFile).doesNotContain("foreign key", "enum(");
    }

    private String normalize(String sql) {
        return sql.toLowerCase().replaceAll("\\s+", " ");
    }
}
