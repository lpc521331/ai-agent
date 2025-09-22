package com.liupc.aiagent.controller;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.errors.OpenAIException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/api/qwen")
@CrossOrigin(origins = "*") // 允许跨域访问，方便前端调用
@Log
public class QwenController {

    Logger logger = LoggerFactory.getLogger(QwenController.class);

    // 从配置文件读取API密钥和基础URL
    @Value("${langchain4j.community.dashscope.chat-model.api-key}")
    private String apiKey;

    @Value("${langchain4j.community.dashscope.chat-model.qwen.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String baseUrl;

    // 创建OpenAI客户端（单例复用）
    private OpenAIClient createClient() {

        logger.info("ApiKey:"+apiKey);
        logger.info("baseUrl:"+baseUrl);
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .build();
    }

    /**
     * 简单问答接口（GET请求，适合浏览器直接访问）
     * 访问示例：http://localhost:8080/api/qwen/chat?message=你好，介绍一下自己
     */
    @GetMapping("/chat")
    public Map<String, Object> chatGet(@RequestParam String message) {
        return chat(message);
    }

    /**
     * 问答接口（POST请求，适合前端表单提交）
     * 请求体示例：{"message":"你好，介绍一下自己"}
     */
    @PostMapping("/chat")
    public Map<String, Object> chatPost(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "请输入问题");
        return chat(message);
    }

    // 核心处理方法
    private Map<String, Object> chat(String message) {
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
                result.put("answer", answer);
            } else {
                result.put("success", false);
                result.put("error", "未获取到回答");
            }
        } catch (OpenAIException e) {
            result.put("success", false);
            result.put("error", "API调用错误: " + e.getMessage());
            result.put("statusCode", 500);
        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "系统错误: " + e.getMessage());
        } finally {
            // 关闭客户端资源
            if (client instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) client).close();
                } catch (Exception e) {
                    // 忽略关闭异常
                }
            }
        }

        return result;
    }
}
