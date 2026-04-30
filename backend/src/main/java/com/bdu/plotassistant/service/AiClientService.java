package com.bdu.plotassistant.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.function.Consumer;

public interface AiClientService {

    /**
     * 同步调用 - 用于生成场景描述、世界观描述等短文本
     * 固定参数：temperature=0.7, max_tokens=2000, response_format=json_object
     *
     * @param systemPrompt 系统提示词（角色设定）
     * @param userPrompt   用户提示词（具体请求）
     * @return AI 生成的纯文本内容（已提取 JSON 中的 content）
     */
    String syncCompletion(String systemPrompt, String userPrompt);

    /**
     * 流式调用 (SSE) - 用于大纲、角色集等长文本生成
     * 直接解析 OpenAI SSE 流，逐段发送给前端，30秒超时
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param onComplete   流结束回调，接收完整内容（用于保存到DB），可为 null
     * @return SseEmitter 供 Controller 直接返回
     */
    SseEmitter streamCompletion(String systemPrompt, String userPrompt, Consumer<String> onComplete);

    /**
     * 测试 AI API 连接是否可用
     * 发送简单请求："请回复 OK"，检查是否收到响应
     *
     * @return 测试结果："OK - 连接正常" 或错误信息
     */
    String testConnection();
}
