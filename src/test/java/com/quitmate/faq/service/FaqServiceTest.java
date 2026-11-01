package com.quitmate.faq.service;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.faq.enums.FaqCategory;
import com.quitmate.faq.repository.FaqJpaRepository;
import com.quitmate.faq.service.request.FaqCreateServiceRequest;
import com.quitmate.faq.service.response.FaqCreateResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class FaqServiceTest extends IntegrationTestSupport {

	@Autowired
	private FaqService faqService;

	@Autowired
	private FaqJpaRepository faqJpaRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void deleteFaqs() {
		faqJpaRepository.deleteAllInBatch();
	}

	@DisplayName("FAQ 상단 고정 여부는 앱 서버가 읽는 pinned 컬럼에 저장한다.")
	@Test
	void createFaqSavesPinnedColumn() {
		// given
		FaqCreateServiceRequest request = FaqCreateServiceRequest.builder()
			.category(FaqCategory.INFO_FRIEND)
			.pinned(true)
			.sortOrder(1)
			.title("QuitMate는 어떤 앱인가요?")
			.description("금연을 돕는 앱입니다.")
			.build();

		// when
		FaqCreateResponse response = faqService.createFaq(request);

		// then
		Boolean pinned = jdbcTemplate.queryForObject(
			"select pinned from faq where id = ?", Boolean.class, response.getId());
		assertThat(pinned).isTrue();
	}
}
