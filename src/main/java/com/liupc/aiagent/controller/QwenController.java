package com.liupc.aiagent.controller;

import com.liupc.aiagent.service.LargeModelService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.errors.OpenAIException;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private static final Logger logger = LoggerFactory.getLogger(QwenController.class);

    @Qualifier("qwenServiceImpl")
    @Autowired
    LargeModelService largeModelService;
    /**
     * 简单问答接口（GET请求，适合浏览器直接访问）
     * 访问示例：http://localhost:8080/api/qwen/chat?message=你好，介绍一下自己
     */
    @GetMapping("/chat")
    public Map<String, Object> chatGet(@RequestParam String message) {
        logger.info("用户输入的问题："+message);
        return largeModelService.handleMessage(message);
    }

    /**
     * 问答接口（POST请求，适合前端表单提交）
     * 请求体示例：{"message":"你好，介绍一下自己"}
     */
    @PostMapping("/chat")
    public Map<String, Object> chatPost(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "请输入问题");
        return largeModelService.handleMessage(message);
    }


}
