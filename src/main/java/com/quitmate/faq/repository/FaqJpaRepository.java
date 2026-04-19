package com.quitmate.faq.repository;

import com.quitmate.faq.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FaqJpaRepository extends JpaRepository<Faq, Long> {
}
