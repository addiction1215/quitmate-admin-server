package com.quitmate;

import com.quitmate.global.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

	@GetMapping("/health-check")
	public ApiResponse<String> healthCheck() {
		return ApiResponse.ok("OK");
	}
}

