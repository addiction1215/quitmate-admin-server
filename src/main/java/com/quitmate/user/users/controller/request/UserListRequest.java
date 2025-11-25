package com.quitmate.user.users.controller.request;

import com.quitmate.global.page.request.PageInfoRequest;
import com.quitmate.user.users.entity.enums.UserSearchCategory;
import com.quitmate.user.users.entity.enums.UserSortType;
import com.quitmate.user.users.service.request.UserListServiceRequest;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.glassfish.jersey.Beta;

@Getter
public class UserListRequest extends PageInfoRequest {

    private final UserSortType sortBy;          // 정렬 기준: CREATED_DATE 또는 NICK_NAME (선택)
    private final UserSearchCategory category;  // 검색 카테고리: CREATED_DATE, EMAIL, NICK_NAME (선택)
    private final String keyword;               // 검색어 (선택)

    public UserListRequest(UserSearchCategory category, UserSortType sortBy, String keyword) {
        this.category = category;
        this.sortBy = sortBy;
        this.keyword = keyword;
    }

    public UserListServiceRequest toServiceRequest() {
        return UserListServiceRequest.builder()
                .sortBy(sortBy)
                .category(category)
                .keyword(keyword)
                .page(getPage())
                .size(getSize())
                .build();
    }
}
