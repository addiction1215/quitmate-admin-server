package com.quitmate.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @Scheduled 를 켠다. 관리자 서버의 첫 스케줄 작업은 위치정보 열람 이력 보관 기간 삭제(KAN-208)다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
