package com.quitmate.challenge.metrics.service;

import com.quitmate.challenge.challange.entity.Challenge;
import com.quitmate.challenge.challange.service.ChallengeReadService;
import com.quitmate.challenge.metrics.repository.ChallengeMetricsQueryRepository;
import com.quitmate.challenge.metrics.repository.response.ChallengeTitle;
import com.quitmate.challenge.metrics.repository.response.MissionStatusCount;
import com.quitmate.challenge.metrics.repository.response.ParticipationRow;
import com.quitmate.challenge.metrics.service.request.ChallengeMetricsServiceRequest;
import com.quitmate.challenge.metrics.service.response.ChallengeMetrics;
import com.quitmate.challenge.metrics.service.response.ChallengeMetricsDetailResponse;
import com.quitmate.challenge.metrics.service.response.ChallengeMetricsListResponse;
import com.quitmate.challenge.metrics.service.response.MetricRatio;
import com.quitmate.challenge.metrics.service.response.MissionMetrics;
import com.quitmate.challenge.mission.entity.Mission;
import com.quitmate.challenge.mission.repository.MissionRepository;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.security.AdminAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 어드민 챌린지 지표(KAN-194). 앱 서버가 쓰는 참여·미션 기록을 읽기만 한다. 관리자 계정만 쓸 수 있다.
 *
 * <p>대상은 기간 안에 시작한 참여이고 상태는 지금 상태다. 비율 정의는 {@link ChallengeMetrics}·{@link MissionMetrics}에 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChallengeMetricsReadService {

    /** 참여를 한 번에 읽는 수 */
    static final int CHUNK_SIZE = 500;
    private static final String INVALID_DATE = "기간은 2026-11-01 형식(yyyy-MM-dd)으로 보내 주세요.";
    private static final String INVALID_RANGE = "기간 시작일(from)은 끝 날짜(to)보다 늦을 수 없습니다.";

    private final ChallengeMetricsQueryRepository metricsQueryRepository;
    private final ChallengeReadService challengeReadService;
    private final MissionRepository missionRepository;
    private final AdminAccessService adminAccessService;

    /** 사용 중인 챌린지 전체의 참여 지표(최근 챌린지부터) */
    public ChallengeMetricsListResponse getChallengeMetricsList(ChallengeMetricsServiceRequest request) {
        adminAccessService.getCurrentAdminId();
        ChallengeMetricsPeriod period = period(request);

        List<ChallengeTitle> challenges = metricsQueryRepository.findActiveChallenges();
        Map<Long, ChallengeMetricsCalculator> calculators = new LinkedHashMap<>();
        challenges.forEach(challenge -> calculators.put(challenge.id(), new ChallengeMetricsCalculator(period)));
        addParticipations(calculators);

        List<ChallengeMetrics> metrics = challenges.stream()
                .map(challenge -> calculators.get(challenge.id()).result(challenge.id(), challenge.title()))
                .toList();
        return new ChallengeMetricsListResponse(period.from(), period.to(), metrics);
    }

    /** 챌린지 하나의 참여 지표와 사용 중인 미션별 완료율 */
    public ChallengeMetricsDetailResponse getChallengeMetrics(Long challengeId, ChallengeMetricsServiceRequest request) {
        adminAccessService.getCurrentAdminId();
        ChallengeMetricsPeriod period = period(request);
        Challenge challenge = challengeReadService.findById(challengeId);

        ChallengeMetricsCalculator calculator = new ChallengeMetricsCalculator(period);
        addParticipations(Map.of(challenge.getId(), calculator));

        return new ChallengeMetricsDetailResponse(period.from(), period.to(),
                calculator.result(challenge.getId(), challenge.getTitle()), missionMetrics(challenge.getId(), period));
    }

    /** 참여를 ID 순서로 {@value #CHUNK_SIZE}건씩 읽어 챌린지별 계산기에 넣는다 */
    private void addParticipations(Map<Long, ChallengeMetricsCalculator> calculators) {
        long afterId = 0L;
        List<ParticipationRow> rows;
        do {
            rows = metricsQueryRepository.findParticipationsAfter(calculators.keySet(), afterId, CHUNK_SIZE);
            for (ParticipationRow row : rows) {
                calculators.get(row.challengeId()).add(row);
                afterId = row.id();
            }
        } while (rows.size() == CHUNK_SIZE);
    }

    private List<MissionMetrics> missionMetrics(Long challengeId, ChallengeMetricsPeriod period) {
        Map<Long, long[]> counts = new HashMap<>();   // missionId → [전체, 완료]
        for (MissionStatusCount count : metricsQueryRepository.countMissionHistories(challengeId,
                period.startInclusive(), period.endExclusive())) {
            long[] total = counts.computeIfAbsent(count.missionId(), id -> new long[2]);
            total[0] += count.count();
            if (count.status() == MissionStatus.COMPLETED) {
                total[1] += count.count();
            }
        }
        return missionRepository.findByChallengeId(challengeId).stream()
                .sorted(Comparator.comparing(Mission::getId))
                .map(mission -> {
                    long[] total = counts.getOrDefault(mission.getId(), new long[2]);
                    return new MissionMetrics(mission.getId(), mission.getTitle(), mission.getCategory(),
                            MetricRatio.of(total[1], total[0]));
                })
                .toList();
    }

    private static ChallengeMetricsPeriod period(ChallengeMetricsServiceRequest request) {
        LocalDate from = parseDate(request.getFrom());
        LocalDate to = parseDate(request.getTo());
        if (from != null && to != null && from.isAfter(to)) {
            throw new QuitmateException(INVALID_RANGE);
        }
        return new ChallengeMetricsPeriod(from, to);
    }

    private static LocalDate parseDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new QuitmateException(INVALID_DATE);
        }
    }
}
