package com.liupc.aiagent.controller;

import com.liupc.aiagent.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/rag")
@CrossOrigin(origins = "*") // 允许跨域访问
public class VectorRagController {

    private static final Logger logger = LoggerFactory.getLogger(VectorRagController.class);

    // 注入RAG服务（核心业务逻辑全部委托给Service）
    @Autowired
    private RagService ragService;

    /**
     * RAG问答接口（GET请求，适合快速测试）
     * 访问示例：http://localhost:8080/api/rag/chat?message=什么是向量数据库？
     */
    @GetMapping("/chat")
    public Map<String, Object> chatGet(@RequestParam String message) {
        logger.info("接收RAG GET请求 - 用户问题: {}", message);
        // 直接委托Service处理，Controller不做业务逻辑
        return ragService.ragChat(message);
    }

    /**
     * RAG问答接口（POST请求，适合前端正式调用）
     * 请求体示例：{"message": "什么是向量数据库？"}
     */
    @PostMapping("/chat")
    public Map<String, Object> chatPost(@RequestBody Map<String, String> request) {
        String message = request.getOrDefault("message", "请输入您的问题");
        logger.info("接收RAG POST请求 - 用户问题: {}", message);
        return ragService.ragChat(message);
    }
}