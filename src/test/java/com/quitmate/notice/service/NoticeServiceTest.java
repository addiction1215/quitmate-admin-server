package com.quitmate.notice.service;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.alertSetting.repository.AlertSettingJpaRepository;
import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.repository.NoticeJpaRepository;
import com.quitmate.notice.service.request.NoticeCreateServiceRequest;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.user.users.entity.User;
import com.quitmate.user.users.entity.enums.SettingStatus;
import com.quitmate.user.users.entity.enums.SnsType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class NoticeServiceTest extends IntegrationTestSupport {

	@Autowired
	private NoticeService noticeService;

	@Autowired
	private NoticeJpaRepository noticeJpaRepository;

	@Autowired
	private AlertSettingJpaRepository alertSettingJpaRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void deleteNoticesAndAlertSettings() {
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
			.content("QuitMate 서비스 이용 안내")
			.build();

		// when
		NoticeCreateResponse response = noticeService.createNotice(request);

		// then
		assertThat(response.getContent()).isEqualTo("QuitMate 서비스 이용 안내");
		assertThat(noticeJpaRepository.findById(response.getId())).isPresent();
	}
}
