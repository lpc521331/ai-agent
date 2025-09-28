package com.liupc.aiagent.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量数据库检索器（Spring Bean 版本）
 * 负责从 pgvector 中检索与问题相似的文档
 */
@Component // 注册为 Spring 组件，由 Spring 容器管理
@Slf4j // 日志注解，替代 System.err
public class PgvectorRetriever {

    // 注入 Spring 管理的数据源（从配置文件读取连接信息）
    private final DataSource dataSource;

    // 嵌入模型（作为实例变量，由当前 Bean 初始化）
    private final EmbeddingModel embeddingModel;

    // 可配置的距离阈值（从配置文件读取，默认0.5，需根据实际数据调整）
    @Value("${rag.similarity.threshold:0.5}")
    private double distanceThreshold;

    // 构造函数注入数据源（推荐方式，强制依赖）
    @Autowired
    public PgvectorRetriever(DataSource dataSource) {
        this.dataSource = dataSource;
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel(); // 初始化嵌入模型
        log.info("PgvectorRetriever 初始化完成，使用数据源: {}", dataSource);
    }

    /**
     * 检索与问题最相似的 topN 文档片段（实例方法，替代静态方法）
     * @param question 用户问题
     * @param topN 最多返回的文档数量
     * @return 相似文档内容列表
     * @throws SQLException 数据库操作异常（向上抛出，由调用方处理）
     */
    public List<String> retrieveSimilar(String question, int topN) throws SQLException {
        log.info("开始检索相似文档 - 问题: {}, 数量: {}", question, topN);

        // 1. 生成问题的向量嵌入
        Embedding queryEmbedding = embeddingModel.embed(question).content();
        List<Float> queryVector = queryEmbedding.vectorAsList();
        String queryVectorStr = "[" + queryVector.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", ")) + "]";
        log.debug("问题向量: {}", queryVectorStr);

        // 2. 执行 pgvector 相似性查询（欧氏距离 <->）
        List<String> results = new ArrayList<>();

        // SQL 增加 WHERE 条件过滤距离过大的结果
        String sql = "SELECT text, embedding <=> ?::vector AS distance "  // 余弦相似度（替换为 <-> 可保留欧氏距离）
                + "FROM rag_documents "
                + "WHERE embedding <=> ?::vector < ? "  // 距离小于阈值
                + "ORDER BY distance "
                + "LIMIT ?";

        // 使用 Spring 数据源获取连接（try-with-resources 自动关闭资源）
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, queryVectorStr);  // 用于计算距离
            pstmt.setString(2, queryVectorStr);  // 用于 WHERE 条件过滤
            pstmt.setDouble(3, distanceThreshold);  // 阈值
            pstmt.setInt(4, topN);  // 最大返回数量
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String docText = rs.getString("text");
                results.add(docText);
                log.debug("检索到文档: {}", docText.substring(0, Math.min(50, docText.length())) + "..."); // 日志截断长文本
            }

            log.info("检索完成，获取到 {} 条相似文档", results.size());
            return results;

        } catch (SQLException e) {
            log.error("检索文档失败 - 问题: {}, 错误信息: {}", question, e.getMessage(), e); // 详细日志
            throw e; // 向上抛出，让上层 Service 处理
        }
    }

    /**
     * 将文本片段和向量存入pgvector
     */
    public void storeSegmentsToPgvector(List<TextSegment> segments) throws SQLException {
        String sql = "INSERT INTO rag_documents (text, embedding) VALUES (?, vector(?))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (TextSegment segment : segments) {
                    // 生成向量
                    Embedding embedding = embeddingModel.embed(segment.text()).content();
                    List<Float> vector = embedding.vectorAsList();

                    // 转换向量为pgvector格式
                    String vectorStr = "[" + vector.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", ")) + "]";

                    pstmt.setString(1, segment.text());
                    pstmt.setString(2, vectorStr);
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            } catch (SQLException e) {
            log.error("插入向量数据库失败, 错误信息: {}", e.getMessage(), e); // 详细日志
            throw e; // 向上抛出，让上层 Service 处理
        }
    }
}