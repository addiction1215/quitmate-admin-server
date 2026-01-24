package com.quitmate.challenge.challange.controller.request;

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
