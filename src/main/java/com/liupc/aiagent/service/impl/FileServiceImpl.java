package com.liupc.aiagent.service.impl;

import com.liupc.aiagent.config.PgvectorRetriever;
import com.liupc.aiagent.service.FileService;
import com.liupc.aiagent.util.FileTextExtractor;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private PgvectorRetriever pgvectorRetriever;

    /**
     * 处理文件并存储到pgvector
     */
    public int processAndStoreFile(String filePath) throws Exception {
        // 1. 解析文件提取文本
        String text = FileTextExtractor.extractText(filePath);

        // 2. 分割文本为片段（技术文档推荐参数：500字符/段，重叠100字符）
        Document document = Document.from(text);
        List<TextSegment> segments = DocumentSplitters.recursive(500, 100).split(document);

        // 3. 生成向量并存储到pgvector
        pgvectorRetriever.storeSegmentsToPgvector(segments);
        return segments.size();
    }
}