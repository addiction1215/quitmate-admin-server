package com.quitmate.faq.service;

import com.quitmate.faq.service.request.FaqCreateServiceRequest;
import com.quitmate.faq.service.request.FaqUpdateServiceRequest;
import com.quitmate.faq.service.response.FaqCreateResponse;
import com.quitmate.faq.service.response.FaqUpdateResponse;

public interface FaqService {
    FaqCreateResponse createFaq(FaqCreateServiceRequest request);
    FaqUpdateResponse updateFaq(FaqUpdateServiceRequest request);
    void deleteFaq(Long id);
}
