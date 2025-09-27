package com.liupc.aiagent.vector;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class PgvectorTableCreator {
    public static void createDocumentTable() throws SQLException {
        try (Connection conn = PgvectorUtils.getConnection();
             Statement stmt = conn.createStatement()) {
            // 启用 pgvector 扩展
            stmt.execute("CREATE EXTENSION IF NOT EXISTS vector");
            
            // 创建表：id（主键）、text（文档片段）、embedding（向量，384 维，对应 All-MiniLM 模型）
            String sql = "CREATE TABLE IF NOT EXISTS rag_documents (" +
                    "id SERIAL PRIMARY KEY," +
                    "text TEXT NOT NULL," +
                    "embedding vector(384) NOT NULL)";  // vector(维度)
            stmt.execute(sql);
            System.out.println("表创建成功");
        } catch (SQLException e) {
            System.err.println("创建表时发生错误: " + e.getMessage());
            throw e;
        }
    }

    public static void main(String[] args) {
        try {
            createDocumentTable();
        } catch (SQLException e) {
            System.err.println("无法创建表，请检查数据库连接和权限: " + e.getMessage());
            e.printStackTrace();
        }
    }
}