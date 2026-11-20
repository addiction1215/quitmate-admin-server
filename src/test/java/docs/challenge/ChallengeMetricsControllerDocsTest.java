package docs.challenge;

import com.quitmate.challenge.metrics.controller.ChallengeMetricsController;
import com.quitmate.challenge.metrics.service.ChallengeMetricsReadService;
import com.quitmate.challenge.metrics.service.response.ChallengeMetrics;
import com.quitmate.challenge.metrics.service.response.ChallengeMetricsDetailResponse;
import com.quitmate.challenge.metrics.service.response.ChallengeMetricsListResponse;
import com.quitmate.challenge.metrics.service.response.MetricRatio;
import com.quitmate.challenge.metrics.service.response.MissionMetrics;
import com.quitmate.challenge.mission.entity.MissionCategoryStatus;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.request.ParameterDescriptor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ChallengeMetricsControllerDocsTest extends RestDocsSupport {

    private static final String BASE = "/api/v1/admin/challenge";
    private static final LocalDate FROM = LocalDate.of(2026, 11, 1);
    private static final LocalDate TO = LocalDate.of(2026, 11, 30);

    private final ChallengeMetricsReadService readService = mock(ChallengeMetricsReadService.class);

    @Override
    protected Object initController() {
        return new ChallengeMetricsController(readService);
    }

    @DisplayName("챌린지 지표 목록 조회 API")
    @Test
    void 챌린지_지표_목록_조회_API() throws Exception {
        given(readService.getChallengeMetricsList(any()))
                .willReturn(new ChallengeMetricsListResponse(FROM, TO, List.of(sample(), empty())));

        List<FieldDescriptor> fields = envelope("응답 데이터");
        fields.addAll(periodFields());
        fields.add(fieldWithPath("data.challenges[]").type(JsonFieldType.ARRAY)
                .description("사용 중인 챌린지별 지표(챌린지 ID 내림차순). 참여가 없는 챌린지도 0으로 나옴"));
        fields.addAll(metricsFields("data.challenges[]."));

        mockMvc.perform(get(BASE + "/metrics").param("from", "2026-11-01").param("to", "2026-11-30"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("challenge-metrics-list",
                        preprocessResponse(prettyPrint()),
                        queryParameters(periodParameters()),
                        responseFields(fields)
                ));
    }

    @DisplayName("챌린지 지표 상세 조회 API")
    @Test
    void 챌린지_지표_상세_조회_API() throws Exception {
        given(readService.getChallengeMetrics(anyLong(), any()))
                .willReturn(new ChallengeMetricsDetailResponse(FROM, TO, sample(), List.of(
                        new MissionMetrics(10L, "10분 참기", MissionCategoryStatus.HOLD, MetricRatio.of(9, 13)),
                        new MissionMetrics(11L, "흡연 장소 피하기", MissionCategoryStatus.LOCATION, MetricRatio.of(7, 13)),
                        new MissionMetrics(12L, "물 마시기 인증", MissionCategoryStatus.REPLACE_ACTION, MetricRatio.of(0, 0))
                )));

        List<FieldDescriptor> fields = envelope("응답 데이터");
        fields.addAll(periodFields());
        fields.add(fieldWithPath("data.challenge").type(JsonFieldType.OBJECT).description("챌린지 지표"));
        fields.addAll(metricsFields("data.challenge."));
        fields.addAll(List.of(
                fieldWithPath("data.missions[]").type(JsonFieldType.ARRAY)
                        .description("사용 중인 미션별 완료율(미션 ID 순). 삭제한 미션은 빠짐"),
                fieldWithPath("data.missions[].missionId").type(JsonFieldType.NUMBER).description("미션 ID"),
                fieldWithPath("data.missions[].title").type(JsonFieldType.STRING).description("미션 제목"),
                fieldWithPath("data.missions[].category").type(JsonFieldType.STRING)
                        .description("미션 종류: " + Arrays.toString(MissionCategoryStatus.values()))
        ));
        fields.addAll(ratioFields("data.missions[].completion",
                "미션 완료율 = 완료(COMPLETED)한 이 미션 기록 ÷ 기간 안에 시작한 참여의 이 미션 기록(참여를 포기했어도 분모에 듦). "
                        + "참여 뒤에 추가한 미션은 그 참여에 기록이 없어 미션마다 분모가 다름. KAN-197 정리 전 옛 승인 대기(READY)는 완료로 세지 않음"));

        mockMvc.perform(get(BASE + "/{id}/metrics", 3L).param("from", "2026-11-01").param("to", "2026-11-30"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("challenge-metrics-detail",
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("id").description("챌린지 ID. 없거나 삭제한 챌린지면 400")),
                        queryParameters(periodParameters()),
                        responseFields(fields)
                ));
    }

    private static ChallengeMetrics sample() {
        return new ChallengeMetrics(3L, "출근길 금연", 12, 13, 4, 6, 3, 0,
                MetricRatio.of(6, 13), MetricRatio.of(3, 13), MetricRatio.of(1, 3), 26.4, 5.2, 4);
    }

    private static ChallengeMetrics empty() {
        return new ChallengeMetrics(2L, "주말 금연", 0, 0, 0, 0, 0, 0,
                MetricRatio.of(0, 0), MetricRatio.of(0, 0), MetricRatio.of(0, 0), null, null, 0);
    }

    private static ParameterDescriptor[] periodParameters() {
        return new ParameterDescriptor[]{
                parameterWithName("from").optional()
                        .description("기간 시작일 yyyy-MM-dd(이 날 포함). 참여 시작일(한국 날짜) 기준. 없으면 처음부터"),
                parameterWithName("to").optional()
                        .description("기간 끝 날짜 yyyy-MM-dd(이 날 포함). 참여 시작일(한국 날짜) 기준. 없으면 지금까지. from보다 앞서면 400")
        };
    }

    private static List<FieldDescriptor> envelope(String dataDescription) {
        return new ArrayList<>(List.of(
                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                fieldWithPath("data").type(JsonFieldType.OBJECT).description(dataDescription)
        ));
    }

    private static List<FieldDescriptor> periodFields() {
        return List.of(
                fieldWithPath("data.from").type(JsonFieldType.STRING).optional()
                        .description("요청한 기간 시작일(참여 시작일 기준, 포함). 안 줬으면 null"),
                fieldWithPath("data.to").type(JsonFieldType.STRING).optional()
                        .description("요청한 기간 끝 날짜(참여 시작일 기준, 포함). 안 줬으면 null")
        );
    }

    private static List<FieldDescriptor> metricsFields(String p) {
        List<FieldDescriptor> fields = new ArrayList<>(List.of(
                fieldWithPath(p + "challengeId").type(JsonFieldType.NUMBER).description("챌린지 ID"),
                fieldWithPath(p + "title").type(JsonFieldType.STRING).description("챌린지 제목"),
                fieldWithPath(p + "participantCount").type(JsonFieldType.NUMBER)
                        .description("참여자 수(명). 기간 안에 참여를 시작한 서로 다른 사용자 수"),
                fieldWithPath(p + "participationCount").type(JsonFieldType.NUMBER)
                        .description("참여 수(건). 기간 안에 시작한 참여(challenge_history) 수 = 아래 네 상태 수의 합. 포기 뒤 다시 참여하면 한 사람이 2건"),
                fieldWithPath(p + "progressingCount").type(JsonFieldType.NUMBER).description("그중 지금 진행 중(PROGRESSING)"),
                fieldWithPath(p + "completedCount").type(JsonFieldType.NUMBER).description("그중 완료(COMPLETED)"),
                fieldWithPath(p + "cancelledCount").type(JsonFieldType.NUMBER).description("그중 포기(CANCELLED)"),
                fieldWithPath(p + "otherStatusCount").type(JsonFieldType.NUMBER)
                        .description("그중 다른 상태(FAILED 등). 지금은 어느 서버도 만들지 않아 보통 0")
        ));
        fields.addAll(ratioFields(p + "completion", "완료율 = completedCount ÷ participationCount(건)"));
        fields.addAll(ratioFields(p + "cancellation", "중도 포기율 = cancelledCount ÷ participationCount(건)"));
        fields.addAll(ratioFields(p + "rejoin",
                "재참여율 = 포기 뒤 같은 챌린지에 다시 참여한 사용자 ÷ 기간 안에 시작한 참여를 포기한 사용자(명). "
                        + "다시 참여는 그 포기한 참여보다 뒤에 시작한 참여(기간 밖이어도, 지금 상태는 무관)"));
        fields.addAll(List.of(
                fieldWithPath(p + "averageHoursToComplete").type(JsonFieldType.NUMBER).optional()
                        .description("완료한 참여의 시작 → 완료 평균 시간(시간, 소수 한 자리). 완료가 없으면 null"),
                fieldWithPath(p + "averageHoursToCancel").type(JsonFieldType.NUMBER).optional()
                        .description("포기한 참여의 시작 → 포기 평균 시간(시간, 소수 한 자리). 포기가 없으면 null"),
                fieldWithPath(p + "endTimeFallbackCount").type(JsonFieldType.NUMBER)
                        .description("두 평균에 쓴 완료·포기 중 완료·포기 시각이 비어 마지막 수정 시각(updated_date)으로 대신한 건수. "
                                + "KAN-190 배포 전에 끝난 옛 기록. 0보다 크면 화면에 \"옛 기록은 마지막 수정 시각 기준\"을 표시")
        ));
        return fields;
    }

    private static List<FieldDescriptor> ratioFields(String path, String description) {
        return List.of(
                fieldWithPath(path).type(JsonFieldType.OBJECT).description(description),
                fieldWithPath(path + ".numerator").type(JsonFieldType.NUMBER).description("분자"),
                fieldWithPath(path + ".denominator").type(JsonFieldType.NUMBER).description("분모"),
                fieldWithPath(path + ".percent").type(JsonFieldType.NUMBER).optional()
                        .description("분자 ÷ 분모 × 100을 반올림한 정수. 분모가 0이면 null")
        );
    }
}
