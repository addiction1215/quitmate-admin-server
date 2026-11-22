package com.quitmate.locationAccessLog;

import com.quitmate.locationAccessLog.entity.AdminLocationAccessLog;
import com.quitmate.locationAccessLog.enums.LocationAccessResourceType;
import com.quitmate.locationAccessLog.repository.AdminLocationAccessLogJpaRepository;
import com.quitmate.locationAccessLog.service.LocationAccessLogRetentionScheduler;
import com.quitmate.locationAccessLog.service.LocationAccessLogRetentionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/** 위치정보 열람 이력 보관 기간(6개월) 지난 행 자동 삭제(KAN-208) */
@ActiveProfiles("test")
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class LocationAccessLogRetentionTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 19, 4, 40, 0);
    private static final LocalDateTime CUTOFF = NOW.minusMonths(6); // 2026-05-19 04:40:00

    @Autowired
    private LocationAccessLogRetentionService retentionService;
    @Autowired
    private LocationAccessLogRetentionScheduler scheduler;
    @Autowired
    private AdminLocationAccessLogJpaRepository repository;

    @AfterEach
    void clean() {
        repository.deleteAllInBatch();
    }

    private void save(LocalDateTime accessedAt) {
        repository.save(AdminLocationAccessLog.of(1L, 2L, LocationAccessResourceType.MISSION_HISTORY_ADDRESS, 3L,
                "GET /api/v1/admin/mission-history/{id}", accessedAt));
    }

    private List<LocalDateTime> remaining() {
        return repository.findAll().stream().map(AdminLocationAccessLog::getAccessedAt).sorted().toList();
    }

    @DisplayName("6개월 지난 행만 지우고, 기준 시각과 같거나 그 뒤인 행은 남긴다")
    @Test
    void deletesOnlyOlderThanSixMonths() {
        save(CUTOFF.minusDays(30));
        save(CUTOFF.minusSeconds(1));
        save(CUTOFF);
        save(CUTOFF.plusSeconds(1));
        save(NOW.minusDays(1));

        long deleted = retentionService.purgeExpired(NOW, 1000);

        assertThat(deleted).isEqualTo(2);
        assertThat(remaining()).containsExactly(CUTOFF, CUTOFF.plusSeconds(1), NOW.minusDays(1));
    }

    @DisplayName("빈 표에서는 0건이고 오류가 없다. 다시 돌려도 같다(멱등)")
    @Test
    void emptyTableAndRepeat() {
        assertThat(retentionService.purgeExpired(NOW, 1000)).isZero();

        save(CUTOFF.minusDays(1));
        assertThat(retentionService.purgeExpired(NOW, 1000)).isEqualTo(1);
        assertThat(retentionService.purgeExpired(NOW, 1000)).isZero();
        assertThat(repository.count()).isZero();
    }

    @DisplayName("묶음보다 많아도 반복해서 전부 지우고, 최근 행은 남긴다")
    @Test
    void deletesInBatches() {
        IntStream.range(0, 25).forEach(i -> save(CUTOFF.minusDays(1).minusMinutes(i)));
        save(CUTOFF.plusDays(1));
        save(CUTOFF.plusDays(2));

        long deleted = retentionService.purgeExpired(NOW, 10);

        assertThat(deleted).isEqualTo(25);
        assertThat(remaining()).containsExactly(CUTOFF.plusDays(1), CUTOFF.plusDays(2));
    }

    @DisplayName("묶음 크기와 딱 맞게 쌓여 있어도 남김없이 지운다")
    @Test
    void deletesWhenExactMultipleOfBatch() {
        IntStream.range(0, 20).forEach(i -> save(CUTOFF.minusHours(1).minusMinutes(i)));

        assertThat(retentionService.purgeExpired(NOW, 10)).isEqualTo(20);
        assertThat(repository.count()).isZero();
    }

    @DisplayName("스케줄은 매일 04:40 한국 시각이고, 실행하면 지난 행만 지운다(로그에 회원 id 없음)")
    @Test
    void schedulerRuns(CapturedOutput output) throws Exception {
        Method method = LocationAccessLogRetentionScheduler.class.getMethod("purgeExpiredLogs");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);
        assertThat(scheduled.cron()).isEqualTo("0 40 4 * * *");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");

        save(LocalDateTime.now().minusMonths(7));
        save(LocalDateTime.now().minusDays(1));

        scheduler.purgeExpiredLogs();

        assertThat(repository.count()).isEqualTo(1);
        assertThat(output.getAll()).contains("위치정보 열람 이력 보관 기간 지난 행 삭제 - 1건");
    }
}
