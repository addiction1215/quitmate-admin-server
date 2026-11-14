package com.quitmate.notice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 공지 푸시를 어디까지 보냈는지(KAN-109). 이 칸이 생기기 전에 등록한 공지는 null 이다.
 *
 * <pre>
 * 등록하며 바로 보냄          : SENT
 * 야간에 등록한 이벤트 공지   : DEFERRED ─(예정 시각 뒤 스케줄러가 한 번만 집음)→ SENDING ─→ SENT
 *                                                                              └(오류·서버 멈춤)→ FAILED
 *                               └(보내기 전에 공지 삭제)→ CANCELED
 * </pre>
 *
 * <p>FAILED 는 다시 보내지 않는다. 일부 또는 전부에게 이미 갔을 수 있어, 중복 발송보다 누락을 택한다.</p>
 */
@Getter
@RequiredArgsConstructor
public enum NoticePushStatus {
    DEFERRED("발송 예정"),
    SENDING("발송 중"),
    SENT("발송함"),
    FAILED("발송 실패"),
    CANCELED("발송 취소");

    private final String description;
}
