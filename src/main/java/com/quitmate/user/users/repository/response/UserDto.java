package com.quitmate.user.users.repository.response;

import com.quitmate.user.users.entity.enums.Sex;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserDto {
    private Long id;                    // 회원ID
    private LocalDateTime createdDate;  // 가입일자
    private LocalDateTime lastLoginAt;  // 최근 로그인
    private String email;               // 이메일
    private String nickName;            // 유저명
    private String birthDay;            // 생년월일
    private Sex sex;                    // 성별

    public UserDto(Long id, LocalDateTime createdDate, LocalDateTime lastLoginAt, String email, String nickName,
                   String birthDay, Sex sex) {
        this.id = id;
        this.createdDate = createdDate;
        this.lastLoginAt = lastLoginAt;
        this.email = email;
        this.nickName = nickName;
        this.birthDay = birthDay;
        this.sex = sex;
    }
}
