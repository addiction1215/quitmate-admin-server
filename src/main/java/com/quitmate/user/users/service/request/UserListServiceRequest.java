package com.quitmate.user.users.service.request;

import com.quitmate.global.page.request.PageInfoServiceRequest;
import com.quitmate.user.users.entity.enums.UserSearchCategory;
import com.quitmate.user.users.entity.enums.UserSortType;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
public class UserListServiceRequest extends PageInfoServiceRequest {

    private final UserSortType sortBy;
    private final UserSearchCategory category;
    private final String keyword;

    @Builder
    public UserListServiceRequest(int page, int size, UserSearchCategory category, String keyword, UserSortType sortBy) {
        super(page, size);
        this.category = category;
        this.keyword = keyword;
        this.sortBy = sortBy;
    }
}
