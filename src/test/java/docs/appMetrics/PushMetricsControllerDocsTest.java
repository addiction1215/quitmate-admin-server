package docs.appMetrics;

import com.quitmate.appMetrics.push.controller.PushMetricsController;
import com.quitmate.appMetrics.push.service.PushMetricsService;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse.Daily;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse.ErrorCode;
import com.quitmate.appMetrics.push.service.response.PushMetricsResponse.Summary;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PushMetricsControllerDocsTest extends RestDocsSupport {

    private static final OffsetDateTime FETCHED_AT = OffsetDateTime.parse("2026-11-19T22:10:00+09:00");
    private static final LocalDate END = LocalDate.of(2026, 11, 19);

    private final PushMetricsService pushMetricsService = mock(PushMetricsService.class);

    @Override
    protected Object initController() {
        return new PushMetricsController(pushMetricsService);
    }

    @DisplayName("앱 지표 푸시 조회 API")
    @Test
    void 앱_지표_푸시_조회_API() throws Exception {
        List<Daily> daily = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            daily.add(new Daily(END.minusDays(i), 100, i == 0 ? 2L : 0L, i == 0 ? 1L : 0L, 30L));
        }
        given(pushMetricsService.getMetrics(any())).willReturn(new PushMetricsResponse(
                7, END.minusDays(6), END, FETCHED_AT, true, true,
                new Summary(700, 690L, 10L, 640L, 6L, 44L, 210L, 0.3043, 181),
                daily,
                List.of(new ErrorCode("DeviceNotRegistered", 12), new ErrorCode("MessageRateExceeded", 4))));

        mockMvc.perform(get("/api/v1/app-metrics/push").param("period", "7"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("app-metrics-push",
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("period").description("조회 기간(일): 7, 28, 90 중 하나. 그 밖·빈 값은 400")
                        ),
                        responseFields(
                                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT).description("푸시 지표"),
                                fieldWithPath("data.period").type(JsonFieldType.NUMBER).description("조회 기간(일)"),
                                fieldWithPath("data.startDate").type(JsonFieldType.STRING).description("시작 날짜 yyyy-MM-dd(끝 날짜 포함 period일)"),
                                fieldWithPath("data.endDate").type(JsonFieldType.STRING).description("끝 날짜 yyyy-MM-dd. 오늘(한국 날짜)"),
                                fieldWithPath("data.fetchedAt").type(JsonFieldType.STRING).description("DB를 실제로 읽은 시각(+09:00). 캐시(1시간)에서 줄 때도 그 시각"),
                                fieldWithPath("data.deliveryTracking").type(JsonFieldType.BOOLEAN).description("기간에 push_delivery 행이 하나라도 있으면 true. false면 ticket·receipt 숫자는 null"),
                                fieldWithPath("data.openTracking").type(JsonFieldType.BOOLEAN).description("기간 하루 흡연 시간대 푸시 중 열린 것이 하나라도 있으면 true. false면 opened·openRate는 null"),
                                fieldWithPath("data.summary").type(JsonFieldType.OBJECT).description("기간 합계"),
                                fieldWithPath("data.summary.requested").type(JsonFieldType.NUMBER).description("발송 요청(사람 단위): 기간에 생긴 push_outbox 행 수(종류·상태 무관). push_delivery 행은 더하지 않음"),
                                fieldWithPath("data.summary.ticketOk").type(JsonFieldType.NUMBER).optional().description("Expo 티켓 OK(push_delivery, 기기 단위, 친구 푸시 포함). 전달 추적 전이면 null"),
                                fieldWithPath("data.summary.ticketError").type(JsonFieldType.NUMBER).optional().description("Expo 티켓 ERROR. 전달 추적 전이면 null"),
                                fieldWithPath("data.summary.receiptOk").type(JsonFieldType.NUMBER).optional().description("Expo 영수증 OK. 전달 추적 전이면 null"),
                                fieldWithPath("data.summary.receiptError").type(JsonFieldType.NUMBER).optional().description("Expo 영수증 ERROR. 전달 추적 전이면 null"),
                                fieldWithPath("data.summary.receiptPending").type(JsonFieldType.NUMBER).optional().description("영수증 확인 전(PENDING). 전달 추적 전이면 null"),
                                fieldWithPath("data.summary.opened").type(JsonFieldType.NUMBER).optional().description("하루 흡연 시간대 푸시 중 열린 것(opened_at 있음). 열림 추적 전이면 null"),
                                fieldWithPath("data.summary.openRate").type(JsonFieldType.NUMBER).optional().description("열림률 0~1(소수 넷째 자리) = opened / 보낸 하루 흡연 시간대 푸시(SENT 또는 열린 것). 열림 추적 전이면 null"),
                                fieldWithPath("data.summary.tokenUsers").type(JsonFieldType.NUMBER).description("푸시 토큰이 있는 회원 수(기간 무관, 지금 값)"),
                                fieldWithPath("data.daily[]").type(JsonFieldType.ARRAY).description("날짜별. 날짜 오름차순 period개"),
                                fieldWithPath("data.daily[].date").type(JsonFieldType.STRING).description("날짜 yyyy-MM-dd(created_date 한국 날짜)"),
                                fieldWithPath("data.daily[].requested").type(JsonFieldType.NUMBER).description("그날 발송 요청(사람 단위, push_outbox). 없으면 0"),
                                fieldWithPath("data.daily[].ticketError").type(JsonFieldType.NUMBER).optional().description("그날 티켓 ERROR. 전달 추적 전이면 null, 추적 중 기록 없는 날은 0"),
                                fieldWithPath("data.daily[].receiptError").type(JsonFieldType.NUMBER).optional().description("그날 영수증 ERROR. 전달 추적 전이면 null"),
                                fieldWithPath("data.daily[].opened").type(JsonFieldType.NUMBER).optional().description("그날 보낸 하루 흡연 시간대 푸시 중 열린 것. 열림 추적 전이면 null"),
                                fieldWithPath("data.errorCodes[]").type(JsonFieldType.ARRAY).description("ticket_error·receipt_error를 합친 오류 코드별 건수(기기 단위, 많은 순, 최대 20). 없으면 빈 배열"),
                                fieldWithPath("data.errorCodes[].code").type(JsonFieldType.STRING).description("오류 코드(예: DeviceNotRegistered)"),
                                fieldWithPath("data.errorCodes[].count").type(JsonFieldType.NUMBER).description("건수")
                        )
                ));
    }

    @DisplayName("앱 지표 푸시 조회 API - 전달·열림 추적 전")
    @Test
    void 앱_지표_푸시_조회_API_추적_전() throws Exception {
        List<Daily> daily = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            daily.add(new Daily(END.minusDays(i), 100, null, null, null));
        }
        given(pushMetricsService.getMetrics(any())).willReturn(new PushMetricsResponse(
                7, END.minusDays(6), END, FETCHED_AT, false, false,
                new Summary(700, null, null, null, null, null, null, null, 181),
                daily, List.of()));

        mockMvc.perform(get("/api/v1/app-metrics/push").param("period", "7"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("app-metrics-push-not-tracked",
                        preprocessResponse(prettyPrint())
                ));
    }
}
