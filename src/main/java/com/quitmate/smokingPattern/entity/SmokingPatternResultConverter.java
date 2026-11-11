package com.quitmate.smokingPattern.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quitmate.smokingPattern.result.SmokingPatternResult;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * result_json(TEXT)에 든 JSON 문자열을 진단 결과로 읽는다. 앱 서버 {@code SmokingPatternResultConverter}와 같은 JSON이다.
 *
 * <p>관리자 서버는 읽기만 한다. 알고리즘이 바뀌어 모르는 필드·enum 값이 들어와도 읽을 수 있게 무시하거나 null로 둔다.
 * JSON이 깨진 행 하나 때문에 전체 대시보드가 실패하지 않도록, 읽지 못하면 null을 돌려주고 경고 로그만 남긴다.
 */
@Slf4j
@Converter
public class SmokingPatternResultConverter implements AttributeConverter<SmokingPatternResult, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

    @Override
    public String convertToDatabaseColumn(SmokingPatternResult result) {
        if (result == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("흡연 패턴 진단 결과를 JSON으로 바꾸지 못했습니다.", e);
        }
    }

    @Override
    public SmokingPatternResult convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, SmokingPatternResult.class);
        } catch (JsonProcessingException e) {
            log.warn("흡연 패턴 진단 결과 JSON을 읽지 못했습니다 - {}", e.getOriginalMessage());
            return null;
        }
    }
}
