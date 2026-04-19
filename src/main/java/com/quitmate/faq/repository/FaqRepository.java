package com.quitmate.faq.repository;

import com.quitmate.faq.entity.Faq;

import java.util.Optional;

public interface FaqRepository {
    Faq save(Faq faq);
    Optional<Faq> findById(Long id);
}
