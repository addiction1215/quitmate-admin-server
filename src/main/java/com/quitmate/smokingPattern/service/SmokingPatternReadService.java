package com.quitmate.smokingPattern.service;

import com.quitmate.global.exception.NotFoundException;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.page.request.PageInfoServiceRequest;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.security.AdminAccessService;
import com.quitmate.smokingPattern.entity.SmokingPatternSnapshot;
import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.repository.SmokingPatternSnapshotJpaRepository;
import com.quitmate.smokingPattern.repository.SmokingPatternSnapshotQueryRepository;
import com.quitmate.smokingPattern.repository.UserPassPeriodRepository;
import com.quitmate.smokingPattern.repository.response.PassPeriod;
import com.quitmate.smokingPattern.repository.response.SmokingPatternSnapshotRow;
import com.quitmate.smokingPattern.service.request.SmokingPatternSnapshotListServiceRequest;
import com.quitmate.smokingPattern.service.response.SmokingPatternDistributionResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternSnapshotDetailResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternSnapshotSummaryResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternWeekResponse;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 어드민 「흡연 패턴 진단」 조회(KAN-141). 앱 서버 주간 배치(KAN-134)가 만든 스냅숏을 읽기만 한다. 관리자 계정만 쓸 수 있다.
 *
 * <p>유료·무료는 그 분석 주(월 00:00 ~ 다음 월 00:00)와 겹치는, 회수되지 않은 QuitMate 90 이용권이 있었는지로 나눈다.
 * 개인정보는 회원 ID와 닉네임만 준다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SmokingPatternReadService {

    /** 전체 분포를 계산할 때 한 번에 읽는 스냅숏 수 */
    static final int CHUNK_SIZE = 500;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_WEEKS = 12;
    private static final int MAX_WEEKS = 52;
    private static final int WEEK_DAYS = 7;
    private static final String INVALID_PERIOD_START = "분석 주 시작일은 월요일 날짜를 2026-11-02 형식으로 보내 주세요.";

    private final SmokingPatternSnapshotJpaRepository snapshotJpaRepository;
    private final SmokingPatternSnapshotQueryRepository snapshotQueryRepository;
    private final UserPassPeriodRepository userPassPeriodRepository;
    private final UserRepository userRepository;
    private final AdminAccessService adminAccessService;

    /** 스냅숏이 있는 최근 주(최근 주부터) */
    public List<SmokingPatternWeekResponse> getWeeks(Integer size) {
        adminAccessService.getCurrentAdminId();
        int weeks = size == null ? DEFAULT_WEEKS : Math.max(1, Math.min(MAX_WEEKS, size));
        return snapshotJpaRepository.findWeeks(PageRequest.of(0, weeks)).stream()
                .map(SmokingPatternWeekResponse::of)
                .toList();
    }

    /** 한 주의 사용자별 스냅숏(회원 ID 순). 주를 안 주면 가장 최근 주 */
    public PageCustom<SmokingPatternSnapshotSummaryResponse> getWeekSnapshots(
            SmokingPatternSnapshotListServiceRequest request) {
        adminAccessService.getCurrentAdminId();
        PageRequest pageable = pageable(request);
        Optional<LocalDate> periodStart = resolvePeriodStart(request.getPeriodStart());
        if (periodStart.isEmpty()) {
            return PageCustom.of(Page.empty(pageable));
        }

        LocalDate start = periodStart.get();
        Set<Long> paidUserIds = userPassPeriodRepository.findPaidUserIds(weekFrom(start), weekTo(start));
        return PageCustom.of(snapshotQueryRepository
                .findWeekSnapshots(start, parseDataSufficiency(request.getDataSufficiency()), request.getUserId(),
                        request.getKeyword(), pageable)
                .map(dto -> SmokingPatternSnapshotSummaryResponse.of(dto, paidUserIds.contains(dto.userId()))));
    }

    /** 한 회원의 주차별 스냅숏(최근 주부터) */
    public PageCustom<SmokingPatternSnapshotSummaryResponse> getUserSnapshots(Long userId,
                                                                             PageInfoServiceRequest request) {
        adminAccessService.getCurrentAdminId();
        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundException("해당 회원이 없습니다.");
        }
        List<PassPeriod> passes = passPeriods(userId);
        return PageCustom.of(snapshotQueryRepository.findUserSnapshots(userId, pageable(request))
                .map(dto -> SmokingPatternSnapshotSummaryResponse.of(dto, paidDuring(passes, dto.periodStart()))));
    }

    /** 스냅숏 한 건의 지표 1~9 전체(어드민 전용 지표 7·8 포함) */
    public SmokingPatternSnapshotDetailResponse getSnapshot(Long snapshotId) {
        adminAccessService.getCurrentAdminId();
        SmokingPatternSnapshot snapshot = snapshotJpaRepository.findById(snapshotId)
                .orElseThrow(() -> new NotFoundException("해당 진단 스냅숏이 없습니다."));
        String nickName = userRepository.findById(snapshot.getUserId()).map(User::getNickName).orElse(null);
        boolean paid = paidDuring(passPeriods(snapshot.getUserId()), snapshot.getPeriodStart());
        return SmokingPatternSnapshotDetailResponse.of(snapshot, nickName, paid);
    }

    /**
     * 한 주의 전체·유료·무료 지표 분포. 주를 안 주면 가장 최근 주.
     * 사용자가 많아도 메모리에 다 올리지 않도록 그 주 스냅숏을 id 순서로 {@value #CHUNK_SIZE}건씩 나눠 읽는다.
     */
    public SmokingPatternDistributionResponse getDistribution(String periodStartValue) {
        adminAccessService.getCurrentAdminId();
        SmokingPatternDistributionCalculator all = new SmokingPatternDistributionCalculator();
        SmokingPatternDistributionCalculator paid = new SmokingPatternDistributionCalculator();
        SmokingPatternDistributionCalculator free = new SmokingPatternDistributionCalculator();

        Optional<LocalDate> periodStart = resolvePeriodStart(periodStartValue);
        if (periodStart.isEmpty()) {
            return new SmokingPatternDistributionResponse(null, null, all.result(), paid.result(), free.result());
        }

        LocalDate start = periodStart.get();
        Set<Long> paidUserIds = userPassPeriodRepository.findPaidUserIds(weekFrom(start), weekTo(start));
        long afterId = 0L;
        List<SmokingPatternSnapshotRow> rows;
        do {
            rows = snapshotJpaRepository.findRowsAfter(start, afterId, PageRequest.of(0, CHUNK_SIZE));
            for (SmokingPatternSnapshotRow row : rows) {
                all.add(row);
                (paidUserIds.contains(row.userId()) ? paid : free).add(row);
                afterId = row.id();
            }
        } while (rows.size() == CHUNK_SIZE);

        return new SmokingPatternDistributionResponse(start, start.plusDays(WEEK_DAYS - 1),
                all.result(), paid.result(), free.result());
    }

    /** 값이 있으면 월요일인지 확인하고, 없으면 스냅숏이 있는 가장 최근 주 */
    private Optional<LocalDate> resolvePeriodStart(String value) {
        if (!StringUtils.hasText(value)) {
            return snapshotJpaRepository.findLatestPeriodStart();
        }
        try {
            LocalDate date = LocalDate.parse(value.trim());
            if (date.getDayOfWeek() != DayOfWeek.MONDAY) {
                throw new QuitmateException(INVALID_PERIOD_START);
            }
            return Optional.of(date);
        } catch (DateTimeParseException e) {
            throw new QuitmateException(INVALID_PERIOD_START);
        }
    }

    private static DataSufficiencyLevel parseDataSufficiency(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return DataSufficiencyLevel.valueOf(value.trim());
        } catch (IllegalArgumentException e) {
            throw new QuitmateException("데이터 충족도는 SUFFICIENT, PARTIAL, INSUFFICIENT 중 하나로 보내 주세요.");
        }
    }

    private List<PassPeriod> passPeriods(Long userId) {
        return userPassPeriodRepository.findPassPeriods(List.of(userId)).getOrDefault(userId, List.of());
    }

    private static boolean paidDuring(List<PassPeriod> passes, LocalDate periodStart) {
        return passes.stream().anyMatch(pass -> pass.overlaps(weekFrom(periodStart), weekTo(periodStart)));
    }

    private static LocalDateTime weekFrom(LocalDate periodStart) {
        return periodStart.atStartOfDay();
    }

    private static LocalDateTime weekTo(LocalDate periodStart) {
        return periodStart.plusDays(WEEK_DAYS).atStartOfDay();
    }

    private static PageRequest pageable(PageInfoServiceRequest request) {
        return PageRequest.of(Math.max(0, request.getPage() - 1), Math.max(1, Math.min(MAX_PAGE_SIZE, request.getSize())));
    }
}
