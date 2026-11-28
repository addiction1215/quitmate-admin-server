package com.quitmate.appMetrics.ga4.displayname.repository.impl;

import com.quitmate.appMetrics.ga4.displayname.entity.Ga4DisplayName;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import com.quitmate.appMetrics.ga4.displayname.repository.Ga4DisplayNameJpaRepository;
import com.quitmate.appMetrics.ga4.displayname.repository.Ga4DisplayNameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class Ga4DisplayNameRepositoryImpl implements Ga4DisplayNameRepository {

    private final Ga4DisplayNameJpaRepository ga4DisplayNameJpaRepository;

    @Override
    public List<Ga4DisplayName> findAll() {
        return ga4DisplayNameJpaRepository.findAll();
    }

    @Override
    public List<Ga4DisplayName> findAllByValueType(Ga4ValueType valueType) {
        return ga4DisplayNameJpaRepository.findAllByValueType(valueType);
    }

    @Override
    public Optional<Ga4DisplayName> findByValueTypeAndRawValue(Ga4ValueType valueType, String rawValue) {
        return ga4DisplayNameJpaRepository.findByValueTypeAndRawValue(valueType, rawValue);
    }

    @Override
    public Optional<Ga4DisplayName> findById(Long id) {
        return ga4DisplayNameJpaRepository.findById(id);
    }

    @Override
    public Ga4DisplayName saveAndFlush(Ga4DisplayName displayName) {
        return ga4DisplayNameJpaRepository.saveAndFlush(displayName);
    }

    @Override
    public void delete(Ga4DisplayName displayName) {
        ga4DisplayNameJpaRepository.delete(displayName);
    }
}
