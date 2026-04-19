package com.quitmate.faq.service.impl;

import com.quitmate.faq.entity.Faq;
import com.quitmate.faq.repository.FaqRepository;
import com.quitmate.faq.service.FaqService;
import com.quitmate.faq.service.request.FaqCreateServiceRequest;
import com.quitmate.faq.service.request.FaqUpdateServiceRequest;
import com.quitmate.faq.service.response.FaqCreateResponse;
import com.quitmate.faq.service.response.FaqUpdateResponse;
import com.quitmate.global.exception.QuitmateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class FaqServiceImpl implements FaqService {

    private static final String UNKNOWN_FAQ = "해당 FAQ는 존재하지 않습니다.";

    private final FaqRepository faqRepository;

    @Override
    public FaqCreateResponse createFaq(FaqCreateServiceRequest request) {
        return FaqCreateResponse.createResponse(faqRepository.save(request.toEntity()));
    }

    @Override
    public FaqUpdateResponse updateFaq(FaqUpdateServiceRequest request) {
        Faq faq = faqRepository.findById(request.getId())
                .orElseThrow(() -> new QuitmateException(UNKNOWN_FAQ));
        faq.update(request.getTitle(), request.getDescription());
        return FaqUpdateResponse.createResponse(faq);
    }

    @Override
    public void deleteFaq(Long id) {
        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new QuitmateException(UNKNOWN_FAQ));
        faq.delete();
    }
}
