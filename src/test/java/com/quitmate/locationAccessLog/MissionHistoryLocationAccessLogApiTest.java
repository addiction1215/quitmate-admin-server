package com.quitmate.locationAccessLog;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.challenge.challange.repository.ChallengeJpaRepository;
import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import com.quitmate.challenge.mission.repository.MissionJpaRepository;
import com.quitmate.challenge.missionhistory.entity.MissionHistory;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
import com.quitmate.challenge.missionhistory.repository.MissionHistoryJpaRepository;
import com.quitmate.global.jwt.JwtTokenGenerator;
import com.quitmate.global.jwt.dto.LoginUserInfo;
import com.quitmate.locationAccessLog.entity.AdminLocationAccessLog;
import com.quitmate.locationAccessLog.enums.LocationAccessResourceType;
import com.quitmate.locationAccessLog.repository.AdminLocationAccessLogJpaRepository;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 위치정보 열람 이력(KAN-208). 실제 SecurityConfig·JWT 필터(KAN-206)를 지나 미션 이력 상세를 부르고 이력 표를 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class MissionHistoryLocationAccessLogApiTest {

    private static final String DETAIL = "/api/v1/admin/mission-history/{id}";
    private static final String DETAIL_API = "GET /api/v1/admin/mission-history/{id}";
    private static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    private static final String SEOUL_CITY_HALL = "37.566535,126.977969";
    private static final String GWANGHWAMUN = "37.575987,126.976823";
    private static final List<String> COORDINATE_PARTS = List.of("37.566535", "126.977969", "37.575987", "126.976823");
    private static final String TABLE = "admin_location_access_log";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ChallengeJpaRepository challengeJpaRepository;
    @Autowired
    private MissionJpaRepository missionJpaRepository;
    @Autowired
    private MissionHistoryJpaRepository missionHistoryJpaRepository;
    @Autowired
    private AdminLocationAccessLogJpaRepository accessLogJpaRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;
    private User member;
    private Challenge challenge;

    @BeforeEach
    void setUp() {
        // 다른 테스트가 남긴 이력이 줄 수 세기에 섞이지 않게
        accessLogJpaRepository.deleteAllInBatch();
        admin = userRepository.save(user("admin@test.com", Role.ADMIN));
        member = userRepository.save(user("member@test.com", Role.USER));
        challenge = challengeJpaRepository.save(Challenge.builder()
                .title("금연 챌린지")
                .content("설명")
                .badge("badge/test")
                .reward(100)
                .build());
    }

    @AfterEach
    void tearDown() {
        accessLogJpaRepository.deleteAllInBatch();
        jdbcTemplate.update("delete from mission_history_address");
        missionHistoryJpaRepository.deleteAllInBatch();
        missionJpaRepository.deleteAllInBatch();
        challengeJpaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("관리자가 좌표가 담긴 미션 이력 상세를 열면 이력이 한 줄 생긴다(관리자·회원·종류·행 id·API·한국 시각)")
    @Test
    void adminViewingCoordinatesLeavesOneRow() throws Exception {
        // given
        MissionHistory history = saveHistory(MissionCategoryStatus.LOCATION, List.of(SEOUL_CITY_HALL, GWANGHWAMUN));
        LocalDateTime before = LocalDateTime.now(KOREA);

        // when
        mockMvc.perform(get(DETAIL, history.getId()).header("Authorization", token(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationData.addresses", contains(SEOUL_CITY_HALL, GWANGHWAMUN)));
        LocalDateTime after = LocalDateTime.now(KOREA);

        // then
        List<AdminLocationAccessLog> logs = accessLogJpaRepository.findAll();
        assertThat(logs).singleElement().satisfies(log -> {
            assertThat(log.getAdminUserId()).isEqualTo(admin.getId());
            assertThat(log.getTargetUserId()).isEqualTo(member.getId());
            assertThat(log.getResourceType()).isEqualTo(LocationAccessResourceType.MISSION_HISTORY_ADDRESS);
            assertThat(log.getResourceId()).isEqualTo(history.getId());
            assertThat(log.getApi()).isEqualTo(DETAIL_API);
            // 서버 JVM 시간대와 무관하게 한국 시각(CI 는 UTC). 저장 정밀도 차이만큼 여유를 둔다
            assertThat(log.getAccessedAt()).isBetween(before.minusSeconds(1), after.plusSeconds(1));
        });
    }

    @DisplayName("열 때마다 한 줄씩 쌓인다")
    @Test
    void eachViewLeavesItsOwnRow() throws Exception {
        // given
        MissionHistory history = saveHistory(MissionCategoryStatus.LOCATION, List.of(SEOUL_CITY_HALL));

        // when
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(get(DETAIL, history.getId()).header("Authorization", token(admin)))
                    .andExpect(status().isOk());
        }

        // then
        assertThat(accessLogJpaRepository.findAll())
                .extracting(AdminLocationAccessLog::getResourceId)
                .containsExactly(history.getId(), history.getId());
    }

    @DisplayName("이력 표에는 위치 값(주소·좌표)이 없다: 칸은 7개뿐이고 어느 값에도 좌표가 들어가지 않는다")
    @Test
    void accessLogHoldsNoLocationValue() throws Exception {
        // given
        MissionHistory history = saveHistory(MissionCategoryStatus.LOCATION, List.of(SEOUL_CITY_HALL, GWANGHWAMUN));

        // when
        mockMvc.perform(get(DETAIL, history.getId()).header("Authorization", token(admin)))
                .andExpect(status().isOk());

        // then
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("select * from " + TABLE);
        assertThat(rows).hasSize(1);
        Map<String, Object> row = rows.get(0);
        assertThat(row.keySet().stream().map(key -> key.toLowerCase(Locale.ROOT)).collect(Collectors.toSet()))
                .containsExactlyInAnyOrder("id", "admin_user_id", "target_user_id", "resource_type", "resource_id",
                        "api", "accessed_at");
        assertThat(row.values()).allSatisfy(value -> assertThat(String.valueOf(value))
                .doesNotContain(COORDINATE_PARTS.toArray(String[]::new)));
    }

    @DisplayName("관리자가 아니면(일반 회원) 403이고 이력이 생기지 않는다. 토큰이 없으면 401이고 이력이 없다")
    @Test
    void nonAdminIsForbiddenAndLeavesNoRow() throws Exception {
        // given
        MissionHistory history = saveHistory(MissionCategoryStatus.LOCATION, List.of(SEOUL_CITY_HALL));

        // when
        mockMvc.perform(get(DETAIL, history.getId()).header("Authorization", token(member)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("관리자 계정만 사용할 수 있습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());
        mockMvc.perform(get(DETAIL, history.getId()))
                .andExpect(status().isUnauthorized());

        // then
        assertThat(accessLogJpaRepository.count()).isZero();
    }

    @DisplayName("위치 값이 나가지 않으면 남기지 않는다: 좌표가 빈 LOCATION·사진 유형·참기 유형 상세, 없는 이력(400), 미션 이력 목록")
    @Test
    void noRowWhenNoCoordinateGoesOut() throws Exception {
        // given
        MissionHistory emptyLocation = saveHistory(MissionCategoryStatus.LOCATION, List.of());
        MissionHistory photo = saveHistory(MissionCategoryStatus.REPLACE_ACTION, List.of());
        MissionHistory hold = saveHistory(MissionCategoryStatus.HOLD, List.of());
        String token = token(admin);

        // when
        mockMvc.perform(get(DETAIL, emptyLocation.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationData.addresses", empty()));
        mockMvc.perform(get(DETAIL, photo.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationData").doesNotExist());
        mockMvc.perform(get(DETAIL, hold.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.locationData").doesNotExist());
        mockMvc.perform(get(DETAIL, 999_999L).header("Authorization", token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/admin/mission-history").param("page", "1").param("size", "20")
                        .header("Authorization", token))
                .andExpect(status().isOk());

        // then
        assertThat(accessLogJpaRepository.count()).isZero();
    }

    /**
     * 표 이름을 바꿔 실제 DB 오류를 낸다. 이력 쓰기가 조회와 같은 트랜잭션이면 조회 트랜잭션이 rollback-only 가 되어 500 이 나므로,
     * 200 이 나오는 것으로 트랜잭션 분리(REQUIRES_NEW)도 함께 확인한다.
     */
    @DisplayName("이력 저장이 실패해도 상세는 200으로 좌표까지 그대로 나가고, 로그에 위치 값 없는 ERROR 한 줄이 남는다")
    @Test
    void logFailureDoesNotBlockResponse(CapturedOutput output) throws Exception {
        // given
        MissionHistory history = saveHistory(MissionCategoryStatus.LOCATION, List.of(SEOUL_CITY_HALL, GWANGHWAMUN));
        jdbcTemplate.execute("alter table " + TABLE + " rename to " + TABLE + "_off");

        // when
        try {
            mockMvc.perform(get(DETAIL, history.getId()).header("Authorization", token(admin)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusCode").value(200))
                    .andExpect(jsonPath("$.data.locationData.addresses", contains(SEOUL_CITY_HALL, GWANGHWAMUN)));
        } finally {
            jdbcTemplate.execute("alter table " + TABLE + "_off rename to " + TABLE);
        }

        // then
        assertThat(accessLogJpaRepository.count()).isZero();
        String expected = "위치정보 열람 이력 저장 실패 - 관리자 " + admin.getId() + " 회원 " + member.getId()
                + " MISSION_HISTORY_ADDRESS " + history.getId();
        List<String> failureLines = output.getAll().lines().filter(line -> line.contains(expected)).toList();
        assertThat(failureLines).hasSize(1);
        assertThat(failureLines.get(0)).contains("ERROR")
                .doesNotContain(COORDINATE_PARTS.toArray(String[]::new));
    }

    private MissionHistory saveHistory(MissionCategoryStatus category, List<String> addresses) {
        Mission mission = missionJpaRepository.save(Mission.builder()
                .challenge(challenge)
                .category(category)
                .title(category + " 미션")
                .reward(10)
                .content("설명")
                .build());
        return missionHistoryJpaRepository.save(MissionHistory.builder()
                .mission(mission)
                .user(member)
                .status(MissionStatus.COMPLETED)
                .addresses(addresses)
                .gpsVerifyCount(addresses.size())
                .abstinenceTime(category == MissionCategoryStatus.HOLD ? 600 : null)
                .build());
    }

    private String token(User user) throws Exception {
        return "Bearer " + jwtTokenGenerator.generate(LoginUserInfo.of(user.getId())).getAccessToken();
    }

    private User user(String email, Role role) {
        return User.builder()
                .email(email)
                .password("encoded-password")
                .nickName("닉네임")
                .phoneNumber("010-0000-0000")
                .sex(Sex.MALE)
                .role(role)
                .snsType(SnsType.NORMAL)
                .settingStatus(SettingStatus.COMPLETE)
                .build();
    }
}
