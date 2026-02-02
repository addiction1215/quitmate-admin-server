package com.quitmate.alertHistory.service.alertHistory;

import com.quitmate.alertHistory.entity.AlertHistory;
import com.quitmate.alertHistory.repository.AlertHistoryRepository;
import com.quitmate.alertHistory.service.alertHistory.request.AlertHistoryServiceRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AlertHistoryService {

    private final AlertHistoryRepository alertHistoryRepository;

    public AlertHistory createAlertHistory(AlertHistoryServiceRequest alertHistoryServiceRequest) {
        return alertHistoryRepository.save(alertHistoryServiceRequest.toEntity());
    }
}
