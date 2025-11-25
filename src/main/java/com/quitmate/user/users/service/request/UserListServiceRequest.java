package com.quitmate.user.users.service.request;

import com.quitmate.global.page.request.PageInfoServiceRequest;
import com.quitmate.user.users.entity.enums.UserSearchCategory;
import com.quitmate.user.users.entity.enums.UserSortType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@NoArgsConstructor
@SuperBuilder
public class UserListServiceRequest extends PageInfoServiceRequest {

    private UserSortType sortBy;
    private UserSearchCategory category;
    private String keyword;

    @Override
    public Pageable toPageable() {
        Sort sort = getSort();
        return PageRequest.of(getPage() - 1, getSize(), sort);
    }

    private Sort getSort() {
        UserSortType sortType = sortBy != null ? sortBy : UserSortType.CREATED_DATE;
        
        if (sortType == UserSortType.NICK_NAME) {
            return Sort.by(Sort.Direction.ASC, "nickName");
        }
        // 기본값: 가입일자 내림차순
        return Sort.by(Sort.Direction.DESC, "createdDate");
    }
}
