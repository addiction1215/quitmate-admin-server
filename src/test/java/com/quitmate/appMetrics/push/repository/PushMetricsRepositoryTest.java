package com.quitmate.appMetrics.push.repository;

import com.quitmate.appMetrics.push.repository.response.DeliveryDailyCount;
import com.quitmate.appMetrics.push.repository.response.ErrorCodeCount;
import com.quitmate.appMetrics.push.repository.response.OutboxDailyCount;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.Role;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.Sex;
import com.quitmate.user.users.entity.enums.SnsType;
import com.quitmate.user.users.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.quitmate.appMetrics.push.PushMetricsFixture.createTablesIfMissing;
import static com.quitmate.appMetrics.push.PushMetricsFixture.dailySmokingOutbox;
import static com.quitmate.appMetrics.push.PushMetricsFixture.delivery;
import static com.quitmate.appMetrics.push.PushMetricsFixture.deleteAll;
import static com.quitmate.appMetrics.push.PushMetricsFixture.otherOutbox;
import static com.quitmate.appMetrics.push.PushMetricsFixture.pushToken;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 푸시 지표 SQL을 테스트 DB(H2, PostgreSQL 모드)에서 그대로 돌려 숫자 정의와 날짜 경계를 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
class PushMetricsRepositoryTest {

    /** 2026-11-13 ~ 2026-11-19 (period 7, 오늘 11-19) */
    private static final LocalDateTime FROM = LocalDateTime.of(2026, 11, 13, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2026, 11, 20, 0, 0);
    private static final LocalDate DAY1 = LocalDate.of(2026, 11, 13);
    private static final LocalDate DAY7 = LocalDate.of(2026, 11, 19);

    @Autowired
    private PushMetricsRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private UserRepository userRepository;

    private long userId;

    @BeforeEach
    void setUp() {
        createTablesIfMissing(jdbcTemplate);
        userId = userRepository.save(user("member@test.com")).getId();
    }

    @AfterEach
    void tearDown() {
        deleteAll(jdbcTemplate);
        userRepository.deleteAllInBatch();
    }

    @DisplayName("push_outbox: 생성 시각 한국 날짜로 [시작 00:00, 끝 다음 날 00:00) 안만 센다. 발송 요청은 종류·상태 무관 전부")
    @Test
    void outboxDailyCounts() {
        // 경계 밖
        dailySmokingOutbox(jdbcTemplate, userId, LocalDateTime.of(2026, 11, 12, 23, 59, 59, 999_999_000), "SENT", null);
        dailySmokingOutbox(jdbcTemplate, userId, TO, "SENT", null);
        // 첫날: 하루 흡연 SENT 2(열림 1), 실패 1, 슬롯 없는 푸시 1(열림이어도 하루 흡연 열림에는 안 셈)
        dailySmokingOutbox(jdbcTemplate, userId, FROM, "SENT", FROM.plusMinutes(3));
        dailySmokingOutbox(jdbcTemplate, userId, FROM.plusHours(8), "SENT", null);
        dailySmokingOutbox(jdbcTemplate, userId, FROM.plusHours(9), "FAILED", null);
        otherOutbox(jdbcTemplate, userId, FROM.plusHours(10), "SENT", FROM.plusHours(11));
        // 마지막 날: 아직 보내기 전(RETRY)인데 먼저 열린 것(여러 기기 중 한 대 먼저 열림)은 분모에도 넣는다
        dailySmokingOutbox(jdbcTemplate, userId, LocalDateTime.of(2026, 11, 19, 23, 59, 59), "RETRY", DAY7.atTime(23, 59, 59));
        dailySmokingOutbox(jdbcTemplate, userId, DAY7.atTime(12, 0), "CANCELED", null);

        List<OutboxDailyCount> counts = repository.findOutboxDailyCounts(FROM, TO);

        assertThat(counts).containsExactlyInAnyOrder(
                new OutboxDailyCount(DAY1, 4, 2, 1),
                new OutboxDailyCount(DAY7, 2, 1, 1));
    }

    @DisplayName("push_delivery: outbox 없는 행, 티켓·영수증 상태별로 센다")
    @Test
    void deliveryDailyCounts() {
        long outboxId = dailySmokingOutbox(jdbcTemplate, userId, FROM.plusHours(8), "SENT", null);
        delivery(jdbcTemplate, userId, outboxId, FROM.plusHours(8), "OK", null, "OK", null);
        delivery(jdbcTemplate, userId, outboxId, FROM.plusHours(8), "OK", null, "ERROR", "DeviceNotRegistered");
        delivery(jdbcTemplate, userId, null, FROM.plusHours(9), "ERROR", "DeviceNotRegistered", "SKIPPED", null);
        delivery(jdbcTemplate, userId, null, DAY7.atTime(21, 0), "OK", null, "PENDING", null);
        delivery(jdbcTemplate, userId, null, TO, "OK", null, "PENDING", null);

        List<DeliveryDailyCount> counts = repository.findDeliveryDailyCounts(FROM, TO);

        assertThat(counts).containsExactlyInAnyOrder(
                new DeliveryDailyCount(DAY1, 3, 1, 2, 1, 1, 1, 0),
                new DeliveryDailyCount(DAY7, 1, 1, 1, 0, 0, 0, 1));
    }

    @DisplayName("오류 코드: ticket_error·receipt_error를 합쳐 많은 순(같으면 코드 순), 빈 값·기간 밖은 빼고 최대 20개")
    @Test
    void errorCodeCounts() {
        delivery(jdbcTemplate, userId, null, FROM.plusHours(1), "ERROR", "DeviceNotRegistered", "SKIPPED", null);
        delivery(jdbcTemplate, userId, null, FROM.plusHours(2), "OK", null, "ERROR", "DeviceNotRegistered");
        delivery(jdbcTemplate, userId, null, FROM.plusHours(3), "OK", null, "ERROR", "MessageRateExceeded");
        delivery(jdbcTemplate, userId, null, FROM.plusHours(4), "ERROR", "InvalidCredentials", "SKIPPED", "");
        delivery(jdbcTemplate, userId, null, FROM.minusSeconds(1), "ERROR", "MessageTooBig", "SKIPPED", null);
        for (int i = 0; i < 25; i++) {
            delivery(jdbcTemplate, userId, null, FROM.plusHours(5), "ERROR", "Code" + (char) ('A' + i), "SKIPPED", null);
        }

        List<ErrorCodeCount> codes = repository.findErrorCodeCounts(FROM, TO);

        assertThat(codes).hasSize(PushMetricsRepository.ERROR_CODE_LIMIT);
        assertThat(codes.get(0)).isEqualTo(new ErrorCodeCount("DeviceNotRegistered", 2));
        assertThat(codes.get(1)).isEqualTo(new ErrorCodeCount("CodeA", 1));
        assertThat(codes).extracting(ErrorCodeCount::code).doesNotContain("MessageTooBig", "");
    }

    @DisplayName("토큰 보유 회원: 토큰이 있는 push 행의 서로 다른 user_id 수(기기 여러 대여도 1명)")
    @Test
    void countTokenUsers() {
        long other = userRepository.save(user("other@test.com")).getId();
        long noToken = userRepository.save(user("no-token@test.com")).getId();
        pushToken(jdbcTemplate, userId, "device-1", "ExponentPushToken[a]");
        pushToken(jdbcTemplate, userId, "device-2", "ExponentPushToken[b]");
        pushToken(jdbcTemplate, other, "device-3", "ExponentPushToken[c]");
        pushToken(jdbcTemplate, noToken, "device-4", null);
        pushToken(jdbcTemplate, noToken, "device-5", "");
        pushToken(jdbcTemplate, null, "device-6", "ExponentPushToken[d]");

        assertThat(repository.countTokenUsers()).isEqualTo(2);
    }

    @DisplayName("행이 없으면 빈 목록·0")
    @Test
    void empty() {
        assertThat(repository.findOutboxDailyCounts(FROM, TO)).isEmpty();
        assertThat(repository.findDeliveryDailyCounts(FROM, TO)).isEmpty();
        assertThat(repository.findErrorCodeCounts(FROM, TO)).isEmpty();
        assertThat(repository.countTokenUsers()).isZero();
    }

    private static User user(String email) {
        return User.builder()
                .email(email)
                .password("encoded")
                .nickName("닉네임")
                .phoneNumber("010-0000-0000")
                .sex(Sex.MALE)
                .role(Role.USER)
                .snsType(SnsType.NORMAL)
                .settingStatus(SettingStatus.COMPLETE)
                .build();
    }
}
