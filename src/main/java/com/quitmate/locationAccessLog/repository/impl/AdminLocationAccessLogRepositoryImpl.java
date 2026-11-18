package com.quitmate.locationAccessLog.repository.impl;

import com.quitmate.locationAccessLog.entity.AdminLocationAccessLog;
import com.quitmate.locationAccessLog.repository.AdminLocationAccessLogJpaRepository;
import com.quitmate.locationAccessLog.repository.AdminLocationAccessLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminLocationAccessLogRepositoryImpl implements AdminLocationAccessLogRepository {

    private final AdminLocationAccessLogJpaRepository adminLocationAccessLogJpaRepository;

    @Override
    public AdminLocationAccessLog save(AdminLocationAccessLog log) {
        return adminLocationAccessLogJpaRepository.save(log);
    }
}
