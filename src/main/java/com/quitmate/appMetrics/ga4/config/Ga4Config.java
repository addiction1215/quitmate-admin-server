package com.quitmate.appMetrics.ga4.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * GA4 연결 설정 파일을 서버를 띄울 때 읽는다(KAN-218).
 *
 * <p>키를 jar·저장소·GitHub Secrets에 넣지 않도록, 운영 서버에 둔 파일을 실행 중에 읽는다.
 * 파일 위치는 환경 변수(또는 시스템 속성) {@code APP_METRICS_CONFIG}, 없으면
 * {@code /home/ubuntu/app-metrics/app-metrics.properties}. 배포가 {@code /home/ubuntu/admin-app}을 지우고 다시 깔기 때문에
 * 그 밖에 둔다. 파일이 없거나 읽을 수 없으면 건너뛰고 GA4는 '연결 전'(enabled=false)으로 응답한다.
 * 이 파일을 새로 두거나 고치면 서버를 다시 띄워야 읽힌다. 키 json은 처음 GA4를 부를 때 읽고, 읽지 못했으면 다음 호출에서
 * 다시 읽는다. 한 번 읽은 키를 바꿨으면 서버를 다시 띄운다.
 */
@Configuration
@EnableConfigurationProperties(Ga4Properties.class)
@PropertySource(
        value = "file:${APP_METRICS_CONFIG:/home/ubuntu/app-metrics/app-metrics.properties}",
        ignoreResourceNotFound = true,
        encoding = "UTF-8")
public class Ga4Config {
}
