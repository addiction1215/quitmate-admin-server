package com.quitmate.global.appserver;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Map;

/**
 * 앱 서버 관리자 API 호출. 부를 때마다 {@link AppServerTokenIssuer}로 5분짜리 관리자 토큰을 만들어 보낸다.
 *
 * <p>앱 서버가 준 400·409는 사유(message)를 그대로 넘긴다. 그 밖의 실패는 관리자 화면이 로그아웃되지 않도록
 * 401·403이 아닌 502·503·504로 바꾼다.
 */
@Slf4j
@Component
public class AppServerClient {

    private static final String BASE_URL_NOT_CONFIGURED =
            "관리자 서버에 앱 서버 주소가 설정되지 않아 앱 서버를 부를 수 없습니다.";

    private final AppServerProperties properties;
    private final AppServerTokenIssuer tokenIssuer;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public AppServerClient(AppServerProperties properties, AppServerTokenIssuer tokenIssuer,
                           ObjectMapper objectMapper, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.tokenIssuer = tokenIssuer;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = restClientBuilder.clone().requestFactory(requestFactory).build();
    }

    public <T> T get(String path, Map<String, String> queryParams,
                     ParameterizedTypeReference<AppServerResponse<T>> responseType, Long adminId) {
        return call(HttpMethod.GET, uri(path, queryParams), null, responseType, adminId);
    }

    public <T> T post(String path, Object body,
                      ParameterizedTypeReference<AppServerResponse<T>> responseType, Long adminId) {
        return call(HttpMethod.POST, uri(path, Map.of()), body, responseType, adminId);
    }

    private <T> T call(HttpMethod method, URI uri, Object body,
                       ParameterizedTypeReference<AppServerResponse<T>> responseType, Long adminId) {
        String token = tokenIssuer.issue(adminId);
        try {
            RestClient.RequestBodySpec request = restClient.method(method)
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .accept(MediaType.APPLICATION_JSON);
            if (body != null) {
                request.contentType(MediaType.APPLICATION_JSON).body(body);
            }
            AppServerResponse<T> response = request.retrieve().body(responseType);
            if (response == null) {
                throw new AppServerException(HttpStatus.BAD_GATEWAY, "앱 서버 응답이 비어 있습니다.");
            }
            return response.getData();
        } catch (RestClientResponseException e) {
            throw translate(method, uri, e);
        } catch (RestClientException e) {
            // 응답 시간 초과는 연결 단계(ResourceAccessException)나 본문 읽기 단계(RestClientException) 어디서든 난다.
            if (isTimeout(e)) {
                log.warn("앱 서버 응답 시간 초과 - {} {}", method, uri.getPath());
                throw new AppServerException(HttpStatus.GATEWAY_TIMEOUT, method == HttpMethod.POST
                        ? "앱 서버 응답이 늦어 결과를 받지 못했습니다. 아직 처리 중일 수 있으니 잠시 뒤 날짜별 상태를 확인해 주세요."
                        : "앱 서버 응답이 늦습니다. 잠시 뒤 다시 시도해 주세요.", e);
            }
            if (e instanceof ResourceAccessException) {
                log.warn("앱 서버 연결 실패 - {} {} ({})", method, uri.getPath(), rootCauseName(e));
                throw new AppServerException(HttpStatus.BAD_GATEWAY, "앱 서버에 연결하지 못했습니다.", e);
            }
            log.warn("앱 서버 응답을 읽지 못함 - {} {} ({})", method, uri.getPath(), rootCauseName(e));
            throw new AppServerException(HttpStatus.BAD_GATEWAY, "앱 서버 응답을 읽지 못했습니다.", e);
        }
    }

    private static boolean isTimeout(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SocketTimeoutException) {
                return true;
            }
        }
        return false;
    }

    private static String rootCauseName(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName();
    }

    private AppServerException translate(HttpMethod method, URI uri, RestClientResponseException e) {
        int status = e.getStatusCode().value();
        String appMessage = readMessage(e.getResponseBodyAsString());
        log.warn("앱 서버 오류 응답 - {} {} → {} {}", method, uri.getPath(), status, appMessage);
        return switch (status) {
            case 400 -> new AppServerException(HttpStatus.BAD_REQUEST,
                    orDefault(appMessage, "앱 서버가 요청을 받지 않았습니다."));
            case 409 -> new AppServerException(HttpStatus.CONFLICT,
                    orDefault(appMessage, "지금은 처리할 수 없는 상태입니다."));
            case 401 -> new AppServerException(HttpStatus.BAD_GATEWAY,
                    "앱 서버가 관리자 서버 토큰을 받지 않았습니다. 관리자 서버의 앱 서버 서명 키 설정을 확인해 주세요.");
            case 403 -> new AppServerException(HttpStatus.BAD_GATEWAY,
                    "앱 서버가 관리자 권한을 인정하지 않았습니다.");
            case 404 -> new AppServerException(HttpStatus.BAD_GATEWAY,
                    "앱 서버에서 API를 찾지 못했습니다. 앱 서버 주소와 배포 버전을 확인해 주세요.");
            default -> new AppServerException(HttpStatus.BAD_GATEWAY,
                    "앱 서버에서 오류가 났습니다. (" + status + ")");
        };
    }

    private String readMessage(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode message = objectMapper.readTree(body).get("message");
            return message == null || message.isNull() ? null : message.asText();
        } catch (Exception e) {
            return null;
        }
    }

    private static String orDefault(String message, String defaultMessage) {
        return StringUtils.hasText(message) ? message : defaultMessage;
    }

    private URI uri(String path, Map<String, String> queryParams) {
        if (!properties.hasBaseUrl()) {
            throw new AppServerException(HttpStatus.SERVICE_UNAVAILABLE, BASE_URL_NOT_CONFIGURED);
        }
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.normalizedBaseUrl())
                    .path(path);
            queryParams.forEach(builder::queryParam);
            return builder.encode().build().toUri();
        } catch (IllegalArgumentException e) {
            throw new AppServerException(HttpStatus.SERVICE_UNAVAILABLE,
                    "관리자 서버의 앱 서버 주소 설정이 올바르지 않습니다.", e);
        }
    }
}
