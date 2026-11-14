package com.quitmate.notice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AdPushQuietHoursTest {

	@DisplayName("한국 시각 20:00 이상, 08:00 미만이 광고 푸시 보류 시간이다. 서버 시계가 UTC여도 한국 시각으로 판단한다.")
	@ParameterizedTest(name = "{0} → 보류 {1}")
	@CsvSource({
		"2026-11-12T19:59:59, false",
		"2026-11-12T20:00:00, true",
		"2026-11-12T20:59:59, true",
		"2026-11-12T21:00:00, true",
		"2026-11-12T23:59:59, true",
		"2026-11-13T00:00:00, true",
		"2026-11-13T03:00:00, true",
		"2026-11-13T07:59:59, true",
		"2026-11-13T08:00:00, false",
		"2026-11-13T12:00:00, false"
	})
	void isQuiet(LocalDateTime koreaTime, boolean expected) {
		// given
		AdPushQuietHours quietHours = new AdPushQuietHours(utcClockAt(koreaTime));

		// when // then
		assertThat(quietHours.now()).isEqualTo(koreaTime);
		assertThat(quietHours.isQuiet(quietHours.now())).isEqualTo(expected);
	}

	@DisplayName("보류 시간에 등록하면 다음 08:00에 보낸다. 20:00~자정은 다음 날, 자정~08:00은 그날 08:00이다. 보류 시간이 아니면 그 시각 그대로다.")
	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
		"2026-11-14T20:00:00, 2026-11-15T08:00:00",
		"2026-11-14T23:59:59, 2026-11-15T08:00:00",
		"2026-11-15T00:00:00, 2026-11-15T08:00:00",
		"2026-11-15T07:59:59, 2026-11-15T08:00:00",
		"2026-11-15T08:00:00, 2026-11-15T08:00:00",
		"2026-11-15T19:59:59, 2026-11-15T19:59:59",
		"2026-12-31T22:00:00, 2027-01-01T08:00:00"
	})
	void nextSendableAt(LocalDateTime koreaTime, LocalDateTime expected) {
		// given
		AdPushQuietHours quietHours = new AdPushQuietHours(utcClockAt(koreaTime));

		// when // then
		assertThat(quietHours.nextSendableAt(koreaTime)).isEqualTo(expected);
	}

	@DisplayName("보류 시작 시각은 상수 하나(20:00)이고, 안내 문구는 이 상수로 만든다.")
	@Test
	void label() {
		assertThat(AdPushQuietHours.START).isEqualTo(LocalTime.of(20, 0));
		assertThat(AdPushQuietHours.END).isEqualTo(LocalTime.of(8, 0));
		assertThat(AdPushQuietHours.label()).isEqualTo("20:00~08:00");
		assertThat(AdPushCopy.deferredPushReason(LocalDateTime.of(2026, 11, 15, 8, 0)))
			.isEqualTo("야간(20:00~08:00)에는 광고 푸시를 바로 보내지 않아요. 공지는 등록했고, 푸시는 11월 15일 08:00에 보내요.");
	}

	private Clock utcClockAt(LocalDateTime koreaTime) {
		return Clock.fixed(koreaTime.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneOffset.UTC);
	}
}
