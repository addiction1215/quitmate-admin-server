package com.quitmate.appMetrics.ga4.displayname.controller;

import com.quitmate.appMetrics.ga4.displayname.controller.request.Ga4DisplayNameSaveRequest;
import com.quitmate.appMetrics.ga4.displayname.service.Ga4DisplayNameService;
import com.quitmate.appMetrics.ga4.displayname.service.response.Ga4DisplayNameResponse;
import com.quitmate.appMetrics.ga4.displayname.service.response.Ga4UnnamedValuesResponse;
import com.quitmate.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 「GA4 이름 설정」(KAN-230). 앱 지표의 화면·플랫폼·버전 원래 값에 보일 이름을 붙인다.
 * 관리자 계정만 쓸 수 있다(SecurityConfig, KAN-206). type은 SCREEN·PLATFORM·APP_VERSION(그 밖은 400).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/app-metrics/ga4/display-names")
public class Ga4DisplayNameController {

    private final Ga4DisplayNameService ga4DisplayNameService;

    /** 종류별 목록: 원래 값, 지금 이름, 어디서 온 이름인지, 고친 사람·시각 */
    @GetMapping
    public ApiResponse<List<Ga4DisplayNameResponse>> getDisplayNames(
            @RequestParam(name = "type", required = false) String type) {
        return ApiResponse.ok(ga4DisplayNameService.getDisplayNames(type));
    }

    /** 최근 90일 GA4에 나온 값 중 이름이 없는 것. GA4 연결 전이면 enabled=false, 호출 실패면 503 */
    @GetMapping("/unnamed")
    public ApiResponse<Ga4UnnamedValuesResponse> getUnnamedValues(
            @RequestParam(name = "type", required = false) String type) {
        return ApiResponse.ok(ga4DisplayNameService.getUnnamedValues(type));
    }

    /** 종류 + 원래 값 기준으로 이름 저장(없으면 추가, 있으면 수정) */
    @PutMapping
    public ApiResponse<Ga4DisplayNameResponse> save(@Valid @RequestBody Ga4DisplayNameSaveRequest request) {
        return ApiResponse.ok(ga4DisplayNameService.save(request.toServiceRequest()));
    }

    /** 관리자 이름을 지우고 기본 이름으로 되돌린다 */
    @DeleteMapping("/{id}")
    public ApiResponse<Ga4DisplayNameResponse> delete(@PathVariable("id") Long id) {
        return ApiResponse.ok(ga4DisplayNameService.delete(id));
    }
}
