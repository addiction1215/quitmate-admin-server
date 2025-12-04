package docs.user;

import com.quitmate.global.page.response.PageCustom;
import com.quitmate.global.page.response.PageableCustom;
import com.quitmate.user.users.controller.UserController;
import com.quitmate.user.users.entity.enums.Sex;
import com.quitmate.user.users.entity.enums.UserSearchCategory;
import com.quitmate.user.users.entity.enums.UserSortType;
import com.quitmate.user.users.service.UserReadService;
import com.quitmate.user.users.service.response.UserListResponse;
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

public class UserControllerDocsTest extends RestDocsSupport {

    private final UserReadService userReadService = mock(UserReadService.class);

    @Override
    protected Object initController() {
        return new UserController(userReadService);
    }

    @DisplayName("회원 목록 조회 API")
    @Test
    void 회원_목록_조회_API() throws Exception {
        // given
        UserListResponse user1 = UserListResponse.builder()
                .id(1L)
                .createdDate(LocalDateTime.of(2024, 1, 1, 10, 0))
                .email("user1@example.com")
                .nickName("사용자1")
                .birthDay("19900101")
                .sex(Sex.MALE)
                .build();

        UserListResponse user2 = UserListResponse.builder()
                .id(2L)
                .createdDate(LocalDateTime.of(2024, 1, 2, 10, 0))
                .email("user2@example.com")
                .nickName("사용자2")
                .birthDay("19910202")
                .sex(Sex.FEMALE)
                .build();

        PageableCustom pageableCustom = PageableCustom.builder()
                .currentPage(1)
                .totalPage(1)
                .totalElement(2L)
                .build();

        PageCustom<UserListResponse> pageCustom = PageCustom.<UserListResponse>builder()
                .content(List.of(user1, user2))
                .pageInfo(pageableCustom)
                .build();

        given(userReadService.getUserList(any()))
                .willReturn(pageCustom);

        // when // then
        mockMvc.perform(
                        get("/api/v1/users")
                                .param("page", "1")
                                .param("size", "10")
                                .param("sortBy", "CREATED_DATE")
                                .param("category", "EMAIL")
                                .param("keyword", "user")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document("user-list",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 1)").optional(),
                                parameterWithName("size").description("페이지 크기 (기본값: 10)").optional(),
                                parameterWithName("sortBy").description("정렬 기준: " + 
                                        Arrays.toString(UserSortType.values()) + " (선택)").optional(),
                                parameterWithName("category").description("검색 카테고리: " + 
                                        Arrays.toString(UserSearchCategory.values()) + " (선택)").optional(),
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
                                        .description("회원 목록"),
                                fieldWithPath("data.content[].id").type(JsonFieldType.NUMBER)
                                        .description("회원 ID"),
                                fieldWithPath("data.content[].createdDate").type(JsonFieldType.STRING)
                                        .description("가입일자"),
                                fieldWithPath("data.content[].email").type(JsonFieldType.STRING)
                                        .description("이메일"),
                                fieldWithPath("data.content[].nickName").type(JsonFieldType.STRING)
                                        .description("유저명"),
                                fieldWithPath("data.content[].birthDay").type(JsonFieldType.STRING)
                                        .description("생년월일 (YYYYMMDD)"),
                                fieldWithPath("data.content[].sex").type(JsonFieldType.STRING)
                                        .description("성별: " + Arrays.toString(Sex.values())),
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
