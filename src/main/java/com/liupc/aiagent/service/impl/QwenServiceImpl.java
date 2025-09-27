package com.liupc.aiagent.service.impl;

import com.liupc.aiagent.service.LargeModelService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.errors.OpenAIException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class QwenServiceImpl implements LargeModelService {


    // 从配置文件读取API密钥和基础URL
    @Value("${langchain4j.community.dashscope.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.community.dashscope.chat-model.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;


    /**
     * 创建OpenAI客户端（复用逻辑）
     */
    private OpenAIClient createClient() {
        log.info("创建Qwen客户端 - apiKey: {}, baseUrl: {}", apiKey, baseUrl);
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * 核心处理方法：调用大模型并返回结果
     */
    @Override
    public Map<String, Object> handleMessage(String message) {
        Map<String, Object> result = new HashMap<>();
        OpenAIClient client = createClient();

        try {
            // 构建请求参数
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model("qwen-plus") // 阿里千问模型
                    .addUserMessage(message) // 添加用户消息
                    .temperature(0.7) // 随机性
                    .maxTokens(1024) // 最大响应长度
                    .build();

            // 调用API
            ChatCompletion chatCompletion = client.chat().completions().create(params);

            // 处理响应结果
            if (!chatCompletion.choices().isEmpty()) {
                Optional<String> answer = chatCompletion.choices().get(0).message().content();
                result.put("success", true);
                result.put("answer", answer.orElse("未获取到具体内容"));
            } else {
                result.put("success", false);
                result.put("error", "未获取到回答");
            }
        } catch (OpenAIException e) {
            log.error("API调用异常", e);
            result.put("success", false);
            result.put("error", "API调用错误: " + e.getMessage());
            result.put("statusCode", 500);
        } catch (Exception e) {
            log.error("系统处理异常", e);
            result.put("success", false);
            result.put("error", "系统错误: " + e.getMessage());
        } finally {
            // 关闭客户端资源
            if (client instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) client).close();
                } catch (Exception e) {
                    log.warn("客户端关闭异常", e);
                }
            }
        }

        return result;
    }
}