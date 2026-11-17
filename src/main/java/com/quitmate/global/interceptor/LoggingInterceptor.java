package com.quitmate.global.interceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * 모든 API의 JSON 요청·응답 본문을 INFO로 남긴다.
 * 비밀번호·토큰·주소·좌표는 값을 {@link #REDACTED}로 바꿔 남긴다(KAN-205). 앱 서버 LoggingInterceptor와 같은 규칙이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingInterceptor implements HandlerInterceptor {

    static final String REDACTED = "[REDACTED]";

    /** 본문을 통째로 남기지 않는 경로. 로그인은 요청에 비밀번호, 응답에 토큰이 있다. */
    private static final Set<String> SENSITIVE_PATHS = Set.of(
            "/api/v1/auth/login"
    );

    /** 비밀번호·토큰 키. 키 이름은 소문자로 바꾸고 '_'·'-'를 뺀 뒤 비교한다. */
    private static final Set<String> SECRET_FIELDS = Set.of(
            "password",
            "currentpassword",
            "newpassword",
            "newpasswordconfirm",
            "token",
            "accesstoken",
            "refreshtoken",
            "authcode"
    );

    /**
     * 이름에 이 낱말이 들어간 키는 주소·좌표로 보고 가린다.
     * 미션 이력 상세의 locationData.addresses(앱이 보낸 "위도,경도"), 앱 서버와 같은 address·latitude·longitude·lastSmokeAddress를 덮는다.
     */
    private static final List<String> LOCATION_KEYWORDS = List.of(
            "address",
            "latitude",
            "longitude"
    );

    /** 낱말로는 안 잡히는 좌표 키(정확도). */
    private static final Set<String> LOCATION_FIELDS = Set.of(
            "locationaccuracy",
            "accuracymeters"
    );

    private final ObjectMapper objectMapper;

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
        String path = request.getRequestURI();

        //요청 응답이 appliction/json일 경우에만 내용 처리
        String requestStr = sanitizeBody(path, extractJsonBody(cachingRequest.getContentType(), cachingRequest.getContentAsByteArray()));
        String responseStr = sanitizeBody(path, extractJsonBody(cachingResponse.getContentType(), cachingResponse.getContentAsByteArray()));

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

    /**
     * 로그에 남길 본문. 민감 경로는 통째로, 그 밖은 민감 키 값만 가린다.
     * JSON으로 읽지 못한 본문은 어디에 무엇이 있는지 알 수 없어 통째로 가린다.
     */
    String sanitizeBody(String requestUri, String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        if (isSensitivePath(requestUri)) {
            return REDACTED;
        }
        return maskSensitiveFields(body);
    }

    private boolean isSensitivePath(String requestUri) {
        return SENSITIVE_PATHS.contains(normalizePath(requestUri));
    }

    private String normalizePath(String requestUri) {
        if (requestUri == null || requestUri.isEmpty()) {
            return "";
        }
        String path = requestUri;
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private String maskSensitiveFields(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            maskNode(node);
            return cleanString(objectMapper.writeValueAsString(node));
        } catch (Exception e) {
            return REDACTED;
        }
    }

    /** 객체·배열을 끝까지 내려가며 민감 키의 값을 가린다. 값이 null이면 보냈는지 여부만 보이도록 그대로 둔다. */
    private void maskNode(JsonNode node) {
        if (node == null || !node.isContainerNode()) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                JsonNode value = objectNode.get(fieldName);
                if (isSensitiveField(fieldName)) {
                    if (!value.isNull()) {
                        objectNode.put(fieldName, REDACTED);
                    }
                    continue;
                }
                maskNode(value);
            }
            return;
        }
        node.forEach(this::maskNode);
    }

    static boolean isSensitiveField(String fieldName) {
        String key = fieldName.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        if (SECRET_FIELDS.contains(key) || LOCATION_FIELDS.contains(key)) {
            return true;
        }
        return LOCATION_KEYWORDS.stream().anyMatch(key::contains);
    }

    private String extractJsonBody(String contentType, byte[] contentBytes) {
        if (contentType != null && contentType.contains("application/json") && contentBytes.length != 0) {
            return cleanString(new String(contentBytes, StandardCharsets.UTF_8));
        }
        return "";
    }
}
