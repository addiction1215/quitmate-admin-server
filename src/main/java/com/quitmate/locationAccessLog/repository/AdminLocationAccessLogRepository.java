package com.quitmate.locationAccessLog.repository;

import com.quitmate.locationAccessLog.entity.AdminLocationAccessLog;

import java.time.LocalDateTime;

public interface AdminLocationAccessLogRepository {

    AdminLocationAccessLog save(AdminLocationAccessLog log);

    /**
     * accessed_at 이 cutoff 보다 이른(cutoff 와 같은 행은 남김) 행을 오래된 것부터 최대 limit 건 지운다.
     * 다른 서버가 먼저 지웠어도(0건) 오류가 아니다(멱등).
     *
     * @return 실제로 지운 건수
     */
    int deleteAccessedBefore(LocalDateTime cutoff, int limit);
}
