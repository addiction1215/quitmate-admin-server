package com.quitmate.challenge.challange.controller.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SearchCategory {
    TITLE("제목"),
    BADGE("뱃지"),
    REWARD("리워드 포인트");

    private final String description;
}
