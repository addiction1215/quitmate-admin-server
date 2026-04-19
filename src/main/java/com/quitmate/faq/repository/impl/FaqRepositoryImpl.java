package com.quitmate.faq.repository.impl;

import com.quitmate.faq.entity.Faq;
import com.quitmate.faq.repository.FaqJpaRepository;
import com.quitmate.faq.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class FaqRepositoryImpl implements FaqRepository {

    private final FaqJpaRepository faqJpaRepository;

    @Override
    public Faq save(Faq faq) {
        return faqJpaRepository.save(faq);
    }

    @Override
    public Optional<Faq> findById(Long id) {
        return faqJpaRepository.findById(id);
    }
}
