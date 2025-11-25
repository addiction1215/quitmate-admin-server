package com.quitmate.user.users.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSortType {
    CREATED_DATE("createdDate", "가입일자"),
    NICK_NAME("nickName", "유저명");

    private final String value;
    private final String description;

    public static UserSortType fromValue(String value) {
        if (value == null) {
            return CREATED_DATE;  // 기본값
        }
        
        for (UserSortType type : UserSortType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return CREATED_DATE;  // 기본값
    }
}
