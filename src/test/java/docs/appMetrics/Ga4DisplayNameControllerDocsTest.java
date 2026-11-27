package docs.appMetrics;

import com.quitmate.appMetrics.ga4.displayname.controller.Ga4DisplayNameController;
import com.quitmate.appMetrics.ga4.displayname.controller.request.Ga4DisplayNameSaveRequest;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4NameOrigin;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4SourceType;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import com.quitmate.appMetrics.ga4.displayname.service.Ga4DisplayNameService;
import com.quitmate.appMetrics.ga4.displayname.service.response.Ga4DisplayNameResponse;
import com.quitmate.appMetrics.ga4.displayname.service.response.Ga4UnnamedValuesResponse;
import com.quitmate.appMetrics.ga4.displayname.service.response.Ga4UnnamedValuesResponse.UnnamedValue;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
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

public class Ga4DisplayNameControllerDocsTest extends RestDocsSupport {

    private static final String URL = "/api/v1/app-metrics/ga4/display-names";
    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 11, 27, 21, 30, 0);

    private final Ga4DisplayNameService ga4DisplayNameService = mock(Ga4DisplayNameService.class);

    @Override
    protected Object initController() {
        return new Ga4DisplayNameController(ga4DisplayNameService);
    }

    @DisplayName("GA4 이름 목록 조회 API")
    @Test
    void GA4_이름_목록_조회_API() throws Exception {
        given(ga4DisplayNameService.getDisplayNames(any())).willReturn(List.of(
                new Ga4DisplayNameResponse(null, Ga4ValueType.SCREEN, "(main)/(tabs)", "[앱] 홈 탭",
                        Ga4NameOrigin.DEFAULT, "[앱] 홈 탭", Ga4SourceType.APP, null, null),
                new Ga4DisplayNameResponse(3L, Ga4ValueType.SCREEN, "(main)/(tabs)/awards", "[앱] 챌린지 목록",
                        Ga4NameOrigin.ADMIN, "[앱] 챌린지 탭", Ga4SourceType.APP, 12L, UPDATED_AT)));

        mockMvc.perform(get(URL).param("type", "SCREEN"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("app-metrics-ga4-display-names",
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("type").description("값 종류: SCREEN(화면), PLATFORM(플랫폼), APP_VERSION(앱 버전). 그 밖·빈 값은 400")
                        ),
                        responseFields(commonFields(JsonFieldType.ARRAY, "종류별 이름 목록. 기본 이름표 값(설계 문서 순서) 다음에 관리자만 이름을 넣은 값(원래 값 순서)"))
                                .andWithPrefix("data[].", nameFields())
                ));
    }

    @DisplayName("GA4 이름 없는 값 조회 API")
    @Test
    void GA4_이름_없는_값_조회_API() throws Exception {
        given(ga4DisplayNameService.getUnnamedValues(any())).willReturn(new Ga4UnnamedValuesResponse(
                true, Ga4ValueType.SCREEN, LocalDate.of(2026, 8, 29), LocalDate.of(2026, 11, 26),
                OffsetDateTime.parse("2026-11-27T21:30:00+09:00"),
                List.of(new UnnamedValue("금연 앱 Quitmate — 혼자 참지 마세요", 300, "[웹] 금연 앱 Quitmate — 혼자 참지 마세요",
                                Ga4SourceType.WEB),
                        new UnnamedValue("(main)/new-screen", 20, "(main)/new-screen", null))));

        mockMvc.perform(get(URL + "/unnamed").param("type", "SCREEN"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("app-metrics-ga4-display-names-unnamed",
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("type").description("값 종류: SCREEN, PLATFORM, APP_VERSION")
                        ),
                        responseFields(commonFields(JsonFieldType.OBJECT, "이름 없는 값"))
                                .and(
                                        fieldWithPath("data.enabled").type(JsonFieldType.BOOLEAN).description("GA4 연결 설정이 있으면 true. false면 날짜는 null, values는 빈 배열"),
                                        fieldWithPath("data.valueType").type(JsonFieldType.STRING).description("값 종류"),
                                        fieldWithPath("data.startDate").type(JsonFieldType.STRING).description("시작 날짜 yyyy-MM-dd(끝 날짜 포함 90일)"),
                                        fieldWithPath("data.endDate").type(JsonFieldType.STRING).description("끝 날짜 yyyy-MM-dd(GA4 속성 보고 시간대 기준 어제)"),
                                        fieldWithPath("data.fetchedAt").type(JsonFieldType.STRING).description("GA4를 실제로 부른 시각(+09:00). 종류별 1시간 캐시"),
                                        fieldWithPath("data.values[]").type(JsonFieldType.ARRAY).description("관리자 이름·기본 이름이 모두 없는 값(숫자 내림차순). 화면·버전은 GA4 상위 200개 안에서 찾는다"),
                                        fieldWithPath("data.values[].rawValue").type(JsonFieldType.STRING).description("GA4에 들어온 원래 값"),
                                        fieldWithPath("data.values[].count").type(JsonFieldType.NUMBER).description("화면은 조회 수, 플랫폼·버전은 활성 사용자(90일)"),
                                        fieldWithPath("data.values[].displayName").type(JsonFieldType.STRING).description("지금 보이는 이름(원래 값 또는 '[웹] 원래 값')"),
                                        fieldWithPath("data.values[].sourceType").type(JsonFieldType.STRING).optional().description("짐작한 앱/웹 구분(괄호로 시작하지 않는 화면은 WEB). 모르면 null")
                                )
                ));
    }

    @DisplayName("GA4 이름 저장 API")
    @Test
    void GA4_이름_저장_API() throws Exception {
        given(ga4DisplayNameService.save(any())).willReturn(
                new Ga4DisplayNameResponse(3L, Ga4ValueType.SCREEN, "(main)/(tabs)/awards", "[앱] 챌린지 목록",
                        Ga4NameOrigin.ADMIN, "[앱] 챌린지 탭", Ga4SourceType.APP, 12L, UPDATED_AT));
        Ga4DisplayNameSaveRequest request = Ga4DisplayNameSaveRequest.builder()
                .valueType("SCREEN")
                .rawValue("(main)/(tabs)/awards")
                .displayName("[앱] 챌린지 목록")
                .sourceType("APP")
                .build();

        mockMvc.perform(put(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("app-metrics-ga4-display-names-save",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("valueType").type(JsonFieldType.STRING).description("값 종류: SCREEN, PLATFORM, APP_VERSION"),
                                fieldWithPath("rawValue").type(JsonFieldType.STRING).description("GA4에 들어온 원래 값(정확히 같아야 이름이 붙는다, 255자까지)"),
                                fieldWithPath("displayName").type(JsonFieldType.STRING).description("보일 이름(앞뒤 공백은 지움, 1~100자)"),
                                fieldWithPath("sourceType").type(JsonFieldType.STRING).optional().description("APP·WEB. 화면(SCREEN)에만 넣고 다른 종류에 넣으면 400. 없으면 구분 없음")
                        ),
                        responseFields(commonFields(JsonFieldType.OBJECT, "저장한 뒤의 이름"))
                                .andWithPrefix("data.", nameFields())
                ));
    }

    @DisplayName("GA4 이름 되돌리기 API")
    @Test
    void GA4_이름_되돌리기_API() throws Exception {
        given(ga4DisplayNameService.delete(anyLong())).willReturn(
                new Ga4DisplayNameResponse(null, Ga4ValueType.SCREEN, "(main)/(tabs)/awards", "[앱] 챌린지 탭",
                        Ga4NameOrigin.DEFAULT, "[앱] 챌린지 탭", Ga4SourceType.APP, null, null));

        mockMvc.perform(delete(URL + "/{id}", 3L))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("app-metrics-ga4-display-names-delete",
                        preprocessResponse(prettyPrint()),
                        pathParameters(
                                parameterWithName("id").description("관리자 이름 ID(목록의 id). 없으면 404")
                        ),
                        responseFields(commonFields(JsonFieldType.OBJECT, "되돌린 뒤의 이름(기본 이름, 없으면 원래 값)"))
                                .andWithPrefix("data.", nameFields())
                ));
    }

    private static List<FieldDescriptor> commonFields(JsonFieldType dataType, String dataDescription) {
        return List.of(
                fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"),
                fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"),
                fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"),
                fieldWithPath("data").type(dataType).description(dataDescription));
    }

    private static List<FieldDescriptor> nameFields() {
        return List.of(
                fieldWithPath("id").type(JsonFieldType.NUMBER).optional().description("관리자 이름 ID. 관리자 이름이 없으면 null(되돌릴 것이 없음)"),
                fieldWithPath("valueType").type(JsonFieldType.STRING).description("값 종류"),
                fieldWithPath("rawValue").type(JsonFieldType.STRING).description("GA4에 들어온 원래 값"),
                fieldWithPath("displayName").type(JsonFieldType.STRING).description("지금 보이는 이름"),
                fieldWithPath("origin").type(JsonFieldType.STRING).description("이름이 어디서 왔는지: ADMIN(관리자), DEFAULT(기본 이름표), NONE(없음)"),
                fieldWithPath("defaultName").type(JsonFieldType.STRING).optional().description("기본 이름표의 이름(되돌리면 이 이름). 없으면 null"),
                fieldWithPath("sourceType").type(JsonFieldType.STRING).optional().description("앱 화면 APP / 웹 페이지 WEB(화면만). 모르면 null"),
                fieldWithPath("updatedBy").type(JsonFieldType.NUMBER).optional().description("관리자 이름을 마지막으로 고친 관리자 userId. 관리자 이름이 없으면 null"),
                fieldWithPath("updatedAt").type(JsonFieldType.STRING).optional().description("관리자 이름을 마지막으로 고친 시각(yyyy-MM-ddTHH:mm:ss). 관리자 이름이 없으면 null"));
    }
}
