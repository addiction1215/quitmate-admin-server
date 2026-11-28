package com.quitmate.appMetrics.ga4.displayname.controller.request;

import com.quitmate.appMetrics.ga4.displayname.entity.Ga4DisplayName;
import com.quitmate.appMetrics.ga4.displayname.service.request.Ga4DisplayNameSaveServiceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이름 저장 요청. 종류·앱/웹 구분은 글자로 받아 서비스에서 확인한다(모르는 값도 400 문구가 보이도록).
 */
@Getter
@NoArgsConstructor
public class Ga4DisplayNameSaveRequest {

    @NotBlank(message = "값 종류(valueType)는 필수입니다.")
    private String valueType;

    @NotBlank(message = "원래 값(rawValue)은 필수입니다.")
    @Size(max = Ga4DisplayName.RAW_VALUE_MAX_LENGTH, message = "원래 값(rawValue)은 255자까지 받습니다.")
    private String rawValue;

    @NotBlank(message = "이름(displayName)은 비워 둘 수 없습니다.")
    private String displayName;

    /** APP·WEB. 화면(SCREEN)에만 넣고, 없으면 구분 없음 */
    private String sourceType;

    @Builder
    public Ga4DisplayNameSaveRequest(String valueType, String rawValue, String displayName, String sourceType) {
        this.valueType = valueType;
        this.rawValue = rawValue;
        this.displayName = displayName;
        this.sourceType = sourceType;
    }

    public Ga4DisplayNameSaveServiceRequest toServiceRequest() {
        return Ga4DisplayNameSaveServiceRequest.builder()
                .valueType(valueType)
                .rawValue(rawValue)
                .displayName(displayName)
                .sourceType(sourceType)
                .build();
    }
}
