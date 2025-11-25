package com.quitmate.inquiry.inquryQuestion.entity;

import com.quitmate.global.BaseTimeEntity;
import com.quitmate.inquiry.inquryQuestion.enums.InquiryStatus;
import com.quitmate.user.users.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private String title;

    private String question;

    @Enumerated(EnumType.STRING)
    private InquiryStatus inquiryStatus;

    @Builder
    public InquiryQuestion(Long id, String question, String title, User user, InquiryStatus inquiryStatus) {
        this.id = id;
        this.question = question;
        this.title = title;
        this.user = user;
        this.inquiryStatus = inquiryStatus;
    }

    public void updateStatus(InquiryStatus inquiryStatus) {
        this.inquiryStatus = inquiryStatus;
    }

}