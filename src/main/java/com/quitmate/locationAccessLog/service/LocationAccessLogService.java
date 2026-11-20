package com.quitmate.locationAccessLog.service;

import com.quitmate.locationAccessLog.entity.AdminLocationAccessLog;
import com.quitmate.locationAccessLog.enums.LocationAccessResourceType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.HandlerMapping;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 관리자 위치정보 열람 이력을 남긴다(KAN-208, 법무 검토 KAN-136).
 *
 * <ul>
 *   <li>위치 값(주소·좌표)은 받지도 남기지도 않는다. 누가·언제·어느 회원의·무엇을·어느 API 로 봤는지만 남긴다.</li>
 *   <li>위치 값이 실제로 하나라도 응답에 나갈 때만 부른다. 위치 칸이 비어 있는 응답은 남기지 않는다
 *       ("회원 X 의 위치를 본 사람" 조회에 실제로는 아무것도 못 본 열람이 섞이지 않게).</li>
 *   <li>여러 회원의 위치를 한 번에 주는 API 가 생기면 회원마다 한 줄씩 남긴다((target_user_id, accessed_at) 인덱스로 바로 찾게).</li>
 *   <li>저장이 실패해도 예외를 던지지 않는다. 조회 응답을 막지 않고, ERROR 로그 한 줄(위치 값·예외 메시지 없이)만 남긴다.
 *       관리자 서버에는 Slack 알림 수단이 없다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationAccessLogService {

    static final ZoneId KOREA = ZoneId.of("Asia/Seoul");
    static final String UNKNOWN_API = "UNKNOWN";

    private final LocationAccessLogWriter locationAccessLogWriter;

    /**
     * @param adminUserId  열람한 관리자 userId(KAN-206 인증 정보)
     * @param targetUserId 위치정보 주인 회원 id
     * @param resourceType 본 위치정보 종류
     * @param resourceId   종류별 행 id
     */
    public void record(Long adminUserId, Long targetUserId, LocationAccessResourceType resourceType, Long resourceId) {
        try {
            locationAccessLogWriter.write(AdminLocationAccessLog.of(adminUserId, targetUserId, resourceType, resourceId,
                    currentApi(), LocalDateTime.now(KOREA)));
        } catch (RuntimeException e) {
            // DB 오류(DataAccessException)·연결 못 얻음(TransactionException) 모두 조회를 막지 않는다.
            // 예외 메시지에는 SQL 이 들어갈 수 있어 클래스 이름만 남긴다.
            log.error("위치정보 열람 이력 저장 실패 - 관리자 {} 회원 {} {} {} ({})",
                    adminUserId, targetUserId, resourceType, resourceId, causeName(e));
        }
    }

    /** "GET /api/v1/admin/mission-history/{id}" 처럼 메서드 + 경로 패턴. 요청 밖에서 부르면 UNKNOWN */
    private String currentApi() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return UNKNOWN_API;
        }
        HttpServletRequest request = attributes.getRequest();
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return request.getMethod() + " " + (pattern != null ? pattern : request.getRequestURI());
    }

    private String causeName(RuntimeException e) {
        Throwable root = NestedExceptionUtils.getMostSpecificCause(e);
        String name = e.getClass().getSimpleName();
        return root == e ? name : name + " ← " + root.getClass().getSimpleName();
    }
}
