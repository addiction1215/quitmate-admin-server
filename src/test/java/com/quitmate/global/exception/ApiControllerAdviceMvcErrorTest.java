package com.quitmate.global.exception;

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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 스프링 MVC의 "예상된" 요청 오류가 500이 아니라 알맞은 4xx로 나가는지 실제 보안 필터와 함께 확인한다.
 */
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Import(ApiControllerAdviceMvcErrorTest.ProbeController.class)
class ApiControllerAdviceMvcErrorTest {

    @RestController
    static class ProbeController {
        @GetMapping("/api/v1/mvc-probe/params")
        String params(@RequestParam("must") String must, @RequestParam(value = "num", required = false) Integer num) {
            return "ok";
        }

        @PostMapping(value = "/api/v1/mvc-probe/json", consumes = MediaType.APPLICATION_JSON_VALUE)
        String json(@RequestBody String body) {
            return "ok";
        }

        @GetMapping("/api/v1/mvc-probe/boom")
        String boom() {
            throw new IllegalStateException("예상 못 한 오류");
        }
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtTokenGenerator jwtTokenGenerator;
    @Autowired
    private UserRepository userRepository;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        User admin = userRepository.save(User.builder()
                .email("admin@test.com")
                .password("encoded")
                .nickName("닉네임")
                .phoneNumber("010-0000-0000")
                .sex(Sex.MALE)
                .role(Role.ADMIN)
                .snsType(SnsType.NORMAL)
                .settingStatus(SettingStatus.COMPLETE)
                .build());
        token = "Bearer " + jwtTokenGenerator.generate(LoginUserInfo.of(admin.getId())).getAccessToken();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAllInBatch();
    }

    @DisplayName("아직 없는 주소(앱 지표 크래시)는 500이 아니라 404")
    @Test
    void noResource() throws Exception {
        mockMvc.perform(get("/api/v1/app-metrics/crash").header("Authorization", token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("요청한 주소를 찾을 수 없습니다."));
    }

    @DisplayName("지원하지 않는 요청 방식은 405")
    @Test
    void methodNotSupported() throws Exception {
        mockMvc.perform(post("/api/v1/mvc-probe/params").header("Authorization", token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.statusCode").value(405))
                .andExpect(jsonPath("$.message").value("지원하지 않는 요청 방식입니다."));
    }

    @DisplayName("필수 파라미터가 없으면 400이고 파라미터 이름만 알려 준다")
    @Test
    void missingParameter() throws Exception {
        mockMvc.perform(get("/api/v1/mvc-probe/params").header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("필수 값이 없습니다: must"));
    }

    @DisplayName("파라미터 형식이 틀리면 400이고 값은 응답에 넣지 않는다")
    @Test
    void typeMismatch() throws Exception {
        mockMvc.perform(get("/api/v1/mvc-probe/params").param("must", "a").param("num", "SECRET-abc")
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("값의 형식이 올바르지 않습니다: num"));
    }

    @DisplayName("지원하지 않는 본문 형식은 415")
    @Test
    void mediaTypeNotSupported() throws Exception {
        mockMvc.perform(post("/api/v1/mvc-probe/json").contentType(MediaType.TEXT_PLAIN).content("x")
                        .header("Authorization", token))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.statusCode").value(415))
                .andExpect(jsonPath("$.message").value("지원하지 않는 요청 형식입니다."));
    }

    @DisplayName("진짜 예상 못 한 예외는 여전히 500")
    @Test
    void unexpectedStill500() throws Exception {
        mockMvc.perform(get("/api/v1/mvc-probe/boom").header("Authorization", token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("서버 로직 에러"));
    }

    @DisplayName("토큰 없이 없는 주소를 불러도 401은 보안 필터 몫이라 그대로")
    @Test
    void noTokenStays401() throws Exception {
        mockMvc.perform(get("/api/v1/app-metrics/crash"))
                .andExpect(status().isUnauthorized());
    }
}
