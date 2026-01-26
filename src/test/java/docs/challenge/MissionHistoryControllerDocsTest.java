package docs.challenge;

import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import com.quitmate.challenge.missionhistory.controller.MissionHistoryController;
import com.quitmate.challenge.missionhistory.controller.request.MissionHistoryUpdateRequest;
import com.quitmate.challenge.missionhistory.entity.MissionStatus;
import com.quitmate.challenge.missionhistory.service.MissionHistoryReadService;
import com.quitmate.challenge.missionhistory.service.MissionHistoryService;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryDetailResponse;
import com.quitmate.challenge.missionhistory.service.response.MissionHistoryListResponse;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MissionHistoryControllerDocsTest extends RestDocsSupport {

    private final MissionHistoryReadService missionHistoryReadService = mock(MissionHistoryReadService.class);
    private final MissionHistoryService missionHistoryService = mock(MissionHistoryService.class);

    @Override
    protected Object initController() {
        return new MissionHistoryController(missionHistoryReadService, missionHistoryService);
    }

    @DisplayName("미션 히스토리 목록 조회 API")
    @Test
    void 미션_히스토리_목록_조회_API() throws Exception {
        // given
        MissionHistoryListResponse history1 = MissionHistoryListResponse.builder()
                .missionHistoryId(1L)
                .challengeTitle("금연 챌린지")
                .missionTitle("흡연 루트 피하기")
                .category(MissionCategoryStatus.LOCATION)
                .status(MissionStatus.READY)
                .userName("홍길동")
                .createdDate(LocalDateTime.of(2024, 1, 25, 10, 30))
                .build();

        MissionHistoryListResponse history2 = MissionHistoryListResponse.builder()
                .missionHistoryId(2L)
                .challengeTitle("건강 챌린지")
                .missionTitle("물 2L 마시기")
                .category(MissionCategoryStatus.REPLACE_ACTION)
                .status(MissionStatus.READY)
                .userName("김철수")
                .createdDate(LocalDateTime.of(2024, 1, 25, 11, 0))
                .build();

        PageableCustom pageableCustom = PageableCustom.builder()
                .currentPage(1)
                .totalPage(1)
                .totalElement(2L)
                .build();

        PageCustom<MissionHistoryListResponse> pageCustom = PageCustom.<MissionHistoryListResponse>builder()
                .content(List.of(history1, history2))
                .pageInfo(pageableCustom)
                .build();

        given(missionHistoryReadService.getMissionHistoryList(any()))
                .willReturn(pageCustom);

        // when // then
        mockMvc.perform(
                        get("/api/v1/admin/mission-history")
                                .param("page", "1")
                                .param("size", "10")
                                .param("sortBy", "CREATED_DATE")
                                .param("category", "CHALLENGE_TITLE")
                                .param("keyword", "금연")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("mission-history-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 1)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 10)").optional(),
                                parameterWithName("sortBy").description("정렬 기준 (CREATED_DATE, STATUS) (선택)").optional(),
                                parameterWithName("category").description("검색 카테고리 (CHALLENGE_TITLE, MISSION_TITLE, USER_NAME, STATUS) (선택)").optional(),
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
                                        .description("미션 히스토리 목록"),
                                fieldWithPath("data.content[].missionHistoryId").type(JsonFieldType.NUMBER)
                                        .description("미션 히스토리 ID"),
                                fieldWithPath("data.content[].challengeTitle").type(JsonFieldType.STRING)
                                        .description("챌린지 제목"),
                                fieldWithPath("data.content[].missionTitle").type(JsonFieldType.STRING)
                                        .description("미션 제목"),
                                fieldWithPath("data.content[].category").type(JsonFieldType.STRING)
                                        .description("미션 카테고리: " + Arrays.toString(MissionCategoryStatus.values())),
                                fieldWithPath("data.content[].status").type(JsonFieldType.STRING)
                                        .description("미션 상태: " + Arrays.toString(MissionStatus.values())),
                                fieldWithPath("data.content[].userName").type(JsonFieldType.STRING)
                                        .description("사용자명"),
                                fieldWithPath("data.content[].createdDate").type(JsonFieldType.STRING)
                                        .description("요청 일시"),
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

    @DisplayName("미션 히스토리 상세 조회 API - LOCATION 타입")
    @Test
    void 미션_히스토리_상세_조회_API_LOCATION() throws Exception {
        // given
        MissionHistoryDetailResponse.LocationVerificationData locationData =
                MissionHistoryDetailResponse.LocationVerificationData.of(5);

        MissionHistoryDetailResponse response = MissionHistoryDetailResponse.builder()
                .missionHistoryId(1L)
                .challengeTitle("금연 챌린지")
                .missionTitle("흡연 루트 피하기")
                .category(MissionCategoryStatus.LOCATION)
                .status(MissionStatus.READY)
                .userName("홍길동")
                .createdDate(LocalDateTime.of(2024, 1, 25, 10, 30))
                .locationData(locationData)
                .photoData(null)
                .abstinenceData(null)
                .build();

        given(missionHistoryReadService.getMissionHistoryDetail(anyLong()))
                .willReturn(response);

        // when // then
        mockMvc.perform(
                        get("/api/v1/admin/mission-history/{id}", 1L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("mission-history-detail-location",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("미션 히스토리 ID")
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
                                fieldWithPath("data.missionHistoryId").type(JsonFieldType.NUMBER)
                                        .description("미션 히스토리 ID"),
                                fieldWithPath("data.challengeTitle").type(JsonFieldType.STRING)
                                        .description("챌린지 제목"),
                                fieldWithPath("data.missionTitle").type(JsonFieldType.STRING)
                                        .description("미션 제목"),
                                fieldWithPath("data.category").type(JsonFieldType.STRING)
                                        .description("미션 카테고리: " + Arrays.toString(MissionCategoryStatus.values())),
                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                        .description("미션 상태: " + Arrays.toString(MissionStatus.values())),
                                fieldWithPath("data.userName").type(JsonFieldType.STRING)
                                        .description("사용자명"),
                                fieldWithPath("data.createdDate").type(JsonFieldType.STRING)
                                        .description("요청 일시"),
                                fieldWithPath("data.locationData").type(JsonFieldType.OBJECT)
                                        .description("위치 인증 데이터 (LOCATION 타입일 때만)").optional(),
                                fieldWithPath("data.locationData.gpsVerifyCount").type(JsonFieldType.NUMBER)
                                        .description("GPS 검증 횟수"),
                                fieldWithPath("data.photoData").type(JsonFieldType.NULL)
                                        .description("사진 인증 데이터 (REPLACE_ACTION 타입일 때만)").optional(),
                                fieldWithPath("data.abstinenceData").type(JsonFieldType.NULL)
                                        .description("금연 시간 데이터 (HOLD 타입일 때만)").optional()
                        )
                ));
    }

    @DisplayName("미션 히스토리 상세 조회 API - REPLACE_ACTION 타입")
    @Test
    void 미션_히스토리_상세_조회_API_REPLACE_ACTION() throws Exception {
        // given
        MissionHistoryDetailResponse.PhotoVerificationData photoData =
                MissionHistoryDetailResponse.PhotoVerificationData.of(
                        "https://example.com/photo1.jpg",
                        "https://example.com/photo2.jpg",
                        "https://example.com/photo3.jpg"
                );

        MissionHistoryDetailResponse response = MissionHistoryDetailResponse.builder()
                .missionHistoryId(2L)
                .challengeTitle("금연 챌린지")
                .missionTitle("대체 행동 실천")
                .category(MissionCategoryStatus.REPLACE_ACTION)
                .status(MissionStatus.READY)
                .userName("김철수")
                .createdDate(LocalDateTime.of(2024, 1, 25, 11, 0))
                .locationData(null)
                .photoData(photoData)
                .abstinenceData(null)
                .build();

        given(missionHistoryReadService.getMissionHistoryDetail(anyLong()))
                .willReturn(response);

        // when // then
        mockMvc.perform(
                        get("/api/v1/admin/mission-history/{id}", 2L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("mission-history-detail-photo",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("미션 히스토리 ID")
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
                                fieldWithPath("data.missionHistoryId").type(JsonFieldType.NUMBER)
                                        .description("미션 히스토리 ID"),
                                fieldWithPath("data.challengeTitle").type(JsonFieldType.STRING)
                                        .description("챌린지 제목"),
                                fieldWithPath("data.missionTitle").type(JsonFieldType.STRING)
                                        .description("미션 제목"),
                                fieldWithPath("data.category").type(JsonFieldType.STRING)
                                        .description("미션 카테고리: " + Arrays.toString(MissionCategoryStatus.values())),
                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                        .description("미션 상태: " + Arrays.toString(MissionStatus.values())),
                                fieldWithPath("data.userName").type(JsonFieldType.STRING)
                                        .description("사용자명"),
                                fieldWithPath("data.createdDate").type(JsonFieldType.STRING)
                                        .description("요청 일시"),
                                fieldWithPath("data.locationData").type(JsonFieldType.NULL)
                                        .description("위치 인증 데이터 (LOCATION 타입일 때만)").optional(),
                                fieldWithPath("data.photoData").type(JsonFieldType.OBJECT)
                                        .description("사진 인증 데이터 (REPLACE_ACTION 타입일 때만)").optional(),
                                fieldWithPath("data.photoData.photoUrl1").type(JsonFieldType.STRING)
                                        .description("사진 URL 1"),
                                fieldWithPath("data.photoData.photoUrl2").type(JsonFieldType.STRING)
                                        .description("사진 URL 2"),
                                fieldWithPath("data.photoData.photoUrl3").type(JsonFieldType.STRING)
                                        .description("사진 URL 3"),
                                fieldWithPath("data.abstinenceData").type(JsonFieldType.NULL)
                                        .description("금연 시간 데이터 (HOLD 타입일 때만)").optional()
                        )
                ));
    }

    @DisplayName("미션 히스토리 상세 조회 API - HOLD 타입")
    @Test
    void 미션_히스토리_상세_조회_API_HOLD() throws Exception {
        // given
        MissionHistoryDetailResponse.AbstinenceVerificationData abstinenceData =
                MissionHistoryDetailResponse.AbstinenceVerificationData.of(3600);

        MissionHistoryDetailResponse response = MissionHistoryDetailResponse.builder()
                .missionHistoryId(3L)
                .challengeTitle("금연 챌린지")
                .missionTitle("1시간 참기")
                .category(MissionCategoryStatus.HOLD)
                .status(MissionStatus.READY)
                .userName("이영희")
                .createdDate(LocalDateTime.of(2024, 1, 25, 12, 0))
                .locationData(null)
                .photoData(null)
                .abstinenceData(abstinenceData)
                .build();

        given(missionHistoryReadService.getMissionHistoryDetail(anyLong()))
                .willReturn(response);

        // when // then
        mockMvc.perform(
                        get("/api/v1/admin/mission-history/{id}", 3L)
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("mission-history-detail-abstinence",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("미션 히스토리 ID")
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
                                fieldWithPath("data.missionHistoryId").type(JsonFieldType.NUMBER)
                                        .description("미션 히스토리 ID"),
                                fieldWithPath("data.challengeTitle").type(JsonFieldType.STRING)
                                        .description("챌린지 제목"),
                                fieldWithPath("data.missionTitle").type(JsonFieldType.STRING)
                                        .description("미션 제목"),
                                fieldWithPath("data.category").type(JsonFieldType.STRING)
                                        .description("미션 카테고리: " + Arrays.toString(MissionCategoryStatus.values())),
                                fieldWithPath("data.status").type(JsonFieldType.STRING)
                                        .description("미션 상태: " + Arrays.toString(MissionStatus.values())),
                                fieldWithPath("data.userName").type(JsonFieldType.STRING)
                                        .description("사용자명"),
                                fieldWithPath("data.createdDate").type(JsonFieldType.STRING)
                                        .description("요청 일시"),
                                fieldWithPath("data.locationData").type(JsonFieldType.NULL)
                                        .description("위치 인증 데이터 (LOCATION 타입일 때만)").optional(),
                                fieldWithPath("data.photoData").type(JsonFieldType.NULL)
                                        .description("사진 인증 데이터 (REPLACE_ACTION 타입일 때만)").optional(),
                                fieldWithPath("data.abstinenceData").type(JsonFieldType.OBJECT)
                                        .description("금연 시간 데이터 (HOLD 타입일 때만)").optional(),
                                fieldWithPath("data.abstinenceData.abstinenceTime").type(JsonFieldType.NUMBER)
                                        .description("금연 시간 (초 단위)")
                        )
                ));
    }

    @DisplayName("미션 결과 심사 API - 성공 처리")
    @Test
    void 미션_결과_심사_API_성공() throws Exception {
        // given
        MissionHistoryUpdateRequest request = MissionHistoryUpdateRequest.builder()
                .isSuccess(true)
                .build();

        // when // then
        mockMvc.perform(
                        patch("/api/v1/admin/mission-history/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("mission-history-approve",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("미션 히스토리 ID")
                        ),
                        requestFields(
                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN)
                                        .description("심사 결과 (true: 성공/COMPLETED, false: 실패/FAILED)")
                        )
                ));
    }

    @DisplayName("미션 결과 심사 API - 실패 처리")
    @Test
    void 미션_결과_심사_API_실패() throws Exception {
        // given
        MissionHistoryUpdateRequest request = MissionHistoryUpdateRequest.builder()
                .isSuccess(false)
                .build();

        // when // then
        mockMvc.perform(
                        patch("/api/v1/admin/mission-history/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("mission-history-reject",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("미션 히스토리 ID")
                        ),
                        requestFields(
                                fieldWithPath("isSuccess").type(JsonFieldType.BOOLEAN)
                                        .description("심사 결과 (true: 성공/COMPLETED, false: 실패/FAILED)")
                        )
                ));
    }
}
