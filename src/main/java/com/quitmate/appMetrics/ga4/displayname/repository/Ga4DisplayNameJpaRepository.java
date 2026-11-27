package com.quitmate.appMetrics.ga4.displayname.repository;

import com.quitmate.appMetrics.ga4.displayname.entity.Ga4DisplayName;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Ga4DisplayNameJpaRepository extends JpaRepository<Ga4DisplayName, Long> {

    List<Ga4DisplayName> findAllByValueType(Ga4ValueType valueType);

    Optional<Ga4DisplayName> findByValueTypeAndRawValue(Ga4ValueType valueType, String rawValue);
}
