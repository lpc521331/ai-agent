package com.liupc.aiagent.util;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

/**
 * 将长文本分割为适合向量生成的片段（避免向量丢失细节）
 */
public class TextSplitter {

    /**
     * 分割文本为片段
     * @param text 原始文本
     * @param chunkSize 每个片段最大字符数（推荐 300-500）
     * @param chunkOverlap 片段重叠字符数（推荐 50-100，保持上下文）
     * @return 分割后的文本片段列表
     */
    public static List<TextSegment> splitText(String text, int chunkSize, int chunkOverlap) {
        Document document = Document.from(text); // 用 LangChain4j 包装文本
        // 递归分割（优先按段落/句子，再按字符长度）
        return DocumentSplitters.recursive(chunkSize, chunkOverlap).split(document);
    }
}