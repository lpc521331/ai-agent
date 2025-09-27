package com.liupc.aiagent.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import java.io.*;

/**
 * 解析本地文件提取文本（支持 PDF、DOCX、TXT）
 */
public class FileTextExtractor {

    /**
     * 根据文件类型提取文本
     * @param filePath 本地文件路径（如 "D:/docs/JavaGuide.pdf"）
     * @return 提取的纯文本
     */
    public static String extractText(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("文件不存在：" + filePath);
        }

        // 按文件后缀选择解析方式
        if (filePath.endsWith(".pdf")) {
            return extractPdfText(file);
        } else if (filePath.endsWith(".docx")) {
            return extractDocxText(file);
        } else if (filePath.endsWith(".txt")) {
            return extractTxtText(file);
        } else {
            throw new UnsupportedOperationException("不支持的文件格式：" + filePath);
        }
    }

    // 解析 PDF 文本
    private static String extractPdfText(File file) throws IOException {
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document); // 提取所有页文本
        }
    }

    // 解析 Word (DOCX) 文本
    private static String extractDocxText(File file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new FileInputStream(file))) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString().trim();
        }
    }

    // 解析 TXT 文本
    private static String extractTxtText(File file) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            StringBuilder text = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append("\n");
            }
            return text.toString().trim();
        }
    }
}