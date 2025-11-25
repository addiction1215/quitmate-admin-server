package com.quitmate.inquiry.inquiryAnswer.entity;

import com.quitmate.global.BaseTimeEntity;
import com.quitmate.inquiry.inquryQuestion.entity.InquiryQuestion;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InquiryAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inquiry_question_id")
    private InquiryQuestion inquiryQuestion;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder
    public InquiryAnswer(Long id, InquiryQuestion inquiryQuestion, String content) {
        this.id = id;
        this.inquiryQuestion = inquiryQuestion;
        this.content = content;
    }
}
