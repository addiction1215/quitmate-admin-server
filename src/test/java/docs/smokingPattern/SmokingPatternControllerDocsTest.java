package docs.smokingPattern;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import com.quitmate.smokingPattern.controller.SmokingPatternController;
import com.quitmate.smokingPattern.enums.BaselineComparisonType;
import com.quitmate.smokingPattern.enums.DataSufficiencyLevel;
import com.quitmate.smokingPattern.enums.RoutineType;
import com.quitmate.smokingPattern.enums.TrendDirection;
import com.quitmate.smokingPattern.repository.response.SmokingPatternSnapshotDto;
import com.quitmate.smokingPattern.result.SmokingPatternResult;
import com.quitmate.smokingPattern.service.SmokingPatternDistributionCalculator;
import com.quitmate.smokingPattern.service.SmokingPatternReadService;
import com.quitmate.smokingPattern.service.response.SmokingPatternDistributionResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternSnapshotDetailResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternSnapshotSummaryResponse;
import com.quitmate.smokingPattern.service.response.SmokingPatternWeekResponse;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.quitmate.smokingPattern.SmokingPatternFixture.okResult;
import static com.quitmate.smokingPattern.SmokingPatternFixture.relapse;
import static com.quitmate.smokingPattern.SmokingPatternFixture.row;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SmokingPatternControllerDocsTest extends RestDocsSupport {

    private static final String BASE = "/api/v1/admin/smoking-pattern";
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);
    private static final LocalDateTime CREATED = LocalDateTime.of(2026, 11, 9, 3, 0, 5);

    private final SmokingPatternReadService readService = mock(SmokingPatternReadService.class);

    @Override
    protected Object initController() {
        return new SmokingPatternController(readService);
    }

    @DisplayName("진단 주 목록 조회 API")
    @Test
    void 진단_주_목록_조회_API() throws Exception {
        given(readService.getWeeks(any())).willReturn(List.of(
                new SmokingPatternWeekResponse(WEEK, WEEK.plusDays(6), 128),
                new SmokingPatternWeekResponse(WEEK.minusWeeks(1), WEEK.minusDays(1), 121)));

        mockMvc.perform(get(BASE + "/weeks").param("size", "12"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("smoking-pattern-weeks",
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("size").optional().description("최근 몇 주까지 볼지(기본 12, 최대 52)")
                        ),
                        responseFields(withEnvelope(JsonFieldType.ARRAY, "스냅숏이 있는 분석 주. 최근 주부터",
                                fieldWithPath("data[].periodStart").type(JsonFieldType.STRING).description("분석 주 월요일 yyyy-MM-dd"),
                                fieldWithPath("data[].periodEnd").type(JsonFieldType.STRING).description("분석 주 일요일 yyyy-MM-dd"),
                                fieldWithPath("data[].snapshotCount").type(JsonFieldType.NUMBER).description("그 주 스냅숏 수(= 사용자 수)")
                        ))
                ));
    }

    @DisplayName("주별 사용자 진단 목록 조회 API")
    @Test
    void 주별_사용자_진단_목록_조회_API() throws Exception {
        given(readService.getWeekSnapshots(any())).willReturn(PageCustom.<SmokingPatternSnapshotSummaryResponse>builder()
                .content(List.of(summary(5001L, true)))
                .pageInfo(PageableCustom.builder().currentPage(1).totalPage(1).totalElement(1).build())
                .build());

        mockMvc.perform(get(BASE + "/snapshots")
                        .param("periodStart", "2026-11-02")
                        .param("dataSufficiency", "SUFFICIENT")
                        .param("keyword", "금연")
                        .param("page", "1")
                        .param("size", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("smoking-pattern-week-snapshots",
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("periodStart").optional().description("분석 주 월요일 yyyy-MM-dd. 없으면 가장 최근 주. 월요일이 아니면 400"),
                                parameterWithName("dataSufficiency").optional().description("데이터 충족도로 거르기: SUFFICIENT / PARTIAL / INSUFFICIENT"),
                                parameterWithName("userId").optional().description("회원 ID로 거르기"),
                                parameterWithName("keyword").optional().description("닉네임 또는 이메일 일부로 거르기(응답에 이메일은 없음)"),
                                parameterWithName("page").description("페이지 번호(1부터)"),
                                parameterWithName("size").description("페이지 크기(최대 100)")
                        ),
                        responseFields(withPage("그 주 사용자별 스냅숏. 회원 ID 순", summaryFields()))
                ));
    }

    @DisplayName("회원 주차별 진단 목록 조회 API")
    @Test
    void 회원_주차별_진단_목록_조회_API() throws Exception {
        given(readService.getUserSnapshots(anyLong(), any())).willReturn(PageCustom.<SmokingPatternSnapshotSummaryResponse>builder()
                .content(List.of(summary(5001L, true)))
                .pageInfo(PageableCustom.builder().currentPage(1).totalPage(1).totalElement(1).build())
                .build());

        mockMvc.perform(get(BASE + "/users/{userId}/snapshots", 81L).param("page", "1").param("size", "12"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("smoking-pattern-user-snapshots",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("userId").description("회원 ID. 없는 회원이면 404")),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호(1부터)"),
                                parameterWithName("size").description("페이지 크기(최대 100)")
                        ),
                        responseFields(withPage("회원의 주차별 스냅숏. 최근 주부터", summaryFields()))
                ));
    }

    @DisplayName("진단 상세 조회 API")
    @Test
    void 진단_상세_조회_API() throws Exception {
        SmokingPatternResult result = sampleResult();
        given(readService.getSnapshot(anyLong())).willReturn(new SmokingPatternSnapshotDetailResponse(5001L, 81L,
                "금연하는하루", WEEK, WEEK.plusDays(6), "v1", DataSufficiencyLevel.SUFFICIENT, 7, 42, true, result,
                CREATED, CREATED));

        mockMvc.perform(get(BASE + "/snapshots/{snapshotId}", 5001L))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("smoking-pattern-snapshot-detail",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("snapshotId").description("스냅숏 ID. 없으면 404")),
                        responseFields(withEnvelope(JsonFieldType.OBJECT, "진단 스냅숏 상세",
                                fieldWithPath("data.id").type(JsonFieldType.NUMBER).description("스냅숏 ID"),
                                fieldWithPath("data.userId").type(JsonFieldType.NUMBER).description("회원 ID"),
                                fieldWithPath("data.nickName").type(JsonFieldType.STRING).optional().description("닉네임(회원이 없으면 null)"),
                                fieldWithPath("data.periodStart").type(JsonFieldType.STRING).description("분석 주 월요일"),
                                fieldWithPath("data.periodEnd").type(JsonFieldType.STRING).description("분석 주 일요일"),
                                fieldWithPath("data.algorithmVersion").type(JsonFieldType.STRING).description("계산 기준 버전"),
                                fieldWithPath("data.dataSufficiency").type(JsonFieldType.STRING).description("지표 9 데이터 충족도: SUFFICIENT / PARTIAL / INSUFFICIENT"),
                                fieldWithPath("data.observedDays").type(JsonFieldType.NUMBER).description("지난주 상태를 아는 날 수(0~7)"),
                                fieldWithPath("data.smokeCount").type(JsonFieldType.NUMBER).description("지난주 흡연 개비 합계"),
                                fieldWithPath("data.paid").type(JsonFieldType.BOOLEAN).description("그 주와 겹치는(회수되지 않은) QuitMate 90 이용권이 있었는지"),
                                subsectionWithPath("data.result").type(JsonFieldType.OBJECT).optional()
                                        .description("지표 1~9 전체(앱 서버 result_json 그대로, 아래 「지표 구조」 참고). JSON을 읽지 못했으면 null"),
                                fieldWithPath("data.createdDate").type(JsonFieldType.STRING).description("스냅숏을 만든 시각"),
                                fieldWithPath("data.updatedDate").type(JsonFieldType.STRING).description("같은 주를 다시 계산해 고친 시각")
                        ))
                ));
    }

    @DisplayName("진단 전체 분포 조회 API")
    @Test
    void 진단_전체_분포_조회_API() throws Exception {
        SmokingPatternDistributionCalculator all = new SmokingPatternDistributionCalculator();
        SmokingPatternDistributionCalculator paid = new SmokingPatternDistributionCalculator();
        SmokingPatternDistributionCalculator free = new SmokingPatternDistributionCalculator();
        all.add(row(5001L, 81L, sampleResult()));
        paid.add(row(5001L, 81L, sampleResult()));
        given(readService.getDistribution(anyString())).willReturn(new SmokingPatternDistributionResponse(
                WEEK, WEEK.plusDays(6), all.result(), paid.result(), free.result()));

        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                fieldWithPath("data.periodStart").type(JsonFieldType.STRING).optional().description("분석 주 월요일. 스냅숏이 하나도 없으면 null"),
                fieldWithPath("data.periodEnd").type(JsonFieldType.STRING).optional().description("분석 주 일요일. 스냅숏이 하나도 없으면 null")
        ));
        fields.addAll(distributionFields("data.all.", "전체"));
        fields.addAll(distributionFields("data.paid.", "유료(그 주와 겹치는 이용권이 있는 사용자)"));
        fields.addAll(distributionFields("data.free.", "무료(나머지)"));

        mockMvc.perform(get(BASE + "/distribution").param("periodStart", "2026-11-02"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("smoking-pattern-distribution",
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("periodStart").optional().description("분석 주 월요일 yyyy-MM-dd. 없으면 가장 최근 주. 월요일이 아니면 400")
                        ),
                        responseFields(fields)
                ));
    }

    private static List<FieldDescriptor> distributionFields(String p, String group) {
        return List.of(
                fieldWithPath(p.substring(0, p.length() - 1)).type(JsonFieldType.OBJECT).description(group + " 분포"),
                fieldWithPath(p + "snapshotCount").type(JsonFieldType.NUMBER).description("스냅숏 수(= 사용자 수)"),
                fieldWithPath(p + "unreadableResultCount").type(JsonFieldType.NUMBER).description("result_json을 읽지 못한 스냅숏 수(JSON에서 읽는 지표에서 빠짐)"),
                subsectionWithPath(p + "algorithmVersions").type(JsonFieldType.OBJECT).description("계산 기준 버전별 스냅숏 수 {버전: 수}"),
                subsectionWithPath(p + "dataSufficiency").type(JsonFieldType.OBJECT).description("지표 9 충족도별 수 {SUFFICIENT, PARTIAL, INSUFFICIENT}"),
                fieldWithPath(p + "observedDays").type(JsonFieldType.ARRAY).description("상태를 아는 날 수별 사용자 수. 8칸, 칸 번호 = 일수(0~7)"),
                fieldWithPath(p + "smokeCountTotal").type(JsonFieldType.NUMBER).description("지난주 흡연 개비 합계"),
                fieldWithPath(p + "smokeCountAverage").type(JsonFieldType.NUMBER).optional().description("한 명당 지난주 흡연 개비 평균(소수 한 자리). 스냅숏이 없으면 null"),
                fieldWithPath(p + "peakHour.hours").type(JsonFieldType.ARRAY).description("지표 1 가장 많이 피운 시(0~23시)별 사용자 수. 24칸"),
                fieldWithPath(p + "peakHour.notCalculated").type(JsonFieldType.NUMBER).description("지표 1을 계산하지 못한 사용자 수"),
                subsectionWithPath(p + "peakSlot").type(JsonFieldType.OBJECT).description("지표 1 6시간 시간대별 {counts: {DAWN, MORNING, AFTERNOON, EVENING}, notCalculated}"),
                subsectionWithPath(p + "baselineComparison").type(JsonFieldType.OBJECT).description("지표 4 평소 기준 대비 {counts: {LESS, SIMILAR, MORE}, notCalculated}"),
                subsectionWithPath(p + "trendDirection").type(JsonFieldType.OBJECT).description("지표 5 추세 {counts: {DECREASED, SIMILAR, INCREASED}, notCalculated}"),
                subsectionWithPath(p + "routineType").type(JsonFieldType.OBJECT).description("지표 6 {counts: {ROUTINE, SCATTERED}, notCalculated}"),
                fieldWithPath(p + "cravingResponse.calculatedUsers").type(JsonFieldType.NUMBER).description("지표 7(어드민 전용)을 계산한 사용자 수"),
                fieldWithPath(p + "cravingResponse.noDataUsers").type(JsonFieldType.NUMBER).description("지난주 갈망 세션이 없는 사용자 수"),
                fieldWithPath(p + "cravingResponse.sessionCount").type(JsonFieldType.NUMBER).description("갈망 세션 수 합계"),
                fieldWithPath(p + "cravingResponse.completedCount").type(JsonFieldType.NUMBER).description("완료한 세션 수 합계"),
                fieldWithPath(p + "cravingResponse.smokedAfterCount").type(JsonFieldType.NUMBER).description("세션 뒤 정해진 시간 안에 흡연 기록이 있는 세션 수(추정)"),
                fieldWithPath(p + "cravingResponse.notSmokedAfterCount").type(JsonFieldType.NUMBER).description("세션 뒤 흡연 기록이 없는 세션 수(추정)"),
                fieldWithPath(p + "cravingResponse.notSmokedAfterPercent").type(JsonFieldType.NUMBER).optional().description("흡연 기록이 없는 비율(%). 세션이 없으면 null"),
                fieldWithPath(p + "relapseMoments.calculatedUsers").type(JsonFieldType.NUMBER).description("지표 8(어드민 전용)을 계산한 사용자 수(금연 확정일 있음)"),
                fieldWithPath(p + "relapseMoments.noDataUsers").type(JsonFieldType.NUMBER).description("지난주 금연 확정일이 없는 사용자 수"),
                fieldWithPath(p + "relapseMoments.smokeFreeDays").type(JsonFieldType.NUMBER).description("금연 확정일 수 합계"),
                fieldWithPath(p + "relapseMoments.relapseCount").type(JsonFieldType.NUMBER).description("금연 확정 뒤 다시 피운 순간 수 합계"),
                fieldWithPath(p + "relapseMoments.relapsedUsers").type(JsonFieldType.NUMBER).description("다시 피운 순간이 있는 사용자 수"),
                subsectionWithPath(p + "relapseMoments.byDayOfWeek").type(JsonFieldType.OBJECT).description("다시 피운 순간의 요일별 수 {MONDAY ~ SUNDAY}"),
                fieldWithPath(p + "relapseMoments.byHour").type(JsonFieldType.ARRAY).description("다시 피운 날 첫 기록 시각(0~23시)별 수. 24칸"),
                fieldWithPath(p + "relapseMoments.unknownTimeCount").type(JsonFieldType.NUMBER).description("시각을 모르는 다시 피운 순간 수")
        );
    }

    private static SmokingPatternResult sampleResult() {
        return okResult(WEEK, 8, TrendDirection.DECREASED, RoutineType.ROUTINE, BaselineComparisonType.LESS, 4, 1,
                List.of(relapse("2026-11-04", "WEDNESDAY", "08:15")));
    }

    private static SmokingPatternSnapshotSummaryResponse summary(Long id, boolean paid) {
        return SmokingPatternSnapshotSummaryResponse.of(new SmokingPatternSnapshotDto(id, 81L, "금연하는하루", WEEK,
                WEEK.plusDays(6), "v1", DataSufficiencyLevel.SUFFICIENT, 7, 42, 8, RoutineType.ROUTINE,
                TrendDirection.DECREASED, CREATED), paid);
    }

    private static List<FieldDescriptor> summaryFields() {
        String p = "data.content[].";
        return List.of(
                fieldWithPath(p + "id").type(JsonFieldType.NUMBER).description("스냅숏 ID(상세 조회에 씀)"),
                fieldWithPath(p + "userId").type(JsonFieldType.NUMBER).description("회원 ID"),
                fieldWithPath(p + "nickName").type(JsonFieldType.STRING).optional().description("닉네임(회원이 없으면 null). 이메일 등 다른 개인정보는 주지 않음"),
                fieldWithPath(p + "periodStart").type(JsonFieldType.STRING).description("분석 주 월요일"),
                fieldWithPath(p + "periodEnd").type(JsonFieldType.STRING).description("분석 주 일요일"),
                fieldWithPath(p + "algorithmVersion").type(JsonFieldType.STRING).description("계산 기준 버전"),
                fieldWithPath(p + "dataSufficiency").type(JsonFieldType.STRING).description("지표 9 데이터 충족도: SUFFICIENT / PARTIAL / INSUFFICIENT"),
                fieldWithPath(p + "observedDays").type(JsonFieldType.NUMBER).description("지난주 상태를 아는 날 수(0~7)"),
                fieldWithPath(p + "smokeCount").type(JsonFieldType.NUMBER).description("지난주 흡연 개비 합계"),
                fieldWithPath(p + "peakHour").type(JsonFieldType.NUMBER).optional().description("지표 1 가장 많이 피운 시(0~23). 계산하지 못했으면 null"),
                fieldWithPath(p + "routineType").type(JsonFieldType.STRING).optional().description("지표 6 ROUTINE / SCATTERED. 계산하지 못했으면 null"),
                fieldWithPath(p + "trendDirection").type(JsonFieldType.STRING).optional().description("지표 5 DECREASED / SIMILAR / INCREASED. 계산하지 못했으면 null"),
                fieldWithPath(p + "paid").type(JsonFieldType.BOOLEAN).description("그 주와 겹치는(회수되지 않은) QuitMate 90 이용권이 있었는지"),
                fieldWithPath(p + "createdDate").type(JsonFieldType.STRING).description("스냅숏을 만든 시각")
        );
    }

    private static List<FieldDescriptor> withEnvelope(JsonFieldType dataType, String dataDescription,
                                                      FieldDescriptor... dataFields) {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                fieldWithPath("data").type(dataType).description(dataDescription)
        ));
        fields.addAll(List.of(dataFields));
        return fields;
    }

    private static List<FieldDescriptor> withPage(String contentDescription, List<FieldDescriptor> rowFields) {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"),
                fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description(contentDescription)
        ));
        fields.addAll(rowFields);
        fields.addAll(List.of(
                fieldWithPath("data.pageInfo").type(JsonFieldType.OBJECT).description("페이지 정보"),
                fieldWithPath("data.pageInfo.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지"),
                fieldWithPath("data.pageInfo.totalPage").type(JsonFieldType.NUMBER).description("전체 페이지 수"),
                fieldWithPath("data.pageInfo.totalElement").type(JsonFieldType.NUMBER).description("전체 건수")
        ));
        return fields;
    }
}
