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

@Service
@Slf4j
public class OllamaServiceImpl implements LargeModelService {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat-model.model-name}")
    private String ollamaModelName;

    /**
     * 创建Ollama客户端（Ollama无需真实API Key，填充占位符即可）
     */
    private OpenAIClient createOllamaClient() {
        log.info("创建Ollama客户端 - baseUrl: {}, model: {}", ollamaBaseUrl, ollamaModelName);
        return OpenAIOkHttpClient.builder()
                .baseUrl(ollamaBaseUrl)
                .apiKey("ollama") // Ollama不需要API Key，仅为满足客户端参数要求
                .build();
    }

    /**
     * 实现Ollama模型调用逻辑
     */
    @Override
    public Map<String, Object> handleMessage(String message) {
        Map<String, Object> result = new HashMap<>();
        OpenAIClient client = createOllamaClient();

        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(ollamaModelName)
                    .addUserMessage(message)
                    .temperature(0.7)
                    .maxTokens(1024)
                    .build();

            ChatCompletion chatCompletion = client.chat().completions().create(params);
            String answer = chatCompletion.choices().get(0).message().content().orElse("未获取到回答");

            result.put("answer", answer);
            result.put("success", true);
        } catch (OpenAIException e) {
            log.error("Ollama API调用异常", e);
            result.put("success", false);
            result.put("error", "Ollama调用错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("Ollama处理异常", e);
            result.put("success", false);
            result.put("error", "系统错误: " + e.getMessage());
        } finally {
            // 关闭客户端资源
            if (client instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) client).close();
                } catch (Exception e) {
                    log.warn("Ollama客户端关闭异常", e);
                }
            }
        }

        return result;
    }
}