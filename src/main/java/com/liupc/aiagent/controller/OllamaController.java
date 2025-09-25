package com.liupc.aiagent.controller;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.java.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ollama")
@CrossOrigin(origins = "*") // 允许跨域访问，方便前端调用
@Log
public class OllamaController {

    Logger logger = LoggerFactory.getLogger(OllamaController.class);

    // 从配置文件读取Ollama和Qwen配置
    @Value("${spring.ai.ollama.base-url}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.chat-model.model-name}")
    private String ollamaModelName;

    @Value("${langchain4j.community.dashscope.chat-model.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String qwenBaseUrl;

    @Value("${langchain4j.community.dashscope.chat-model.api-key:}")
    private String qwenApiKey;

    // 创建Qwen客户端
    private OpenAIClient createQwenClient() {
        logger.info("Qwen ApiKey:" + qwenApiKey);
        logger.info("Qwen baseUrl:" + qwenBaseUrl);
        return OpenAIOkHttpClient.builder()
                .apiKey(qwenApiKey)
                .baseUrl(qwenBaseUrl)
                .build();
    }

    /**
     * 简单问答接口（GET请求），默认使用Ollama
     * 访问示例：http://localhost:8080/api/ollama/chat?message=你好，介绍一下自己&useQwen=false
     */
    @GetMapping("/chat")
    public Map<String, Object> chatGet(@RequestParam String message, @RequestParam(defaultValue = "false") boolean useQwen) {
        return chat(message, useQwen);
    }

    /**
     * 问答接口（POST请求）
     * 请求体示例：{"message":"你好，介绍一下自己", "useQwen": false}
     */
    @PostMapping("/chat")
    public Map<String, Object> chatPost(@RequestBody Map<String, Object> request) {
        String message = (String) request.getOrDefault("message", "请输入问题");
        boolean useQwen = (Boolean) request.getOrDefault("useQwen", false);
        return chat(message, useQwen);
    }

    // 核心处理方法
    private Map<String, Object> chat(String message, boolean useQwen) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (useQwen) {
                if (qwenApiKey == null || qwenApiKey.isEmpty()) {
                    result.put("success", false);
                    result.put("error", "Qwen API Key未配置");
                    return result;
                }
                result.put("answer", callQwen(message));
                result.put("model", "Qwen-max");
            } else {
                result.put("answer", callOllama(message));
                result.put("model", "Ollama");
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "系统错误: " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    // 调用Ollama模型
    private String callOllama(String message) {
        logger.info("调用Ollama，baseUrl: " + ollamaBaseUrl + "，model: " + ollamaModelName);
        OpenAIClient client = OpenAIOkHttpClient.builder()
                .baseUrl(ollamaBaseUrl)
                .apiKey("ollama") // Ollama不需要API密钥，但库要求设置，可以设置为任意值
                .build();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model(ollamaModelName) // 使用配置的模型名称
                .addUserMessage(message)
                .temperature(0.7)
                .maxTokens(1024)
                .build();

        ChatCompletion chatCompletion = client.chat().completions().create(params);
        return chatCompletion.choices().get(0).message().content().orElse("未获取到回答");
    }

    // 调用Qwen模型
    private String callQwen(String message) {
        OpenAIClient client = createQwenClient();

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model("qwen-max") // 使用qwen-max模型
                .addUserMessage(message)
                .temperature(0.7)
                .maxTokens(1024)
                .build();

        ChatCompletion chatCompletion = client.chat().completions().create(params);
        return chatCompletion.choices().get(0).message().content().orElse("未获取到回答");
    }
}