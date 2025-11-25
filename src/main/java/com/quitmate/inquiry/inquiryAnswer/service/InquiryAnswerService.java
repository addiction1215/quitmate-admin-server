package com.quitmate.inquiry.inquiryAnswer.service;

import com.quitmate.inquiry.inquiryAnswer.service.request.InquiryAnswerCreateServiceRequest;
import com.quitmate.inquiry.inquiryAnswer.service.response.InquiryAnswerCreateResponse;

public interface InquiryAnswerService {

    InquiryAnswerCreateResponse createInquiryAnswer(InquiryAnswerCreateServiceRequest request);
}
