package docs.inquiry;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import com.quitmate.inquiry.inquryQuestion.controller.InquiryQuestionController;
import com.quitmate.inquiry.inquryQuestion.enums.InquiryStatus;
import com.quitmate.inquiry.inquryQuestion.service.InquiryQuestionReadService;
import com.quitmate.inquiry.inquryQuestion.service.response.InquiryQuestionDetailResponse;
import com.quitmate.inquiry.inquryQuestion.service.response.InquiryQuestionListResponse;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class InquiryQuestionControllerDocsTest extends RestDocsSupport {

    private final InquiryQuestionReadService inquiryQuestionReadService = mock(InquiryQuestionReadService.class);

    @Override
    protected Object initController() {
        return new InquiryQuestionController(inquiryQuestionReadService);
    }

    @DisplayName("문의사항 목록 조회 API")
    @Test
    void 문의사항_목록_조회_API() throws Exception {
        // given
        InquiryQuestionListResponse inquiry1 = InquiryQuestionListResponse.builder()
                .id(1L)
                .writerName("사용자1")
                .title("문의사항 제목1")
                .createdDate(LocalDateTime.of(2024, 1, 1, 10, 0))
                .status(InquiryStatus.WAITING)
                .build();

        InquiryQuestionListResponse inquiry2 = InquiryQuestionListResponse.builder()
                .id(2L)
                .writerName("사용자2")
                .title("문의사항 제목2")
                .createdDate(LocalDateTime.of(2024, 1, 2, 10, 0))
                .status(InquiryStatus.DONE)
                .build();

        PageableCustom pageableCustom = PageableCustom.builder()
                .currentPage(1)
                .totalPage(1)
                .totalElement(2L)
                .build();

        PageCustom<InquiryQuestionListResponse> pageCustom = PageCustom.<InquiryQuestionListResponse>builder()
                .content(List.of(inquiry1, inquiry2))
                .pageInfo(pageableCustom)
                .build();

        given(inquiryQuestionReadService.getInquiryQuestionList(any()))
                .willReturn(pageCustom);

        // when // then
        mockMvc.perform(
                        get("/api/v1/inquiry-questions")
                                .param("page", "1")
                                .param("size", "10")
                                .param("sortBy", "CREATED_DATE")
                                .param("category", "TITLE")
                                .param("keyword", "문의")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("inquiry-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 1)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 10)").optional(),
                                parameterWithName("sortBy").description("정렬 기준 (CREATED_DATE, WRITER_NAME, STATUS) (선택)").optional(),
                                parameterWithName("category").description("검색 카테고리 (CREATED_DATE, WRITER_NAME, TITLE, STATUS) (선택)").optional(),
                                parameterWithName("keyword").description("검색어 (선택)").optional()
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER)
                                        .description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING)
                                        .description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("응답 데이터"),
                                fieldWithPath("data.content[]").type(JsonFieldType.ARRAY)
                                        .description("문의사항 목록"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER)
                                        .description("문의사항 ID"),
                                fieldWithPath("data.content[].writerName").type(JsonFieldType.STRING)
                                        .description("작성자명"),
                                fieldWithPath("data.content[].title").type(JsonFieldType.STRING)
                                        .description("제목"),
                                fieldWithPath("data.content[].createdDate").type(JsonFieldType.STRING)
                                        .description("작성일시"),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING)
                                        .description("상태: " + Arrays.toString(InquiryStatus.values())),
                                fieldWithPath("data.pageInfo").type(JsonFieldType.OBJECT)
                                        .description("페이징 정보"),
                                fieldWithPath("data.pageInfo.currentPage").type(JsonFieldType.NUMBER)
                                        .description("현재 페이지"),
                                fieldWithPath("data.pageInfo.totalPage").type(JsonFieldType.NUMBER)
                                        .description("전체 페이지 수"),
                                fieldWithPath("data.pageInfo.totalElement").type(JsonFieldType.NUMBER)
                                        .description("전체 요소 수")
                        )
                ));
    }

    @DisplayName("문의사항 상세 조회 API")
    @Test
    void 문의사항_상세_조회_API() throws Exception {
        // given
        InquiryQuestionDetailResponse.InquiryAnswerInfo answerInfo =
                InquiryQuestionDetailResponse.InquiryAnswerInfo.builder()
                        .answerId(1L)
                        .content("답변 내용입니다.")
                        .createdDate(LocalDateTime.of(2024, 1, 3, 10, 0))
                        .build();

        InquiryQuestionDetailResponse response = InquiryQuestionDetailResponse.builder()
                .id(1L)
                .writerName("사용자1")
                .title("문의사항 제목")
                .question("문의사항 내용입니다.")
                .createdDate(LocalDateTime.of(2024, 1, 1, 10, 0))
                .status(InquiryStatus.DONE)
                .answer(answerInfo)
                .build();

        given(inquiryQuestionReadService.getInquiryQuestionDetail(anyLong()))
                .willReturn(response);

        // when // then
        mockMvc.perform(
                        get("/api/v1/inquiry-questions/{id}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("inquiry-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("문의사항 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER)
                                        .description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING)
                                        .description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("응답 데이터"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER)
                                        .description("문의사항 ID"),
                                fieldWithPath("data.writerName").type(JsonFieldType.STRING)
                                        .description("작성자명"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING)
                                        .description("제목"),
                                fieldWithPath("data.question").type(JsonFieldType.STRING)
                                        .description("문의 내용"),
                                fieldWithPath("data.createdDate").type(JsonFieldType.STRING)
                                        .description("작성일시"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                        .description("상태: " + Arrays.toString(InquiryStatus.values())),
                                fieldWithPath("data.answer").type(JsonFieldType.OBJECT)
                                        .description("답변 정보").optional(),
                                fieldWithPath("data.answer.answerId").type(JsonFieldType.NUMBER)
                                        .description("답변 ID"),
                                fieldWithPath("data.answer.content").type(JsonFieldType.STRING)
                                        .description("답변 내용"),
                                fieldWithPath("data.answer.createdDate").type(JsonFieldType.STRING)
                                        .description("답변 작성일시")
                        )
                ));
    }
}
