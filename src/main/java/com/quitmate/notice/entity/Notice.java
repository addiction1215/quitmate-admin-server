package com.quitmate.notice.entity;

import com.quitmate.global.BaseTimeEntity;
import com.quitmate.notice.enums.NoticePushStatus;
import com.quitmate.notice.enums.NoticeType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * 공지. 앱 서버도 같은 {@code notice} 테이블을 읽기만 한다(앱 서버 엔티티에는 푸시 칸이 없다).
 *
 * <p>{@code @DynamicUpdate}: 푸시 칸(push_*)은 등록 뒤에는 {@code NoticeJpaRepository}의 조건부 UPDATE 로만 바꾼다.
 * 관리자가 공지를 고치거나 지울 때 바뀐 칸만 UPDATE 해야, 그사이 스케줄러가 바꾼 푸시 상태(예: SENDING)를
 * 읽어 둔 옛 값(DEFERRED)으로 덮어 같은 푸시를 두 번 보내는 일이 없다(KAN-109).</p>
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@DynamicUpdate
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private NoticeType type;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String useYn;

    /** 푸시 발송 상태. 이 칸이 생기기 전에 등록한 공지는 null */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "push_status", length = 20)
    private NoticePushStatus pushStatus;

    /** 보류한 푸시를 보낼 시각(한국 시각). 야간에 등록한 이벤트 공지에만 채운다 */
    @Column(name = "push_scheduled_at")
    private LocalDateTime pushScheduledAt;

    /** 푸시를 보내기 시작한 시각(한국 시각). SENT·SENDING·FAILED 에 채운다 */
    @Column(name = "push_sent_at")
    private LocalDateTime pushSentAt;

    @Builder
    public Notice(Long id, NoticeType type, String title, String content) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.content = content;
        this.useYn = "Y";
    }

    public void update(NoticeType type, String title, String content) {
        this.type = type;
        this.title = title;
        this.content = content;
    }

    public void delete() {
        this.useYn = "N";
    }

    /** 등록하며 푸시를 바로 보낸다. 저장 전에만 부른다 */
    public void markPushSent(LocalDateTime sentAt) {
        requireNotSaved();
        this.pushStatus = NoticePushStatus.SENT;
        this.pushSentAt = sentAt;
    }

    /** 푸시를 {@code scheduledAt} 까지 보류한다. 저장 전에만 부른다 */
    public void deferPush(LocalDateTime scheduledAt) {
        requireNotSaved();
        this.pushStatus = NoticePushStatus.DEFERRED;
        this.pushScheduledAt = scheduledAt;
    }

    private void requireNotSaved() {
        if (this.id != null) {
            throw new IllegalStateException("저장한 공지의 푸시 상태는 조건부 UPDATE 로만 바꿉니다. noticeId: " + this.id);
        }
    }
}
