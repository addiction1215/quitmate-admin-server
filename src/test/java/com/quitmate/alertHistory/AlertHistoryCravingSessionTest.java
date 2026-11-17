package com.quitmate.alertHistory;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.alertHistory.entity.AlertDestinationType;
import com.quitmate.alertHistory.entity.AlertHistory;
import com.quitmate.alertHistory.repository.AlertHistoryRepository;
import com.quitmate.firebase.enums.PushMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class AlertHistoryCravingSessionTest extends IntegrationTestSupport {

	@Autowired
	private AlertHistoryRepository alertHistoryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void deleteAlertHistories() {
		alertHistoryRepository.deleteAllInBatch();
	}

	@DisplayName("앱 서버가 남긴 다음 고비 알림 이력(CRAVING_SESSION)을 읽는다.")
	@Test
	void readCravingSessionAlertHistory() {
		// given: 앱 서버가 쓴 값처럼 DB에 직접 넣는다.
		jdbcTemplate.update("""
			insert into alert_history (alert_description, alert_history_status, alert_destination_type, alert_destination_info)
			values ('곧 다음 고비가 올 시간이에요.', 'UNCHECKED', 'CRAVING_SESSION', '2026-11-17')
			""");
		Long id = jdbcTemplate.queryForObject("select max(id) from alert_history", Long.class);

		// when
		AlertHistory alertHistory = alertHistoryRepository.findById(id).orElseThrow();

		// then
		assertThat(alertHistory.getAlertDestinationType()).isEqualTo(AlertDestinationType.CRAVING_SESSION);
		assertThat(alertHistory.getAlertDestinationType().getText()).isEqualTo("다음 고비 알림");
		assertThat(alertHistory.getAlertDestinationInfo()).isEqualTo("2026-11-17");
	}

	@DisplayName("모든 알림 유형에 푸시 제목이 있다. 다음 고비 알림은 기본 제목을 쓴다.")
	@Test
	void everyAlertTypeHasPushTitle() {
		assertThat(Arrays.stream(AlertDestinationType.values()).map(PushMessage::from)).doesNotContainNull();
		assertThat(PushMessage.from(AlertDestinationType.CRAVING_SESSION)).isEqualTo(PushMessage.DEFAULT);
	}
}
