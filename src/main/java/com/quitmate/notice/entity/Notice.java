package com.quitmate.notice.entity;

import com.quitmate.global.BaseTimeEntity;
import com.quitmate.notice.enums.NoticeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NoticeType type;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String useYn;

    @Builder
    public Notice(Long id, NoticeType type, String content) {
        this.id = id;
        this.type = type;
        this.content = content;
        this.useYn = "Y";
    }

    public void update(NoticeType type, String content) {
        this.type = type;
        this.content = content;
    }
}
