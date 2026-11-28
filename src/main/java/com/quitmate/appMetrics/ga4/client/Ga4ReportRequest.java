package com.quitmate.appMetrics.ga4.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GA4 Data API {@code runReport} 한 번에 보낼 내용(KAN-218).
 *
 * <p>날짜는 GA4 속성의 보고 시간대 기준으로 풀리도록 상대 날짜({@code 28daysAgo}, {@code yesterday})로 보낸다.
 *
 * @param name          로그에서 어느 보고서인지 알아보는 이름(GA4로 보내지 않음)
 * @param startDate     시작 날짜. {@code NdaysAgo}·{@code yesterday}·{@code YYYY-MM-DD}
 * @param endDate       끝 날짜
 * @param dimensions    차원 API 이름(예: {@code date}, {@code platform}). 없으면 기간 전체 합계 한 줄
 * @param metrics       지표 API 이름(예: {@code activeUsers}). 응답의 지표 값도 이 순서로 온다
 * @param orderByMetric 이 지표로 내림차순 정렬. 없으면 null
 * @param limit         최대 행 수. 없으면 null(GA4 기본값)
 * @param inListFilter  이 차원 값이 목록 중 하나인 행만 센다(KAN-230, 예: platform이 Android·iOS). 없으면 null
 */
public record Ga4ReportRequest(String name, String startDate, String endDate, List<String> dimensions,
                               List<String> metrics, String orderByMetric, Integer limit, InListFilter inListFilter) {

    public Ga4ReportRequest {
        dimensions = List.copyOf(dimensions);
        metrics = List.copyOf(metrics);
    }

    public Ga4ReportRequest(String name, String startDate, String endDate, List<String> dimensions,
                            List<String> metrics, String orderByMetric, Integer limit) {
        this(name, startDate, endDate, dimensions, metrics, orderByMetric, limit, null);
    }

    /**
     * GA4 {@code dimensionFilter.filter.inListFilter}. 대소문자는 가리지 않는다(GA4 기본값).
     *
     * @param fieldName 차원 API 이름(요청 차원에 없어도 된다)
     * @param values    허용할 값
     */
    public record InListFilter(String fieldName, List<String> values) {

        public InListFilter {
            values = List.copyOf(values);
        }
    }

    /** runReport 요청 본문(JSON) */
    public Map<String, Object> toBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dateRanges", List.of(Map.of("startDate", startDate, "endDate", endDate)));
        if (!dimensions.isEmpty()) {
            body.put("dimensions", dimensions.stream().map(name -> Map.of("name", name)).toList());
        }
        body.put("metrics", metrics.stream().map(name -> Map.of("name", name)).toList());
        if (orderByMetric != null) {
            body.put("orderBys", List.of(Map.of("metric", Map.of("metricName", orderByMetric), "desc", true)));
        }
        if (limit != null) {
            body.put("limit", limit);
        }
        if (inListFilter != null) {
            body.put("dimensionFilter", Map.of("filter", Map.of(
                    "fieldName", inListFilter.fieldName(),
                    "inListFilter", Map.of("values", inListFilter.values()))));
        }
        return body;
    }
}
