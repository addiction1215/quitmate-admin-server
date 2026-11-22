package com.quitmate.locationAccessLog.service;

import com.quitmate.locationAccessLog.repository.AdminLocationAccessLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 위치정보 열람 이력의 보관 기간(기본 6개월, 기획 결정 KAN-208)이 지난 행을 지운다.
 *
 * <ul>
 *   <li>기준은 {@code accessed_at < (지금 한국 시각 - 보관 개월 수)}. 기준 시각과 같은 행은 남긴다.</li>
 *   <li>한 트랜잭션에 batchSize 건씩 나눠 지우고, 지울 것이 없거나 batchSize 보다 적게 지워질 때까지 반복한다.
 *       오래 쌓인 뒤 첫 실행이어도 긴 잠금이 생기지 않는다.</li>
 *   <li>멱등이다. 서버가 겹쳐 떠서(무중단 배포 중 두 WAS) 같이 돌아도 이미 지워진 행은 0건일 뿐 오류가 아니다.</li>
 *   <li>보관 개월 수는 설정 {@code location-access-log.retention-months}(기본 6). application-*.yml 은 건드리지 않는다.</li>
 * </ul>
 */
@Slf4j
@Service
public class LocationAccessLogRetentionService {

    static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    static final int DEFAULT_BATCH_SIZE = 1000;
    /** 무한 반복 방지. 1000건 × 10,000회 = 천만 건 */
    static final int MAX_BATCHES = 10_000;

    private final AdminLocationAccessLogRepository adminLocationAccessLogRepository;
    private final TransactionTemplate transactionTemplate;
    private final int retentionMonths;

    public LocationAccessLogRetentionService(AdminLocationAccessLogRepository adminLocationAccessLogRepository,
                                             PlatformTransactionManager transactionManager,
                                             @Value("${location-access-log.retention-months:6}") int retentionMonths) {
        this.adminLocationAccessLogRepository = adminLocationAccessLogRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.retentionMonths = retentionMonths;
    }

    /** 지금(한국 시각) 기준으로 지운다. @return 지운 총 건수 */
    public long purgeExpired() {
        return purgeExpired(LocalDateTime.now(KOREA), DEFAULT_BATCH_SIZE);
    }

    /** 기준 시각·묶음 크기를 지정해 지운다(테스트용으로도 쓴다) */
    public long purgeExpired(LocalDateTime now, int batchSize) {
        LocalDateTime cutoff = now.minusMonths(retentionMonths);
        long total = 0;
        for (int i = 0; i < MAX_BATCHES; i++) {
            Integer deleted = transactionTemplate.execute(status ->
                    adminLocationAccessLogRepository.deleteAccessedBefore(cutoff, batchSize));
            int count = deleted == null ? 0 : deleted;
            total += count;
            if (count < batchSize) {
                break;
            }
        }
        return total;
    }
}
