package docs.challenge;

import com.quitmate.challenge.challange.controller.ChallengeController;
import com.quitmate.challenge.challange.controller.request.ChallengeCreateRequest;
import com.quitmate.challenge.challange.controller.request.ChallengeSearchRequest;
import com.quitmate.challenge.challange.controller.request.ChallengeUpdateRequest;
import com.quitmate.challenge.challange.controller.request.SearchCategory;
import com.quitmate.challenge.challange.controller.request.SortType;
import com.quitmate.challenge.challange.service.ChallengeReadService;
import com.quitmate.challenge.challange.service.ChallengeService;
import com.quitmate.challenge.challange.service.response.ChallengeCreateResponse;
import com.quitmate.challenge.challange.service.response.ChallengeDetailResponse;
import com.quitmate.challenge.challange.service.response.ChallengeListResponse;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ChallengeControllerDocsTest extends RestDocsSupport {

    private final ChallengeReadService challengeReadService = mock(ChallengeReadService.class);
    private final ChallengeService challengeService = mock(ChallengeService.class);

    @Override
    protected Object initController() {
        return new ChallengeController(challengeReadService, challengeService);
    }

    @DisplayName("챌린지 목록 조회 API")
    @Test
    void 챌린지_목록_조회_API() throws Exception {
        // given
        ChallengeSearchRequest request = new ChallengeSearchRequest(
                SearchCategory.TITLE,
                "금연",
                SortType.ID
        );
        request.setPage(1);
        request.setSize(10);

        ChallengeListResponse challenge1 = ChallengeListResponse.builder()
                .challengeId(1L)
                .title("금연 챌린지")
                .badge("금연 뱃지")
                .reward(100)
                .build();

        ChallengeListResponse challenge2 = ChallengeListResponse.builder()
                .challengeId(2L)
                .title("건강 챌린지")
                .badge("건강 뱃지")
                .reward(200)
                .build();

        PageableCustom pageableCustom = PageableCustom.builder()
                .currentPage(1)
                .totalPage(1)
                .totalElement(2L)
                .build();

        PageCustom<ChallengeListResponse> pageCustom = PageCustom.<ChallengeListResponse>builder()
                .content(List.of(challenge1, challenge2))
                .pageInfo(pageableCustom)
                .build();

        given(challengeReadService.getChallengeList(request.toServiceRequest()))
                .willReturn(pageCustom);

        // when // then
        mockMvc.perform(
                        get("/api/v1/admin/challenge")
                                .param("page", "1")
                                .param("size", "10")
                                .param("sortType", "ID")
                                .param("category", "TITLE")
                                .param("keyword", "금연")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("challenge-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 1)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 10)").optional(),
                                parameterWithName("sortType").description("정렬 기준 (ID, USER)").optional(),
                                parameterWithName("category").description("검색 카테고리 (TITLE, BADGE, REWARD)").optional(),
                                parameterWithName("keyword").description("검색어").optional()
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
                                        .description("챌린지 목록"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER)
                                        .description("챌린지 ID"),
                                fieldWithPath("data.content[].title").type(JsonFieldType.STRING)
                                        .description("챌린지 제목"),
                                fieldWithPath("data.content[].badge").type(JsonFieldType.STRING)
                                        .description("뱃지명"),
                                fieldWithPath("data.content[].reward").type(JsonFieldType.NUMBER)
                                        .description("보상 포인트"),
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

    @DisplayName("챌린지 상세 조회 API")
    @Test
    void 챌린지_상세_조회_API() throws Exception {
        // given
        ChallengeDetailResponse.MissionResponse mission1 = ChallengeDetailResponse.MissionResponse.builder()
                .missionId(1L)
                .category(MissionCategoryStatus.LOCATION)
                .title("흡연 루트 피하기")
                .reward(50)
                .content("평소 흡연하던 장소를 피해 다른 루트로 이동하세요")
                .build();

        ChallengeDetailResponse.MissionResponse mission2 = ChallengeDetailResponse.MissionResponse.builder()
                .missionId(2L)
                .category(MissionCategoryStatus.REPLACE_ACTION)
                .title("대체 행동 실천")
                .reward(30)
                .content("담배 생각이 날 때 물 마시기")
                .build();

        ChallengeDetailResponse response = ChallengeDetailResponse.builder()
                .challengeId(1L)
                .title("금연 챌린지")
                .badge("금연 뱃지")
                .reward(100)
                .missions(List.of(mission1, mission2))
                .build();

        given(challengeReadService.getChallengeDetail(1L))
                .willReturn(response);

        // when // then
        mockMvc.perform(
                        get("/api/v1/admin/challenge/{id}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("challenge-detail",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("챌린지 ID")
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
                                fieldWithPath("data.challengeId").type(JsonFieldType.NUMBER)
                                        .description("챌린지 ID"),
                                fieldWithPath("data.title").type(JsonFieldType.STRING)
                                        .description("챌린지 제목"),
                                fieldWithPath("data.badge").type(JsonFieldType.STRING)
                                        .description("뱃지명"),
                                fieldWithPath("data.reward").type(JsonFieldType.NUMBER)
                                        .description("보상 포인트"),
                                fieldWithPath("data.missions[]").type(JsonFieldType.ARRAY)
                                        .description("미션 목록"),
                                fieldWithPath("data.missions[].missionId").type(JsonFieldType.NUMBER)
                                        .description("미션 ID"),
                                fieldWithPath("data.missions[].category").type(JsonFieldType.STRING)
                                        .description("미션 카테고리: " + Arrays.toString(MissionCategoryStatus.values())),
                                fieldWithPath("data.missions[].title").type(JsonFieldType.STRING)
                                        .description("미션 제목"),
                                fieldWithPath("data.missions[].reward").type(JsonFieldType.NUMBER)
                                        .description("미션 보상 포인트"),
                                fieldWithPath("data.missions[].content").type(JsonFieldType.STRING)
                                        .description("미션 내용")
                        )
                ));
    }

    @DisplayName("챌린지 생성 API")
    @Test
    void 챌린지_생성_API() throws Exception {
        // given
        ChallengeCreateRequest.MissionRequest mission1 = ChallengeCreateRequest.MissionRequest.builder()
                .category(MissionCategoryStatus.LOCATION)
                .title("흡연 루트 피하기")
                .reward(50)
                .content("평소 흡연하던 장소를 피해 다른 루트로 이동하세요")
                .build();

        ChallengeCreateRequest.MissionRequest mission2 = ChallengeCreateRequest.MissionRequest.builder()
                .category(MissionCategoryStatus.REPLACE_ACTION)
                .title("대체 행동 실천")
                .reward(30)
                .content("담배 생각이 날 때 물 마시기")
                .build();

        ChallengeCreateRequest request = ChallengeCreateRequest.builder()
                .title("금연 챌린지")
                .badge("금연 뱃지")
                .reward(100)
                .missions(List.of(mission1, mission2))
                .build();

        ChallengeCreateResponse response = ChallengeCreateResponse.builder()
                .challengeId(1L)
                .build();

        given(challengeService.createChallenge(any()))
                .willReturn(response);

        // when // then
        mockMvc.perform(
                        post("/api/v1/admin/challenge")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("challenge-create",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING)
                                        .description("챌린지 제목"),
                                fieldWithPath("badge").type(JsonFieldType.STRING)
                                        .description("뱃지명"),
                                fieldWithPath("reward").type(JsonFieldType.NUMBER)
                                        .description("보상 포인트"),
                                fieldWithPath("missions[]").type(JsonFieldType.ARRAY)
                                        .description("미션 목록"),
                                fieldWithPath("missions[].category").type(JsonFieldType.STRING)
                                        .description("미션 카테고리: " + Arrays.toString(MissionCategoryStatus.values())),
                                fieldWithPath("missions[].title").type(JsonFieldType.STRING)
                                        .description("미션 제목"),
                                fieldWithPath("missions[].reward").type(JsonFieldType.NUMBER)
                                        .description("미션 보상 포인트"),
                                fieldWithPath("missions[].content").type(JsonFieldType.STRING)
                                        .description("미션 내용")
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
                                fieldWithPath("data.challengeId").type(JsonFieldType.NUMBER)
                                        .description("생성된 챌린지 ID")
                        )
                ));
    }

    @DisplayName("챌린지 수정 API")
    @Test
    void 챌린지_수정_API() throws Exception {
        // given
        ChallengeUpdateRequest.MissionUpdateRequest mission1 = ChallengeUpdateRequest.MissionUpdateRequest.builder()
                .category(MissionCategoryStatus.LOCATION)
                .title("흡연 루트 피하기")
                .reward(50)
                .content("평소 흡연하던 장소를 피해 다른 루트로 이동하세요")
                .build();

        ChallengeUpdateRequest request = ChallengeUpdateRequest.builder()
                .title("금연 챌린지 수정")
                .badge("금연 뱃지 수정")
                .reward(150)
                .missions(List.of(mission1))
                .build();

        // when // then
        mockMvc.perform(
                        put("/api/v1/admin/challenge/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("challenge-update",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("챌린지 ID")
                        ),
                        requestFields(
                                fieldWithPath("title").type(JsonFieldType.STRING)
                                        .description("챌린지 제목"),
                                fieldWithPath("badge").type(JsonFieldType.STRING)
                                        .description("뱃지명"),
                                fieldWithPath("reward").type(JsonFieldType.NUMBER)
                                        .description("보상 포인트"),
                                fieldWithPath("missions[]").type(JsonFieldType.ARRAY)
                                        .description("미션 목록"),
                                fieldWithPath("missions[].category").type(JsonFieldType.STRING)
                                        .description("미션 카테고리: " + Arrays.toString(MissionCategoryStatus.values())),
                                fieldWithPath("missions[].title").type(JsonFieldType.STRING)
                                        .description("미션 제목"),
                                fieldWithPath("missions[].reward").type(JsonFieldType.NUMBER)
                                        .description("미션 보상 포인트"),
                                fieldWithPath("missions[].content").type(JsonFieldType.STRING)
                                        .description("미션 내용")
                        )
                ));
    }

    @DisplayName("챌린지 삭제 API")
    @Test
    void 챌린지_삭제_API() throws Exception {
        // when // then
        mockMvc.perform(
                        delete("/api/v1/admin/challenge/{id}", 1L)
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("challenge-delete",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("챌린지 ID")
                        )
                ));
    }
}
