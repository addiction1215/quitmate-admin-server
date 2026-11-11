package com.quitmate.cigaretteHistoryBatch.service;

import com.quitmate.cigaretteHistoryBatch.service.request.CigaretteHistoryBatchRunServiceRequest;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchAttemptResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchDateStatusResponse;
import com.quitmate.cigaretteHistoryBatch.service.response.CigaretteHistoryBatchRunResponse;
import com.quitmate.global.appserver.AppServerClient;
import com.quitmate.global.appserver.AppServerResponse;
import com.quitmate.global.exception.QuitmateException;
import com.quitmate.global.security.AdminAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 재처리·날짜별 상태·실행 이력은 앱 서버 관리자 API(KAN-88)를 부른다.
 * 이관 로직과 락은 앱 서버에만 있고, 관리자 서버는 직접 실행하지 않는다.
 */
@Service
@RequiredArgsConstructor
public class CigaretteHistoryBatchRunService {

    static final String RUNS_PATH = "/api/v1/admin/cigarette-history-batch/runs";
    private static final Pattern DATE = Pattern.compile("\\d{8}");

    private static final ParameterizedTypeReference<AppServerResponse<CigaretteHistoryBatchRunResponse>> RUN_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<AppServerResponse<List<CigaretteHistoryBatchDateStatusResponse>>>
            DATE_STATUS_LIST_TYPE = new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<AppServerResponse<List<CigaretteHistoryBatchAttemptResponse>>>
            ATTEMPT_LIST_TYPE = new ParameterizedTypeReference<>() {
            };

    private final AppServerClient appServerClient;
    private final AdminAccessService adminAccessService;

    /**
     * 그 날짜를 다시 이관한다. 이미 끝난 날짜(DONE)를 다시 돌리지 않도록 force는 항상 false로 보낸다.
     */
    public CigaretteHistoryBatchRunResponse run(CigaretteHistoryBatchRunServiceRequest request) {
        Long adminId = adminAccessService.getCurrentAdminId();
        String targetDate = requireDate(request.getTargetDate(), "이관할 날짜는");
        return appServerClient.post(RUNS_PATH, new AppServerRunRequest(targetDate, false), RUN_TYPE, adminId);
    }

    public List<CigaretteHistoryBatchDateStatusResponse> getDateStatuses(String from, String to) {
        Long adminId = adminAccessService.getCurrentAdminId();
        Map<String, String> query = Map.of(
                "from", requireDate(from, "시작일(from)은"),
                "to", requireDate(to, "종료일(to)은"));
        return nullToEmpty(appServerClient.get(RUNS_PATH, query, DATE_STATUS_LIST_TYPE, adminId));
    }

    public List<CigaretteHistoryBatchAttemptResponse> getAttempts(String targetDate) {
        Long adminId = adminAccessService.getCurrentAdminId();
        String date = requireDate(targetDate, "날짜는");
        return nullToEmpty(appServerClient.get(RUNS_PATH + "/" + date + "/attempts", Map.of(),
                ATTEMPT_LIST_TYPE, adminId));
    }

    /**
     * 형식만 본다(경로에 넣기 전 안전 확인). 오늘·미래·기준일 같은 규칙은 앱 서버가 판단해 400으로 돌려준다.
     */
    private static String requireDate(String date, String subject) {
        if (date == null || !DATE.matcher(date.trim()).matches()) {
            throw new QuitmateException(subject + " yyyyMMdd 형식이어야 합니다. 예: 20261105");
        }
        return date.trim();
    }

    private static <T> List<T> nullToEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }

    /**
     * 앱 서버 CigaretteHistoryBatchRunRequest와 같은 모양.
     */
    record AppServerRunRequest(String targetDate, boolean force) {
    }
}
