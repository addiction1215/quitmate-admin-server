package com.quitmate.appMetrics.ga4.displayname.service;

import com.quitmate.appMetrics.ga4.displayname.repository.Ga4DisplayNameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * GA4 지표 응답에 붙일 지금 이름표를 읽는다(KAN-230).
 *
 * <p>이름 표를 읽지 못해도(표가 아직 없음·DB 오류) 지표 화면은 깨지지 않게 기본 이름표만으로 이어 간다.
 * 이름은 보조 정보라서 숫자 조회를 막지 않는다. 바깥 트랜잭션을 두지 않는다(저장소 조회가 자기 트랜잭션에서 실패해도
 * 여기서 잡고 끝나도록. 바깥에 두면 rollback-only 표시가 남아 커밋 때 다시 터진다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Ga4DisplayNameReader {

    private final Ga4DisplayNameRepository ga4DisplayNameRepository;

    public Ga4DisplayNames current() {
        try {
            return Ga4DisplayNames.of(ga4DisplayNameRepository.findAll());
        } catch (DataAccessException e) {
            log.warn("GA4 이름 표를 읽지 못해 기본 이름으로 보여 줍니다 - {}", e.getClass().getSimpleName());
            return Ga4DisplayNames.defaultsOnly();
        }
    }
}
