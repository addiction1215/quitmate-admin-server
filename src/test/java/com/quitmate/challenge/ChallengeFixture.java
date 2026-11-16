package com.quitmate.challenge;

import com.quitmate.challenge.challengehistory.entity.ChallengeStatus;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;

/**
 * 챌린지·미션·참여·미션 기록을 SQL로 넣는다. 참여 시작·마지막 수정·완료·포기 시각을 테스트가 정해야 해서
 * JPA auditing(@CreatedDate·@LastModifiedDate)을 거치지 않는다.
 */
public final class ChallengeFixture {

    private ChallengeFixture() {
    }

    public static long insertChallenge(JdbcTemplate jdbcTemplate, String title) {
        return insertChallenge(jdbcTemplate, title, "Y");
    }

    public static long insertChallenge(JdbcTemplate jdbcTemplate, String title, String useYn) {
        LocalDateTime now = LocalDateTime.of(2026, 11, 1, 9, 0);
        jdbcTemplate.update("""
                        insert into challenge (title, content, badge, reward, use_yn, created_date, updated_date)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                title, title + " 설명", "badge/" + title, 100, useYn, now, now);
        return jdbcTemplate.queryForObject("select max(id) from challenge", Long.class);
    }

    public static long insertMission(JdbcTemplate jdbcTemplate, long challengeId, MissionCategoryStatus category,
                                     String title, Integer minHoldSeconds) {
        return insertMission(jdbcTemplate, challengeId, category, title, minHoldSeconds, "Y");
    }

    public static long insertMission(JdbcTemplate jdbcTemplate, long challengeId, MissionCategoryStatus category,
                                     String title, Integer minHoldSeconds, String useYn) {
        LocalDateTime now = LocalDateTime.of(2026, 11, 1, 9, 0);
        jdbcTemplate.update("""
                        insert into mission (challenge_id, category, title, reward, content, use_yn, min_hold_seconds,
                            created_date, updated_date)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                challengeId, category.name(), title, 10, title + " 설명", useYn, minHoldSeconds, now, now);
        return jdbcTemplate.queryForObject("select max(id) from mission", Long.class);
    }

    /**
     * 챌린지 참여(challenge_history) 한 건. completedAt·cancelledAt이 null이면 KAN-190 전에 끝난 옛 행처럼 비워 둔다.
     */
    public static long insertParticipation(JdbcTemplate jdbcTemplate, long challengeId, long userId,
                                           ChallengeStatus status, LocalDateTime startedAt, LocalDateTime updatedAt,
                                           LocalDateTime completedAt, LocalDateTime cancelledAt) {
        jdbcTemplate.update("""
                        insert into challenge_history (challenge_id, user_id, status, created_date, updated_date,
                            completed_at, cancelled_at)
                        values (?, ?, ?, ?, ?, ?, ?)
                        """,
                challengeId, userId, status.name(), startedAt, updatedAt, completedAt, cancelledAt);
        return jdbcTemplate.queryForObject("select max(id) from challenge_history", Long.class);
    }

    public static long insertMissionHistory(JdbcTemplate jdbcTemplate, long challengeHistoryId, long missionId,
                                            long userId, MissionStatus status) {
        LocalDateTime now = LocalDateTime.of(2026, 11, 2, 9, 0);
        jdbcTemplate.update("""
                        insert into mission_history (challenge_history_id, mission_id, user_id, status, complete_at,
                            gps_verify_count, abstinence_time, created_date, updated_date)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                challengeHistoryId, missionId, userId, status.name(),
                status == MissionStatus.COMPLETED ? now : null, 0, 0, now, now);
        return jdbcTemplate.queryForObject("select max(id) from mission_history", Long.class);
    }

    public static String missionHistoryStatus(JdbcTemplate jdbcTemplate, long missionHistoryId) {
        return jdbcTemplate.queryForObject("select status from mission_history where id = ?", String.class,
                missionHistoryId);
    }

    /** 자식 테이블부터 지운다. 사용자는 IntegrationTestSupport·각 테스트가 지운다 */
    public static void deleteAll(JdbcTemplate jdbcTemplate) {
        jdbcTemplate.update("delete from mission_history_address");
        jdbcTemplate.update("delete from mission_history");
        jdbcTemplate.update("delete from challenge_history");
        jdbcTemplate.update("delete from mission");
        jdbcTemplate.update("delete from challenge");
    }
}
