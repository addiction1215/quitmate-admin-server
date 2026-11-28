package com.quitmate.appMetrics.ga4.displayname.service;

import com.quitmate.appMetrics.ga4.displayname.entity.Ga4DisplayName;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4NameOrigin;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4SourceType;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class Ga4DisplayNamesTest {

    private static Ga4DisplayName admin(Ga4ValueType type, String rawValue, String name, Ga4SourceType sourceType) {
        return Ga4DisplayName.builder().valueType(type).rawValue(rawValue).displayName(name)
                .sourceType(sourceType).updatedBy(7L).build();
    }

    @DisplayName("관리자 이름이 있으면 기본 이름보다 앞선다. 기본 이름은 되돌릴 이름으로 같이 준다")
    @Test
    void adminFirst() {
        Ga4DisplayNames names = Ga4DisplayNames.of(List.of(
                admin(Ga4ValueType.SCREEN, "(main)/(tabs)/awards", "[앱] 챌린지 목록", Ga4SourceType.APP)));

        Ga4DisplayNames.Resolved resolved = names.resolve(Ga4ValueType.SCREEN, "(main)/(tabs)/awards");

        assertThat(resolved.displayName()).isEqualTo("[앱] 챌린지 목록");
        assertThat(resolved.origin()).isEqualTo(Ga4NameOrigin.ADMIN);
        assertThat(resolved.defaultName()).isEqualTo("[앱] 챌린지 탭");
        assertThat(resolved.sourceType()).isEqualTo(Ga4SourceType.APP);
        assertThat(resolved.admin().getUpdatedBy()).isEqualTo(7L);
    }

    @DisplayName("관리자 이름이 없으면 기본 이름표. 기본 이름표의 화면은 앱 화면이고, 플랫폼·버전은 앱/웹 구분이 없다")
    @Test
    void defaults() {
        Ga4DisplayNames names = Ga4DisplayNames.defaultsOnly();

        Ga4DisplayNames.Resolved home = names.resolve(Ga4ValueType.SCREEN, "(main)/(tabs)");
        assertThat(home.displayName()).isEqualTo("[앱] 홈 탭");
        assertThat(home.origin()).isEqualTo(Ga4NameOrigin.DEFAULT);
        assertThat(home.sourceType()).isEqualTo(Ga4SourceType.APP);
        assertThat(home.admin()).isNull();

        assertThat(names.resolve(Ga4ValueType.SCREEN, "index").displayName()).isEqualTo("[앱] 시작(앱 실행 직후)");
        assertThat(names.resolve(Ga4ValueType.SCREEN, "(main)/detail/[challengeId]").displayName()).isEqualTo("[앱] 챌린지 상세");
        assertThat(names.resolve(Ga4ValueType.PLATFORM, "web").displayName()).isEqualTo("웹사이트");
        assertThat(names.resolve(Ga4ValueType.PLATFORM, "Android").displayName()).isEqualTo("앱(Android)");
        assertThat(names.resolve(Ga4ValueType.PLATFORM, "iOS").sourceType()).isNull();
        assertThat(names.resolve(Ga4ValueType.APP_VERSION, "(not set)").displayName()).isEqualTo("버전 없음(웹사이트)");
    }

    @DisplayName("이름이 없는 화면: 괄호로 시작하지 않으면 '[웹] 원래 값'(WEB), 괄호로 시작하면 원래 값 그대로")
    @Test
    void unnamedScreens() {
        Ga4DisplayNames names = Ga4DisplayNames.defaultsOnly();

        Ga4DisplayNames.Resolved webPage = names.resolve(Ga4ValueType.SCREEN, "금연 앱 Quitmate — 혼자 참지 마세요");
        assertThat(webPage.displayName()).isEqualTo("[웹] 금연 앱 Quitmate — 혼자 참지 마세요");
        assertThat(webPage.sourceType()).isEqualTo(Ga4SourceType.WEB);
        assertThat(webPage.origin()).isEqualTo(Ga4NameOrigin.NONE);
        assertThat(webPage.defaultName()).isNull();

        Ga4DisplayNames.Resolved newAppScreen = names.resolve(Ga4ValueType.SCREEN, "(main)/new-screen");
        assertThat(newAppScreen.displayName()).isEqualTo("(main)/new-screen");
        assertThat(newAppScreen.sourceType()).isNull();
        assertThat(newAppScreen.origin()).isEqualTo(Ga4NameOrigin.NONE);

        assertThat(names.resolve(Ga4ValueType.SCREEN, "(not set)").displayName()).isEqualTo("(not set)");
        assertThat(names.resolve(Ga4ValueType.SCREEN, "").displayName()).isEmpty();
    }

    @DisplayName("이름이 없는 플랫폼·버전은 원래 값 그대로('[웹]'을 붙이지 않는다)")
    @Test
    void unnamedPlatformAndVersion() {
        Ga4DisplayNames names = Ga4DisplayNames.defaultsOnly();

        assertThat(names.resolve(Ga4ValueType.APP_VERSION, "1.1.3").displayName()).isEqualTo("1.1.3");
        assertThat(names.resolve(Ga4ValueType.APP_VERSION, "1").origin()).isEqualTo(Ga4NameOrigin.NONE);
        assertThat(names.resolve(Ga4ValueType.PLATFORM, "tvOS").displayName()).isEqualTo("tvOS");
    }

    @DisplayName("원래 값은 정확히 같을 때만 맞춘다(대소문자·종류가 다르면 다른 값)")
    @Test
    void exactMatchOnly() {
        Ga4DisplayNames names = Ga4DisplayNames.of(List.of(
                admin(Ga4ValueType.APP_VERSION, "1", "개발 빌드", null)));

        assertThat(names.resolve(Ga4ValueType.APP_VERSION, "1").displayName()).isEqualTo("개발 빌드");
        assertThat(names.resolve(Ga4ValueType.APP_VERSION, "1.0").displayName()).isEqualTo("1.0");
        assertThat(names.resolve(Ga4ValueType.PLATFORM, "1").displayName()).isEqualTo("1");
        assertThat(names.resolve(Ga4ValueType.PLATFORM, "ios").displayName()).isEqualTo("ios");
    }
}
