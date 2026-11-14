package com.quitmate.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @Scheduled} 작업을 켠다. 지금은 야간에 보류한 이벤트 공지 푸시 발송(KAN-109) 하나뿐이다.
 *
 * <ul>
 *   <li>스레드: Spring Boot 기본 스케줄러(ThreadPoolTaskScheduler, 스레드 1개 {@code scheduling-1}).
 *       작업이 늘어 서로 기다리게 되면 {@code spring.task.scheduling.pool.size} 를 올린다.</li>
 *   <li>{@code @Async}(공지 즉시 푸시)는 그대로 {@code applicationTaskExecutor} 에서 돈다. 스케줄러와 다른 스레드 풀이다.</li>
 *   <li>test 프로필에서는 켜지 않는다. 작업이 테스트 데이터를 집어 가지 않게 하고, 테스트는 작업 메서드를 직접 부른다.</li>
 * </ul>
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {
}
