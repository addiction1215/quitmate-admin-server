package com.quitmate.appMetrics.ga4.client;

import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.quitmate.appMetrics.ga4.config.Ga4Properties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * GA4 Data API를 부를 Bearer 토큰을 서비스 계정 키로 받는다(KAN-218).
 *
 * <p>키 json은 처음 부를 때 한 번 읽는다. 토큰은 google-auth 라이브러리가 들고 있다가 만료가 가까우면 다시 받는다.
 * 키 형식은 서비스 계정({@code "type": "service_account"})만 받는다.
 * 키·토큰 값은 로그·예외 메시지에 넣지 않는다.
 */
@Component
public class Ga4AccessTokenProvider {

    /** GA4 보고서 읽기 전용 권한 */
    static final String SCOPE = "https://www.googleapis.com/auth/analytics.readonly";

    private final Ga4Properties properties;
    /** 테스트에서 가짜 토큰 서버를 끼울 때만 쓴다. null이면 라이브러리 기본 HTTP 클라이언트 */
    private final HttpTransportFactory transportFactory;
    private volatile GoogleCredentials credentials;

    @Autowired
    public Ga4AccessTokenProvider(Ga4Properties properties) {
        this(properties, null);
    }

    Ga4AccessTokenProvider(Ga4Properties properties, HttpTransportFactory transportFactory) {
        this.properties = properties;
        this.transportFactory = transportFactory;
    }

    /**
     * @throws Ga4UnavailableException 키 파일이 없거나 읽지 못했거나, 토큰을 받지 못했으면
     */
    public String accessToken() {
        try {
            GoogleCredentials loaded = credentials();
            loaded.refreshIfExpired();
            AccessToken token = loaded.getAccessToken();
            if (token == null || !StringUtils.hasText(token.getTokenValue())) {
                throw new Ga4UnavailableException("GA4 토큰 응답이 비어 있습니다.");
            }
            return token.getTokenValue();
        } catch (Ga4UnavailableException e) {
            throw e;
        } catch (Exception e) {
            // 라이브러리 예외 메시지에는 토큰 서버 응답 본문이 들어갈 수 있어 종류만 남긴다.
            throw new Ga4UnavailableException("GA4 토큰을 받지 못했습니다(" + e.getClass().getSimpleName() + ").", e);
        }
    }

    private GoogleCredentials credentials() throws IOException {
        GoogleCredentials current = credentials;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (credentials == null) {
                Path file = properties.credentialsFile()
                        .orElseThrow(() -> new Ga4UnavailableException("GA4 서비스 계정 키 파일 경로가 없습니다."));
                try (InputStream in = Files.newInputStream(file)) {
                    ServiceAccountCredentials account = transportFactory == null
                            ? ServiceAccountCredentials.fromStream(in)
                            : ServiceAccountCredentials.fromStream(in, transportFactory);
                    credentials = account.createScoped(List.of(SCOPE));
                }
            }
            return credentials;
        }
    }
}
