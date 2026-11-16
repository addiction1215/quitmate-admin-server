package com.quitmate.challenge.metrics.service.response;

/**
 * 비율 하나. 관리자 화면이 "분모 중 분자"와 퍼센트를 같이 보여 줄 수 있게 셋을 함께 준다.
 *
 * @param numerator   분자
 * @param denominator 분모. 0이면 percent는 null
 * @param percent     numerator ÷ denominator × 100을 반올림한 정수. 분모가 0이면 null
 */
public record MetricRatio(long numerator, long denominator, Integer percent) {

    public static MetricRatio of(long numerator, long denominator) {
        return new MetricRatio(numerator, denominator,
                denominator == 0 ? null : (int) Math.round(numerator * 100.0 / denominator));
    }
}
