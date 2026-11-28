package com.quitmate.appMetrics.ga4.displayname.service;

import com.quitmate.appMetrics.ga4.displayname.entity.Ga4DisplayName;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4NameOrigin;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4SourceType;
import com.quitmate.appMetrics.ga4.displayname.enums.Ga4ValueType;
import com.quitmate.appMetrics.ga4.displayname.repository.Ga4DisplayNameRepository;
import com.quitmate.appMetrics.ga4.displayname.service.request.Ga4DisplayNameSaveServiceRequest;
import com.quitmate.appMetrics.ga4.displayname.service.response.Ga4DisplayNameResponse;
import com.quitmate.appMetrics.ga4.displayname.service.response.Ga4UnnamedValuesResponse;
import com.quitmate.appMetrics.ga4.displayname.service.response.Ga4UnnamedValuesResponse.UnnamedValue;
import com.quitmate.appMetrics.ga4.service.Ga4MetricsService;
import com.quitmate.appMetrics.ga4.service.response.Ga4RecentValues;
import com.quitmate.global.exception.NotFoundException;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.security.AdminAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 「GA4 이름 설정」 화면(KAN-230). 관리자 계정이면 누구나 이름을 넣고 고치고 되돌릴 수 있고, 고친 사람을 남긴다.
 *
 * <p>이름을 바꾸면 「앱 지표」에 바로 보인다. 지표 응답은 캐시한 GA4 숫자에 요청마다 이름을 붙이기 때문이다
 * ({@link Ga4MetricsService#getMetrics}).
 */
@Service
@RequiredArgsConstructor
public class Ga4DisplayNameService {

    static final String EMPTY_RAW_VALUE = "원래 값(rawValue)은 비워 둘 수 없습니다.";
    static final String RAW_VALUE_TOO_LONG = "원래 값(rawValue)은 " + Ga4DisplayName.RAW_VALUE_MAX_LENGTH + "자까지 받습니다.";
    static final String EMPTY_NAME = "이름(displayName)은 비워 둘 수 없습니다.";
    static final String NAME_TOO_LONG = "이름(displayName)은 " + Ga4DisplayName.DISPLAY_NAME_MAX_LENGTH + "자까지 쓸 수 있습니다.";
    static final String SOURCE_TYPE_SCREEN_ONLY = "앱/웹 구분(sourceType)은 화면(SCREEN) 이름에만 넣을 수 있습니다.";
    static final String UNKNOWN_NAME = "해당 GA4 이름이 없습니다.";

    private final Ga4DisplayNameRepository ga4DisplayNameRepository;
    private final Ga4MetricsService ga4MetricsService;
    private final AdminAccessService adminAccessService;

    /**
     * 종류별 목록: 기본 이름표의 값(설계 문서 순서) 다음에, 관리자만 이름을 넣은 값(원래 값 순서).
     */
    @Transactional(readOnly = true)
    public List<Ga4DisplayNameResponse> getDisplayNames(String typeValue) {
        Ga4ValueType type = Ga4ValueType.from(typeValue);
        List<Ga4DisplayName> adminRows = ga4DisplayNameRepository.findAllByValueType(type);
        Ga4DisplayNames names = Ga4DisplayNames.of(adminRows);

        Set<String> rawValues = new LinkedHashSet<>(Ga4DefaultDisplayNames.of(type).keySet());
        adminRows.stream()
                .map(Ga4DisplayName::getRawValue)
                .filter(rawValue -> !rawValues.contains(rawValue))
                .sorted()
                .forEach(rawValues::add);

        List<Ga4DisplayNameResponse> responses = new ArrayList<>();
        for (String rawValue : rawValues) {
            responses.add(Ga4DisplayNameResponse.of(type, rawValue, names.resolve(type, rawValue)));
        }
        return responses;
    }

    /**
     * 최근 90일 GA4에 나온 값 중 관리자 이름·기본 이름이 모두 없는 것. GA4를 부르는 동안 DB 연결을 잡지 않으려고
     * 트랜잭션 밖에서 GA4를 먼저 부르고 이름 표를 읽는다.
     *
     * @throws com.quitmate.appMetrics.ga4.client.Ga4UnavailableException GA4에서 받지 못했으면(503)
     */
    public Ga4UnnamedValuesResponse getUnnamedValues(String typeValue) {
        Ga4ValueType type = Ga4ValueType.from(typeValue);
        Ga4RecentValues recent = ga4MetricsService.getRecentValues(type);
        Ga4DisplayNames names = Ga4DisplayNames.of(ga4DisplayNameRepository.findAllByValueType(type));

        // GA4 순서(숫자 내림차순, 같으면 원래 값 순)를 그대로 둔다
        List<UnnamedValue> values = new ArrayList<>();
        for (Ga4RecentValues.ValueCount value : recent.values()) {
            Ga4DisplayNames.Resolved resolved = names.resolve(type, value.rawValue());
            if (resolved.origin() == Ga4NameOrigin.NONE) {
                values.add(new UnnamedValue(value.rawValue(), value.count(), resolved.displayName(), resolved.sourceType()));
            }
        }
        return new Ga4UnnamedValuesResponse(recent.enabled(), type, recent.startDate(), recent.endDate(),
                recent.fetchedAt(), values);
    }

    /**
     * 종류 + 원래 값으로 이름을 저장한다. 없으면 추가, 있으면 고친다. 이름은 앞뒤 공백을 지우고 저장하고,
     * 원래 값은 GA4 값과 정확히 같아야 해서 그대로 둔다.
     */
    @Transactional
    public Ga4DisplayNameResponse save(Ga4DisplayNameSaveServiceRequest request) {
        Ga4ValueType type = Ga4ValueType.from(request.getValueType());
        String rawValue = validRawValue(request.getRawValue());
        String displayName = validDisplayName(request.getDisplayName());
        Ga4SourceType sourceType = Ga4SourceType.fromNullable(request.getSourceType());
        if (sourceType != null && type != Ga4ValueType.SCREEN) {
            throw new QuitmateException(SOURCE_TYPE_SCREEN_ONLY);
        }
        Long adminId = adminAccessService.getCurrentAdminId();

        Ga4DisplayName saved = ga4DisplayNameRepository.findByValueTypeAndRawValue(type, rawValue)
                .map(existing -> {
                    existing.rename(displayName, sourceType, adminId);
                    return existing;
                })
                .orElseGet(() -> Ga4DisplayName.builder()
                        .valueType(type)
                        .rawValue(rawValue)
                        .displayName(displayName)
                        .sourceType(sourceType)
                        .updatedBy(adminId)
                        .build());
        saved = ga4DisplayNameRepository.saveAndFlush(saved);
        return Ga4DisplayNameResponse.of(type, rawValue, Ga4DisplayNames.of(List.of(saved)).resolve(type, rawValue));
    }

    /**
     * 관리자 이름을 지우고 기본 이름(없으면 원래 값)으로 되돌린다. 돌려주는 것은 되돌린 뒤의 이름이다.
     *
     * @throws NotFoundException 없는 ID면(404)
     */
    @Transactional
    public Ga4DisplayNameResponse delete(Long id) {
        adminAccessService.getCurrentAdminId();
        Ga4DisplayName row = ga4DisplayNameRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(UNKNOWN_NAME));
        ga4DisplayNameRepository.delete(row);
        return Ga4DisplayNameResponse.of(row.getValueType(), row.getRawValue(),
                Ga4DisplayNames.defaultsOnly().resolve(row.getValueType(), row.getRawValue()));
    }

    private static String validRawValue(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            throw new QuitmateException(EMPTY_RAW_VALUE);
        }
        if (rawValue.length() > Ga4DisplayName.RAW_VALUE_MAX_LENGTH) {
            throw new QuitmateException(RAW_VALUE_TOO_LONG);
        }
        return rawValue;
    }

    private static String validDisplayName(String displayName) {
        String trimmed = displayName == null ? "" : displayName.trim();
        if (trimmed.isEmpty()) {
            throw new QuitmateException(EMPTY_NAME);
        }
        if (trimmed.length() > Ga4DisplayName.DISPLAY_NAME_MAX_LENGTH) {
            throw new QuitmateException(NAME_TOO_LONG);
        }
        return trimmed;
    }
}
