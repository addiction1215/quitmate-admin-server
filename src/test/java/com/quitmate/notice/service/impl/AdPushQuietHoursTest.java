package com.quitmate.notice.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AdPushQuietHoursTest {

	@DisplayName("한국 시각 21:00 이상, 08:00 미만이 광고 푸시 야간이다. 서버 시계가 UTC여도 한국 시각으로 판단한다.")
	@ParameterizedTest(name = "{0} → 야간 {1}")
	@CsvSource({
		"2026-11-12T20:59:59, false",
		"2026-11-12T21:00:00, true",
		"2026-11-12T23:59:59, true",
		"2026-11-13T00:00:00, true",
		"2026-11-13T03:00:00, true",
		"2026-11-13T07:59:59, true",
		"2026-11-13T08:00:00, false",
		"2026-11-13T12:00:00, false"
	})
	void isQuietNow(LocalDateTime koreaTime, boolean expected) {
		// given
		Clock utcClock = Clock.fixed(koreaTime.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneOffset.UTC);
		AdPushQuietHours quietHours = new AdPushQuietHours(utcClock);

		// when // then
		assertThat(quietHours.isQuietNow()).isEqualTo(expected);
	}
}
