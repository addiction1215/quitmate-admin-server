package com.quitmate.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 서버 시계. 시각 판단은 이 시계의 instant 를 한국 시각으로 바꿔서 한다(서버 시간대와 무관).
 * 테스트에서는 {@code @MockitoBean Clock} 으로 바꿔 끼운다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
