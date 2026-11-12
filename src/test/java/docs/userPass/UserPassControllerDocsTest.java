package docs.userPass;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import com.quitmate.user.userPass.controller.UserPassController;
import com.quitmate.user.userPass.controller.request.UserPassGrantRequest;
import com.quitmate.user.userPass.controller.request.UserPassRevokeRequest;
import com.quitmate.user.userPass.enums.UserPassProduct;
import com.quitmate.user.userPass.enums.UserPassSource;
import com.quitmate.user.userPass.enums.UserPassStatus;
import com.quitmate.user.userPass.service.UserPassService;
import com.quitmate.user.userPass.service.response.UserPassResponse;
import docs.RestDocsSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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

public class UserPassControllerDocsTest extends RestDocsSupport {

    private final UserPassService userPassService = mock(UserPassService.class);

    @Override
    protected Object initController() {
        return new UserPassController(userPassService);
    }

    @DisplayName("이용권 목록 조회 API")
    @Test
    void 이용권_목록_조회_API() throws Exception {
        given(userPassService.getUserPasses(any())).willReturn(PageCustom.<UserPassResponse>builder()
                .content(List.of(response(UserPassStatus.ACTIVE, null, "베타 테스터")))
                .pageInfo(PageableCustom.builder().currentPage(1).totalPage(1).totalElement(1).build())
                .build());

        mockMvc.perform(get("/api/v1/admin/user-passes")
                        .param("page", "1").param("size", "20")
                        .param("userId", "81").param("status", "ACTIVE"))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("user-pass-list",
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호(1부터)"),
                                parameterWithName("size").description("페이지 크기"),
                                parameterWithName("userId").optional().description("이 회원의 이용권만(없으면 전체)"),
                                parameterWithName("status").optional().description("ACTIVE(이용 중) / SCHEDULED(시작 전) / EXPIRED(만료) / REVOKED(회수). 없으면 전체")
                        ),
                        responseFields(pageResponse())
                ));
    }

    @DisplayName("이용권 부여 API")
    @Test
    void 이용권_부여_API() throws Exception {
        given(userPassService.grant(any())).willReturn(response(UserPassStatus.ACTIVE, null, "베타 테스터"));
        UserPassGrantRequest request = UserPassGrantRequest.builder()
                .userId(81L)
                .product(UserPassProduct.QUITMATE_90)
                .startsAt(LocalDateTime.of(2026, 11, 12, 0, 0))
                .endsAt(LocalDateTime.of(2027, 2, 10, 0, 0))
                .memo("베타 테스터")
                .build();

        mockMvc.perform(post("/api/v1/admin/user-passes")
                        .content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andDo(document("user-pass-grant",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("userId").type(JsonFieldType.NUMBER).description("회원 ID"),
                                fieldWithPath("product").type(JsonFieldType.STRING).optional().description("상품. 없으면 QUITMATE_90"),
                                fieldWithPath("startsAt").type(JsonFieldType.STRING).description("시작 시각(한국 시간, 포함)"),
                                fieldWithPath("endsAt").type(JsonFieldType.STRING).description("만료 시각(한국 시간, 이 시각부터 무효). 시작보다 뒤이고 지금 이후여야 함"),
                                fieldWithPath("memo").type(JsonFieldType.STRING).optional().description("메모(255자까지)")
                        ),
                        responseFields(singleResponse())
                ));
    }

    @DisplayName("이용권 회수 API")
    @Test
    void 이용권_회수_API() throws Exception {
        given(userPassService.revoke(any())).willReturn(
                response(UserPassStatus.REVOKED, LocalDateTime.of(2026, 11, 20, 15, 0), "베타 테스터\n[회수] 테스트 종료"));

        mockMvc.perform(patch("/api/v1/admin/user-passes/{passId}/revoke", 5L)
                        .content(objectMapper.writeValueAsString(UserPassRevokeRequest.builder().reason("테스트 종료").build()))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("user-pass-revoke",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        pathParameters(parameterWithName("passId").description("이용권 ID")),
                        requestFields(
                                fieldWithPath("reason").type(JsonFieldType.STRING).optional()
                                        .description("회수 사유(선택). 본문을 아예 보내지 않아도 됨. 메모 뒤에 \"[회수] 사유\"로 붙음")
                        ),
                        responseFields(singleResponse())
                ));
    }

    private static UserPassResponse response(UserPassStatus status, LocalDateTime revokedAt, String memo) {
        return UserPassResponse.builder()
                .id(5L)
                .userId(81L)
                .email("member@example.com")
                .nickName("금연왕")
                .product(UserPassProduct.QUITMATE_90)
                .source(UserPassSource.ADMIN_GRANT)
                .startsAt(LocalDateTime.of(2026, 11, 12, 0, 0))
                .endsAt(LocalDateTime.of(2027, 2, 10, 0, 0))
                .revokedAt(revokedAt)
                .status(status)
                .memo(memo)
                .createdDate(LocalDateTime.of(2026, 11, 12, 10, 30))
                .build();
    }

    private static List<FieldDescriptor> passFields(String prefix) {
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(fieldWithPath(prefix + "id").type(JsonFieldType.NUMBER).description("이용권 ID"));
        fields.add(fieldWithPath(prefix + "userId").type(JsonFieldType.NUMBER).description("회원 ID"));
        fields.add(fieldWithPath(prefix + "email").type(JsonFieldType.STRING).optional().description("회원 이메일(회원이 없으면 null)"));
        fields.add(fieldWithPath(prefix + "nickName").type(JsonFieldType.STRING).optional().description("회원 닉네임(회원이 없으면 null)"));
        fields.add(fieldWithPath(prefix + "product").type(JsonFieldType.STRING).description("상품: QUITMATE_90"));
        fields.add(fieldWithPath(prefix + "source").type(JsonFieldType.STRING).description("출처: ADMIN_GRANT(어드민 부여) / IAP(인앱 결제)"));
        fields.add(fieldWithPath(prefix + "startsAt").type(JsonFieldType.STRING).description("시작 시각(포함)"));
        fields.add(fieldWithPath(prefix + "endsAt").type(JsonFieldType.STRING).description("만료 시각(이 시각부터 무효)"));
        fields.add(fieldWithPath(prefix + "revokedAt").type(JsonFieldType.STRING).optional().description("회수 시각"));
        fields.add(fieldWithPath(prefix + "status").type(JsonFieldType.STRING).description("조회 시각 기준 상태: ACTIVE / SCHEDULED / EXPIRED / REVOKED"));
        fields.add(fieldWithPath(prefix + "memo").type(JsonFieldType.STRING).optional().description("메모"));
        fields.add(fieldWithPath(prefix + "createdDate").type(JsonFieldType.STRING).optional().description("부여(생성) 시각"));
        return fields;
    }

    /** 단건 응답: data = 이용권 하나 */
    private static List<FieldDescriptor> singleResponse() {
        List<FieldDescriptor> fields = common();
        fields.add(fieldWithPath("data").type(JsonFieldType.OBJECT).description("이용권"));
        fields.addAll(passFields("data."));
        return fields;
    }

    /** 목록 응답: data = { content[], pageInfo } */
    private static List<FieldDescriptor> pageResponse() {
        List<FieldDescriptor> fields = common();
        fields.add(fieldWithPath("data").type(JsonFieldType.OBJECT).description("응답 데이터"));
        fields.add(fieldWithPath("data.content[]").type(JsonFieldType.ARRAY).description("이용권 목록. 최근 부여 순"));
        fields.addAll(passFields("data.content[]."));
        fields.add(fieldWithPath("data.pageInfo").type(JsonFieldType.OBJECT).description("페이지 정보"));
        fields.add(fieldWithPath("data.pageInfo.currentPage").type(JsonFieldType.NUMBER).description("현재 페이지"));
        fields.add(fieldWithPath("data.pageInfo.totalPage").type(JsonFieldType.NUMBER).description("전체 페이지 수"));
        fields.add(fieldWithPath("data.pageInfo.totalElement").type(JsonFieldType.NUMBER).description("전체 건수"));
        return fields;
    }

    private static List<FieldDescriptor> common() {
        List<FieldDescriptor> fields = new ArrayList<>();
        fields.add(fieldWithPath("statusCode").type(JsonFieldType.NUMBER).description("코드"));
        fields.add(fieldWithPath("httpStatus").type(JsonFieldType.STRING).description("상태"));
        fields.add(fieldWithPath("message").type(JsonFieldType.STRING).description("메세지"));
        return fields;
    }
}
