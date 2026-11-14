package com.quitmate.notice.service.impl;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.alertSetting.repository.AlertSettingJpaRepository;
import com.quitmate.firebase.request.SendFirebaseServiceRequest;
import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.repository.NoticeJpaRepository;
import com.quitmate.notice.repository.NoticeRepository;
import com.quitmate.notice.service.NoticeService;
import com.quitmate.notice.service.request.NoticeCreateServiceRequest;
import com.quitmate.notice.service.request.NoticeUpdateServiceRequest;
import com.quitmate.user.push.entity.Push;
import com.quitmate.user.push.repository.PushJpaRepository;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.SnsType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DeferredNoticePushSenderTest extends IntegrationTestSupport {

	/** 이벤트 공지를 등록하는 밤 */
	private static final LocalDateTime NIGHT = LocalDateTime.of(2026, 11, 14, 23, 0);
	/** 그 공지의 푸시를 보낼 시각 */
	private static final LocalDateTime EIGHT = LocalDateTime.of(2026, 11, 15, 8, 0);

	@Autowired
	private DeferredNoticePushSender sender;

	@Autowired
	private NoticeService noticeService;

	@Autowired
	private NoticeRepository noticeRepository;

	@Autowired
	private NoticeJpaRepository noticeJpaRepository;

	@Autowired
	private AlertSettingJpaRepository alertSettingJpaRepository;

	@Autowired
	private PushJpaRepository pushJpaRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private Clock clock;

	@AfterEach
	void deleteNoticesAndAlertSettings() {
		pushJpaRepository.deleteAllInBatch();
		noticeJpaRepository.deleteAllInBatch();
		alertSettingJpaRepository.deleteAllInBatch();
	}

	@DisplayName("밤에 보류한 이벤트 공지 푸시를 08:00에 광고 표시를 붙여 동의한 사람에게 보내고 SENT로 바꾼다.")
	@Test
	void sendsDeferredPushAtEight() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		userWithSetting("not-agreed@test.com", "ExponentPushToken[not-agreed]", "ON", "NOT_AGREED");
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");

		// when
		koreaNow(EIGHT);
		int sent = sender.sendDuePushes();

		// then
		assertThat(sent).isEqualTo(1);
		List<SendFirebaseServiceRequest> requests = onlySentRequests();
		assertThat(tokens(requests)).containsExactly("ExponentPushToken[agreed]");
		assertThat(requests.get(0).getTitle()).isEqualTo("(광고) 겨울 이벤트");
		assertThat(requests.get(0).getBody()).isEqualTo("이벤트 내용" + AdPushCopy.BODY_FOOTER);
		assertThat(requests.get(0).getSendFirebaseDataDto().getNotice_id()).isEqualTo(noticeId);
		assertThat(pushStatusOf(noticeId)).isEqualTo("SENT");
		assertThat(pushSentAtOf(noticeId)).isEqualTo(EIGHT);
	}

	@DisplayName("08:00 전(07:59:59)에는 보내지 않는다.")
	@Test
	void doesNotSendBeforeEight() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");

		// when
		koreaNow(EIGHT.minusSeconds(1));
		int sent = sender.sendDuePushes();

		// then
		assertThat(sent).isZero();
		verify(expoNotiService, never()).sendBatchPushNotification(any());
		assertThat(pushStatusOf(noticeId)).isEqualTo("DEFERRED");
	}

	@DisplayName("08:00에 서버가 꺼져 있었으면 켜진 뒤(10:30) 보낸다.")
	@Test
	void sendsLateWhenServerWasDownAtEight() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");

		// when
		koreaNow(EIGHT.plusHours(2).plusMinutes(30));
		sender.sendDuePushes();

		// then
		assertThat(tokens(onlySentRequests())).containsExactly("ExponentPushToken[agreed]");
		assertThat(pushStatusOf(noticeId)).isEqualTo("SENT");
	}

	@DisplayName("서버가 보낼 시간 내내 꺼져 있다가 보류 시간(21:30)에 켜지면, 예정 시각이 지났어도 다음 08:00까지 보내지 않는다.")
	@Test
	void waitsForNextEightWhenBackInQuietHours() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");

		// when
		koreaNow(EIGHT.withHour(21).withMinute(30));
		int sentAtNight = sender.sendDuePushes();
		koreaNow(EIGHT.plusDays(1));
		int sentNextMorning = sender.sendDuePushes();

		// then
		assertThat(sentAtNight).isZero();
		assertThat(sentNextMorning).isEqualTo(1);
		verify(expoNotiService, times(1)).sendBatchPushNotification(any());
		assertThat(pushStatusOf(noticeId)).isEqualTo("SENT");
	}

	@DisplayName("대상은 보낼 때 다시 뽑는다. 보류한 사이 동의를 철회한 사람은 빠지고, 새로 동의한 사람은 받는다.")
	@Test
	void selectsTargetsAtSendTime() {
		// given
		User withdrawn = userWithSetting("withdrawn@test.com", "ExponentPushToken[withdrawn]", "ON", "AGREED");
		User agreedLater = userWithSetting("later@test.com", "ExponentPushToken[later]", "ON", "NOT_AGREED");
		deferredEventNotice("겨울 이벤트", "이벤트 내용");
		changeAdPushConsent(withdrawn, "WITHDRAWN");
		changeAdPushConsent(agreedLater, "AGREED");

		// when
		koreaNow(EIGHT);
		sender.sendDuePushes();

		// then
		assertThat(tokens(onlySentRequests())).containsExactly("ExponentPushToken[later]");
	}

	@DisplayName("1분 뒤 다시 돌아도 이미 보낸 공지는 다시 보내지 않는다.")
	@Test
	void sendsOnlyOnceAcrossRuns() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		deferredEventNotice("겨울 이벤트", "이벤트 내용");

		// when
		koreaNow(EIGHT);
		int first = sender.sendDuePushes();
		koreaNow(EIGHT.plusMinutes(1));
		int second = sender.sendDuePushes();

		// then
		assertThat(first).isEqualTo(1);
		assertThat(second).isZero();
		verify(expoNotiService, times(1)).sendBatchPushNotification(any());
	}

	@DisplayName("보류한 푸시는 조건부 UPDATE로 한 번만 집힌다. 다른 서버가 먼저 집어 SENDING이면 이 서버는 보내지 않는다.")
	@Test
	void doesNotSendPushClaimedByAnotherServer() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");

		// when
		int claimedByOther = noticeRepository.claimDeferredPush(noticeId, EIGHT);
		int claimedAgain = noticeRepository.claimDeferredPush(noticeId, EIGHT);
		koreaNow(EIGHT);
		int sent = sender.sendDuePushes();

		// then
		assertThat(claimedByOther).isEqualTo(1);
		assertThat(claimedAgain).isZero();
		assertThat(sent).isZero();
		verify(expoNotiService, never()).sendBatchPushNotification(any());
		assertThat(pushStatusOf(noticeId)).isEqualTo("SENDING");
	}

	@DisplayName("예정 시각 전에는 조건부 UPDATE로도 집을 수 없다.")
	@Test
	void cannotClaimBeforeScheduledAt() {
		// given
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");

		// when // then
		assertThat(noticeRepository.claimDeferredPush(noticeId, EIGHT.minusSeconds(1))).isZero();
		assertThat(pushStatusOf(noticeId)).isEqualTo("DEFERRED");
	}

	@DisplayName("08:00 전에 공지를 지우면 보내지 않는다.")
	@Test
	void doesNotSendDeletedNotice() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");
		noticeService.deleteNotice(noticeId);

		// when
		koreaNow(EIGHT);
		int sent = sender.sendDuePushes();

		// then
		assertThat(sent).isZero();
		verify(expoNotiService, never()).sendBatchPushNotification(any());
		assertThat(pushStatusOf(noticeId)).isEqualTo("CANCELED");
	}

	@DisplayName("08:00 전에 공지를 고치면 보낼 때의 최신 제목·내용으로 보낸다.")
	@Test
	void sendsLatestTitleAndContent() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");
		noticeService.updateNotice(updateRequest(noticeId, NoticeType.EVENT, "고친 이벤트", "고친 내용"));

		// when
		koreaNow(EIGHT);
		sender.sendDuePushes();

		// then
		SendFirebaseServiceRequest request = onlySentRequests().get(0);
		assertThat(request.getTitle()).isEqualTo("(광고) 고친 이벤트");
		assertThat(request.getBody()).isEqualTo("고친 내용" + AdPushCopy.BODY_FOOTER);
	}

	@DisplayName("보류 중 이벤트가 아닌 유형(안내)으로 고치면, 보류 시간에는 그대로 기다렸다가 08:00에 안내 공지 대상·문구로 보낸다.")
	@Test
	void sendsAsServiceNewsWhenTypeChanged() {
		// given
		userWithSetting("service-on@test.com", "ExponentPushToken[service-on]", "ON", "NOT_AGREED");
		userWithSetting("ad-only@test.com", "ExponentPushToken[ad-only]", "OFF", "AGREED");
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");
		noticeService.updateNotice(updateRequest(noticeId, NoticeType.NOTICE, "서비스 안내", "안내 내용"));

		// when
		koreaNow(NIGHT.plusMinutes(30));
		int sentAtNight = sender.sendDuePushes();
		koreaNow(EIGHT);
		int sentAtEight = sender.sendDuePushes();

		// then
		assertThat(sentAtNight).isZero();
		assertThat(sentAtEight).isEqualTo(1);
		List<SendFirebaseServiceRequest> requests = onlySentRequests();
		assertThat(tokens(requests)).containsExactly("ExponentPushToken[service-on]");
		assertThat(requests.get(0).getTitle()).isEqualTo("서비스 안내");
		assertThat(requests.get(0).getBody()).isEqualTo("안내 내용");
	}

	@DisplayName("보내다 오류가 나면 FAILED로 두고 다시 보내지 않는다(일부만 갔을 수 있어 중복보다 누락을 택한다).")
	@Test
	void failedSendIsNotRetried() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");
		willThrow(new IllegalStateException("알림 기록 저장 실패"))
			.given(expoNotiService).sendBatchPushNotification(any());

		// when
		koreaNow(EIGHT);
		int first = sender.sendDuePushes();
		koreaNow(EIGHT.plusMinutes(1));
		int second = sender.sendDuePushes();

		// then
		assertThat(first).isZero();
		assertThat(second).isZero();
		verify(expoNotiService, times(1)).sendBatchPushNotification(any());
		assertThat(pushStatusOf(noticeId)).isEqualTo("FAILED");
	}

	@DisplayName("한 공지가 실패해도 같은 실행의 다음 공지는 보낸다.")
	@Test
	void continuesAfterOneFailure() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		Long failing = deferredEventNotice("첫 이벤트", "첫 내용");
		Long next = deferredEventNotice("둘째 이벤트", "둘째 내용");
		willThrow(new IllegalStateException("알림 기록 저장 실패"))
			.willDoNothing()
			.given(expoNotiService).sendBatchPushNotification(any());

		// when
		koreaNow(EIGHT);
		int sent = sender.sendDuePushes();

		// then
		assertThat(sent).isEqualTo(1);
		assertThat(pushStatusOf(failing)).isEqualTo("FAILED");
		assertThat(pushStatusOf(next)).isEqualTo("SENT");
	}

	@DisplayName("보내던 서버가 멈춰 SENDING으로 30분 넘게 남은 공지는 FAILED로 바꾸고 다시 보내지 않는다. 30분 안이면 그대로 둔다.")
	@Test
	void stalledSendingBecomesFailedWithoutResend() {
		// given
		userWithSetting("agreed@test.com", "ExponentPushToken[agreed]", "ON", "AGREED");
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");
		assertThat(noticeRepository.claimDeferredPush(noticeId, EIGHT)).isEqualTo(1); // 보내다 서버가 멈췄다

		// when
		koreaNow(EIGHT.plus(DeferredNoticePushSender.SENDING_TIMEOUT));
		sender.sendDuePushes();
		String withinTimeout = pushStatusOf(noticeId);
		koreaNow(EIGHT.plus(DeferredNoticePushSender.SENDING_TIMEOUT).plusSeconds(1));
		sender.sendDuePushes();

		// then
		assertThat(withinTimeout).isEqualTo("SENDING");
		assertThat(pushStatusOf(noticeId)).isEqualTo("FAILED");
		verify(expoNotiService, never()).sendBatchPushNotification(any());
	}

	@DisplayName("받을 사람이 없어도 SENT로 바꿔 다시 집지 않는다.")
	@Test
	void sendsEvenWithoutRecipients() {
		// given
		Long noticeId = deferredEventNotice("겨울 이벤트", "이벤트 내용");

		// when
		koreaNow(EIGHT);
		sender.sendDuePushes();

		// then
		assertThat(onlySentRequests()).isEmpty();
		assertThat(pushStatusOf(noticeId)).isEqualTo("SENT");
	}

	private Long deferredEventNotice(String title, String content) {
		koreaNow(NIGHT);
		Long noticeId = noticeService.createNotice(NoticeCreateServiceRequest.builder()
			.type(NoticeType.EVENT)
			.title(title)
			.content(content)
			.build()).getId();
		assertThat(pushStatusOf(noticeId)).isEqualTo("DEFERRED");
		return noticeId;
	}

	private NoticeUpdateServiceRequest updateRequest(Long id, NoticeType type, String title, String content) {
		return NoticeUpdateServiceRequest.builder()
			.id(id)
			.type(type)
			.title(title)
			.content(content)
			.build();
	}

	private void koreaNow(LocalDateTime koreaTime) {
		given(clock.instant()).willReturn(koreaTime.atZone(AdPushQuietHours.KOREA).toInstant());
	}

	private User userWithSetting(String email, String pushToken, String serviceNews, String adPushConsentStatus) {
		User user = userRepository.save(createUser(email, "1234", SnsType.NORMAL, SettingStatus.COMPLETE));
		pushJpaRepository.save(Push.of("device-" + email, user, pushToken));
		alertSettingJpaRepository.save(AlertSetting.createDefault(user));
		jdbcTemplate.update(
			"update alert_setting set service_news = ?, ad_push_consent_status = ? where user_id = ?",
			serviceNews, adPushConsentStatus, user.getId());
		return user;
	}

	/** 동의·철회는 앱 서버가 쓰므로, 관리자 서버 밖에서 쓴 값처럼 DB에 직접 넣는다 */
	private void changeAdPushConsent(User user, String adPushConsentStatus) {
		jdbcTemplate.update("update alert_setting set ad_push_consent_status = ? where user_id = ?",
			adPushConsentStatus, user.getId());
	}

	private String pushStatusOf(Long noticeId) {
		return jdbcTemplate.queryForObject("select push_status from notice where id = ?", String.class, noticeId);
	}

	private LocalDateTime pushSentAtOf(Long noticeId) {
		return jdbcTemplate.queryForObject("select push_sent_at from notice where id = ?", Timestamp.class, noticeId)
			.toLocalDateTime();
	}

	@SuppressWarnings("unchecked")
	private List<SendFirebaseServiceRequest> onlySentRequests() {
		ArgumentCaptor<List<SendFirebaseServiceRequest>> captor = ArgumentCaptor.forClass(List.class);
		verify(expoNotiService, times(1)).sendBatchPushNotification(captor.capture());
		return captor.getValue();
	}

	private List<String> tokens(List<SendFirebaseServiceRequest> requests) {
		return requests.stream().map(request -> request.getPush().getPushToken()).toList();
	}
}
