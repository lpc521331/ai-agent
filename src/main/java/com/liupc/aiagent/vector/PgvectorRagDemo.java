package com.liupc.aiagent.vector;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;

import java.sql.SQLException;
import java.util.List;

public class PgvectorRagDemo {

    // OpenAI API配置
    private static final String OPENAI_API_KEY = System.getenv().getOrDefault("OPENAI_API_KEY", "");
    private static final String OPENAI_BASE_URL = "https://api.openai.com/v1";
    private static final String OPENAI_MODEL = "gpt-4o-mini";

    public static String answer(String question) {
        // 1. 检索相似文档
        List<String> relevantDocs;
        try {
            relevantDocs = PgvectorRetriever.retrieveSimilar(question, 3);
        } catch (SQLException e) {
            return "数据库检索失败: " + e.getMessage();
        }

        // 2. 构建提示词
        String prompt = "基于以下知识回答问题：\n" +
                String.join("\n---\n", relevantDocs) +
                "\n问题：" + question;

        // 3. 调用大模型生成回答
        return callQwen(prompt);
    }

    // 从环境变量获取Qwen API密钥（DashScope的API Key）
    private static final String QWEN_API_KEY = System.getenv("DASHSCOPE_API_KEY"); 
    private static final String QWEN_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"; // Qwen兼容OpenAI的接口地址
    private static final String QWEN_MODEL = "qwen-plus"; // Qwen模型名称（可选qwen-plus/qwen-max/qwen-turbo等）
    
    /**
     * 调用Qwen大模型生成回答
     * @param prompt 提示词（包含用户问题+上下文等）
     * @return 模型返回的回答或错误信息
     */
    private static String callQwen(String prompt) {
        // 检查API密钥是否配置
        if (QWEN_API_KEY == null || QWEN_API_KEY.isEmpty()) {
            return "错误：DASHSCOPE_API_KEY 环境变量未设置。请设置 DASHSCOPE_API_KEY 环境变量以使用 Qwen 服务。";
        }

        try {
            // 创建Qwen客户端（兼容OpenAI客户端接口）
            OpenAIClient client = OpenAIOkHttpClient.builder()
                    .apiKey(QWEN_API_KEY) // 使用Qwen的API Key（DashScope的密钥）
                    .baseUrl(QWEN_BASE_URL) // Qwen的兼容接口地址
                    .build();

            // 构建Qwen请求参数
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(QWEN_MODEL) // 指定Qwen模型（如qwen-plus）
                    .addUserMessage(prompt) // 传入提示词
                    .temperature(0.7) // 随机性（0-1，越高越随机）
                    .maxTokens(1024) // 最大响应长度
                    .build();

            // 调用Qwen API并获取结果
            ChatCompletion chatCompletion = client.chat().completions().create(params);
            // 提取回答内容（若为空返回默认提示）
            return chatCompletion.choices().get(0).message().content().orElse("未获取到Qwen的回答");
        } catch (Exception e) {
            // 捕获异常并返回错误信息
            return "调用 Qwen API 时发生错误: " + e.getMessage();
        }
    }

    public static void main(String[] args) {
        String answer = answer("Java 推荐用什么方式创建线程？");
        System.out.println("回答：" + answer);
    }
}