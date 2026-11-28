package com.quitmate.appMetrics.ga4.displayname.service;

import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 기본 이름표(KAN-228 설계 문서 5장). DB에 넣지 않고 코드에 둔다. 관리자가 표에 이름을 넣으면 그 이름이 앞선다.
 *
 * <p>화면 값은 앱이 보내는 expo-router 경로 그대로다(앱 {@code src/hooks/useTelemetry.ts}). 기본 이름은 모두 앱 화면이다.
 * 목록 순서는 설계 문서 순서(탭 → 시작·로그인 → 설문 → 챌린지 → 기록·목표 → 알림 → 설정)이고 이름 설정 화면 목록도 이 순서다.
 */
public final class Ga4DefaultDisplayNames {

    private static final Map<Ga4ValueType, Map<String, String>> NAMES = new EnumMap<>(Ga4ValueType.class);

    static {
        Map<String, String> screens = new LinkedHashMap<>();
        // 탭
        screens.put("(main)/(tabs)", "[앱] 홈 탭");
        screens.put("(main)/(tabs)/stats", "[앱] 통계 탭");
        screens.put("(main)/(tabs)/awards", "[앱] 챌린지 탭");
        screens.put("(main)/(tabs)/my", "[앱] 내 정보 탭");
        screens.put("(main)/(tabs)/settings", "[앱] 설정");
        // 시작·로그인·회원가입
        screens.put("index", "[앱] 시작(앱 실행 직후)");
        screens.put("(auth)/login", "[앱] 로그인");
        screens.put("(auth)/login/findPassword", "[앱] 비밀번호 찾기");
        screens.put("(auth)/login/resetPassword", "[앱] 비밀번호 재설정");
        screens.put("(auth)/signup", "[앱] 회원가입");
        screens.put("(auth)/signup/nextStep", "[앱] 회원가입 2단계");
        screens.put("(auth)/agreement", "[앱] 약관 동의");
        screens.put("(auth)/callback", "[앱] 소셜 로그인 처리 중");
        // 첫 설문
        screens.put("(survey)/onboarding", "[앱] 온보딩");
        screens.put("(survey)/Survey", "[앱] 흡연 설문");
        screens.put("(survey)/nextStep", "[앱] 설문 다음 단계");
        screens.put("(survey)/result", "[앱] 설문 결과");
        // 챌린지·미션
        screens.put("(main)/detail/[challengeId]", "[앱] 챌린지 상세");
        screens.put("(main)/detail/challenge-history/[challengeHistoryId]", "[앱] 참여한 챌린지 기록");
        screens.put("(main)/progress/[challengeHistoryId]", "[앱] 챌린지 진행");
        screens.put("(main)/mission/hold/[missionHistoryId]", "[앱] 미션: 참기");
        screens.put("(main)/mission/location/[missionHistoryId]", "[앱] 미션: 위치 인증");
        screens.put("(main)/mission/replace/[missionHistoryId]", "[앱] 미션: 대체 행동");
        // 금연 기록·목표
        screens.put("(main)/smoking-calendar", "[앱] 흡연 달력");
        screens.put("(main)/smoking-tendency", "[앱] 흡연 성향");
        screens.put("(main)/craving-hold", "[앱] 흡연 욕구 참기");
        screens.put("(main)/quit-goal", "[앱] 금연 목표");
        screens.put("(main)/quit-goal/create", "[앱] 금연 목표 만들기");
        screens.put("(main)/quit-goal/edit", "[앱] 금연 목표 고치기");
        screens.put("(main)/quit-goal/achieved", "[앱] 금연 목표 달성");
        screens.put("(main)/quit-goal/savings", "[앱] 아낀 돈");
        screens.put("(main)/quit-goal/history", "[앱] 지난 금연 목표");
        screens.put("(main)/quit-goal/history/[id]", "[앱] 지난 금연 목표 상세");
        // 알림·공지·프로필
        screens.put("(main)/notification", "[앱] 알림함");
        screens.put("(main)/notice/[id]", "[앱] 공지 상세");
        screens.put("(main)/profile/edit", "[앱] 프로필 고치기");
        // 설정 하위 화면
        screens.put("(main)/(tabs)/settings/notification", "[앱] 설정: 알림");
        screens.put("(main)/(tabs)/settings/notification/feedbackTime", "[앱] 설정: 금연 확인 알림 시간");
        screens.put("(main)/(tabs)/settings/friends", "[앱] 설정: 친구");
        screens.put("(main)/(tabs)/settings/blockedFriends", "[앱] 설정: 차단한 친구");
        screens.put("(main)/(tabs)/settings/notices", "[앱] 설정: 공지사항");
        screens.put("(main)/(tabs)/settings/inquiry", "[앱] 설정: 문의");
        screens.put("(main)/(tabs)/settings/inquiry/ask", "[앱] 설정: 문의하기");
        screens.put("(main)/(tabs)/settings/inquiry/faq", "[앱] 설정: 자주 묻는 질문");
        screens.put("(main)/(tabs)/settings/inquiry/my", "[앱] 설정: 내 문의");
        screens.put("(main)/(tabs)/settings/inquiry/my/[id]", "[앱] 설정: 내 문의 상세");
        screens.put("(main)/settings/inquiry/edit/[id]", "[앱] 설정: 문의 고치기");
        screens.put("(main)/(tabs)/settings/quitHelp", "[앱] 설정: 금연 도움");
        screens.put("(main)/(tabs)/settings/info", "[앱] 설정: 앱 정보");
        screens.put("(main)/(tabs)/settings/terms", "[앱] 설정: 약관");
        screens.put("(main)/(tabs)/settings/terms/adPush", "[앱] 설정: 광고 알림 동의");
        screens.put("(main)/(tabs)/settings/privacy", "[앱] 설정: 개인정보 처리방침");
        screens.put("(main)/(tabs)/settings/withdrawal", "[앱] 설정: 회원 탈퇴");
        NAMES.put(Ga4ValueType.SCREEN, Collections.unmodifiableMap(screens));

        Map<String, String> platforms = new LinkedHashMap<>();
        platforms.put("Android", "앱(Android)");
        platforms.put("iOS", "앱(iOS)");
        platforms.put("web", "웹사이트");
        NAMES.put(Ga4ValueType.PLATFORM, Collections.unmodifiableMap(platforms));

        Map<String, String> appVersions = new LinkedHashMap<>();
        appVersions.put("(not set)", "버전 없음(웹사이트)");
        NAMES.put(Ga4ValueType.APP_VERSION, Collections.unmodifiableMap(appVersions));
    }

    private Ga4DefaultDisplayNames() {
    }

    /** 종류별 기본 이름(원래 값 → 이름), 설계 문서 순서 */
    public static Map<String, String> of(Ga4ValueType type) {
        return NAMES.get(type);
    }

    /** 정확히 같은 원래 값의 기본 이름. 없으면 null */
    public static String find(Ga4ValueType type, String rawValue) {
        return rawValue == null ? null : NAMES.get(type).get(rawValue);
    }
}
