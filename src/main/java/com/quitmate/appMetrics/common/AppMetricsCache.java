package com.quitmate.appMetrics.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 기간별 조회 결과를 서버 메모리에 잠깐 들고 있는 캐시(앱 지표, KAN-217).
 *
 * <ul>
 *   <li>담은 뒤 {@code ttl}이 지나면 다음 요청이 다시 불러온다.</li>
 *   <li>불러오다 예외가 나면 담지 않는다. 예외는 그대로 던지고, 다음 요청이 다시 불러온다.</li>
 *   <li>비어 있는 기간을 동시에 부르면 둘 다 불러올 수 있다(나중 값이 남는다). 관리자 화면 조회라 막지 않는다.</li>
 *   <li>서버를 여러 대 띄우면 서버마다 따로 들고 있다.</li>
 * </ul>
 *
 * <p>GA4(KAN-218)·푸시(KAN-221) 두 PR에 같은 내용으로 들어 있다. 어느 PR이 먼저 머지돼도 충돌하지 않도록
 * 이 파일은 두 브랜치에서 똑같이 둔다.
 */
public class AppMetricsCache<T> {

    private final Duration ttl;
    private final Clock clock;
    private final Map<AppMetricsPeriod, Entry<T>> entries = new ConcurrentHashMap<>();

    public AppMetricsCache(Duration ttl, Clock clock) {
        this.ttl = ttl;
        this.clock = clock;
    }

    /**
     * 담긴 값이 아직 살아 있으면 그 값을, 없거나 지났으면 {@code loader}로 새로 불러와 담고 돌려준다.
     */
    public T get(AppMetricsPeriod period, Supplier<T> loader) {
        Entry<T> cached = entries.get(period);
        if (cached != null && clock.instant().isBefore(cached.expiresAt())) {
            return cached.value();
        }
        T loaded = loader.get();
        entries.put(period, new Entry<>(loaded, clock.instant().plus(ttl)));
        return loaded;
    }

    private record Entry<T>(T value, Instant expiresAt) {
    }
}
