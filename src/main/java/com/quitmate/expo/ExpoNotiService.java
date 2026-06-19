package com.quitmate.expo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpoNotiService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final String EXPO_RECEIPTS_URL = "https://exp.host/--/api/v2/push/getReceipts";

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
            log.info("Expo 배치 푸시 전송 성공 - 건수: {}, status: {}, body: {}",
                    bodies.size(), response.getStatusCode(), response.getBody());

            List<String> ticketIds = extractTicketIds(response.getBody());
            if (!ticketIds.isEmpty()) {
                callExpoReceiptsApi(ticketIds);
            }
        } catch (Exception e) {
            log.error("Expo 배치 푸시 전송 실패 - message: {}", e.getMessage());
        }
    }

    private List<String> extractTicketIds(String responseBody) {
        List<String> ticketIds = new ArrayList<>();
        try {
            JsonNode data = objectMapper.readTree(responseBody).path("data");
            if (!data.isArray()) {
                return ticketIds;
            }

            for (JsonNode ticket : data) {
                String status = ticket.path("status").asText();
                if ("ok".equals(status) && ticket.hasNonNull("id")) {
                    ticketIds.add(ticket.path("id").asText());
                    continue;
                }

                log.warn("Expo 푸시 ticket 오류 - status: {}, message: {}, details.error: {}",
                        status,
                        ticket.path("message").asText(null),
                        ticket.path("details").path("error").asText(null));
            }
        } catch (Exception e) {
            log.warn("Expo 푸시 ticket 응답 파싱 실패 - message: {}, body: {}", e.getMessage(), responseBody);
        }
        return ticketIds;
    }

    private void callExpoReceiptsApi(List<String> ticketIds) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");
            headers.set("Accept-Encoding", "gzip, deflate");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("ids", ticketIds), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(EXPO_RECEIPTS_URL, entity, String.class);
            log.info("Expo 푸시 receipt 조회 성공 - ticketIds: {}, status: {}, body: {}",
                    ticketIds, response.getStatusCode(), response.getBody());

            logReceiptResults(response.getBody());
        } catch (Exception e) {
            log.error("Expo 푸시 receipt 조회 실패 - ticketIds: {}, message: {}", ticketIds, e.getMessage());
        }
    }

    private void logReceiptResults(String responseBody) {
        try {
            JsonNode data = objectMapper.readTree(responseBody).path("data");
            data.fields().forEachRemaining(entry -> {
                JsonNode receipt = entry.getValue();
                log.info("Expo 푸시 receipt 결과 - ticketId: {}, status: {}, message: {}, details.error: {}",
                        entry.getKey(),
                        receipt.path("status").asText(null),
                        receipt.path("message").asText(null),
                        receipt.path("details").path("error").asText(null));
            });
        } catch (Exception e) {
            log.warn("Expo 푸시 receipt 응답 파싱 실패 - message: {}, body: {}", e.getMessage(), responseBody);
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
