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

class AlertHistoryWeeklyReportTest extends IntegrationTestSupport {

	@Autowired
	private AlertHistoryRepository alertHistoryRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void deleteAlertHistories() {
		alertHistoryRepository.deleteAllInBatch();
	}

	@DisplayName("앱 서버가 남긴 주간 리포트 알림 이력(WEEKLY_REPORT)을 읽는다.")
	@Test
	void readWeeklyReportAlertHistory() {
		// given: 앱 서버가 쓴 값처럼 DB에 직접 넣는다.
		jdbcTemplate.update("""
			insert into alert_history (alert_description, alert_history_status, alert_destination_type, alert_destination_info)
			values ('지난주 흡연 패턴 리포트가 나왔어요.', 'UNCHECKED', 'WEEKLY_REPORT', '2026-11-02')
			""");
		Long id = jdbcTemplate.queryForObject("select max(id) from alert_history", Long.class);

		// when
		AlertHistory alertHistory = alertHistoryRepository.findById(id).orElseThrow();

		// then
		assertThat(alertHistory.getAlertDestinationType()).isEqualTo(AlertDestinationType.WEEKLY_REPORT);
		assertThat(alertHistory.getAlertDestinationType().getText()).isEqualTo("주간 흡연 패턴 리포트");
		assertThat(alertHistory.getAlertDestinationInfo()).isEqualTo("2026-11-02");
	}

	@DisplayName("모든 알림 유형에 푸시 제목이 있다. 주간 리포트는 기본 제목을 쓴다.")
	@Test
	void everyAlertTypeHasPushTitle() {
		assertThat(Arrays.stream(AlertDestinationType.values()).map(PushMessage::from)).doesNotContainNull();
		assertThat(PushMessage.from(AlertDestinationType.WEEKLY_REPORT)).isEqualTo(PushMessage.DEFAULT);
	}
}
