package com.quitmate.alertSetting.entity.enums;

/**
 * 광고성 앱 푸시 수신 동의 상태. 앱 서버 {@code AdPushConsentStatus}와 같은 값이다(KAN-107).
 * 동의·철회와 이력은 앱 서버가 쓰고, 관리자 서버는 읽기만 한다.
 */
public enum AdPushConsentStatus {
    NOT_AGREED,
    AGREED,
    WITHDRAWN
}
