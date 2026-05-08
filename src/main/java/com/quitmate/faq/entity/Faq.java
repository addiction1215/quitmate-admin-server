package com.quitmate.faq.entity;

import com.quitmate.faq.enums.FaqCategory;
import com.quitmate.global.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Faq extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private FaqCategory category;

    @Column(name = "is_pinned")
    private Boolean pinned;

    private Integer sortOrder;

    private String useYn;

    @Builder
    public Faq(Long id, String title, String description, FaqCategory category, Boolean pinned, Integer sortOrder) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.pinned = pinned;
        this.sortOrder = sortOrder;
        this.useYn = "Y";
    }

    public void update(FaqCategory category, Boolean pinned, Integer sortOrder, String title, String description) {
        this.category = category;
        this.pinned = pinned;
        this.sortOrder = sortOrder;
        this.title = title;
        this.description = description;
    }

    public void delete() {
        this.useYn = "N";
    }
}
