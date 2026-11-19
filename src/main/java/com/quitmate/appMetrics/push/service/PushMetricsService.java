package com.quitmate.appMetrics.push.service;

import com.quitmate.appMetrics.common.AppMetricsCache;
import com.quitmate.appMetrics.common.AppMetricsPeriod;
import com.quitmate.appMetrics.push.repository.PushMetricsRepository;
import com.quitmate.appMetrics.push.repository.response.DeliveryDailyCount;
import com.quitmate.appMetrics.push.repository.response.OutboxDailyCount;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse.Daily;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse.ErrorCode;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse.Summary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

/**
 * 「앱 지표」 푸시 섹션(KAN-221). 앱 서버 표(push_outbox·push_delivery·push)를 읽기만 한다.
 *
 * <ul>
 *   <li>기간: 끝 날짜는 오늘(한국 날짜), 시작은 끝에서 period-1일 전. 각 행의 created_date 한국 날짜로 나눈다.</li>
 *   <li>발송 요청(requested) = 기간에 생긴 push_outbox 행(사람 단위). push_delivery(기기 단위)는 단위가 달라 더하지 않는다.</li>
 *   <li>전달 추적(deliveryTracking): 기간에 push_delivery 행이 없으면(KAN-219·220 배포 전) ticket·receipt 숫자는 null.
 *       ticket·receipt·오류 코드는 push_delivery 기준(기기 단위, 친구 푸시 포함)이다.</li>
 *   <li>열림 추적(openTracking): 기간 하루 흡연 시간대 푸시(schedule_id 있음) 중 열린 것이 없으면 opened·openRate는 null.
 *       열림률 분모는 그중 보낸 것(status = SENT) 또는 열린 것이다. 실패·취소·만료·대기 건은 사용자에게 가지 않았으므로 뺀다.</li>
 *   <li>결과는 기간별로 1시간 캐시한다.</li>
 * </ul>
 * 숫자 정의(SQL 조건)는 {@code PushMetricsSql} 한 곳에 있다.
 */
@Service
@Transactional(readOnly = true)
public class PushMetricsService {

    static final Duration CACHE_TTL = Duration.ofHours(1);
    static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int OPEN_RATE_SCALE = 4;

    private final PushMetricsRepository repository;
    private final Clock clock;
    private final AppMetricsCache<PushMetricsResponse> cache;

    @Autowired
    public PushMetricsService(PushMetricsRepository repository) {
        this(repository, Clock.system(KST));
    }

    PushMetricsService(PushMetricsRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
        this.cache = new AppMetricsCache<>(CACHE_TTL, clock);
    }

    /**
     * @param periodValue 쿼리 period 그대로. 7·28·90이 아니면 400
     */
    public PushMetricsResponse getMetrics(String periodValue) {
        AppMetricsPeriod period = AppMetricsPeriod.from(periodValue);
        return cache.get(period, () -> load(period));
    }

    private PushMetricsResponse load(AppMetricsPeriod period) {
        OffsetDateTime fetchedAt = OffsetDateTime.now(clock).truncatedTo(ChronoUnit.SECONDS);
        LocalDate endDate = fetchedAt.atZoneSameInstant(KST).toLocalDate();
        LocalDate startDate = period.startDate(endDate);
        LocalDateTime from = startDate.atStartOfDay();
        LocalDateTime to = endDate.plusDays(1).atStartOfDay();

        Map<LocalDate, OutboxDailyCount> outbox =
                byDate(repository.findOutboxDailyCounts(from, to), OutboxDailyCount::date);
        Map<LocalDate, DeliveryDailyCount> delivery =
                byDate(repository.findDeliveryDailyCounts(from, to), DeliveryDailyCount::date);

        boolean deliveryTracking = delivery.values().stream().anyMatch(day -> day.total() > 0);
        long openedTotal = outbox.values().stream().mapToLong(OutboxDailyCount::dailySmokingOpened).sum();
        boolean openTracking = openedTotal > 0;

        List<Daily> daily = new ArrayList<>();
        long requestedTotal = 0;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            OutboxDailyCount outboxDay = outbox.get(date);
            DeliveryDailyCount deliveryDay = delivery.get(date);
            long requested = outboxDay == null ? 0 : outboxDay.requested();
            requestedTotal += requested;
            daily.add(new Daily(
                    date,
                    requested,
                    deliveryTracking ? (deliveryDay == null ? 0L : deliveryDay.ticketError()) : null,
                    deliveryTracking ? (deliveryDay == null ? 0L : deliveryDay.receiptError()) : null,
                    openTracking ? (outboxDay == null ? 0L : outboxDay.dailySmokingOpened()) : null));
        }

        Summary summary = new Summary(
                requestedTotal,
                deliveryTracking ? sum(delivery, DeliveryDailyCount::ticketOk) : null,
                deliveryTracking ? sum(delivery, DeliveryDailyCount::ticketError) : null,
                deliveryTracking ? sum(delivery, DeliveryDailyCount::receiptOk) : null,
                deliveryTracking ? sum(delivery, DeliveryDailyCount::receiptError) : null,
                deliveryTracking ? sum(delivery, DeliveryDailyCount::receiptPending) : null,
                openTracking ? openedTotal : null,
                openTracking ? openRate(openedTotal, sum(outbox, OutboxDailyCount::dailySmokingSent)) : null,
                repository.countTokenUsers());

        List<ErrorCode> errorCodes = deliveryTracking
                ? repository.findErrorCodeCounts(from, to).stream()
                .map(row -> new ErrorCode(row.code(), row.count()))
                .toList()
                : List.of();

        return new PushMetricsResponse(period.days(), startDate, endDate, fetchedAt,
                deliveryTracking, openTracking, summary, daily, errorCodes);
    }

    /** 분모(보낸 것 또는 열린 것)는 열린 것을 늘 포함하므로 openTracking이면 0이 아니다. */
    private static Double openRate(long opened, long sent) {
        if (sent <= 0) {
            return null;
        }
        return BigDecimal.valueOf(opened)
                .divide(BigDecimal.valueOf(sent), OPEN_RATE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static <T> Map<LocalDate, T> byDate(List<T> rows, Function<T, LocalDate> date) {
        return rows.stream().collect(Collectors.toMap(date, Function.identity(), (first, second) -> first));
    }

    private static <T> long sum(Map<LocalDate, T> rows, ToLongFunction<T> value) {
        return rows.values().stream().mapToLong(value).sum();
    }
}
