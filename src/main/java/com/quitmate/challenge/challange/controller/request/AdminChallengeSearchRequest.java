package com.quitmate.challenge.challange.controller.request;

import com.addiction.challenge.challange.service.request.AdminChallengeSearchServiceRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
@NoArgsConstructor
public class AdminChallengeSearchRequest {
    
    private SearchCategory category;
    private String keyword;
    private SortType sortType;
    private Integer page = 0;
    private Integer size = 10;
    
    @Builder
    public AdminChallengeSearchRequest(SearchCategory category, String keyword, SortType sortType, Integer page, Integer size) {
        this.category = category;
        this.keyword = keyword;
        this.sortType = sortType != null ? sortType : SortType.ID_DESC;
        this.page = page != null ? page : 0;
        this.size = size != null ? size : 10;
    }
    
    public enum SearchCategory {
        TITLE("제목"),
        BADGE("뱃지"),
        REWARD("리워드 포인트");
        
        private final String description;
        
        SearchCategory(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum SortType {
        ID_ASC("챌린지 ID 오름차순", "id", Sort.Direction.ASC),
        ID_DESC("챌린지 ID 내림차순", "id", Sort.Direction.DESC),
        ACCUMULATED_COUNT_ASC("누적 유지 수 오름차순", "accumulatedCount", Sort.Direction.ASC),
        ACCUMULATED_COUNT_DESC("누적 유지 수 내림차순", "accumulatedCount", Sort.Direction.DESC);
        
        private final String description;
        private final String property;
        private final Sort.Direction direction;
        
        SortType(String description, String property, Sort.Direction direction) {
            this.description = description;
            this.property = property;
            this.direction = direction;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getProperty() {
            return property;
        }
        
        public Sort.Direction getDirection() {
            return direction;
        }
    }
    
    public AdminChallengeSearchServiceRequest toServiceRequest() {
        return AdminChallengeSearchServiceRequest.builder()
                .category(category)
                .keyword(keyword)
                .sortType(sortType)
                .pageable(createPageable())
                .build();
    }
    
    private Pageable createPageable() {
        if (sortType == null) {
            return PageRequest.of(page, size);
        }
        return PageRequest.of(page, size, Sort.by(sortType.getDirection(), sortType.getProperty()));
    }
}
