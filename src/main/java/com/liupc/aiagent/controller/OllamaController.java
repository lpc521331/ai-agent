package com.liupc.aiagent.controller;

import com.liupc.aiagent.service.LargeModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ollama")
@CrossOrigin(origins = "*")
public class OllamaController {

    private static final Logger logger = LoggerFactory.getLogger(OllamaController.class);

    // 注入Qwen和Ollama的Service实现（通过@Qualifier指定具体实现类）
    @Autowired
    @Qualifier("qwenServiceImpl")
    private LargeModelService qwenService;

    @Autowired
    @Qualifier("ollamaServiceImpl")
    private LargeModelService ollamaService;

    /**
     * GET请求接口：根据useQwen参数选择模型
     */
    @GetMapping("/chat")
    public Map<String, Object> chatGet(
            @RequestParam String message,
            @RequestParam(defaultValue = "false") boolean useQwen
    ) {
        logger.info("接收GET请求 - 消息: {}, 使用Qwen: {}", message, useQwen);
        return chat(message, useQwen);
    }

    /**
     * POST请求接口：根据useQwen参数选择模型
     */
    @PostMapping("/chat")
    public Map<String, Object> chatPost(@RequestBody Map<String, Object> request) {
        String message = (String) request.getOrDefault("message", "请输入问题");
        boolean useQwen = (Boolean) request.getOrDefault("useQwen", false);
        logger.info("接收POST请求 - 消息: {}, 使用Qwen: {}", message, useQwen);
        return chat(message, useQwen);
    }

    /**
     * 核心分发逻辑：根据useQwen选择对应的Service处理
     */
    private Map<String, Object> chat(String message, boolean useQwen) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 选择调用Qwen还是Ollama的Service
            LargeModelService targetService = useQwen ? qwenService : ollamaService;
            Map<String, Object> modelResult = targetService.handleMessage(message);

            // 补充模型标识信息
            result.putAll(modelResult);
            result.put("model", useQwen ? "Qwen-max" : "Ollama");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", "系统错误: " + e.getMessage());
            logger.error("请求处理异常", e);
        }

        return result;
    }
}