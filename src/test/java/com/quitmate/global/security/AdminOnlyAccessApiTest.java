package com.quitmate.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quitmate.global.jwt.JwtTokenGenerator;
import com.quitmate.global.jwt.dto.LoginUserInfo;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공개 경로를 뺀 모든 관리자 API가 DB role = ADMIN인 회원만 받는지, 실제 SecurityConfig·JWT 필터로 확인한다(KAN-206).
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AdminOnlyAccessApiTest {

    private static final String ADMIN_ONLY = "관리자 계정만 사용할 수 있습니다.";
    private static final String PASSWORD = "password-1234";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    private User admin;
    private User member;

    @BeforeEach
    void setUp() {
        admin = userRepository.save(user("admin@test.com", Role.ADMIN));
        member = userRepository.save(user("member@test.com", Role.USER));
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("일반 회원(role USER) 토큰은 사용자 목록·미션 이력 상세·공지 등 모든 관리자 API가 403이다")
    @Test
    void memberTokenIsForbidden() throws Exception {
        String token = token(member.getId());

        for (MockHttpServletRequestBuilder request : adminApis()) {
            expectAdminOnly(mockMvc.perform(request.header("Authorization", token)));
        }
    }

    @DisplayName("탈퇴(use_yn = 'N')한 관리자, DB에 없는 회원의 토큰도 403이다")
    @Test
    void withdrawnOrUnknownAccountIsForbidden() throws Exception {
        User withdrawn = userRepository.save(user("withdrawn@test.com", Role.ADMIN));
        jdbcTemplate.update("update users set use_yn = 'N' where id = ?", withdrawn.getId());
        Long unknownId = withdrawn.getId() + 10_000;

        for (Long userId : List.of(withdrawn.getId(), unknownId)) {
            expectAdminOnly(mockMvc.perform(usersList().header("Authorization", token(userId))));
            expectAdminOnly(mockMvc.perform(get("/api/v1/admin/mission-history/{id}", 1L)
                    .header("Authorization", token(userId))));
        }
    }

    @DisplayName("토큰을 받은 뒤 role이 USER로 바뀌면 다음 요청부터 403이다(요청마다 DB로 확인)")
    @Test
    void roleIsCheckedOnEveryRequest() throws Exception {
        String token = token(admin.getId());
        mockMvc.perform(usersList().header("Authorization", token))
                .andExpect(status().isOk());

        jdbcTemplate.update("update users set role = 'USER' where id = ?", admin.getId());

        expectAdminOnly(mockMvc.perform(usersList().header("Authorization", token)));
    }

    @DisplayName("관리자 토큰은 그대로 동작한다")
    @Test
    void adminTokenWorks() throws Exception {
        String token = token(admin.getId());

        mockMvc.perform(usersList().header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.pageInfo.totalElement").value(2));
        mockMvc.perform(get("/api/v1/notice").param("page", "1").param("size", "20")
                        .header("Authorization", token))
                .andExpect(status().isOk());
        // 보안 단계를 지나 서비스까지 간다: 없는 미션 이력은 기존대로 400
        mockMvc.perform(get("/api/v1/admin/mission-history/{id}", 999_999L).header("Authorization", token))
                .andExpect(status().isBadRequest());
    }

    @DisplayName("토큰이 없으면 지금처럼 401이다")
    @Test
    void noTokenIsUnauthorized() throws Exception {
        mockMvc.perform(usersList())
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/mission-history/{id}", 1L))
                .andExpect(status().isUnauthorized());
    }

    /**
     * JwtAuthenticationFilter는 servletPath로 공개 경로를 거른다. MockMvc는 servletPath가 기본 ""라,
     * 실제 서버(DispatcherServlet이 "/"에 매핑돼 servletPath = 요청 경로)와 같게 넣어 준다.
     */
    @DisplayName("공개 경로는 토큰 없이 그대로: 관리자 로그인 → 받은 토큰으로 사용자 목록, 일반 회원 로그인은 기존대로 400, 헬스체크 200")
    @Test
    void publicPathsWithoutToken() throws Exception {
        String loginResponse = mockMvc.perform(post("/api/v1/auth/login").servletPath("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("admin@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(loginResponse).path("data").path("accessToken").asText();

        mockMvc.perform(usersList().header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/login").servletPath("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("member@test.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("관리자만 로그인할 수 있습니다."));

        mockMvc.perform(get("/health-check").servletPath("/health-check"))
                .andExpect(status().isOk());
    }

    private List<MockHttpServletRequestBuilder> adminApis() {
        String json = MediaType.APPLICATION_JSON_VALUE;
        return List.of(
                usersList(),
                get("/api/v1/admin/mission-history").param("page", "1").param("size", "20"),
                get("/api/v1/admin/mission-history/{id}", 1L),
                patch("/api/v1/admin/mission-history/{id}", 1L).contentType(json).content("{}"),
                get("/api/v1/notice").param("page", "1").param("size", "20"),
                post("/api/v1/notice").contentType(json).content("{}"),
                delete("/api/v1/notice/{id}", 1L),
                get("/api/v1/faq"),
                get("/api/v1/inquiry-questions"),
                get("/api/v1/admin/challenge"),
                get("/api/v1/admin/user-passes"),
                get("/api/v1/admin/smoking-pattern/weeks"),
                get("/api/v1/admin/cigarette-history-batch/issues")
        );
    }

    private void expectAdminOnly(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.statusCode").value(403))
                .andExpect(jsonPath("$.httpStatus").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value(ADMIN_ONLY));
    }

    private MockHttpServletRequestBuilder usersList() {
        return get("/api/v1/users").param("page", "1").param("size", "20");
    }

    private String token(Long userId) throws Exception {
        return "Bearer " + jwtTokenGenerator.generate(LoginUserInfo.of(userId)).getAccessToken();
    }

    private String loginBody(String email) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("email", email, "password", PASSWORD));
    }

    private User user(String email, Role role) {
        return User.builder()
                .email(email)
                .password(bCryptPasswordEncoder.encode(PASSWORD))
                .nickName("닉네임")
                .phoneNumber("010-0000-0000")
                .sex(Sex.MALE)
                .role(role)
                .snsType(SnsType.NORMAL)
                .settingStatus(SettingStatus.COMPLETE)
                .build();
    }
}
