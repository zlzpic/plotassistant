package com.bdu.plotassistant.controller;

import com.bdu.plotassistant.dto.ApiResult;
import com.bdu.plotassistant.service.AiClientService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
public class TestAiController {

    private final AiClientService aiClientService;

    public TestAiController(AiClientService aiClientService) {
        this.aiClientService = aiClientService;
    }

    @GetMapping("/ai-connection")
    public ApiResult<String> testAiConnection() {
        String result = aiClientService.testConnection();
        return ApiResult.success(result);
    }
}
