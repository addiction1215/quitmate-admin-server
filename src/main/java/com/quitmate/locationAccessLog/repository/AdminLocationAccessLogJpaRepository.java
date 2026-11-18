package com.quitmate.locationAccessLog.repository;

import com.quitmate.locationAccessLog.entity.AdminLocationAccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLocationAccessLogJpaRepository extends JpaRepository<AdminLocationAccessLog, Long> {
}
