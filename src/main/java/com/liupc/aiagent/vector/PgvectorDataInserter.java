package com.liupc.aiagent.vector;


import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class PgvectorDataInserter {
    private static final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    public static void insertDocument(String text) throws SQLException {
        // 1. 生成文本的向量（384 维）
        Embedding embedding = embeddingModel.embed(text).content();
        List<Float> vector = embedding.vectorAsList();  // All-MiniLM 输出为 Double 列表

        // 2. 将向量转换为 pgvector 支持的格式（如 "[0.123, 0.456, ...]"）
        String vectorStr = "[" + vector.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")) + "]";

        // 3. 插入数据库
        try (Connection conn = PgvectorUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO rag_documents (text, embedding) VALUES (?, ?::vector)")) {
            pstmt.setString(1, text);
            pstmt.setString(2, vectorStr);  // 直接传入向量字符串
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("插入文档时发生错误: " + e.getMessage());
            throw e;
        }
    }

    public static void main(String[] args) {
        // 测试插入：Java 线程相关的文档片段
        String docText = "Java 中创建线程的两种方式：1. 继承 Thread 类；2. 实现 Runnable 接口。" +
                "推荐使用 Runnable 接口，因为 Java 单继承限制。";
        try {
            insertDocument(docText);
            System.out.println("文档插入成功");
        } catch (SQLException e) {
            System.err.println("无法插入文档，请检查数据库连接和权限: " + e.getMessage());
            e.printStackTrace();
        }
    }
}