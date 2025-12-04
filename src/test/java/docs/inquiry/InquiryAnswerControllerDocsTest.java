package docs.inquiry;

import com.quitmate.inquiry.inquiryAnswer.controller.InquiryAnswerController;
import com.quitmate.inquiry.inquiryAnswer.controller.request.InquiryAnswerCreateRequest;
import com.quitmate.inquiry.inquiryAnswer.service.InquiryAnswerService;
import com.quitmate.inquiry.inquiryAnswer.service.response.InquiryAnswerCreateResponse;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class InquiryAnswerControllerDocsTest extends RestDocsSupport {

    private final InquiryAnswerService inquiryAnswerService = mock(InquiryAnswerService.class);

    @Override
    protected Object initController() {
        return new InquiryAnswerController(inquiryAnswerService);
    }

    @DisplayName("문의사항 답변 등록 API")
    @Test
    void 문의사항_답변_등록_API() throws Exception {
        // given
        InquiryAnswerCreateRequest request = InquiryAnswerCreateRequest.builder()
                .inquiryQuestionId(1L)
                .content("답변 내용입니다.")
                .build();

        InquiryAnswerCreateResponse response = InquiryAnswerCreateResponse.builder()
                .answerId(1L)
                .inquiryQuestionId(1L)
                .content("답변 내용입니다.")
                .build();

        given(inquiryAnswerService.createInquiryAnswer(any()))
                .willReturn(response);

        // when // then
        mockMvc.perform(
                        post("/api/v1/inquiry-answer")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("inquiry-answer",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("inquiryQuestionId").type(JsonFieldType.NUMBER)
                                        .description("문의사항 ID"),
                                fieldWithPath("content").type(JsonFieldType.STRING)
                                        .description("답변 내용")
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
                                fieldWithPath("data.answerId").type(JsonFieldType.NUMBER)
                                        .description("답변 ID"),
                                fieldWithPath("data.inquiryQuestionId").type(JsonFieldType.NUMBER)
                                        .description("문의사항 ID"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING)
                                        .description("답변 내용")
                        )
                ));
    }
}
