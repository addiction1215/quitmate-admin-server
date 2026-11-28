package com.quitmate.appMetrics.ga4.displayname.repository;

import com.quitmate.appMetrics.ga4.displayname.entity.Ga4DisplayName;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;

import java.util.List;
import java.util.Optional;

public interface Ga4DisplayNameRepository {

    /** 관리자가 넣은 이름 전부. 표는 화면·플랫폼·버전 이름 수만큼만 커서 한 번에 읽는다 */
    List<Ga4DisplayName> findAll();

    List<Ga4DisplayName> findAllByValueType(Ga4ValueType valueType);

    Optional<Ga4DisplayName> findByValueTypeAndRawValue(Ga4ValueType valueType, String rawValue);

    Optional<Ga4DisplayName> findById(Long id);

    /** 저장하고 바로 DB에 내보낸다(고친 시각을 응답에 담고, 같은 값 동시 저장의 유니크 위반을 여기서 알려고) */
    Ga4DisplayName saveAndFlush(Ga4DisplayName displayName);

    void delete(Ga4DisplayName displayName);
}
