package com.quitmate.inquiry.inquryQuestion.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum InquiryStatus {

    WAITING("대기"),
    DONE("완료");

    private final String description;

}
