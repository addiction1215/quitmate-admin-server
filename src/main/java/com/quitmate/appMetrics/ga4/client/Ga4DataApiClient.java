package com.quitmate.appMetrics.ga4.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * GA4 Data API(v1beta) {@code runReport}를 REST로 부른다(KAN-218).
 *
 * <p>gRPC 클라이언트(google-analytics-data) 대신 토큰만 google-auth로 받고 HTTP는 Spring RestClient로 보낸다.
 * 실패(권한·쿼터·네트워크·응답 이상)는 모두 {@link Ga4UnavailableException}(503)이다.
 * 사유에는 HTTP 상태와 GA4 오류 상태(예: PERMISSION_DENIED)만 넣고, 토큰·속성 ID·응답 본문은 넣지 않는다.
 */
@Component
public class Ga4DataApiClient {

    static final String DEFAULT_BASE_URL = "https://analyticsdata.googleapis.com";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);
    private static final Pattern DIGITS = Pattern.compile("\\d+");
    private static final Pattern ERROR_STATUS = Pattern.compile("[A-Z_]{1,40}");

    private final Ga4AccessTokenProvider tokenProvider;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final String baseUrl;

    @Autowired
    public Ga4DataApiClient(Ga4AccessTokenProvider tokenProvider, ObjectMapper objectMapper,
                            RestClient.Builder restClientBuilder) {
        this(tokenProvider, objectMapper, restClientBuilder, DEFAULT_BASE_URL, CONNECT_TIMEOUT, READ_TIMEOUT);
    }

    /** 테스트에서 가짜 GA4 서버 주소를 넣을 때 쓴다. */
    Ga4DataApiClient(Ga4AccessTokenProvider tokenProvider, ObjectMapper objectMapper,
                     RestClient.Builder restClientBuilder, String baseUrl,
                     Duration connectTimeout, Duration readTimeout) {
        this.tokenProvider = tokenProvider;
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = restClientBuilder.clone().requestFactory(requestFactory).build();
    }

    /**
     * @param propertyId 숫자만 있는 GA4 속성 ID
     * @throws Ga4UnavailableException 토큰을 받지 못했거나, GA4가 실패를 줬거나, 응답을 읽지 못했으면
     */
    public Ga4Report runReport(String propertyId, Ga4ReportRequest request) {
        if (propertyId == null || !DIGITS.matcher(propertyId).matches()) {
            throw new Ga4UnavailableException("GA4 속성 ID 형식이 올바르지 않습니다.");
        }
        String token = tokenProvider.accessToken();
        URI uri = URI.create(baseUrl + "/v1beta/properties/" + propertyId + ":runReport");
        try {
            JsonNode body = restClient.post()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request.toBody())
                    .retrieve()
                    .body(JsonNode.class);
            return Ga4Report.parse(body, request);
        } catch (RestClientResponseException e) {
            throw new Ga4UnavailableException("GA4 오류 응답 " + e.getStatusCode().value() + " "
                    + errorStatus(e.getResponseBodyAsString()) + "(" + request.name() + ").", e);
        } catch (RestClientException e) {
            throw new Ga4UnavailableException("GA4를 부르지 못했습니다(" + request.name() + ", "
                    + rootCauseName(e) + ").", e);
        }
    }

    /** GA4 오류 본문의 error.status(예: PERMISSION_DENIED, RESOURCE_EXHAUSTED). 읽지 못하면 UNKNOWN */
    private String errorStatus(String body) {
        if (!StringUtils.hasText(body)) {
            return "UNKNOWN";
        }
        try {
            String status = objectMapper.readTree(body).path("error").path("status").asText("");
            return ERROR_STATUS.matcher(status).matches() ? status : "UNKNOWN";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private static String rootCauseName(Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName();
    }
}
