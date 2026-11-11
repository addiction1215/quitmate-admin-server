package com.quitmate.user.userPass.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quitmate.global.jwt.JwtTokenGenerator;
import com.quitmate.global.jwt.dto.LoginUserInfo;
import com.quitmate.user.userPass.repository.UserPassJpaRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 SecurityConfig·JWT 필터로 이용권 관리자 API를 끝까지 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class UserPassApiTest {

    private static final String BASE = "/api/v1/admin/user-passes";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserPassJpaRepository userPassJpaRepository;

    private User admin;
    private User member;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        admin = userRepository.save(user("admin@test.com", Role.ADMIN));
        member = userRepository.save(user("member@test.com", Role.USER));
    }

    @AfterEach
    void tearDown() {
        userPassJpaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @DisplayName("토큰이 없으면 401, 관리자가 아니면 403")
    @Test
    void auth() throws Exception {
        mockMvc.perform(get(BASE))
                .andExpect(status().isUnauthorized());

        String token = token(member);
        mockMvc.perform(get(BASE).header("Authorization", token))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(grantBody(member, today, today.plusDays(90))))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch(BASE + "/{id}/revoke", 1L).header("Authorization", token))
                .andExpect(status().isForbidden());
        assertThat(userPassJpaRepository.count()).isZero();
    }

    @DisplayName("부여 → 목록 → 중복 부여 409 → 회수(본문 없이도 됨) → 다시 회수 409")
    @Test
    void grantListRevoke() throws Exception {
        String token = token(admin);

        // 부여
        String created = mockMvc.perform(post(BASE).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody(member, today, today.plusDays(90))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(member.getId()))
                .andExpect(jsonPath("$.data.product").value("QUITMATE_90"))
                .andExpect(jsonPath("$.data.source").value("ADMIN_GRANT"))
                .andExpect(jsonPath("$.data.startsAt").value(today + "T00:00:00"))
                .andExpect(jsonPath("$.data.endsAt").value(today.plusDays(90) + "T00:00:00"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();
        long passId = objectMapper.readTree(created).path("data").path("id").asLong();

        // 목록(회원·상태로 거르기)
        mockMvc.perform(get(BASE).param("userId", String.valueOf(member.getId())).param("status", "ACTIVE")
                        .param("page", "1").param("size", "20")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(passId))
                .andExpect(jsonPath("$.data.content[0].email").value("member@test.com"))
                .andExpect(jsonPath("$.data.pageInfo.totalElement").value(1));

        // 중복 부여
        mockMvc.perform(post(BASE).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(grantBody(member, today.plusDays(30), today.plusDays(120))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("이용권 ID " + passId)));

        // 회수(본문 없음)
        mockMvc.perform(patch(BASE + "/{id}/revoke", passId).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVOKED"));

        // 다시 회수
        mockMvc.perform(patch(BASE + "/{id}/revoke", passId).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"다시\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 회수된 이용권입니다."));
    }

    @DisplayName("필수값이 없거나 날짜 형식·상태 값이 틀리면 400")
    @Test
    void badRequest() throws Exception {
        String token = token(admin);

        mockMvc.perform(post(BASE).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + member.getId() + ",\"endsAt\":\"" + today.plusDays(90) + "T00:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("시작 시각은 필수입니다."));
        mockMvc.perform(post(BASE).header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":" + member.getId() + ",\"startsAt\":\"2026/11/12\",\"endsAt\":\"2027-02-10T00:00:00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("요청 형식이 올바르지 않습니다")));
        mockMvc.perform(get(BASE).param("status", "UNKNOWN").header("Authorization", token))
                .andExpect(status().isBadRequest());
        assertThat(userPassJpaRepository.count()).isZero();
    }

    private String grantBody(User user, LocalDate startDate, LocalDate endDate) throws Exception {
        JsonNode body = objectMapper.createObjectNode()
                .put("userId", user.getId())
                .put("startsAt", startDate + "T00:00:00")
                .put("endsAt", endDate + "T00:00:00")
                .put("memo", "베타 테스터");
        return objectMapper.writeValueAsString(body);
    }

    private String token(User account) throws Exception {
        return "Bearer " + jwtTokenGenerator.generate(LoginUserInfo.of(account.getId())).getAccessToken();
    }

    private static User user(String email, Role role) {
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
