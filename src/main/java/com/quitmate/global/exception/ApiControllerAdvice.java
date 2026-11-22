package com.quitmate.global.exception;

import com.quitmate.appMetrics.ga4.client.Ga4UnavailableException;
import com.quitmate.global.ApiResponse;
import com.quitmate.global.appserver.AppServerException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.AccessDeniedException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ApiControllerAdvice {

    /**
     * 예상치 못한 서버로직에러 발생시 처리
     */
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiResponse<Object> exception(Exception e) throws IOException {
        log.error(e.getMessage(), e);
        return ApiResponse.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "서버 로직 에러",
                null
        );
    }

    /**
     * 스프링 MVC가 던지는 "예상된" 요청 오류. 따로 잡지 않으면 위의 Exception 처리기로 가서 500이 된다.
     * 401은 내지 않는다(관리자 웹은 401을 받으면 로그아웃한다). 로그에는 메서드·경로·예외 이름만 남긴다(쿼리·값·토큰 제외).
     */
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoResourceFoundException.class)
    public ApiResponse<Object> noResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        logMvcError(request, e);
        return ApiResponse.of(HttpStatus.NOT_FOUND, "요청한 주소를 찾을 수 없습니다.", null);
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResponse<Object> methodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        logMvcError(request, e);
        return ApiResponse.of(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다.", null);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResponse<Object> missingParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        logMvcError(request, e);
        return ApiResponse.of(HttpStatus.BAD_REQUEST, "필수 값이 없습니다: " + e.getParameterName(), null);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ApiResponse<Object> argumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        logMvcError(request, e);
        return ApiResponse.of(HttpStatus.BAD_REQUEST, "값의 형식이 올바르지 않습니다: " + e.getName(), null);
    }

    @ResponseStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ApiResponse<Object> mediaTypeNotSupported(HttpMediaTypeNotSupportedException e, HttpServletRequest request) {
        logMvcError(request, e);
        return ApiResponse.of(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 요청 형식입니다.", null);
    }

    private void logMvcError(HttpServletRequest request, Exception e) {
        log.warn("잘못된 요청 - {} {} ({})", request.getMethod(), request.getRequestURI(), e.getClass().getSimpleName());
    }

    /**
     * validation Exception
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ApiResponse<Object> bindException(BindException e) {
        log.error(e.getMessage());
        return ApiResponse.of(
                HttpStatus.BAD_REQUEST,
                e.getBindingResult().getAllErrors().get(0).getDefaultMessage(),
                null
        );
    }

    /**
     * 임의로 발생시킨 CustomException처리
     */
    /**
     * 요청 본문 JSON을 읽지 못한 경우(날짜·enum 형식 오류 등). 따로 잡지 않으면 500이 된다.
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Object> notReadable(HttpMessageNotReadableException e) {
        log.warn("요청 본문을 읽지 못함: {}", e.getMostSpecificCause().getClass().getSimpleName());
        return ApiResponse.of(
                HttpStatus.BAD_REQUEST,
                "요청 형식이 올바르지 않습니다. 날짜·시각은 2026-11-12T00:00:00 형식으로 보내 주세요.",
                null
        );
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(QuitmateException.class)
    public ApiResponse<Object> QuitmateException(QuitmateException e) {
        log.error(e.getMessage());
        return ApiResponse.of(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                null
        );
    }

    /**
     * FeignClient OAuth 호출 오류
     */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(FeignException.FeignClientException.class)
    public ApiResponse<Object> FeignClientException(FeignException.FeignClientException e) {
        log.error(e.getMessage());
        return ApiResponse.of(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                null
        );
    }

    /**
     * 메서드에 전달된 인자가 부적절할 때 발생하는 예외
     **/
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Object> NotFoundIdxException(IllegalArgumentException e) {
        log.error(e.getMessage());
        return ApiResponse.of(
                HttpStatus.BAD_REQUEST,
                e.getMessage(),
                null
        );
    }

    /**
     * 데이터 제약 조건으로 인한 Exception
     **/
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiResponse<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.error(e.getMessage());
        return ApiResponse.of(
                HttpStatus.BAD_REQUEST,
                "데이터 제약 조건으로 인해 작업에 실패했습니다.",
                null
        );
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictException.class)
    public ApiResponse<Object> conflict(ConflictException e) {
        log.warn(e.getMessage());
        return ApiResponse.of(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public ApiResponse<Object> forbiddenRequest(ForbiddenException e) {
        log.warn(e.getMessage());
        return ApiResponse.of(HttpStatus.FORBIDDEN, e.getMessage(), null);
    }

    /**
     * 앱 서버 호출 실패. 앱 서버가 준 400·409는 사유를 그대로, 그 밖의 실패는 502·503·504로 돌려준다.
     * 관리자 화면은 401을 받으면 로그아웃하므로, 앱 서버의 401·403을 그대로 넘기지 않는다.
     */
    @ExceptionHandler(AppServerException.class)
    public ResponseEntity<ApiResponse<Object>> appServer(AppServerException e) {
        log.warn("앱 서버 호출 실패 - {} {}", e.getStatus(), e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(ApiResponse.of(e.getStatus(), e.getMessage(), null));
    }

    /**
     * GA4 지표를 받지 못함(권한·쿼터·네트워크·응답 이상, KAN-218). 화면은 message로 'GA4 응답 실패'를 띄운다.
     * 사유는 로그에만 남긴다(키·토큰·속성 ID는 사유에 넣지 않는다).
     */
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(Ga4UnavailableException.class)
    public ApiResponse<Object> ga4Unavailable(Ga4UnavailableException e) {
        log.warn("GA4 지표를 받지 못함 - {}", e.getMessage());
        return ApiResponse.of(HttpStatus.SERVICE_UNAVAILABLE, Ga4UnavailableException.RESPONSE_MESSAGE, null);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ApiResponse<Object> notFoundDataException(NotFoundException e) {
        log.error(e.getMessage());
        return ApiResponse.of(
                HttpStatus.NOT_FOUND,
                "데이터가 존재하지 않습니다.",
                null
        );
    }

    // 401 - 커스텀
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UnauthenticatedException.class)
    public ApiResponse<Object> unauthenticated(UnauthenticatedException e) {
        log.error(e.getMessage());
        return ApiResponse.of(HttpStatus.UNAUTHORIZED, e.getMessage(), null);
    }

    // 401 - 스프링 표준
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({
            AuthenticationCredentialsNotFoundException.class,
            InsufficientAuthenticationException.class
    })
    public ApiResponse<Object> spring401(Exception e) {
        log.error(e.getMessage());
        return ApiResponse.of(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.", null);
    }

    // 403 - 권한 부족
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Object> forbidden(AccessDeniedException e) {
        log.error(e.getMessage());
        return ApiResponse.of(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", null);
    }
}
