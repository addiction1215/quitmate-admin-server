package com.quitmate.notice.service;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.alertSetting.repository.AlertSettingJpaRepository;
import com.quitmate.alertHistory.entity.AlertDestinationType;
import com.quitmate.expo.event.PushNotificationEvent;
import com.quitmate.firebase.request.SendFirebaseServiceRequest;
import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.repository.NoticeJpaRepository;
import com.quitmate.notice.service.impl.AdPushCopy;
import com.quitmate.notice.service.impl.AdPushQuietHours;
import com.quitmate.notice.service.request.NoticeCreateServiceRequest;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.user.push.entity.Push;
import com.quitmate.user.push.repository.PushJpaRepository;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.SnsType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@RecordApplicationEvents
public class NoticeServiceTest extends IntegrationTestSupport {

	@Autowired
	private NoticeService noticeService;

	@Autowired
	private NoticeJpaRepository noticeJpaRepository;

	@Autowired
	private AlertSettingJpaRepository alertSettingJpaRepository;

	@Autowired
	private PushJpaRepository pushJpaRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ApplicationEvents events;

	@MockitoBean
	private AdPushQuietHours adPushQuietHours;

	@AfterEach
	void deleteNoticesAndAlertSettings() {
		pushJpaRepository.deleteAllInBatch();
		noticeJpaRepository.deleteAllInBatch();
		alertSettingJpaRepository.deleteAllInBatch();
	}

	@DisplayName("앱 서버에서 애플로 가입한 사용자가 있어도 공지사항을 등록한다.")
	@Test
	void createNoticeWithAppleUser() {
		// given
		User user = userRepository.save(
			createUser("apple@privaterelay.appleid.com", "1234", SnsType.NORMAL, SettingStatus.COMPLETE));
		// 애플 가입자는 앱 서버가 저장하므로, 관리자 서버 밖에서 쓴 값처럼 DB에 직접 넣는다.
		jdbcTemplate.update("update users set sns_type = 'APPLE' where id = ?", user.getId());

		NoticeCreateServiceRequest request = NoticeCreateServiceRequest.builder()
			.type(NoticeType.NOTICE)
			.title("QuitMate 서비스 이용 안내")
			.content("QuitMate 서비스 이용 안내")
			.build();

		// when
		NoticeCreateResponse response = noticeService.createNotice(request);

		// then
		assertThat(response.getContent()).isEqualTo("QuitMate 서비스 이용 안내");
		assertThat(noticeJpaRepository.findById(response.getId())).isPresent();
	}

	@DisplayName("공지 푸시 제목은 공지 제목이고, 본문은 알림 기록 칸(255자)에 맞춰 줄인다.")
	@Test
	void createNoticePushUsesTitleAndShortBody() {
		// given
		User user = userRepository.save(
			createUser("push@test.com", "1234", SnsType.NORMAL, SettingStatus.COMPLETE));
		pushJpaRepository.save(Push.of("device-1", user, "ExponentPushToken[test]"));

		NoticeCreateServiceRequest request = NoticeCreateServiceRequest.builder()
			.type(NoticeType.NOTICE)
			.title("서비스 이용 안내")
			.content("가".repeat(300))
			.build();

		// when
		NoticeCreateResponse response = noticeService.createNotice(request);

		// then
		assertThat(noticeJpaRepository.findById(response.getId()).orElseThrow().getTitle()).isEqualTo("서비스 이용 안내");
		assertThat(events.stream(PushNotificationEvent.class)).hasSize(1);
		SendFirebaseServiceRequest push = events.stream(PushNotificationEvent.class)
			.findFirst().orElseThrow().getRequests().get(0);
		assertThat(push.getTitle()).isEqualTo("서비스 이용 안내");
		assertThat(push.getBody()).hasSize(255).endsWith("…");
		assertThat(response.getPushSkipReason()).isNull();
	}

	@DisplayName("안내·업데이트·점검 공지는 서비스 소식 ON인 사람과 알림 설정 행이 없는 사람에게 보내고, 서비스 소식 OFF인 사람은 뺀다.")
	@ParameterizedTest
	@EnumSource(value = NoticeType.class, names = {"NOTICE", "FEATURE_UPDATE", "NEW_FEATURE", "SERVICE_CHECK"})
	void serviceNewsNoticePushTargetsServiceNewsOn(NoticeType type) {
		// given
		userWithSetting("on@test.com", "ExponentPushToken[on]", "ON", "NOT_AGREED");
		userWithSetting("off@test.com", "ExponentPushToken[off]", "OFF", "AGREED");
		userWithoutSetting("none@test.com", "ExponentPushToken[none]");

		// when
		NoticeCreateResponse response = noticeService.createNotice(noticeRequest(type, "공지 제목", "공지 내용"));

		// then
		assertThat(pushTokens()).containsExactlyInAnyOrder("ExponentPushToken[on]", "ExponentPushToken[none]");
		assertThat(response.getPushSkipReason()).isNull();
		// 행이 없는 사람은 기본값으로 볼 뿐, 관리자 서버가 행을 만들지 않는다.
		assertThat(alertSettingJpaRepository.count()).isEqualTo(2);
	}

	@DisplayName("이벤트 공지는 광고성 푸시에 동의한 사람에게만 보낸다. 미동의·철회·알림 설정 행이 없는 사람은 뺀다.")
	@Test
	void eventNoticePushTargetsAdPushAgreedOnly() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "OFF", "AGREED");
		userWithSetting("not-agreed@test.com", "ExponentPushToken[not-agreed]", "ON", "NOT_AGREED");
		userWithSetting("withdrawn@test.com", "ExponentPushToken[withdrawn]", "ON", "WITHDRAWN");
		userWithoutSetting("none@test.com", "ExponentPushToken[none]");

		// when
		noticeService.createNotice(noticeRequest(NoticeType.EVENT, "겨울 이벤트", "이벤트 내용"));

		// then
		assertThat(pushTokens()).containsExactly("ExponentPushToken[agreed]");
		assertThat(alertSettingJpaRepository.count()).isEqualTo(3);
	}

	@DisplayName("이벤트 공지 푸시는 제목 앞에 (광고)를 붙이고, 본문 끝에 전송자와 수신 거부 방법을 넣는다. 본문은 255자를 넘지 않는다.")
	@Test
	void eventNoticePushHasAdNotation() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");

		// when
		noticeService.createNotice(noticeRequest(NoticeType.EVENT, "겨울 이벤트", "가".repeat(300)));

		// then
		SendFirebaseServiceRequest push = onlyPush();
		assertThat(push.getTitle()).isEqualTo("(광고) 겨울 이벤트");
		assertThat(push.getBody())
			.hasSize(255)
			.startsWith("가가가")
			.contains("…")
			.endsWith("\n\n전송자: QuitMate\n수신 거부: 설정 > 알림 설정에서 끌 수 있어요");
	}

	@DisplayName("짧은 이벤트 공지는 내용을 줄이지 않고 끝에 전송자와 수신 거부 방법만 붙인다.")
	@Test
	void shortEventNoticePushKeepsContent() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");

		// when
		noticeService.createNotice(noticeRequest(NoticeType.EVENT, "겨울 이벤트", "이벤트 내용"));

		// then
		assertThat(onlyPush().getBody()).isEqualTo("이벤트 내용" + AdPushCopy.BODY_FOOTER);
	}

	@DisplayName("안내 공지 푸시에는 (광고) 표시를 붙이지 않는다.")
	@Test
	void serviceNewsNoticePushHasNoAdNotation() {
		// given
		userWithSetting("on@test.com", "ExponentPushToken[on]", "ON", "AGREED");

		// when
		noticeService.createNotice(noticeRequest(NoticeType.NOTICE, "점검 안내", "안내 내용"));

		// then
		SendFirebaseServiceRequest push = onlyPush();
		assertThat(push.getTitle()).isEqualTo("점검 안내");
		assertThat(push.getBody()).isEqualTo("안내 내용");
	}

	@DisplayName("공지 푸시 data에 공지 ID를 넣는다. 알림 기록에 남는 alert_destination_info 는 그대로 '공지사항'이다.")
	@Test
	void noticePushDataHasNoticeId() {
		// given
		userWithoutSetting("none@test.com", "ExponentPushToken[none]");

		// when
		NoticeCreateResponse response = noticeService.createNotice(noticeRequest(NoticeType.NOTICE, "공지 제목", "공지 내용"));

		// then
		SendFirebaseServiceRequest push = onlyPush();
		assertThat(push.getSendFirebaseDataDto().getNotice_id()).isEqualTo(response.getId());
		assertThat(push.getSendFirebaseDataDto().getAlert_destination_type()).isEqualTo(AlertDestinationType.NOTICE);
		assertThat(push.getSendFirebaseDataDto().getAlert_destination_info()).isEqualTo("공지사항");
	}

	@DisplayName("야간(21:00~08:00)에 등록한 이벤트 공지는 저장하지만 광고 푸시는 보내지 않고, 그 까닭을 응답에 담는다.")
	@Test
	void eventNoticeAtNightSavesWithoutPush() {
		// given
		given(adPushQuietHours.isQuietNow()).willReturn(true);
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");

		// when
		NoticeCreateResponse response = noticeService.createNotice(noticeRequest(NoticeType.EVENT, "야간 이벤트", "이벤트 내용"));

		// then
		assertThat(noticeJpaRepository.findById(response.getId())).isPresent();
		assertThat(events.stream(PushNotificationEvent.class)).isEmpty();
		assertThat(response.getPushSkipReason()).isEqualTo(AdPushCopy.QUIET_HOURS_SKIP_REASON);
	}

	@DisplayName("야간이어도 광고가 아닌 공지 푸시는 보낸다.")
	@Test
	void serviceNewsNoticeAtNightIsSent() {
		// given
		given(adPushQuietHours.isQuietNow()).willReturn(true);
		userWithSetting("on@test.com", "ExponentPushToken[on]", "ON", "NOT_AGREED");

		// when
		NoticeCreateResponse response = noticeService.createNotice(noticeRequest(NoticeType.SERVICE_CHECK, "긴급 점검", "점검 내용"));

		// then
		assertThat(pushTokens()).containsExactly("ExponentPushToken[on]");
		assertThat(response.getPushSkipReason()).isNull();
	}

	private User userWithoutSetting(String email, String pushToken) {
		User user = userRepository.save(createUser(email, "1234", SnsType.NORMAL, SettingStatus.COMPLETE));
		pushJpaRepository.save(Push.of("device-" + email, user, pushToken));
		return user;
	}

	/**
	 * 새 칸(서비스 소식·광고 동의)은 앱 서버가 쓰므로, 관리자 서버 밖에서 쓴 값처럼 DB에 직접 넣는다.
	 */
	private User userWithSetting(String email, String pushToken, String serviceNews, String adPushConsentStatus) {
		User user = userWithoutSetting(email, pushToken);
		alertSettingJpaRepository.save(AlertSetting.createDefault(user));
		jdbcTemplate.update(
			"update alert_setting set service_news = ?, ad_push_consent_status = ? where user_id = ?",
			serviceNews, adPushConsentStatus, user.getId());
		return user;
	}

	private NoticeCreateServiceRequest noticeRequest(NoticeType type, String title, String content) {
		return NoticeCreateServiceRequest.builder()
			.type(type)
			.title(title)
			.content(content)
			.build();
	}

	private List<String> pushTokens() {
		return events.stream(PushNotificationEvent.class)
			.flatMap(event -> event.getRequests().stream())
			.map(request -> request.getPush().getPushToken())
			.toList();
	}

	private SendFirebaseServiceRequest onlyPush() {
		List<SendFirebaseServiceRequest> requests = events.stream(PushNotificationEvent.class)
			.flatMap(event -> event.getRequests().stream())
			.toList();
		assertThat(requests).hasSize(1);
		return requests.get(0);
	}
}
