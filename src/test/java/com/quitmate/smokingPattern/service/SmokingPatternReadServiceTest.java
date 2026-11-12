package com.quitmate.smokingPattern.service;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.global.exception.ForbiddenException;
import com.quitmate.global.exception.NotFoundException;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.page.request.PageInfoServiceRequest;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.security.LoginUserInfo;
import com.quitmate.smokingPattern.enums.BaselineComparisonType;
import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.enums.IndicatorStatus;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TrendDirection;
import com.quitmate.smokingPattern.service.request.SmokingPatternSnapshotListServiceRequest;
import com.quitmate.smokingPattern.service.response.SmokingPatternDistributionResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternSnapshotDetailResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternSnapshotSummaryResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternWeekResponse;
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
import java.util.List;

import static com.quitmate.smokingPattern.SmokingPatternFixture.createUserPassTableIfMissing;
import static com.quitmate.smokingPattern.SmokingPatternFixture.deleteAll;
import static com.quitmate.smokingPattern.SmokingPatternFixture.insertPass;
import static com.quitmate.smokingPattern.SmokingPatternFixture.insertSnapshot;
import static com.quitmate.smokingPattern.SmokingPatternFixture.insertSnapshotJson;
import static com.quitmate.smokingPattern.SmokingPatternFixture.insufficientResult;
import static com.quitmate.smokingPattern.SmokingPatternFixture.okResult;
import static com.quitmate.smokingPattern.SmokingPatternFixture.relapse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

class SmokingPatternReadServiceTest extends IntegrationTestSupport {

    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);
    private static final LocalDate PREVIOUS_WEEK = WEEK.minusWeeks(1);

    @Autowired
    private SmokingPatternReadService smokingPatternReadService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;
    private User paidMember;
    private User freeMember;

    @BeforeEach
    void setUp() {
        createUserPassTableIfMissing(jdbcTemplate);
        admin = userRepository.save(user("admin@test.com", "관리자", Role.ADMIN));
        paidMember = userRepository.save(user("paid@test.com", "유료회원", Role.USER));
        freeMember = userRepository.save(user("free@test.com", "무료회원", Role.USER));
        loginAs(admin);
    }

    @AfterEach
    void deleteSnapshots() {
        deleteAll(jdbcTemplate);
    }

    @DisplayName("관리자가 아니면 어떤 조회도 403이다")
    @Test
    void adminOnly() {
        loginAs(freeMember);

        assertThatThrownBy(() -> smokingPatternReadService.getWeeks(null)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> smokingPatternReadService.getWeekSnapshots(listRequest(null, null, null)))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> smokingPatternReadService.getUserSnapshots(freeMember.getId(), page()))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> smokingPatternReadService.getSnapshot(1L)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> smokingPatternReadService.getDistribution(null)).isInstanceOf(ForbiddenException.class);
    }

    @DisplayName("스냅숏이 하나도 없으면 빈 목록과 빈 분포를 준다")
    @Test
    void noSnapshots() {
        assertThat(smokingPatternReadService.getWeeks(null)).isEmpty();
        assertThat(smokingPatternReadService.getWeekSnapshots(listRequest(null, null, null)).getContent()).isEmpty();

        SmokingPatternDistributionResponse distribution = smokingPatternReadService.getDistribution(null);
        assertThat(distribution.periodStart()).isNull();
        assertThat(distribution.all().snapshotCount()).isZero();
    }

    @DisplayName("최근 주부터 스냅숏이 있는 주와 건수를 준다")
    @Test
    void weeks() {
        insertSnapshot(jdbcTemplate, paidMember.getId(), insufficientResult(PREVIOUS_WEEK));
        insertSnapshot(jdbcTemplate, paidMember.getId(), insufficientResult(WEEK));
        insertSnapshot(jdbcTemplate, freeMember.getId(), insufficientResult(WEEK));

        List<SmokingPatternWeekResponse> weeks = smokingPatternReadService.getWeeks(null);

        assertThat(weeks).extracting(SmokingPatternWeekResponse::periodStart).containsExactly(WEEK, PREVIOUS_WEEK);
        assertThat(weeks.get(0).periodEnd()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(weeks.get(0).snapshotCount()).isEqualTo(2);
        assertThat(smokingPatternReadService.getWeeks(1)).hasSize(1);
    }

    @DisplayName("주를 안 주면 가장 최근 주의 사용자별 스냅숏을 회원 ID 순으로, 유료 여부·닉네임과 함께 준다")
    @Test
    void weekSnapshots() {
        // given: 유료 회원은 그 주에 걸친 이용권, 무료 회원은 회수된 이용권과 지난 이용권만
        insertSnapshot(jdbcTemplate, paidMember.getId(), okResult(WEEK, 8, TrendDirection.DECREASED,
                RoutineType.ROUTINE, BaselineComparisonType.LESS, 3, 1, List.of()));
        insertSnapshot(jdbcTemplate, freeMember.getId(), insufficientResult(WEEK));
        insertSnapshot(jdbcTemplate, freeMember.getId(), insufficientResult(PREVIOUS_WEEK));
        insertPass(jdbcTemplate, paidMember.getId(), WEEK.plusDays(6).atStartOfDay(), WEEK.plusDays(96).atStartOfDay(), null);
        insertPass(jdbcTemplate, freeMember.getId(), WEEK.atStartOfDay(), WEEK.plusDays(90).atStartOfDay(),
                WEEK.plusDays(1).atStartOfDay());
        insertPass(jdbcTemplate, freeMember.getId(), WEEK.minusDays(90).atStartOfDay(), WEEK.atStartOfDay(), null);

        // when
        PageCustom<SmokingPatternSnapshotSummaryResponse> page =
                smokingPatternReadService.getWeekSnapshots(listRequest(null, null, null));

        // then
        assertThat(page.getContent()).hasSize(2);
        SmokingPatternSnapshotSummaryResponse paid = page.getContent().get(0);
        assertThat(paid.userId()).isEqualTo(paidMember.getId());
        assertThat(paid.nickName()).isEqualTo("유료회원");
        assertThat(paid.periodStart()).isEqualTo(WEEK);
        assertThat(paid.paid()).isTrue();
        assertThat(paid.peakHour()).isEqualTo(8);
        assertThat(paid.routineType()).isEqualTo(RoutineType.ROUTINE);
        assertThat(paid.dataSufficiency()).isEqualTo(DataSufficiencyLevel.SUFFICIENT);

        SmokingPatternSnapshotSummaryResponse free = page.getContent().get(1);
        assertThat(free.userId()).isEqualTo(freeMember.getId());
        assertThat(free.paid()).isFalse();
        assertThat(free.peakHour()).isNull();
        assertThat(page.getPageInfo().getTotalElement()).isEqualTo(2);
    }

    @DisplayName("주 목록은 충족도·회원 ID·닉네임·이메일 일부로 거른다")
    @Test
    void weekSnapshotsFilters() {
        insertSnapshot(jdbcTemplate, paidMember.getId(), okResult(WEEK, 8, TrendDirection.DECREASED,
                RoutineType.ROUTINE, BaselineComparisonType.LESS, 3, 1, List.of()));
        insertSnapshot(jdbcTemplate, freeMember.getId(), insufficientResult(WEEK));

        assertThat(smokingPatternReadService.getWeekSnapshots(listRequest(WEEK.toString(), "INSUFFICIENT", null))
                .getContent()).extracting(SmokingPatternSnapshotSummaryResponse::userId)
                .containsExactly(freeMember.getId());
        assertThat(smokingPatternReadService.getWeekSnapshots(listRequest(WEEK.toString(), null, "유료"))
                .getContent()).extracting(SmokingPatternSnapshotSummaryResponse::userId)
                .containsExactly(paidMember.getId());
        assertThat(smokingPatternReadService.getWeekSnapshots(listRequest(WEEK.toString(), null, "FREE@test"))
                .getContent()).extracting(SmokingPatternSnapshotSummaryResponse::userId)
                .containsExactly(freeMember.getId());
        assertThat(smokingPatternReadService.getWeekSnapshots(SmokingPatternSnapshotListServiceRequest.builder()
                        .page(1).size(20).periodStart(WEEK.toString()).userId(paidMember.getId()).build())
                .getContent()).hasSize(1);
        assertThat(smokingPatternReadService.getWeekSnapshots(listRequest(PREVIOUS_WEEK.toString(), null, null))
                .getContent()).isEmpty();
    }

    @DisplayName("분석 주 시작일이 월요일이 아니거나 형식이 틀리면, 충족도 값이 틀리면 400")
    @Test
    void badParameters() {
        assertThatThrownBy(() -> smokingPatternReadService.getDistribution("2026-11-03"))
                .isInstanceOf(QuitmateException.class)
                .hasMessageContaining("월요일");
        assertThatThrownBy(() -> smokingPatternReadService.getDistribution("2026/11/02"))
                .isInstanceOf(QuitmateException.class);
        assertThatThrownBy(() -> smokingPatternReadService.getWeekSnapshots(listRequest(WEEK.toString(), "GOOD", null)))
                .isInstanceOf(QuitmateException.class)
                .hasMessageContaining("SUFFICIENT");
    }

    @DisplayName("회원의 주차별 스냅숏을 최근 주부터, 주마다 그때 유료였는지와 함께 준다")
    @Test
    void userSnapshots() {
        insertSnapshot(jdbcTemplate, paidMember.getId(), insufficientResult(PREVIOUS_WEEK));
        insertSnapshot(jdbcTemplate, paidMember.getId(), insufficientResult(WEEK));
        insertSnapshot(jdbcTemplate, freeMember.getId(), insufficientResult(WEEK));
        // 이용권이 이번 주 수요일부터
        insertPass(jdbcTemplate, paidMember.getId(), WEEK.plusDays(2).atTime(15, 0), WEEK.plusDays(92).atStartOfDay(), null);

        PageCustom<SmokingPatternSnapshotSummaryResponse> page =
                smokingPatternReadService.getUserSnapshots(paidMember.getId(), page());

        assertThat(page.getContent()).extracting(SmokingPatternSnapshotSummaryResponse::periodStart)
                .containsExactly(WEEK, PREVIOUS_WEEK);
        assertThat(page.getContent()).extracting(SmokingPatternSnapshotSummaryResponse::paid)
                .containsExactly(true, false);
        assertThat(smokingPatternReadService.getUserSnapshots(admin.getId(), page()).getContent()).isEmpty();
        assertThatThrownBy(() -> smokingPatternReadService.getUserSnapshots(999_999L, page()))
                .isInstanceOf(NotFoundException.class);
    }

    @DisplayName("스냅숏 상세는 어드민 전용 지표 7·8까지 지표 1~9 전체를 준다. 없으면 404")
    @Test
    void snapshotDetail() {
        long id = insertSnapshot(jdbcTemplate, paidMember.getId(), okResult(WEEK, 21, TrendDirection.INCREASED,
                RoutineType.SCATTERED, BaselineComparisonType.MORE, 4, 1,
                List.of(relapse("2026-11-04", "WEDNESDAY", "08:15"))));
        insertPass(jdbcTemplate, paidMember.getId(), WEEK.atStartOfDay(), WEEK.plusDays(90).atStartOfDay(), null);

        SmokingPatternSnapshotDetailResponse detail = smokingPatternReadService.getSnapshot(id);

        assertThat(detail.userId()).isEqualTo(paidMember.getId());
        assertThat(detail.nickName()).isEqualTo("유료회원");
        assertThat(detail.paid()).isTrue();
        assertThat(detail.algorithmVersion()).isEqualTo("v1");
        assertThat(detail.result().peakTime().peakHour()).isEqualTo(21);
        assertThat(detail.result().peakTime().hourlyCounts()).hasSize(24);
        assertThat(detail.result().cravingResponse().status()).isEqualTo(IndicatorStatus.OK);
        assertThat(detail.result().cravingResponse().smokedAfterCount()).isEqualTo(1);
        assertThat(detail.result().relapseMoments().moments()).extracting("time").containsExactly("08:15");
        assertThat(detail.result().dataSufficiency().coveragePercent()).isEqualTo(100);
        assertThatThrownBy(() -> smokingPatternReadService.getSnapshot(id + 1000)).isInstanceOf(NotFoundException.class);
    }

    @DisplayName("앱 서버가 모르는 필드·enum 값을 더 넣어도 읽고, 깨진 JSON은 result만 null로 준다")
    @Test
    void tolerantJson() {
        String json = new com.quitmate.smokingPattern.entity.SmokingPatternResultConverter()
                .convertToDatabaseColumn(insufficientResult(WEEK))
                .replace("\"algorithmVersion\":\"v1\"", "\"algorithmVersion\":\"v1\",\"newIndicator\":{\"a\":1}")
                .replace("\"level\":\"INSUFFICIENT\"", "\"level\":\"SOMETHING_NEW\"");
        long tolerant = insertSnapshotJson(jdbcTemplate, paidMember.getId(), insufficientResult(WEEK), json);
        long broken = insertSnapshotJson(jdbcTemplate, freeMember.getId(), insufficientResult(WEEK), "{not json");

        assertThat(smokingPatternReadService.getSnapshot(tolerant).result().dataSufficiency().level()).isNull();
        assertThat(smokingPatternReadService.getSnapshot(broken).result()).isNull();
        assertThat(smokingPatternReadService.getDistribution(WEEK.toString()).all().unreadableResultCount()).isEqualTo(1);
    }

    @DisplayName("전체 분포를 전체·유료·무료로 나누고, 여러 번에 나눠 읽어도 모든 스냅숏을 센다")
    @Test
    void distribution() {
        // given: 무료 회원 스냅숏을 한 번에 읽는 수보다 많이 넣는다(회원 행 없이 user_id만)
        int freeCount = SmokingPatternReadService.CHUNK_SIZE + 3;
        for (int i = 0; i < freeCount; i++) {
            insertSnapshot(jdbcTemplate, 100_000L + i, insufficientResult(WEEK));
        }
        insertSnapshot(jdbcTemplate, paidMember.getId(), okResult(WEEK, 8, TrendDirection.DECREASED,
                RoutineType.ROUTINE, BaselineComparisonType.LESS, 4, 1,
                List.of(relapse("2026-11-04", "WEDNESDAY", "08:15"))));
        insertSnapshot(jdbcTemplate, paidMember.getId(), insufficientResult(PREVIOUS_WEEK));
        insertPass(jdbcTemplate, paidMember.getId(), WEEK.atStartOfDay(), WEEK.plusDays(90).atStartOfDay(), null);

        // when
        SmokingPatternDistributionResponse response = smokingPatternReadService.getDistribution(WEEK.toString());

        // then
        assertThat(response.periodStart()).isEqualTo(WEEK);
        assertThat(response.periodEnd()).isEqualTo(LocalDate.of(2026, 11, 8));
        assertThat(response.all().snapshotCount()).isEqualTo(freeCount + 1);
        assertThat(response.paid().snapshotCount()).isEqualTo(1);
        assertThat(response.free().snapshotCount()).isEqualTo(freeCount);

        assertThat(response.paid().routineType().counts()).containsEntry("ROUTINE", 1L);
        assertThat(response.paid().cravingResponse().sessionCount()).isEqualTo(4);
        assertThat(response.paid().relapseMoments().byHour().get(8)).isEqualTo(1);
        assertThat(response.free().dataSufficiency()).containsEntry(DataSufficiencyLevel.INSUFFICIENT, (long) freeCount);
        assertThat(response.free().cravingResponse().noDataUsers()).isEqualTo(freeCount);
        assertThat(response.all().peakHour().notCalculated()).isEqualTo(freeCount);
    }

    private void loginAs(User user) {
        given(securityService.getCurrentLoginUserInfo()).willReturn(LoginUserInfo.of(user.getId(), null, null));
    }

    private static SmokingPatternSnapshotListServiceRequest listRequest(String periodStart, String dataSufficiency,
                                                                        String keyword) {
        return SmokingPatternSnapshotListServiceRequest.builder()
                .page(1)
                .size(20)
                .periodStart(periodStart)
                .dataSufficiency(dataSufficiency)
                .keyword(keyword)
                .build();
    }

    private static PageInfoServiceRequest page() {
        return new PageInfoServiceRequest(1, 20);
    }

    private static User user(String email, String nickName, Role role) {
        return User.builder()
                .email(email)
                .password("encoded")
                .nickName(nickName)
                .phoneNumber("010-0000-0000")
                .sex(Sex.MALE)
                .role(role)
                .snsType(SnsType.NORMAL)
                .settingStatus(SettingStatus.COMPLETE)
                .build();
    }
}
