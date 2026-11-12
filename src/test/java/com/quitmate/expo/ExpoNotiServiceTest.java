package com.quitmate.expo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quitmate.alertHistory.entity.AlertDestinationType;
import com.quitmate.alertHistory.service.alertHistory.AlertHistoryService;
import com.quitmate.firebase.request.SendFirebaseDataDto;
import com.quitmate.firebase.request.SendFirebaseServiceRequest;
import com.quitmate.user.push.entity.Push;
import com.quitmate.user.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExpoNotiServiceTest {

	private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private AlertHistoryService alertHistoryService;

	private ExpoNotiService expoNotiService;

	@BeforeEach
	void setUp() {
		expoNotiService = new ExpoNotiService(restTemplate, alertHistoryService, new ObjectMapper());
	}

	@DisplayName("Expo 에는 한 요청에 100건까지만 보낸다. 250건이면 100·100·50건 세 번에 나눠 보내고, 알림 기록은 사용자마다 한 번 남긴다.")
	@Test
	@SuppressWarnings("unchecked")
	void sendsInChunksOfHundred() {
		// given
		given(restTemplate.postForEntity(eq(EXPO_PUSH_URL), any(HttpEntity.class), eq(String.class)))
			.willReturn(ResponseEntity.ok("{\"data\":[]}"));
		List<SendFirebaseServiceRequest> requests = IntStream.rangeClosed(1, 250)
			.mapToObj(i -> pushRequest((long) i, "ExponentPushToken[" + i + "]", noticeData(9L)))
			.toList();

		// when
		expoNotiService.sendBatchPushNotification(requests);

		// then
		ArgumentCaptor<HttpEntity<List<Map<String, Object>>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
		verify(restTemplate, times(3)).postForEntity(eq(EXPO_PUSH_URL), captor.capture(), eq(String.class));
		assertThat(captor.getAllValues()).extracting(entity -> entity.getBody().size())
			.containsExactly(100, 100, 50);
		assertThat(captor.getAllValues().get(2).getBody().get(49).get("to")).isEqualTo("ExponentPushToken[250]");
		verify(alertHistoryService, times(250)).createAlertHistory(any());
	}

	@DisplayName("100건 이하는 한 번에 보낸다.")
	@Test
	void sendsOnceWhenHundredOrLess() {
		// given
		given(restTemplate.postForEntity(eq(EXPO_PUSH_URL), any(HttpEntity.class), eq(String.class)))
			.willReturn(ResponseEntity.ok("{\"data\":[]}"));
		List<SendFirebaseServiceRequest> requests = IntStream.rangeClosed(1, 100)
			.mapToObj(i -> pushRequest((long) i, "ExponentPushToken[" + i + "]", noticeData(9L)))
			.toList();

		// when
		expoNotiService.sendBatchPushNotification(requests);

		// then
		verify(restTemplate, times(1)).postForEntity(eq(EXPO_PUSH_URL), any(HttpEntity.class), eq(String.class));
	}

	@DisplayName("공지 푸시 data에는 notice_id 가 들어가고, 공지가 아닌 푸시 data에는 notice_id 키가 없다.")
	@Test
	@SuppressWarnings("unchecked")
	void noticeIdOnlyInNoticePushData() {
		// given
		given(restTemplate.postForEntity(eq(EXPO_PUSH_URL), any(HttpEntity.class), eq(String.class)))
			.willReturn(ResponseEntity.ok("{\"data\":[]}"));
		SendFirebaseDataDto inquiryData = SendFirebaseDataDto.builder()
			.alert_destination_type(AlertDestinationType.INQUIRY)
			.alert_destination_info("문의")
			.build();

		// when
		expoNotiService.sendBatchPushNotification(List.of(
			pushRequest(1L, "ExponentPushToken[notice]", noticeData(9L)),
			pushRequest(2L, "ExponentPushToken[inquiry]", inquiryData)));

		// then
		ArgumentCaptor<HttpEntity<List<Map<String, Object>>>> captor = ArgumentCaptor.forClass(HttpEntity.class);
		verify(restTemplate).postForEntity(eq(EXPO_PUSH_URL), captor.capture(), eq(String.class));
		List<Map<String, Object>> bodies = captor.getValue().getBody();
		Map<String, Object> noticePushData = (Map<String, Object>) bodies.get(0).get("data");
		Map<String, Object> inquiryPushData = (Map<String, Object>) bodies.get(1).get("data");
		assertThat(noticePushData)
			.containsEntry("alert_destination_type", "NOTICE")
			.containsEntry("alert_destination_info", "공지사항")
			.containsEntry("notice_id", 9L);
		assertThat(inquiryPushData).doesNotContainKey("notice_id");
	}

	private SendFirebaseDataDto noticeData(Long noticeId) {
		return SendFirebaseDataDto.builder()
			.alert_destination_type(AlertDestinationType.NOTICE)
			.alert_destination_info("공지사항")
			.notice_id(noticeId)
			.build();
	}

	private SendFirebaseServiceRequest pushRequest(Long userId, String token, SendFirebaseDataDto data) {
		User user = mock(User.class);
		given(user.getId()).willReturn(userId);
		Push push = Push.of("device-" + userId, user, token);
		return SendFirebaseServiceRequest.builder()
			.push(push)
			.title("공지 제목")
			.body("공지 내용")
			.sound("default")
			.sendFirebaseDataDto(data)
			.build();
	}
}
