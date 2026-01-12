package com.quitmate.user.users.service.response;

import com.quitmate.user.users.repository.response.UserDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class UserListResponse {

    private Long id;                    // 회원ID
    private LocalDateTime createdDate;  // 가입일자
    private String email;               // 이메일
    private String nickName;            // 유저명
    private String birthDay;            // 생년월일
    private String sex;                    // 성별

    @Builder
    private UserListResponse(Long id, LocalDateTime createdDate, String email, String nickName,
                             String birthDay, String sex) {
        this.id = id;
        this.createdDate = createdDate;
        this.email = email;
        this.nickName = nickName;
        this.birthDay = birthDay;
        this.sex = sex;
    }

    public static UserListResponse createResponse(UserDto dto) {
        return UserListResponse.builder()
                .id(dto.getId())
                .createdDate(dto.getCreatedDate())
                .email(dto.getEmail())
                .nickName(dto.getNickName())
                .birthDay(dto.getBirthDay())
                .sex(dto.getSex().getDescription())
                .build();
    }
}
