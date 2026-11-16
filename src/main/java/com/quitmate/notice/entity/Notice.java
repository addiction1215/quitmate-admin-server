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

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 서식 본문(정리된 HTML). 이미지는 src 없이 data-key(S3 파일 키)만 둔다. 서식을 쓰지 않은 공지는 null.
     * content 는 이 본문의 글자 버전으로, 푸시·검색·옛 앱이 쓴다.
     */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String contentHtml;

    private String useYn;

    @Builder
    public Notice(Long id, NoticeType type, String title, String content, String contentHtml) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.contentHtml = contentHtml;
        this.useYn = "Y";
    }

    public void update(NoticeType type, String title, String content, String contentHtml) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.contentHtml = contentHtml;
    }

    public void delete() {
        this.useYn = "N";
    }
}
