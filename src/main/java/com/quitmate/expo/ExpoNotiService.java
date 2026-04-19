package com.quitmate.expo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quitmate.alertHistory.service.alertHistory.AlertHistoryService;
import com.quitmate.alertHistory.service.alertHistory.request.AlertHistoryServiceRequest;
import com.quitmate.firebase.enums.PushMessage;
import com.quitmate.firebase.request.SendFirebaseServiceRequest;
import com.quitmate.user.push.entity.Push;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpoNotiService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final RestTemplate restTemplate;
    private final AlertHistoryService alertHistoryService;
    private final ObjectMapper objectMapper;

    public void sendBatchPushNotification(List<SendFirebaseServiceRequest> requests) {
        if (requests.isEmpty()) return;

        List<Map<String, Object>> bodies = buildPushBodies(requests);
        if (!bodies.isEmpty()) {
            callExpoApi(bodies);
        }
        saveAlertHistories(requests);
    }

    private List<Map<String, Object>> buildPushBodies(List<SendFirebaseServiceRequest> requests) {
        return requests.stream()
                .filter(r -> r.getPush() != null && r.getPush().getPushToken() != null)
                .map(this::toPushBody)
                .toList();
    }

    private Map<String, Object> toPushBody(SendFirebaseServiceRequest request) {
        String title = PushMessage.from(request.getSendFirebaseDataDto().getAlert_destination_type()).getText();
        Map<String, Object> data = objectMapper.convertValue(request.getSendFirebaseDataDto(), Map.class);

        return Map.of(
                "to", request.getPush().getPushToken(),
                "title", title,
                "body", request.getBody(),
                "sound", request.getSound(),
                "data", data
        );
    }

    private void callExpoApi(List<Map<String, Object>> bodies) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("Accept-Encoding", "gzip, deflate");

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(bodies, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(EXPO_PUSH_URL, entity, String.class);
            log.info("Expo 배치 푸시 전송 성공 - 건수: {}, status: {}", bodies.size(), response.getStatusCode());
        } catch (Exception e) {
            log.error("Expo 배치 푸시 전송 실패 - message: {}", e.getMessage());
        }
    }

    private void saveAlertHistories(List<SendFirebaseServiceRequest> requests) {
        requests.stream()
                .filter(r -> r.getPush() != null)
                .collect(Collectors.toMap(
                        r -> r.getPush().getUser().getId(),
                        r -> r,
                        (existing, replacement) -> existing
                ))
                .values()
                .forEach(r -> alertHistoryService.createAlertHistory(AlertHistoryServiceRequest.of(r)));
    }
}
