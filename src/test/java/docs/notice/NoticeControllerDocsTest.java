package docs.notice;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import com.quitmate.notice.controller.NoticeController;
import com.quitmate.notice.controller.request.NoticeCreateRequest;
import com.quitmate.notice.controller.request.NoticeUpdateRequest;
import com.quitmate.notice.enums.NoticePushStatus;
import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.service.NoticeReadService;
import com.quitmate.notice.service.NoticeService;
import com.quitmate.notice.service.impl.AdPushCopy;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.notice.service.response.NoticeListResponse;
import com.quitmate.notice.service.response.NoticeUpdateResponse;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class NoticeControllerDocsTest extends RestDocsSupport {

    private final NoticeReadService noticeReadService = mock(NoticeReadService.class);
    private final NoticeService noticeService = mock(NoticeService.class);

    @Override
    protected Object initController() {
        return new NoticeController(noticeReadService, noticeService);
    }

    @DisplayName("공지사항 목록 조회 API")
    @Test
    void 공지사항_목록_조회_API() throws Exception {
        // given
        NoticeListResponse notice1 = NoticeListResponse.builder()
                .id(1L)
                .type(NoticeType.FEATURE_UPDATE)
                .title("1.2.0 업데이트 안내")
                .content("첫 번째 공지사항입니다.")
                .createdDate(LocalDateTime.of(2024, 1, 1, 10, 0))
                .pushStatus(NoticePushStatus.SENT)
                .pushSentAt(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();

        NoticeListResponse notice2 = NoticeListResponse.builder()
                .id(2L)
                .type(NoticeType.EVENT)
                .title("금연 챌린지 이벤트")
                .content("이벤트 공지사항입니다.")
                .createdDate(LocalDateTime.of(2024, 1, 2, 22, 30))
                .pushStatus(NoticePushStatus.DEFERRED)
                .pushScheduledAt(LocalDateTime.of(2024, 1, 3, 8, 0))
                .build();

        PageableCustom pageableCustom = PageableCustom.builder()
                .currentPage(1)
                .totalPage(1)
                .totalElement(2L)
                .build();

        PageCustom<NoticeListResponse> pageCustom = PageCustom.<NoticeListResponse>builder()
                .content(List.of(notice1, notice2))
                .pageInfo(pageableCustom)
                .build();

        given(noticeReadService.getNoticeList(any()))
                .willReturn(pageCustom);

        // when // then
        mockMvc.perform(
                        get("/api/v1/notice")
                                .param("page", "1")
                                .param("size", "10")
                                .param("sortBy", "CREATED_DATE")
                                .param("category", "CONTENT")
                                .param("keyword", "공지")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("notice-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 1)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 10)").optional(),
                                parameterWithName("sortBy").description("정렬 기준 (CREATED_DATE, TYPE) (선택)").optional(),
                                parameterWithName("category").description("검색 카테고리 (CREATED_DATE, TYPE, TITLE, CONTENT) (선택)").optional(),
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
                                        .description("공지사항 목록"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER)
                                        .description("공지사항 ID"),
                                fieldWithPath("data.content[].type").type(JsonFieldType.STRING)
                                        .description("유형: " + Arrays.toString(NoticeType.values())),
                                fieldWithPath("data.content[].title").type(JsonFieldType.STRING)
                                        .description("제목"),
                                fieldWithPath("data.content[].content").type(JsonFieldType.STRING)
                                        .description("내용"),
                                fieldWithPath("data.content[].createdDate").type(JsonFieldType.STRING)
                                        .description("작성일시"),
                                fieldWithPath("data.content[].pushStatus").type(JsonFieldType.STRING).optional()
                                        .description("푸시 발송 상태: " + Arrays.toString(NoticePushStatus.values())
                                                + ". DEFERRED 발송 예정(야간 이벤트 공지), SENDING 발송 중, SENT 발송함, "
                                                + "FAILED 발송 실패(다시 보내지 않음), CANCELED 보내기 전 삭제. 이 칸이 생기기 전에 등록한 공지는 null"),
                                fieldWithPath("data.content[].pushScheduledAt").type(JsonFieldType.STRING).optional()
                                        .description("보류한 푸시를 보낼 시각(한국 시각). 야간에 등록한 이벤트 공지만, 아니면 null"),
                                fieldWithPath("data.content[].pushSentAt").type(JsonFieldType.STRING).optional()
                                        .description("푸시를 보내기 시작한 시각(한국 시각). SENT·SENDING·FAILED 일 때, 아니면 null"),
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

    @DisplayName("공지사항 생성 API")
    @Test
    void 공지사항_생성_API() throws Exception {
        // given
        NoticeCreateRequest request = NoticeCreateRequest.builder()
                .type(NoticeType.EVENT)
                .title("금연 챌린지 이벤트")
                .content("이벤트 공지사항입니다.")
                .build();

        // 야간(20:00~08:00)에 등록한 이벤트 공지: 푸시를 다음 08:00으로 보류했다.
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 11, 15, 8, 0);
        NoticeCreateResponse response = NoticeCreateResponse.builder()
                .id(1L)
                .type(NoticeType.EVENT)
                .title("금연 챌린지 이벤트")
                .content("이벤트 공지사항입니다.")
                .pushSkipReason(AdPushCopy.deferredPushReason(scheduledAt))
                .pushStatus(NoticePushStatus.DEFERRED)
                .pushScheduledAt(scheduledAt)
                .build();

        given(noticeService.createNotice(any()))
                .willReturn(response);

        // when // then
        mockMvc.perform(
                        post("/api/v1/notice")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("notice-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("type").type(JsonFieldType.STRING)
                                        .description("유형: " + Arrays.toString(NoticeType.values())),
                                fieldWithPath("title").type(JsonFieldType.STRING)
                                        .description("제목 (100자 이하)"),
                                fieldWithPath("content").type(JsonFieldType.STRING)
                                        .description("내용")
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
                                        .description("공지사항 ID"),
                                fieldWithPath("data.type").type(JsonFieldType.STRING)
                                        .description("유형"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING)
                                        .description("제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING)
                                        .description("내용"),
                                fieldWithPath("data.pushSkipReason").type(JsonFieldType.STRING).optional()
                                        .description("공지는 등록했지만 푸시를 바로 보내지 않은 까닭(관리자 화면에 그대로 띄울 수 있는 문장). "
                                                + "야간(20:00~08:00)에 등록한 이벤트 공지에만 채운다(푸시는 pushScheduledAt 에 보냄). 바로 보냈으면 null"),
                                fieldWithPath("data.pushStatus").type(JsonFieldType.STRING)
                                        .description("푸시 발송 상태. 바로 보냈으면 SENT, 보류했으면 DEFERRED"),
                                fieldWithPath("data.pushScheduledAt").type(JsonFieldType.STRING).optional()
                                        .description("보류한 푸시를 보낼 시각(한국 시각, 다음 08:00). 바로 보냈으면 null")
                        )
                ));
    }

    @DisplayName("공지사항 수정 API")
    @Test
    void 공지사항_수정_API() throws Exception {
        // given
        NoticeUpdateRequest request = NoticeUpdateRequest.builder()
                .id(1L)
                .type(NoticeType.FEATURE_UPDATE)
                .title("공지 제목")
                .content("수정된 공지사항입니다.")
                .build();

        NoticeUpdateResponse response = NoticeUpdateResponse.builder()
                .id(1L)
                .type(NoticeType.FEATURE_UPDATE)
                .title("공지 제목")
                .content("수정된 공지사항입니다.")
                .build();

        given(noticeService.updateNotice(any()))
                .willReturn(response);

        // when // then
        mockMvc.perform(
                        patch("/api/v1/notice")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("notice-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("id").type(JsonFieldType.NUMBER)
                                        .description("공지사항 ID"),
                                fieldWithPath("type").type(JsonFieldType.STRING)
                                        .description("유형: " + Arrays.toString(NoticeType.values())),
                                fieldWithPath("title").type(JsonFieldType.STRING)
                                        .description("제목 (100자 이하)"),
                                fieldWithPath("content").type(JsonFieldType.STRING)
                                        .description("내용")
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
                                        .description("공지사항 ID"),
                                fieldWithPath("data.type").type(JsonFieldType.STRING)
                                        .description("유형"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING)
                                        .description("제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING)
                                        .description("내용")
                        )
                ));
    }

    @DisplayName("공지사항 삭제 API")
    @Test
    void 공지사항_삭제_API() throws Exception {
        doNothing().when(noticeService).deleteNotice(any());

        mockMvc.perform(
                        delete("/api/v1/notice/{id}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("notice-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("공지사항 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER)
                                        .description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING)
                                        .description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING)
                                        .description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.NULL)
                                        .description("응답 데이터 (없음)")
                        )
                ));
    }
}
