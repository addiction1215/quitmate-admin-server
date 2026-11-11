package com.quitmate.smokingPattern.repository;

import com.quitmate.smokingPattern.entity.SmokingPatternSnapshot;
import com.quitmate.smokingPattern.repository.response.SmokingPatternSnapshotRow;
import com.quitmate.smokingPattern.repository.response.SmokingPatternWeekDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SmokingPatternSnapshotJpaRepository extends JpaRepository<SmokingPatternSnapshot, Long> {

    @Query("select max(s.periodStart) from SmokingPatternSnapshot s")
    Optional<LocalDate> findLatestPeriodStart();

    /** 최근 주부터. period_start가 인덱스 첫 컬럼이라 인덱스 순서로 묶는다. */
    @Query("select new com.quitmate.smokingPattern.repository.response.SmokingPatternWeekDto(s.periodStart, s.periodEnd, count(s))"
            + " from SmokingPatternSnapshot s group by s.periodStart, s.periodEnd order by s.periodStart desc")
    List<SmokingPatternWeekDto> findWeeks(Pageable pageable);

    /** 한 주의 스냅숏을 id 순서로 나눠 읽는다(afterId 다음부터 pageable 크기만큼). */
    @Query("select new com.quitmate.smokingPattern.repository.response.SmokingPatternSnapshotRow("
            + "s.id, s.userId, s.algorithmVersion, s.dataSufficiency, s.observedDays, s.smokeCount,"
            + " s.peakHour, s.routineType, s.trendDirection, s.result)"
            + " from SmokingPatternSnapshot s where s.periodStart = :periodStart and s.id > :afterId order by s.id")
    List<SmokingPatternSnapshotRow> findRowsAfter(@Param("periodStart") LocalDate periodStart,
                                                  @Param("afterId") long afterId,
                                                  Pageable pageable);
}
