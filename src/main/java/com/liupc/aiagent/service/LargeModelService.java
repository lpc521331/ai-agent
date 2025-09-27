package com.liupc.aiagent.service;

import java.util.Map;

public interface LargeModelService {

    /**
     * 处理用户消息并调用大模型
     * @param message 用户输入消息
     * @return 包含响应结果的Map
     */
    Map<String, Object> handleMessage(String message);
}