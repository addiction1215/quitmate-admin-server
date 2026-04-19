package docs.faq;

import com.quitmate.faq.controller.FaqController;
import com.quitmate.faq.controller.request.FaqCreateRequest;
import com.quitmate.faq.controller.request.FaqUpdateRequest;
import com.quitmate.faq.service.FaqService;
import com.quitmate.faq.service.response.FaqCreateResponse;
import com.quitmate.faq.service.response.FaqListResponse;
import com.quitmate.faq.service.response.FaqUpdateResponse;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FaqControllerDocsTest extends RestDocsSupport {

    private final FaqService faqService = mock(FaqService.class);

    @Override
    protected Object initController() {
        return new FaqController(faqService);
    }

    @DisplayName("FAQ 등록 API")
    @Test
    void FAQ_등록_API() throws Exception {
        FaqCreateRequest request = FaqCreateRequest.builder()
                .title("자주 묻는 질문입니다.")
                .description("자주 묻는 질문에 대한 답변입니다.")
                .build();

        FaqCreateResponse response = FaqCreateResponse.builder()
                .id(1L)
                .title("자주 묻는 질문입니다.")
                .description("자주 묻는 질문에 대한 답변입니다.")
                .build();

        given(faqService.createFaq(any())).willReturn(response);

        mockMvc.perform(
                        post("/api/v1/faq")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("faq-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING).description("FAQ 제목"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("FAQ 내용")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("FAQ ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("FAQ 제목"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("FAQ 내용")
                        )
                ));
    }

    @DisplayName("FAQ 수정 API")
    @Test
    void FAQ_수정_API() throws Exception {
        FaqUpdateRequest request = FaqUpdateRequest.builder()
                .id(1L)
                .title("수정된 질문입니다.")
                .description("수정된 답변입니다.")
                .build();

        FaqUpdateResponse response = FaqUpdateResponse.builder()
                .id(1L)
                .title("수정된 질문입니다.")
                .description("수정된 답변입니다.")
                .build();

        given(faqService.updateFaq(any())).willReturn(response);

        mockMvc.perform(
                        patch("/api/v1/faq")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("faq-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("id").type(JsonFieldType.NUMBER).description("FAQ ID"),
                                fieldWithPath("title").type(JsonFieldType.STRING).description("FAQ 제목"),
                                fieldWithPath("description").type(JsonFieldType.STRING).description("FAQ 내용")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("FAQ ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("FAQ 제목"),
                                fieldWithPath("data.description").type(JsonFieldType.STRING).description("FAQ 내용")
                        )
                ));
    }

    @DisplayName("FAQ 삭제 API")
    @Test
    void FAQ_삭제_API() throws Exception {
        doNothing().when(faqService).deleteFaq(any());

        mockMvc.perform(
                        delete("/api/v1/faq/{id}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("faq-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("FAQ ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.NULL).description("응답 데이터 (없음)")
                        )
                ));
    }

    @DisplayName("FAQ 리스트 조회 API")
    @Test
    void FAQ_리스트_조회_API() throws Exception {
        FaqListResponse item = FaqListResponse.builder()
                .id(1L)
                .title("자주 묻는 질문입니다.")
                .description("자주 묻는 질문에 대한 답변입니다.")
                .build();

        PageCustom<FaqListResponse> pageResponse = PageCustom.of(
                new PageImpl<>(List.of(item), org.springframework.data.domain.PageRequest.of(0, 12), 1)
        );

        given(faqService.getFaqList(any())).willReturn(pageResponse);

        mockMvc.perform(
                        get("/api/v1/faq")
                                .param("page", "1")
                                .param("size", "12")
                                .param("keyword", "자주")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("faq-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 1)"),
                                parameterWithName("size").description("페이지 크기 (기본값: 12)"),
                                parameterWithName("keyword").description("제목 검색어 (선택)")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("FAQ 목록"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("FAQ ID"),
                                fieldWithPath("data.content[].title").type(JsonFieldType.STRING).description("FAQ 제목"),
                                fieldWithPath("data.content[].description").type(JsonFieldType.STRING).description("FAQ 내용"),
                                fieldWithPath("data.pageInfo").type(JsonFieldType.OBJECT).description("페이징 정보"),
                                fieldWithPath("data.pageInfo.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지"),
                                fieldWithPath("data.pageInfo.totalPage").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                fieldWithPath("data.pageInfo.totalElement").type(JsonFieldType.NUMBER).description("전체 데이터 수")
                        )
                ));
    }
}
