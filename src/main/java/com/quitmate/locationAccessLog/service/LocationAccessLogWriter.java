package com.quitmate.locationAccessLog.service;

import com.quitmate.locationAccessLog.entity.AdminLocationAccessLog;
import com.quitmate.locationAccessLog.repository.AdminLocationAccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 위치정보 열람 이력 한 줄을 본 조회와 다른 트랜잭션에 쓴다(KAN-208).
 * 조회는 readOnly 트랜잭션이라, 같은 트랜잭션에 쓰다 실패하면 바깥 트랜잭션이 rollback-only 가 되어 커밋 때 500 이 난다.
 * REQUIRES_NEW 로 나눠 두면 저장 실패는 이 트랜잭션만 되돌리고 조회 응답에는 번지지 않는다.
 * 예외는 그대로 던진다. 잡는 곳은 {@link LocationAccessLogService} 다(같은 빈 안에서 부르면 트랜잭션 경계가 생기지 않아 따로 둔다).
 */
@Component
@RequiredArgsConstructor
public class LocationAccessLogWriter {

    private final AdminLocationAccessLogRepository adminLocationAccessLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AdminLocationAccessLog log) {
        adminLocationAccessLogRepository.save(log);
    }
}
