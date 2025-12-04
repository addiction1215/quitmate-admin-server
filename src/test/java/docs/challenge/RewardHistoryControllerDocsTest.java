package docs.challenge;

import com.quitmate.challenge.rewardHistory.controller.RewardHistoryController;
import com.quitmate.challenge.rewardHistory.enums.RewardType;
import com.quitmate.challenge.rewardHistory.service.RewardHistoryReadService;
import com.quitmate.challenge.rewardHistory.service.response.RewardHistoryListResponse;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RewardHistoryControllerDocsTest extends RestDocsSupport {

    private final RewardHistoryReadService rewardHistoryReadService = mock(RewardHistoryReadService.class);

    @Override
    protected Object initController() {
        return new RewardHistoryController(rewardHistoryReadService);
    }

    @DisplayName("리워드 내역 목록 조회 API")
    @Test
    void 리워드_내역_목록_조회_API() throws Exception {
        // given
        RewardHistoryListResponse history1 = RewardHistoryListResponse.builder()
                .id(1L)
                .createdDate(LocalDateTime.of(2024, 1, 1, 10, 0))
                .userName("사용자1")
                .type(RewardType.ACQUIRE)
                .point(100)
                .remainingPoint(1000)
                .build();

        RewardHistoryListResponse history2 = RewardHistoryListResponse.builder()
                .id(2L)
                .createdDate(LocalDateTime.of(2024, 1, 2, 10, 0))
                .userName("사용자2")
                .type(RewardType.USED)
                .point(-50)
                .remainingPoint(950)
                .build();

        PageableCustom pageableCustom = PageableCustom.builder()
                .currentPage(1)
                .totalPage(1)
                .totalElement(2L)
                .build();

        PageCustom<RewardHistoryListResponse> pageCustom = PageCustom.<RewardHistoryListResponse>builder()
                .content(List.of(history1, history2))
                .pageInfo(pageableCustom)
                .build();

        given(rewardHistoryReadService.getRewardHistoryList(any()))
                .willReturn(pageCustom);

        // when // then
        mockMvc.perform(
                        get("/api/v1/reward-histories")
                                .param("page", "1")
                                .param("size", "10")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("reward-history-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 1)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 10)").optional()
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
                                        .description("리워드 내역 목록"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER)
                                        .description("내역 ID"),
                                fieldWithPath("data.content[].createdDate").type(JsonFieldType.STRING)
                                        .description("발생일자"),
                                fieldWithPath("data.content[].userName").type(JsonFieldType.STRING)
                                        .description("유저명"),
                                fieldWithPath("data.content[].type").type(JsonFieldType.STRING)
                                        .description("유형: " + Arrays.toString(RewardType.values())),
                                fieldWithPath("data.content[].point").type(JsonFieldType.NUMBER)
                                        .description("포인트 증감량"),
                                fieldWithPath("data.content[].remainingPoint").type(JsonFieldType.NUMBER)
                                        .description("남은 포인트"),
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
}
