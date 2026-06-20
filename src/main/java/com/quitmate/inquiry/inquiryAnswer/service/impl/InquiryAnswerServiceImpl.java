package com.quitmate.inquiry.inquiryAnswer.service.impl;

import com.quitmate.alertHistory.entity.AlertDestinationType;
import com.quitmate.alertSetting.entity.AlertSetting;
import com.quitmate.alertSetting.entity.enums.AlertType;
import com.quitmate.alertSetting.service.AlertSettingReadService;
import com.quitmate.expo.event.PushNotificationEvent;
import com.quitmate.firebase.request.SendFirebaseDataDto;
import com.quitmate.firebase.request.SendFirebaseServiceRequest;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.inquiry.inquiryAnswer.entity.InquiryAnswer;
import com.quitmate.inquiry.inquiryAnswer.repository.InquiryAnswerRepository;
import com.quitmate.inquiry.inquiryAnswer.service.InquiryAnswerService;
import com.quitmate.inquiry.inquiryAnswer.service.request.InquiryAnswerCreateServiceRequest;
import com.quitmate.inquiry.inquiryAnswer.service.response.InquiryAnswerCreateResponse;
import com.quitmate.inquiry.inquryQuestion.entity.InquiryQuestion;
import com.quitmate.inquiry.inquryQuestion.enums.InquiryStatus;
import com.quitmate.inquiry.inquryQuestion.service.InquiryQuestionReadService;
import com.quitmate.user.push.entity.Push;
import com.quitmate.user.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InquiryAnswerServiceImpl implements InquiryAnswerService {

    private static final String UNKNOWN_INQUIRY_QUESTION = "해당 문의사항은 존재하지 않습니다.";
    private static final String ALREADY_ANSWERED = "이미 답변이 등록된 문의사항입니다.";
    private static final String INQUIRY_ANSWER_PUSH_BODY = "문의하신 내용에 답변이 등록되었습니다.";

    private final InquiryQuestionReadService inquiryQuestionReadService;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final AlertSettingReadService alertSettingReadService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public InquiryAnswerCreateResponse createInquiryAnswer(InquiryAnswerCreateServiceRequest request) {
        // 1. 문의사항 존재 확인
        InquiryQuestion inquiryQuestion = inquiryQuestionReadService.findById(request.getInquiryQuestionId());

        // 2. 이미 답변이 있는지 확인
        inquiryAnswerRepository.findByInquiryQuestionId(request.getInquiryQuestionId())
                .ifPresent(inquiryAnswer -> {
                    throw new QuitmateException(ALREADY_ANSWERED);
                });

        // 3. 답변 생성
        InquiryAnswer inquiryAnswer = InquiryAnswer.builder()
                .inquiryQuestion(inquiryQuestion)
                .content(request.getContent())
                .build();

        InquiryAnswer savedInquiryAnswer = inquiryAnswerRepository.save(inquiryAnswer);

        // 4. 문의사항 상태를 '완료'로 변경
        inquiryQuestion.updateStatus(InquiryStatus.DONE);

        publishInquiryAnswerPushEvent(inquiryQuestion);

        return InquiryAnswerCreateResponse.createResponse(savedInquiryAnswer);
    }

    private void publishInquiryAnswerPushEvent(InquiryQuestion inquiryQuestion) {
        User user = inquiryQuestion.getUser();
        if (!shouldSendPush(user)) {
            return;
        }

        List<Push> pushes = user.getPushes() != null ? user.getPushes() : List.of();
        if (pushes.isEmpty()) {
            return;
        }

        SendFirebaseDataDto dataDto = SendFirebaseDataDto.builder()
                .alert_destination_type(AlertDestinationType.INQUIRY)
                .alert_destination_info("1:1 문의")
                .build();

        List<SendFirebaseServiceRequest> pushRequests = pushes.stream()
                .map(push -> SendFirebaseServiceRequest.builder()
                        .push(push)
                        .body(INQUIRY_ANSWER_PUSH_BODY)
                        .sound("default")
                        .sendFirebaseDataDto(dataDto)
                        .build())
                .toList();

        eventPublisher.publishEvent(new PushNotificationEvent(pushRequests));
    }

    private boolean shouldSendPush(User user) {
        AlertSetting alertSetting = alertSettingReadService.findByUserOrCreateDefault(user);
        return alertSetting != null && alertSetting.getAll() != AlertType.OFF;
    }
}
