package com.liupc.aiagent.vector;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PgvectorUtils {

    // PostgreSQL 连接信息（替换为你的配置）
    private static final String URL = "jdbc:postgresql://localhost:5432/rag_db";
    private static final String USER = "lpc";
    private static final String PASSWORD = "postgres";
    private static final String DEFAULT_URL = "jdbc:postgresql://localhost:5432/postgres";

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            // 如果是数据库不存在的错误，尝试创建数据库
            if (e.getSQLState().equals("3D000")) { // 数据库不存在
                System.out.println("数据库 rag_db 不存在，正在尝试创建...");
                try {
                    createDatabase();
                    return DriverManager.getConnection(URL, USER, PASSWORD);
                } catch (SQLException createException) {
                    System.err.println("自动创建数据库失败: " + createException.getMessage());
                    System.err.println("请手动创建数据库或检查用户权限");
                    throw createException;
                }
            } else {
                System.err.println("数据库连接失败，请检查以下配置:");
                System.err.println("URL: " + URL);
                System.err.println("USER: " + USER);
                System.err.println("PASSWORD: " + (PASSWORD != null ? "***" : "null"));
                throw e;
            }
        }
    }

    private static void createDatabase() throws SQLException {
        try (Connection conn = DriverManager.getConnection(DEFAULT_URL, USER, PASSWORD);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE rag_db");
            System.out.println("数据库 rag_db 创建成功");
        } catch (SQLException e) {
            System.err.println("创建数据库时发生错误: " + e.getMessage());
            throw e;
        }
    }
}
