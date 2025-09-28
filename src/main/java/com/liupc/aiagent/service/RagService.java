package com.liupc.aiagent.service;

import com.liupc.aiagent.entity.dto.QueryResponse;

import java.util.Map;

public interface RagService {

    /**
     * RAG流程核心方法：检索相似文档 + 构建Prompt + 调用大模型
     * @param userMessage 用户原始问题
     * @return 包含回答、模型标识、成功状态的结果Map
     */
    Map<String, Object> ragChat(String userMessage);
}
