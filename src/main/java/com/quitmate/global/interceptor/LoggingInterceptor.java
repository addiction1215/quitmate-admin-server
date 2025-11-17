package com.quitmate.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) {
        // HandlerMethod(Controller메소드인지)가 아니거나 Filter에서 Wrapping한 reequest가 아니라면 return
        if(!(handler instanceof HandlerMethod) || !(request instanceof ContentCachingRequestWrapper cachingRequest))
            return;

        // Spring Security관련 요청은 return
        if (request.getClass().getName().contains("SecurityContextHolderAwareRequestWrapper")) {
            return;
        }

        // Filter에서 Wrapping한 response
        final ContentCachingResponseWrapper cachingResponse = (ContentCachingResponseWrapper) response;

        String url = request.getRequestURL().toString();
        String queryStr = request.getQueryString();
        String method = cachingRequest.getMethod();

        //요청 응답이 appliction/json일 경우에만 내용 처리
        String requestStr = extractJsonBody(cachingRequest.getContentType(), cachingRequest.getContentAsByteArray());
        String responseStr = extractJsonBody(cachingResponse.getContentType(), cachingResponse.getContentAsByteArray());

        logging(url, queryStr, method, requestStr, responseStr);
    }

    public void logging(String url, String queryStr, String method, String requestStr, String responseStr) {
        StringBuilder logMessageBuilder = new StringBuilder("\n");
        if("GET".equals(method)) {
            url+="?"+ Optional.ofNullable(queryStr).orElse("");
        }
        logMessageBuilder.append("┌───────────────────────────────────────────────────────────────────────────────────────\n");
        logMessageBuilder.append("│Request URL: ").append(url).append("\n");
        logMessageBuilder.append("│Request Method: ").append(method).append("\n");
        if(!"".equals(requestStr)&&requestStr!=null) {
            logMessageBuilder.append("│Request Body: ").append(requestStr).append("\n");
        }
        if(!"".equals(responseStr)&&responseStr!=null) {
            logMessageBuilder.append("│Response Body: ").append(responseStr.replace("\n", "")).append("\n");
        }
        logMessageBuilder.append("└───────────────────────────────────────────────────────────────────────────────────────\n");
        log.info(logMessageBuilder.toString());
    }

    public String cleanString(String str) {
        return str.replace("\n", "").replace("\r", "").replace(" ", "");
    }

    private String extractJsonBody(String contentType, byte[] contentBytes) {
        if (contentType != null && contentType.contains("application/json") && contentBytes.length != 0) {
            return cleanString(new String(contentBytes, StandardCharsets.UTF_8));
        }
        return "";
    }
}
