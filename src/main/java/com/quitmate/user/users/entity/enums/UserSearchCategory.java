package com.quitmate.user.users.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSearchCategory {
    CREATED_DATE("createdDate", "가입일자"),
    EMAIL("email", "이메일"),
    NICK_NAME("nickName", "유저명");

    private final String value;
    private final String description;

    public static UserSearchCategory fromValue(String value) {
        if (value == null) {
            return null;
        }
        
        for (UserSearchCategory category : UserSearchCategory.values()) {
            if (category.value.equalsIgnoreCase(value)) {
                return category;
            }
        }
        return null;
    }
}
