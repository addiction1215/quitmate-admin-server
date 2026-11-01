package com.quitmate.faq.service;

import com.quitmate.IntegrationTestSupport;
import com.quitmate.faq.enums.FaqCategory;
import com.quitmate.faq.repository.FaqJpaRepository;
import com.quitmate.faq.service.request.FaqCreateServiceRequest;
import com.quitmate.faq.service.request.FaqListServiceRequest;
import com.quitmate.faq.service.response.FaqCreateResponse;
import com.quitmate.faq.service.response.FaqListResponse;
import com.quitmate.global.page.response.PageCustom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

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

	@DisplayName("흡연 기록 카테고리로 FAQ를 등록하고 그 카테고리로 조회한다.")
	@Test
	void createAndListSmokingRecordFaq() {
		// given
		faqService.createFaq(FaqCreateServiceRequest.builder()
			.category(FaqCategory.SMOKING_RECORD)
			.pinned(false)
			.sortOrder(10)
			.title("흡연 기록은 어떻게 하나요?")
			.description("홈에서 기록할 수 있습니다.")
			.build());
		faqService.createFaq(FaqCreateServiceRequest.builder()
			.category(FaqCategory.CHALLENGE)
			.pinned(false)
			.sortOrder(10)
			.title("챌린지는 어떻게 참여하나요?")
			.description("챌린지 탭에서 참여할 수 있습니다.")
			.build());

		FaqListServiceRequest request = FaqListServiceRequest.builder()
			.page(1)
			.size(10)
			.category(FaqCategory.SMOKING_RECORD)
			.build();

		// when
		PageCustom<FaqListResponse> response = faqService.getFaqList(request);

		// then
		assertThat(response.getContent())
			.extracting(FaqListResponse::getTitle, FaqListResponse::getCategory)
			.containsExactly(tuple("흡연 기록은 어떻게 하나요?", FaqCategory.SMOKING_RECORD));
	}
}
