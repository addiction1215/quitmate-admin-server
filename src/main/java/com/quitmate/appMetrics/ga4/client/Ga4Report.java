package com.quitmate.appMetrics.ga4.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * GA4 {@code runReport} 응답에서 쓰는 부분만 옮긴 것(KAN-218).
 *
 * @param timeZone GA4 속성의 보고 시간대(응답 metadata.timeZone). 응답에 없으면 null
 * @param rows     행. 차원 값은 요청한 차원 순서, 지표 값은 요청한 지표 순서
 */
public record Ga4Report(String timeZone, List<Row> rows) {

    public Ga4Report {
        rows = List.copyOf(rows);
    }

    public record Row(List<String> dimensions, List<Long> metrics) {

        public Row {
            dimensions = List.copyOf(dimensions);
            metrics = List.copyOf(metrics);
        }

        public String dimension(int index) {
            return dimensions.get(index);
        }

        public long metric(int index) {
            return metrics.get(index);
        }
    }

    /**
     * 응답 JSON을 읽는다. 행이 없으면(데이터 없음) 빈 목록이다.
     *
     * @throws Ga4UnavailableException 차원·지표 개수가 요청과 다르거나 지표 값이 숫자가 아니면(응답 이상)
     */
    static Ga4Report parse(JsonNode body, Ga4ReportRequest request) {
        if (body == null || !body.isObject()) {
            throw new Ga4UnavailableException("GA4 응답이 비어 있습니다(" + request.name() + ").");
        }
        JsonNode timeZone = body.path("metadata").path("timeZone");
        List<Row> rows = new ArrayList<>();
        for (JsonNode row : body.path("rows")) {
            rows.add(parseRow(row, request));
        }
        return new Ga4Report(timeZone.isTextual() ? timeZone.asText() : null, rows);
    }

    private static Row parseRow(JsonNode row, Ga4ReportRequest request) {
        JsonNode dimensionValues = row.path("dimensionValues");
        JsonNode metricValues = row.path("metricValues");
        int dimensionCount = dimensionValues.isArray() ? dimensionValues.size() : 0;
        int metricCount = metricValues.isArray() ? metricValues.size() : 0;
        if (dimensionCount != request.dimensions().size() || metricCount != request.metrics().size()) {
            throw new Ga4UnavailableException("GA4 응답의 차원·지표 개수가 요청과 다릅니다(" + request.name() + ").");
        }
        List<String> dimensions = new ArrayList<>();
        dimensionValues.forEach(value -> dimensions.add(value.path("value").asText("")));
        List<Long> metrics = new ArrayList<>();
        metricValues.forEach(value -> metrics.add(parseMetric(value.path("value").asText(""), request)));
        return new Row(dimensions, metrics);
    }

    /** GA4는 지표 값을 문자열로 준다(예: "118"). 정수가 아닌 값이 오면 반올림한다. */
    private static long parseMetric(String value, Ga4ReportRequest request) {
        try {
            return new BigDecimal(value.trim()).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw new Ga4UnavailableException("GA4 응답의 지표 값이 숫자가 아닙니다(" + request.name() + ").", e);
        }
    }
}
