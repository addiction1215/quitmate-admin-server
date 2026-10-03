package com.quitmate.user.users.service.response;

import com.quitmate.user.users.entity.enums.Sex;
import com.quitmate.user.users.repository.response.UserDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserListResponseTest {

    @Test
    void 최근_로그인_시각을_가입일과_별도로_반환한다() {
        LocalDateTime createdDate = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime lastLoginAt = LocalDateTime.of(2024, 2, 1, 12, 30);
        UserDto dto = new UserDto(1L, createdDate, lastLoginAt, "user@example.com", "사용자", null, Sex.MALE);

        UserListResponse response = UserListResponse.createResponse(dto);

        assertThat(response.getCreatedDate()).isEqualTo(createdDate);
        assertThat(response.getLastLoginAt()).isEqualTo(lastLoginAt);
    }

    @Test
    void 로그인_기록이_없으면_최근_로그인은_null이다() {
        UserDto dto = new UserDto(1L, LocalDateTime.of(2024, 1, 1, 10, 0), null,
                "user@example.com", "사용자", null, Sex.MALE);

        assertThat(UserListResponse.createResponse(dto).getLastLoginAt()).isNull();
    }
}
