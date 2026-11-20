package com.quitmate.challenge.challange.controller.request;

/**
 * 참기(HOLD) 미션 최소 참기 시간(초)의 입력 범위(KAN-194). 비우면 앱 서버 기본값(지금 600초)을 쓴다.
 * 앱 서버는 1초 이상이면 받지만, 관리자 실수(분을 초로 착각 등)를 막으려고 1분~30일로 좁힌다.
 * 홀수 챌린지의 1·3·5·7·9일 참기 미션은 최소 시간을 일수에 맞춰 넣으므로 하루를 넘게 받는다(KAN-190 기획 답 10617).
 */
public final class MinHoldSecondsRule {

    public static final long MIN = 60;
    public static final long MAX = 2_592_000;
    public static final String RANGE_MESSAGE = "최소 참기 시간은 60초(1분)부터 2592000초(30일)까지 넣을 수 있습니다.";

    private MinHoldSecondsRule() {
    }
}
