package com.bdu.plotassistant.service.impl;

import com.bdu.plotassistant.config.AiProperties;
import com.bdu.plotassistant.service.AiClientService;
import com.bdu.plotassistant.utils.BizException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

@Service
public class OpenAiClientServiceImpl implements AiClientService {

    private final AiProperties aiProperties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    // 固定参数
    private static final double TEMPERATURE = 0.7;
    private static final int MAX_TOKENS = 4000;
    private static final long TIMEOUT_MILLIS = 90000; // 90秒

    public OpenAiClientServiceImpl(AiProperties aiProperties, ObjectMapper objectMapper) {
        this.aiProperties = aiProperties;
        this.objectMapper = objectMapper;

        // 配置 HttpClient 使用系统 DNS
        HttpClient httpClient = HttpClient.create()
                .resolver(DefaultAddressResolverGroup.INSTANCE);  // 强制使用系统 DNS 解析

        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))  // 使用自定义 HttpClient
                .baseUrl(aiProperties.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + aiProperties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public String syncCompletion(String systemPrompt, String userPrompt) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                Map<String, Object> requestBody = buildRequestBody(systemPrompt, userPrompt, false);

                String response = webClient.post()
                        .uri("/chat/completions")
                        .bodyValue(requestBody)
                        .retrieve()
                        .bodyToMono(String.class)
                        .timeout(Duration.ofMillis(TIMEOUT_MILLIS))
                        .block();

                return extractContent(response);

            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new BizException("AI 调用失败（重试" + maxRetries + "次后）: " + e.getMessage());
                }

                // 指数退避：第1次等1秒，第2次等2秒
                try {
                    Thread.sleep(attempt * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new BizException("调用被中断");
                }

                System.out.println("AI 调用失败，第" + attempt + "次重试...");
            }
        }

        throw new BizException("AI 调用异常");
    }

    @Override
    public SseEmitter streamCompletion(String systemPrompt, String userPrompt,
                                       Consumer<String> onComplete) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MILLIS);
        StringBuilder fullContent = new StringBuilder();

        try {
            Map<String, Object> requestBody = buildRequestBody(systemPrompt, userPrompt, true);

            Disposable subscription = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofMillis(TIMEOUT_MILLIS))
                    .subscribe(
                            line -> {
                                // 调试日志（生产环境可注释）
                                System.out.println("收到原始行: [" + line + "]");

                                // 空行跳过
                                if (line == null || line.trim().isEmpty()) {
                                    return;
                                }

                                // 处理结束标记 [[DONE]] 或 [DONE]
                                if (line.equals("[[DONE]]") || line.equals("[DONE]")) {
                                    safeComplete(emitter, fullContent.toString(), onComplete);
                                    return;
                                }

                                // 处理 JSON 数组格式（NVIDIA 返回的是 [{...}]）
                                if (line.trim().startsWith("[")) {
                                    try {
                                        JsonNode root = objectMapper.readTree(line);

                                        // NVIDIA 返回的是数组，取第一个元素
                                        if (root.isArray() && root.size() > 0) {
                                            JsonNode chunk = root.get(0);
                                            JsonNode choices = chunk.path("choices");

                                            if (choices.isArray() && choices.size() > 0) {
                                                JsonNode choice = choices.get(0);

                                                // 检查是否结束（finish_reason 不为 null）
                                                if (!choice.path("finish_reason").isNull()) {
                                                    String finishReason = choice.path("finish_reason").asText();
                                                    if ("length".equals(finishReason)) {
                                                        System.out.println("警告：达到 max_tokens 限制，内容可能被截断");
                                                    }
                                                    // 正常结束（无论 stop 还是 length）
                                                    safeComplete(emitter, fullContent.toString(), onComplete);
                                                    return;
                                                }

                                                // 提取 content（delta.content）
                                                String content = choice.path("delta").path("content").asText();
                                                if (content != null && !content.isEmpty()) {
                                                    fullContent.append(content);
                                                    try {
                                                        emitter.send(content);
                                                    } catch (Exception e) {
                                                        // 客户端断开连接
                                                        safeCompleteWithError(emitter, e);
                                                    }
                                                }
                                            }

                                            // 处理 usage 信息（最后一条，没有 choices）
                                            if (choices.isArray() && choices.size() == 0 && chunk.has("usage")) {
                                                // 这是最后一条统计信息，忽略或记录
                                                System.out.println("Token 使用统计: " + chunk.path("usage"));
                                            }
                                        }
                                    } catch (Exception e) {
                                        System.err.println("解析 JSON 失败: " + e.getMessage() + ", 行内容: " + line);
                                        // 解析失败不中断，继续处理下一行
                                    }
                                }
                            },
                            error -> {
                                System.err.println("流式调用异常: " + error.getMessage());
                                error.printStackTrace();
                                safeCompleteWithError(emitter,
                                        new BizException("AI 流式调用失败: " + error.getMessage()));
                            },
                            () -> {
                                // 流正常完成（保险起见）
                                safeComplete(emitter, fullContent.toString(), onComplete);
                            }
                    );

            // 当 emitter 超时时取消订阅
            emitter.onTimeout(() -> {
                subscription.dispose();
                safeCompleteWithError(emitter, new TimeoutException("AI 响应超时"));
            });

            // 当客户端断开时取消订阅
            emitter.onCompletion(() -> {
                subscription.dispose();
            });

        } catch (Exception e) {
            safeCompleteWithError(emitter, e);
        }

        return emitter;
    }

    @Override
    public String testConnection() {
        try {
            // 构建极简测试请求
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", aiProperties.getModel());
            requestBody.put("messages", Arrays.asList(
                    createMessage("user", "请只回复ok两个字母，不要其他内容")
            ));
            requestBody.put("temperature", 0.0); // 最确定性输出
            requestBody.put("max_tokens", 10);   // 只需几个token

            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10)) // 10秒超时足够
                    .block();

            // 解析响应
            String content = extractContent(response);

            if (content != null && content.toUpperCase().contains("OK")) {
                return "OK - 连接正常，模型: " + aiProperties.getModel()+content;
            } else {
                return "警告 - 收到响应但内容异常: " + content;
            }

        } catch (Exception e) {
            return "错误 - 连接失败: " + e.getMessage();
        }
    }

// ========== 私有辅助方法（安全完成）==========

    private void safeComplete(SseEmitter emitter, String fullContent, Consumer<String> onComplete) {
        try {
            if (onComplete != null) {
                onComplete.accept(fullContent);
            }
            emitter.complete();
        } catch (IllegalStateException e) {
            // 已经 completed，忽略
        } catch (Exception e) {
            // 其他异常
        }
    }

    private void safeCompleteWithError(SseEmitter emitter, Throwable error) {
        try {
            emitter.completeWithError(error);
        } catch (IllegalStateException e) {
            // 已经 completed，忽略
        } catch (Exception e) {
            // 其他异常
        }
    }

    // ========== 私有方法 ==========

    private Map<String, Object> buildRequestBody(String systemPrompt, String userPrompt, boolean stream) {
        Map<String, Object> body = new HashMap<>();

        // 模型名称：从配置读取，默认 gpt-4
        body.put("model", aiProperties.getModel());

        // Messages
        body.put("messages", Arrays.asList(
                createMessage("system", systemPrompt),
                createMessage("user", userPrompt)
        ));

        // 固定参数
        body.put("temperature", TEMPERATURE);
        body.put("max_tokens", MAX_TOKENS);
        body.put("stream", stream);

        // 强制 JSON mode
        Map<String, String> responseFormat = new HashMap<>();
        responseFormat.put("type", "json_object");
        body.put("response_format", responseFormat);

        return body;
    }

    private Map<String, String> createMessage(String role, String content) {
        Map<String, String> msg = new HashMap<>();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    private String extractContent(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new BizException("解析 AI 响应失败: " + e.getMessage());
        }
    }

    private String extractDeltaContent(String dataJson) {
        try {
            JsonNode root = objectMapper.readTree(dataJson);
            return root.path("choices").get(0).path("delta").path("content").asText();
        } catch (Exception e) {
            return null; // 解析失败返回空，不中断流
        }
    }
}
