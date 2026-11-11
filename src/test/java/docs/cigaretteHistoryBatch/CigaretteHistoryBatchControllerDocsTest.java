package docs.cigaretteHistoryBatch;

import com.quitmate.cigaretteHistoryBatch.controller.CigaretteHistoryBatchController;
import com.quitmate.cigaretteHistoryBatch.controller.request.CigaretteHistoryBatchIssueIgnoreRequest;
import com.quitmate.cigaretteHistoryBatch.controller.request.CigaretteHistoryBatchRunRequest;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStage;
import com.quitmate.cigaretteHistoryBatch.enums.CigaretteHistoryBatchIssueStatus;
import com.quitmate.cigaretteHistoryBatch.service.CigaretteHistoryBatchIssueService;
import com.quitmate.cigaretteHistoryBatch.service.CigaretteHistoryBatchRunService;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchAttemptResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchDateStatusResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchIssueIgnoreResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchIssueResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchRunResponse;
import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CigaretteHistoryBatchControllerDocsTest extends RestDocsSupport {

    private final CigaretteHistoryBatchIssueService issueService = mock(CigaretteHistoryBatchIssueService.class);
    private final CigaretteHistoryBatchRunService runService = mock(CigaretteHistoryBatchRunService.class);

    @Override
    protected Object initController() {
        return new CigaretteHistoryBatchController(issueService, runService);
    }

    @DisplayName("이관 처리 대기 목록 조회 API")
    @Test
    void 이관_처리_대기_목록_조회_API() throws Exception {
        CigaretteHistoryBatchIssueResponse issue = CigaretteHistoryBatchIssueResponse.builder()
                .id(3L)
                .targetDate("20261105")
                .userId(81L)
                .stage(CigaretteHistoryBatchIssueStage.SMOKED_SAVE)
                .failCount(3)
                .lastErrorType("com.mongodb.MongoWriteException")
                .firstFailedAt(LocalDateTime.of(2026, 11, 6, 0, 0, 1))
                .lastFailedAt(LocalDateTime.of(2026, 11, 8, 0, 0, 2))
                .memo(null)
                .build();
        given(issueService.getPendingIssues(any())).willReturn(PageCustom.<CigaretteHistoryBatchIssueResponse>builder()
                .content(List.of(issue))
                .pageInfo(PageableCustom.builder().currentPage(1).totalPage(1).totalElement(1).build())
                .build());

        mockMvc.perform(get("/api/v1/admin/cigarette-history-batch/issues").param("page", "1").param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("cigarette-history-batch-issue-list",
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호(1부터)"),
                                parameterWithName("size").description("페이지 크기")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("처리 대기(PENDING) 목록. 오래된 날짜·사용자 ID 순"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER).description("처리 대기 건 ID([무시]에 씀)"),
                                fieldWithPath("data.content[].targetDate").type(JsonFieldType.STRING).description("이관 날짜 yyyyMMdd"),
                                fieldWithPath("data.content[].userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                fieldWithPath("data.content[].stage").type(JsonFieldType.STRING).description("실패한 단계: SMOKED_SAVE(흡연 기록 저장) / ZERO_SAVE(0건 문서 저장)"),
                                fieldWithPath("data.content[].failCount").type(JsonFieldType.NUMBER).description("누적 실패 횟수"),
                                fieldWithPath("data.content[].lastErrorType").type(JsonFieldType.STRING).optional().description("마지막 실패 원인(예외 타입)"),
                                fieldWithPath("data.content[].firstFailedAt").type(JsonFieldType.STRING).description("처음 실패 시각"),
                                fieldWithPath("data.content[].lastFailedAt").type(JsonFieldType.STRING).description("마지막 실패 시각"),
                                fieldWithPath("data.content[].memo").type(JsonFieldType.STRING).optional().description("예전에 남긴 관리자 메모(무시했다가 다시 실패한 경우)"),
                                fieldWithPath("data.pageInfo").type(JsonFieldType.OBJECT).description("페이지 정보"),
                                fieldWithPath("data.pageInfo.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지"),
                                fieldWithPath("data.pageInfo.totalPage").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                                fieldWithPath("data.pageInfo.totalElement").type(JsonFieldType.NUMBER).description("전체 건수")
                        )
                ));
    }

    @DisplayName("이관 처리 대기 무시 API")
    @Test
    void 이관_처리_대기_무시_API() throws Exception {
        given(issueService.ignore(any())).willReturn(CigaretteHistoryBatchIssueIgnoreResponse.builder()
                .id(3L)
                .targetDate("20261105")
                .userId(81L)
                .status(CigaretteHistoryBatchIssueStatus.IGNORED)
                .memo("원본이 손상돼 복구하지 않음")
                .resolvedBy(1L)
                .resolvedAt(LocalDateTime.of(2026, 11, 12, 10, 30))
                .build());

        mockMvc.perform(patch("/api/v1/admin/cigarette-history-batch/issues/{issueId}/ignore", 3L)
                        .content(objectMapper.writeValueAsString(
                                CigaretteHistoryBatchIssueIgnoreRequest.builder().memo("원본이 손상돼 복구하지 않음").build()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("cigarette-history-batch-issue-ignore",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("issueId").description("처리 대기 건 ID")),
                        requestFields(
                                fieldWithPath("memo").type(JsonFieldType.STRING).description("무시하는 이유(필수, 500자까지)")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("처리 대기 건 ID"),
                                fieldWithPath("data.targetDate").type(JsonFieldType.STRING).description("이관 날짜 yyyyMMdd"),
                                fieldWithPath("data.userId").type(JsonFieldType.NUMBER).description("사용자 ID"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("IGNORED"),
                                fieldWithPath("data.memo").type(JsonFieldType.STRING).description("관리자 메모"),
                                fieldWithPath("data.resolvedBy").type(JsonFieldType.NUMBER).description("처리한 관리자 ID"),
                                fieldWithPath("data.resolvedAt").type(JsonFieldType.STRING).description("처리 시각")
                        )
                ));
    }

    @DisplayName("흡연 기록 이관 재처리 API")
    @Test
    void 흡연_기록_이관_재처리_API() throws Exception {
        given(runService.run(any())).willReturn(CigaretteHistoryBatchRunResponse.builder()
                .targetDate("20261105")
                .status("DONE")
                .attemptNo(4)
                .triggerType("MANUAL")
                .insertedUserCount(1)
                .skippedUserCount(52)
                .failedUserCount(0)
                .startedAt(LocalDateTime.of(2026, 11, 12, 10, 0))
                .finishedAt(LocalDateTime.of(2026, 11, 12, 10, 0, 2))
                .build());

        mockMvc.perform(post("/api/v1/admin/cigarette-history-batch/runs")
                        .content(objectMapper.writeValueAsString(
                                CigaretteHistoryBatchRunRequest.builder().targetDate("20261105").build()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("cigarette-history-batch-run",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("targetDate").type(JsonFieldType.STRING)
                                        .description("다시 이관할 날짜 yyyyMMdd. 앱 서버에 force=false로 요청합니다.")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("앱 서버 재처리 결과"),
                                fieldWithPath("data.targetDate").type(JsonFieldType.STRING).description("이관 날짜"),
                                fieldWithPath("data.status").type(JsonFieldType.STRING).description("이번 실행 결과: DONE / FAILED(일부 실패, 원본은 남김) / ABANDONED(락을 잃고 멈춤)"),
                                fieldWithPath("data.attemptNo").type(JsonFieldType.NUMBER).description("이 날짜의 몇 번째 실행인지"),
                                fieldWithPath("data.triggerType").type(JsonFieldType.STRING).description("실행 계기(MANUAL)"),
                                fieldWithPath("data.insertedUserCount").type(JsonFieldType.NUMBER).description("새로 저장한 사용자 수"),
                                fieldWithPath("data.skippedUserCount").type(JsonFieldType.NUMBER).description("이미 문서가 있어 건너뛴 사용자 수"),
                                fieldWithPath("data.failedUserCount").type(JsonFieldType.NUMBER).description("재시도까지 실패한 사용자 수"),
                                fieldWithPath("data.startedAt").type(JsonFieldType.STRING).description("시작 시각"),
                                fieldWithPath("data.finishedAt").type(JsonFieldType.STRING).description("종료 시각")
                        )
                ));
    }

    @DisplayName("날짜별 배치 상태 조회 API")
    @Test
    void 날짜별_배치_상태_조회_API() throws Exception {
        given(runService.getDateStatuses(anyString(), anyString())).willReturn(List.of(
                CigaretteHistoryBatchDateStatusResponse.builder()
                        .targetDate("20261105")
                        .status("FAILED")
                        .attemptCount(3)
                        .lastTriggerType("CATCH_UP")
                        .totalUserCount(53)
                        .successUserCount(52)
                        .failedUserCount(1)
                        .lastErrorSummary("사용자 저장 실패 1명")
                        .startedAt(LocalDateTime.of(2026, 11, 8, 0, 0))
                        .finishedAt(LocalDateTime.of(2026, 11, 8, 0, 0, 5))
                        .build()));

        mockMvc.perform(get("/api/v1/admin/cigarette-history-batch/runs")
                        .param("from", "20261105").param("to", "20261105"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("cigarette-history-batch-run-list",
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("from").description("시작일 yyyyMMdd"),
                                parameterWithName("to").description("종료일 yyyyMMdd (앱 서버 기준 최대 93일)")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("날짜별 상태. 실행 기록이 없는 날짜도 status null로 들어 있습니다."),
                                fieldWithPath("data[].targetDate").type(JsonFieldType.STRING).description("날짜 yyyyMMdd"),
                                fieldWithPath("data[].status").type(JsonFieldType.STRING).optional().description("RUNNING / DONE / FAILED. 실행 기록이 없으면 null"),
                                fieldWithPath("data[].attemptCount").type(JsonFieldType.NUMBER).description("누적 실행 횟수"),
                                fieldWithPath("data[].lastTriggerType").type(JsonFieldType.STRING).optional().description("마지막 실행 계기: SCHEDULED / CATCH_UP / MANUAL"),
                                fieldWithPath("data[].totalUserCount").type(JsonFieldType.NUMBER).optional().description("마지막 실행의 처리 대상 사용자 수"),
                                fieldWithPath("data[].successUserCount").type(JsonFieldType.NUMBER).optional().description("마지막 실행의 성공 사용자 수"),
                                fieldWithPath("data[].failedUserCount").type(JsonFieldType.NUMBER).optional().description("마지막 실행의 실패 사용자 수"),
                                fieldWithPath("data[].lastErrorSummary").type(JsonFieldType.STRING).optional().description("마지막 실패 원인 요약"),
                                fieldWithPath("data[].startedAt").type(JsonFieldType.STRING).optional().description("마지막 실행 시작 시각"),
                                fieldWithPath("data[].finishedAt").type(JsonFieldType.STRING).optional().description("마지막 실행 종료 시각")
                        )
                ));
    }

    @DisplayName("날짜별 실행 이력 조회 API")
    @Test
    void 날짜별_실행_이력_조회_API() throws Exception {
        given(runService.getAttempts(anyString())).willReturn(List.of(
                CigaretteHistoryBatchAttemptResponse.builder()
                        .attemptNo(4)
                        .triggerType("MANUAL")
                        .requestedBy(1L)
                        .executor("ip-10-0-1-5:1234:a1b2c3d4")
                        .activeProfile("prd")
                        .status("DONE")
                        .startedAt(LocalDateTime.of(2026, 11, 12, 10, 0))
                        .finishedAt(LocalDateTime.of(2026, 11, 12, 10, 0, 2))
                        .sourceRowCount(3)
                        .totalUserCount(53)
                        .insertedUserCount(1)
                        .skippedUserCount(52)
                        .failedUserCount(0)
                        .retriedUserCount(0)
                        .deletedSourceRowCount(3)
                        .errorType(null)
                        .errorMessage(null)
                        .build()));

        mockMvc.perform(get("/api/v1/admin/cigarette-history-batch/runs/{targetDate}/attempts", "20261105"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("cigarette-history-batch-attempt-list",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("targetDate").description("날짜 yyyyMMdd")),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data[]").type(JsonFieldType.ARRAY).description("실행 이력. 오래된 실행부터"),
                                fieldWithPath("data[].attemptNo").type(JsonFieldType.NUMBER).description("시도 번호"),
                                fieldWithPath("data[].triggerType").type(JsonFieldType.STRING).description("실행 계기: SCHEDULED / CATCH_UP / MANUAL"),
                                fieldWithPath("data[].requestedBy").type(JsonFieldType.NUMBER).optional().description("수동 실행한 관리자 ID"),
                                fieldWithPath("data[].executor").type(JsonFieldType.STRING).description("실행 서버 호스트명:PID:UUID8"),
                                fieldWithPath("data[].activeProfile").type(JsonFieldType.STRING).description("실행 Spring 프로필"),
                                fieldWithPath("data[].status").type(JsonFieldType.STRING).description("RUNNING / DONE / FAILED / ABANDONED"),
                                fieldWithPath("data[].startedAt").type(JsonFieldType.STRING).description("시작 시각"),
                                fieldWithPath("data[].finishedAt").type(JsonFieldType.STRING).optional().description("종료 시각"),
                                fieldWithPath("data[].sourceRowCount").type(JsonFieldType.NUMBER).optional().description("읽은 원본 행 수"),
                                fieldWithPath("data[].totalUserCount").type(JsonFieldType.NUMBER).optional().description("처리 대상 사용자 수"),
                                fieldWithPath("data[].insertedUserCount").type(JsonFieldType.NUMBER).optional().description("새로 저장한 사용자 수"),
                                fieldWithPath("data[].skippedUserCount").type(JsonFieldType.NUMBER).optional().description("건너뛴 사용자 수"),
                                fieldWithPath("data[].failedUserCount").type(JsonFieldType.NUMBER).optional().description("실패한 사용자 수"),
                                fieldWithPath("data[].retriedUserCount").type(JsonFieldType.NUMBER).optional().description("재시도 끝에 성공한 사용자 수"),
                                fieldWithPath("data[].deletedSourceRowCount").type(JsonFieldType.NUMBER).optional().description("삭제한 원본 행 수"),
                                fieldWithPath("data[].errorType").type(JsonFieldType.STRING).optional().description("실행 전체 실패 원인"),
                                fieldWithPath("data[].errorMessage").type(JsonFieldType.STRING).optional().description("실행 전체 실패 메시지")
                        )
                ));
    }
}
