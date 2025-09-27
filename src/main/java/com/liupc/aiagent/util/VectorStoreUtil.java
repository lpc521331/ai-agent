package com.liupc.aiagent.util;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 将文本片段生成向量并存储到 pgvector
 */
@Component // 注册为 Spring 组件，由 Spring 容器管理
@Slf4j // 日志注解，替代 System.err
public class VectorStoreUtil {

    // 嵌入模型（All-MiniLM-L6-v2，轻量级开源模型，384 维向量）
    private final EmbeddingModel embeddingModel ;

    private final DataSource dataSource;

    @Autowired
    public VectorStoreUtil(DataSource dataSource) {
        this.dataSource = dataSource;
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel(); // 初始化嵌入模型
        log.info("PgvectorRetriever 初始化完成，使用数据源: {}", dataSource);
    }
    /**
     * 批量将文本片段存入 pgvector
     * @param segments 文本片段列表
     */
    public void storeSegments(List<TextSegment> segments) throws SQLException {
        // SQL：插入文本和向量（vector() 函数显式转换类型）
        String sql = "INSERT INTO rag_documents (text, embedding) VALUES (?, vector(?))";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (TextSegment segment : segments) {
                // 1. 生成向量
                Embedding embedding = embeddingModel.embed(segment.text()).content();
                List<Float> vector = embedding.vectorAsList();

                // 2. 向量转为 pgvector 格式（[x1, x2, ..., x384]）
                String vectorStr = "[" + vector.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(", ")) + "]";

                // 3. 设置参数并添加到批处理
                pstmt.setString(1, segment.text());
                pstmt.setString(2, vectorStr);
                pstmt.addBatch(); // 批量插入优化
            }

            // 执行批处理
            pstmt.executeBatch();
            System.out.println("成功入库 " + segments.size() + " 个文本片段");
        }
    }
}