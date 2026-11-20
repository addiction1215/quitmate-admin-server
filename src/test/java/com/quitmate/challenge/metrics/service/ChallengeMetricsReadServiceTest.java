package com.quitmate.challenge.metrics.service;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.challenge.challengehistory.entity.ChallengeStatus;
import com.quitmate.challenge.metrics.service.request.ChallengeMetricsServiceRequest;
import com.quitmate.challenge.metrics.service.response.ChallengeMetrics;
import com.quitmate.challenge.metrics.service.response.ChallengeMetricsDetailResponse;
import com.quitmate.challenge.metrics.service.response.ChallengeMetricsListResponse;
import com.quitmate.challenge.metrics.service.response.MetricRatio;
import com.quitmate.challenge.metrics.service.response.MissionMetrics;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
import com.quitmate.global.exception.ForbiddenException;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.Role;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.Sex;
import com.quitmate.user.users.entity.enums.SnsType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.quitmate.challenge.ChallengeFixture.deleteAll;
import static com.quitmate.challenge.ChallengeFixture.insertChallenge;
import static com.quitmate.challenge.ChallengeFixture.insertMission;
import static com.quitmate.challenge.ChallengeFixture.insertMissionHistory;
import static com.quitmate.challenge.ChallengeFixture.insertParticipation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.BDDMockito.given;

/**
 * 챌린지 지표를 실제 쿼리(H2)로 계산한다. 데이터는 아래와 같다(시각은 한국 시각, 기간 11/1~11/7).
 *
 * <pre>
 * 참여 p1 회원1 완료   11/02 10:00 → 완료 12:00           미션: 참기 완료 · 위치 완료 · (삭제된 미션) 완료
 *      p2 회원2 포기   11/03 09:00 → 포기 10:00           미션: 참기 완료 · 위치 진행
 *      p3 회원3 포기   11/04 09:00 → (옛 행) 수정 12:00   미션: 참기 진행 · 위치 진행
 *      p4 회원2 진행   11/05 09:00 (p2 포기 뒤 재참여)      미션: 참기 옛 승인 대기(READY) · 위치 진행
 *      p5 회원4 완료   10/25 09:00 → (옛 행) 수정 13:00   미션: 참기 완료 · 위치 완료          ← 기간 전
 *      p6 회원5 진행   11/07 23:59:59                     미션: 참기 진행 · 위치 진행          ← 기간 끝
 *      p7 회원6 진행   11/08 00:00                        미션: 참기 완료 · 위치 진행          ← 기간 뒤
 * </pre>
 */
class ChallengeMetricsReadServiceTest extends IntegrationTestSupport {

    private static final ChallengeMetricsServiceRequest FIRST_WEEK = ChallengeMetricsServiceRequest.builder()
            .from("2026-11-01").to("2026-11-07").build();
    private static final ChallengeMetricsServiceRequest ALL_TIME = ChallengeMetricsServiceRequest.builder().build();

    @Autowired
    private ChallengeMetricsReadService challengeMetricsReadService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;
    private long challengeId;
    private long emptyChallengeId;
    private long deletedChallengeId;
    private long holdMissionId;
    private long locationMissionId;
    private long laterMissionId;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(member("admin@test.com", Role.ADMIN));
        given(securityService.getCurrentLoginUserInfo()).willReturn(createLoginUserInfo(admin.getId()));
        List<Long> members = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            members.add(userRepository.save(member("member" + i + "@test.com", Role.USER)).getId());
        }

        challengeId = insertChallenge(jdbcTemplate, "출근길 금연");
        holdMissionId = insertMission(jdbcTemplate, challengeId, MissionCategoryStatus.HOLD, "10분 참기", null);
        locationMissionId = insertMission(jdbcTemplate, challengeId, MissionCategoryStatus.LOCATION, "흡연 장소 피하기", null);
        long deletedMissionId = insertMission(jdbcTemplate, challengeId, MissionCategoryStatus.REPLACE_ACTION,
                "삭제된 미션", null, "N");
        laterMissionId = insertMission(jdbcTemplate, challengeId, MissionCategoryStatus.HOLD, "나중에 추가한 미션", 300);
        emptyChallengeId = insertChallenge(jdbcTemplate, "빈 챌린지");
        deletedChallengeId = insertChallenge(jdbcTemplate, "삭제된 챌린지", "N");

        long p1 = participation(members.get(0), ChallengeStatus.COMPLETED, at(2, 10, 0), at(2, 12, 0), at(2, 12, 0), null);
        missions(p1, members.get(0), MissionStatus.COMPLETED, MissionStatus.COMPLETED);
        insertMissionHistory(jdbcTemplate, p1, deletedMissionId, members.get(0), MissionStatus.COMPLETED);
        long p2 = participation(members.get(1), ChallengeStatus.CANCELLED, at(3, 9, 0), at(3, 10, 0), null, at(3, 10, 0));
        missions(p2, members.get(1), MissionStatus.COMPLETED, MissionStatus.PROGRESSING);
        long p3 = participation(members.get(2), ChallengeStatus.CANCELLED, at(4, 9, 0), at(4, 12, 0), null, null);
        missions(p3, members.get(2), MissionStatus.PROGRESSING, MissionStatus.PROGRESSING);
        long p4 = participation(members.get(1), ChallengeStatus.PROGRESSING, at(5, 9, 0), at(5, 9, 0), null, null);
        missions(p4, members.get(1), MissionStatus.READY, MissionStatus.PROGRESSING);
        LocalDateTime october = LocalDateTime.of(2026, 10, 25, 9, 0);
        long p5 = participation(members.get(3), ChallengeStatus.COMPLETED, october, october.plusHours(4), null, null);
        missions(p5, members.get(3), MissionStatus.COMPLETED, MissionStatus.COMPLETED);
        LocalDateTime lastSecond = LocalDateTime.of(2026, 11, 7, 23, 59, 59);
        long p6 = participation(members.get(4), ChallengeStatus.PROGRESSING, lastSecond, lastSecond, null, null);
        missions(p6, members.get(4), MissionStatus.PROGRESSING, MissionStatus.PROGRESSING);
        LocalDateTime nextDay = LocalDateTime.of(2026, 11, 8, 0, 0);
        long p7 = participation(members.get(5), ChallengeStatus.PROGRESSING, nextDay, nextDay, null, null);
        missions(p7, members.get(5), MissionStatus.COMPLETED, MissionStatus.PROGRESSING);
    }

    @AfterEach
    void cleanUp() {
        deleteAll(jdbcTemplate);
    }

    @DisplayName("기간 상세: 참여 시작일이 11/1~11/7인 참여만 세고, 미션별 분모는 그 참여의 미션 기록이다")
    @Test
    void detailInPeriod() {
        // when
        ChallengeMetricsDetailResponse response = challengeMetricsReadService.getChallengeMetrics(challengeId, FIRST_WEEK);

        // then
        assertThat(response.from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(response.to()).isEqualTo(LocalDate.of(2026, 11, 7));
        ChallengeMetrics metrics = response.challenge();
        assertThat(metrics.challengeId()).isEqualTo(challengeId);
        assertThat(metrics.title()).isEqualTo("출근길 금연");
        assertThat(metrics.participantCount()).as("회원1·2·3·5").isEqualTo(4);
        assertThat(metrics.participationCount()).as("p1·p2·p3·p4·p6").isEqualTo(5);
        assertThat(metrics.progressingCount()).isEqualTo(2);
        assertThat(metrics.completedCount()).isEqualTo(1);
        assertThat(metrics.cancelledCount()).isEqualTo(2);
        assertThat(metrics.otherStatusCount()).isZero();
        assertThat(metrics.completion()).isEqualTo(new MetricRatio(1, 5, 20));
        assertThat(metrics.cancellation()).isEqualTo(new MetricRatio(2, 5, 40));
        assertThat(metrics.rejoin()).as("포기한 회원2·3 중 회원2가 다시 참여").isEqualTo(new MetricRatio(1, 2, 50));
        assertThat(metrics.averageHoursToComplete()).isEqualTo(2.0);
        assertThat(metrics.averageHoursToCancel()).as("1시간·3시간(옛 행은 수정 시각)").isEqualTo(2.0);
        assertThat(metrics.endTimeFallbackCount()).isEqualTo(1);

        assertThat(response.missions())
                .extracting(MissionMetrics::missionId, MissionMetrics::title, MissionMetrics::category, MissionMetrics::completion)
                .as("삭제된 미션은 빼고 미션 ID 순. 옛 승인 대기(READY)는 완료가 아니다")
                .containsExactly(
                        tuple(holdMissionId, "10분 참기", MissionCategoryStatus.HOLD, new MetricRatio(2, 5, 40)),
                        tuple(locationMissionId, "흡연 장소 피하기", MissionCategoryStatus.LOCATION, new MetricRatio(1, 5, 20)),
                        tuple(laterMissionId, "나중에 추가한 미션", MissionCategoryStatus.HOLD, new MetricRatio(0, 0, null))
                );
    }

    @DisplayName("기간을 안 주면 전체 참여를 센다")
    @Test
    void detailAllTime() {
        // when
        ChallengeMetricsDetailResponse response = challengeMetricsReadService.getChallengeMetrics(challengeId, ALL_TIME);

        // then
        assertThat(response.from()).isNull();
        assertThat(response.to()).isNull();
        ChallengeMetrics metrics = response.challenge();
        assertThat(metrics.participantCount()).isEqualTo(6);
        assertThat(metrics.participationCount()).isEqualTo(7);
        assertThat(metrics.completion()).isEqualTo(new MetricRatio(2, 7, 29));
        assertThat(metrics.cancellation()).isEqualTo(new MetricRatio(2, 7, 29));
        assertThat(metrics.rejoin()).isEqualTo(new MetricRatio(1, 2, 50));
        assertThat(metrics.averageHoursToComplete()).as("2시간·4시간(옛 행)").isEqualTo(3.0);
        assertThat(metrics.endTimeFallbackCount()).as("p3·p5").isEqualTo(2);
        assertThat(response.missions()).extracting(MissionMetrics::completion).containsExactly(
                new MetricRatio(4, 7, 57), new MetricRatio(2, 7, 29), new MetricRatio(0, 0, null));
    }

    @DisplayName("목록: 사용 중인 챌린지만 최근 것부터, 참여가 없는 챌린지는 0명·비율 null")
    @Test
    void list() {
        // when
        ChallengeMetricsListResponse response = challengeMetricsReadService.getChallengeMetricsList(FIRST_WEEK);

        // then
        assertThat(response.from()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(response.challenges()).extracting(ChallengeMetrics::challengeId)
                .containsExactly(emptyChallengeId, challengeId);
        ChallengeMetrics empty = response.challenges().get(0);
        assertThat(empty.title()).isEqualTo("빈 챌린지");
        assertThat(empty.participantCount()).isZero();
        assertThat(empty.completion()).isEqualTo(new MetricRatio(0, 0, null));
        assertThat(empty.rejoin()).isEqualTo(new MetricRatio(0, 0, null));
        assertThat(empty.averageHoursToComplete()).isNull();
        assertThat(response.challenges().get(1))
                .isEqualTo(challengeMetricsReadService.getChallengeMetrics(challengeId, FIRST_WEEK).challenge());
    }

    @DisplayName("참여가 한 번에 읽는 수(500건)보다 많아도 모두 센다")
    @Test
    void readsAllChunks() {
        // given
        long busyId = insertChallenge(jdbcTemplate, "붐비는 챌린지");
        Long userId = userRepository.save(member("busy@test.com", Role.USER)).getId();
        LocalDateTime start = LocalDateTime.of(2026, 11, 2, 9, 0);
        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < ChallengeMetricsReadService.CHUNK_SIZE + 1; i++) {
            rows.add(new Object[]{busyId, userId, "CANCELLED", start, start.plusMinutes(6)});
        }
        jdbcTemplate.batchUpdate("insert into challenge_history (challenge_id, user_id, status, created_date, updated_date) "
                + "values (?, ?, ?, ?, ?)", rows);

        // when
        ChallengeMetrics metrics = challengeMetricsReadService.getChallengeMetrics(busyId, ALL_TIME).challenge();

        // then
        assertThat(metrics.participationCount()).isEqualTo(501);
        assertThat(metrics.participantCount()).isEqualTo(1);
        assertThat(metrics.rejoin()).as("포기 뒤 다시 참여한 같은 사람 1명").isEqualTo(new MetricRatio(1, 1, 100));
        assertThat(metrics.averageHoursToCancel()).isEqualTo(0.1);
        assertThat(metrics.endTimeFallbackCount()).isEqualTo(501);
    }

    @DisplayName("기간 형식·순서가 틀리거나 없는·삭제된 챌린지면 사람이 읽는 문장으로 400")
    @Test
    void invalidRequest() {
        assertThatThrownBy(() -> challengeMetricsReadService.getChallengeMetricsList(
                ChallengeMetricsServiceRequest.builder().from("2026-11-08").to("2026-11-07").build()))
                .isInstanceOf(QuitmateException.class)
                .hasMessage("기간 시작일(from)은 끝 날짜(to)보다 늦을 수 없습니다.");
        assertThatThrownBy(() -> challengeMetricsReadService.getChallengeMetrics(challengeId,
                ChallengeMetricsServiceRequest.builder().from("2026/11/01").build()))
                .isInstanceOf(QuitmateException.class)
                .hasMessage("기간은 2026-11-01 형식(yyyy-MM-dd)으로 보내 주세요.");
        assertThatThrownBy(() -> challengeMetricsReadService.getChallengeMetrics(deletedChallengeId, ALL_TIME))
                .isInstanceOf(QuitmateException.class)
                .hasMessage("해당 챌린지는 존재하지 않습니다.");
        // 같은 날은 된다
        assertThat(challengeMetricsReadService.getChallengeMetrics(challengeId,
                ChallengeMetricsServiceRequest.builder().from("2026-11-02").to("2026-11-02").build())
                .challenge().participationCount()).isEqualTo(1);
    }

    @DisplayName("관리자가 아니면 403")
    @Test
    void adminOnly() {
        User member = userRepository.save(member("not-admin@test.com", Role.USER));
        given(securityService.getCurrentLoginUserInfo()).willReturn(createLoginUserInfo(member.getId()));

        assertThatThrownBy(() -> challengeMetricsReadService.getChallengeMetricsList(ALL_TIME))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> challengeMetricsReadService.getChallengeMetrics(challengeId, ALL_TIME))
                .isInstanceOf(ForbiddenException.class);
    }

    private long participation(Long userId, ChallengeStatus status, LocalDateTime startedAt, LocalDateTime updatedAt,
                               LocalDateTime completedAt, LocalDateTime cancelledAt) {
        return insertParticipation(jdbcTemplate, challengeId, userId, status, startedAt, updatedAt, completedAt, cancelledAt);
    }

    private void missions(long participationId, Long userId, MissionStatus hold, MissionStatus location) {
        insertMissionHistory(jdbcTemplate, participationId, holdMissionId, userId, hold);
        insertMissionHistory(jdbcTemplate, participationId, locationMissionId, userId, location);
    }

    private static LocalDateTime at(int novemberDay, int hour, int minute) {
        return LocalDateTime.of(2026, 11, novemberDay, hour, minute);
    }

    private static User member(String email, Role role) {
        return User.builder()
                .email(email)
                .password("encoded")
                .nickName("닉네임")
                .phoneNumber("010-0000-0000")
                .sex(Sex.MALE)
                .role(role)
                .snsType(SnsType.NORMAL)
                .settingStatus(SettingStatus.COMPLETE)
                .build();
    }
}
