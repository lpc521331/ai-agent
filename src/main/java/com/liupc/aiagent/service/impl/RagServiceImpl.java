package com.liupc.aiagent.service.impl;

import com.liupc.aiagent.service.LargeModelService;
import com.liupc.aiagent.service.RagService;
import com.liupc.aiagent.config.PgvectorRetriever; // 修改导入语句，使用config包下的PgvectorRetriever
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class RagServiceImpl implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);

    // 注入检索器（Spring Bean）
    @Autowired
    private PgvectorRetriever pgvectorRetriever;

    @Autowired
    @Qualifier("qwenServiceImpl")
    private LargeModelService qwenService;

    @Autowired
    @Qualifier("qwenServiceImpl")
    private LargeModelService fallbackModelService;

    // 从配置文件读取检索参数（灵活配置topK）
    @Value("${rag.retrieve.topK:3}")
    private int retrieveTopK;

    /**
     * 步骤1：检索相似文档
     */
    private List<String> retrieveRelevantDocs(String userMessage) throws SQLException {
        log.info("开始检索相似文档 - 查询词: {}, TopK: {}", userMessage, retrieveTopK);
        List<String> relevantDocs = pgvectorRetriever.retrieveSimilar(userMessage, retrieveTopK);
        log.info("检索完成 - 获取到 {} 条相似文档", relevantDocs.size());
        return relevantDocs;
    }

    /**
     * 步骤2：构建RAG Prompt（注入检索到的文档）
     */
    private String buildRagPrompt(String userMessage, List<String> relevantDocs) {
        // 若未检索到文档，直接返回原始问题（避免空文档干扰）
        if (relevantDocs.isEmpty()) {
            log.warn("未检索到相似文档，将直接回答原始问题");
            return userMessage;
        }

        // 构建包含参考文档的Prompt（格式可自定义）
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请基于以下参考文档，准确、简洁地回答用户问题：\n");
        promptBuilder.append("=== 参考文档开始 ===\n");
        promptBuilder.append(String.join("\n---\n", relevantDocs)); // 文档间用分隔符区分
        promptBuilder.append("\n=== 参考文档结束 ===\n");
        promptBuilder.append("用户问题：").append(userMessage);
        promptBuilder.append("\n注意：1. 优先使用参考文档中的信息回答；2. 若文档中无相关信息,尝试回答。");

        String finalPrompt = promptBuilder.toString();
        log.debug("构建的RAG Prompt: \n{}", finalPrompt);
        return finalPrompt;
    }

    /**
     * 步骤3：调用大模型（支持失败 fallback）
     */
    private Map<String, Object> callModelWithFallback(String ragPrompt) {
        Map<String, Object> modelResult;
        try {
            // 1. 优先调用默认模型（如OpenAI）
            modelResult = qwenService.handleMessage(ragPrompt);
            if ((boolean) modelResult.get("success")) {
                return modelResult;
            }
            log.warn("默认模型（{}）调用失败，尝试 fallback 到备用模型（Qwen）",
                    modelResult.getOrDefault("model", "OpenAI"));
        } catch (Exception e) {
            log.error("默认模型调用异常，触发 fallback", e);
        }

        // 2. fallback 到备用模型（如Qwen）
        return fallbackModelService.handleMessage(ragPrompt);
    }

    /**
     * RAG完整流程入口
     */
    @Override
    public Map<String, Object> ragChat(String userMessage) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 步骤1：检索相似文档
            List<String> relevantDocs = retrieveRelevantDocs(userMessage);

            // 步骤2：构建RAG Prompt
            String ragPrompt = buildRagPrompt(userMessage, relevantDocs);
            logger.info("ragPrompt:{}", ragPrompt);

            // 步骤3：调用大模型（带fallback）
            Map<String, Object> modelResult = callModelWithFallback(ragPrompt);
            logger.info("modelResult:{}", modelResult);

            // 整合结果（补充检索信息，方便前端展示）
            result.putAll(modelResult);
            result.put("retrieveCount", relevantDocs.size()); // 告知前端检索到的文档数量
            result.put("userMessage", userMessage); // 回显用户原始问题

        } catch (SQLException e) {
            // 数据库检索异常（单独捕获，明确错误类型）
            log.error("Pgvector检索异常", e);
            result.put("success", false);
            result.put("error", "文档检索失败: " + e.getMessage());
            result.put("retrieveCount", 0);
        } catch (Exception e) {
            // 其他全局异常
            log.error("RAG流程整体异常", e);
            result.put("success", false);
            result.put("error", "RAG处理错误: " + e.getMessage());
            result.put("retrieveCount", 0);
        }

        return result;
    }
}