package com.quitmate.locationAccessLog.repository.impl;

import com.quitmate.locationAccessLog.entity.AdminLocationAccessLog;
import com.quitmate.locationAccessLog.repository.AdminLocationAccessLogJpaRepository;
import com.quitmate.locationAccessLog.repository.AdminLocationAccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AdminLocationAccessLogRepositoryImpl implements AdminLocationAccessLogRepository {

    private final AdminLocationAccessLogJpaRepository adminLocationAccessLogJpaRepository;

    @Override
    public AdminLocationAccessLog save(AdminLocationAccessLog log) {
        return adminLocationAccessLogJpaRepository.save(log);
    }

    @Override
    public int deleteAccessedBefore(LocalDateTime cutoff, int limit) {
        List<Long> ids = adminLocationAccessLogJpaRepository.findIdsAccessedBefore(cutoff, PageRequest.of(0, limit));
        if (ids.isEmpty()) {
            return 0;
        }
        return adminLocationAccessLogJpaRepository.deleteByIdIn(ids);
    }
}
