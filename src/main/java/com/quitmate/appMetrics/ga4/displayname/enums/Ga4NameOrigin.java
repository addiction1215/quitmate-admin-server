package com.quitmate.appMetrics.ga4.displayname.enums;

/**
 * 지금 보이는 이름이 어디서 왔는지(KAN-230).
 */
public enum Ga4NameOrigin {

    /** 관리자가 넣은 이름(표 ga4_display_name) */
    ADMIN,
    /** 코드에 둔 기본 이름표 */
    DEFAULT,
    /** 둘 다 없음. 원래 값 또는 '[웹] 원래 값'으로 보인다 */
    NONE
}
