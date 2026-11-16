package docs.notice;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import com.quitmate.notice.controller.NoticeController;
import com.quitmate.notice.controller.request.NoticeCreateRequest;
import com.quitmate.notice.controller.request.NoticeUpdateRequest;
import com.quitmate.notice.enums.NoticeType;
import com.quitmate.notice.service.NoticeReadService;
import com.quitmate.notice.service.NoticeService;
import com.quitmate.notice.service.response.NoticeCreateResponse;
import com.quitmate.notice.service.response.NoticeDetailResponse;
import com.quitmate.notice.service.response.NoticeImageUploadResponse;
import com.quitmate.notice.service.response.NoticeListResponse;
import com.quitmate.notice.service.response.NoticeUpdateResponse;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
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
                .build();

        NoticeListResponse notice2 = NoticeListResponse.builder()
                .id(2L)
                .type(NoticeType.EVENT)
                .title("금연 챌린지 이벤트")
                .content("이벤트 공지사항입니다.")
                .createdDate(LocalDateTime.of(2024, 1, 2, 10, 0))
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
                .type(NoticeType.FEATURE_UPDATE)
                .title("공지 제목")
                .content("새로운 공지사항입니다.")
                .contentHtml("<p>새로운 공지사항입니다.</p>")
                .build();

        NoticeCreateResponse response = NoticeCreateResponse.builder()
                .id(1L)
                .type(NoticeType.FEATURE_UPDATE)
                .title("공지 제목")
                .content("새로운 공지사항입니다.")
                .contentHtml("<p>새로운 공지사항입니다.</p>")
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
                                fieldWithPath("content").type(JsonFieldType.STRING).optional()
                                        .description("글자 본문. contentHtml 이 없을 때 필수(옛 방식). contentHtml 을 보내면 서버가 글자 버전으로 덮어쓴다"),
                                fieldWithPath("contentHtml").type(JsonFieldType.STRING).optional()
                                        .description("서식 본문(선택). 허용 태그만 남기고 정리해 저장한다. 이미지는 <img data-key=\"notice/…\"> 로 쓴다. "
                                                + "최대 200,000자. 수정 시 생략하면 기존 값 유지, 빈 문자열이면 서식 해제")
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
                                        .description("내용(글자 버전)"),
                                fieldWithPath("data.contentHtml").type(JsonFieldType.STRING).optional()
                                        .description("정리되어 저장된 서식 본문(img 는 data-key 만, src 없음). 서식 없으면 null"),
                                fieldWithPath("data.pushSkipReason").type(JsonFieldType.STRING).optional()
                                        .description("공지는 등록했지만 푸시를 보내지 않은 까닭(관리자 화면에 그대로 띄울 수 있는 문장). "
                                                + "야간(21:00~08:00)에 등록한 이벤트 공지에만 채운다. 푸시를 보냈으면 null")
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
                .contentHtml("<p>수정된 공지사항입니다.</p>")
                .build();

        NoticeUpdateResponse response = NoticeUpdateResponse.builder()
                .id(1L)
                .type(NoticeType.FEATURE_UPDATE)
                .title("공지 제목")
                .content("수정된 공지사항입니다.")
                .contentHtml("<p>수정된 공지사항입니다.</p>")
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
                                fieldWithPath("content").type(JsonFieldType.STRING).optional()
                                        .description("글자 본문. contentHtml 이 없을 때 필수(옛 방식). contentHtml 을 보내면 서버가 글자 버전으로 덮어쓴다"),
                                fieldWithPath("contentHtml").type(JsonFieldType.STRING).optional()
                                        .description("서식 본문(선택). 허용 태그만 남기고 정리해 저장한다. 이미지는 <img data-key=\"notice/…\"> 로 쓴다. "
                                                + "최대 200,000자. 수정 시 생략하면 기존 값 유지, 빈 문자열이면 서식 해제")
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
                                        .description("내용(글자 버전)"),
                                fieldWithPath("data.contentHtml").type(JsonFieldType.STRING).optional()
                                        .description("정리되어 저장된 서식 본문(img 는 data-key 만, src 없음). 서식 없으면 null")
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

    @DisplayName("공지사항 상세 조회 API")
    @Test
    void 공지사항_상세_조회_API() throws Exception {
        NoticeDetailResponse response = NoticeDetailResponse.builder()
                .id(1L)
                .type(NoticeType.FEATURE_UPDATE)
                .title("1.2.0 업데이트 안내")
                .content("새 기능\n• 다크 모드")
                .contentHtml("<p>새 기능</p><ul><li>다크 모드</li></ul>"
                        + "<img data-key=\"notice/uuid.png\" src=\"https://s3.example/notice/uuid.png?X-Amz-Signature=…\">")
                .createdDate(LocalDateTime.of(2024, 1, 1, 10, 0))
                .build();
        given(noticeReadService.getNotice(any())).willReturn(response);

        mockMvc.perform(get("/api/v1/notice/{id}", 1L))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("notice-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("공지사항 ID")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("공지사항 ID"),
                                fieldWithPath("data.type").type(JsonFieldType.STRING)
                                        .description("유형: " + Arrays.toString(NoticeType.values())),
                                fieldWithPath("data.title").type(JsonFieldType.STRING).description("제목"),
                                fieldWithPath("data.content").type(JsonFieldType.STRING).description("내용(글자 버전)"),
                                fieldWithPath("data.contentHtml").type(JsonFieldType.STRING).optional()
                                        .description("서식 본문. 이미지는 data-key(저장 값)와 1시간 서명 URL src(편집·미리보기용)가 함께 온다. "
                                                + "다시 저장할 때 src 는 서버가 지운다. 서식 없는 공지는 null"),
                                fieldWithPath("data.createdDate").type(JsonFieldType.STRING).description("작성일시")
                        )
                ));
    }

    @DisplayName("공지사항 이미지 업로드 API")
    @Test
    void 공지사항_이미지_업로드_API() throws Exception {
        NoticeImageUploadResponse response = NoticeImageUploadResponse.builder()
                .key("notice/3f2c1a9e-0000-4000-8000-000000000000.png")
                .url("https://s3.example/notice/3f2c1a9e-0000-4000-8000-000000000000.png?X-Amz-Signature=…")
                .build();
        given(noticeService.uploadNoticeImage(any())).willReturn(response);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", "img".getBytes());

        mockMvc.perform(multipart("/api/v1/notice/image").file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("notice-image-upload",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestParts(partWithName("file").description("이미지 파일(JPEG, PNG, GIF, WebP)")),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.key").type(JsonFieldType.STRING)
                                        .description("S3 파일 키(notice/ 로 시작). 본문 img 의 data-key 에 넣는다"),
                                fieldWithPath("data.url").type(JsonFieldType.STRING)
                                        .description("미리보기용 1시간 서명 URL. 에디터 img src 로 쓴다(저장 시 서버가 지움)")
                        )
                ));
    }
}
