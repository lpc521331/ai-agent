package com.liupc.aiagent.vector;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PgvectorRetriever {
    private static final EmbeddingModel embeddingModel = new AllMiniLmL6V2EmbeddingModel();

    // 检索与问题最相似的 topN 文档片段
    public static List<String> retrieveSimilar(String question, int topN) throws SQLException {
        // 1. 生成问题的向量
        Embedding queryEmbedding = embeddingModel.embed(question).content();
        List<Float> queryVector = queryEmbedding.vectorAsList();
        String queryVectorStr = "[" + queryVector.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")) + "]";

        // 2. 执行相似性查询（使用 <-> 计算欧氏距离，值越小越相似）
        List<String> results = new ArrayList<>();
        String sql = "SELECT text, embedding <-> ?::vector AS distance " +  // <-> 是 pgvector 的欧氏距离运算符
                "FROM rag_documents " +
                "ORDER BY distance " +
                "LIMIT ?";

        try (Connection conn = PgvectorUtils.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, queryVectorStr);
            pstmt.setInt(2, topN);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                results.add(rs.getString("text"));
            }
        } catch (SQLException e) {
            System.err.println("检索文档时发生错误: " + e.getMessage());
            throw e;
        }
        return results;
    }

    public static void main(String[] args) {
        try {
            // 测试检索：查询与"Java 如何创建线程"相关的文档
            List<String> similarDocs = retrieveSimilar("Java 线程创建方法有哪些？", 1);
            System.out.println("检索到的相关文档：" + similarDocs);
        } catch (SQLException e) {
            System.err.println("无法检索文档，请检查数据库连接和权限: " + e.getMessage());
            e.printStackTrace();
        }
    }
}