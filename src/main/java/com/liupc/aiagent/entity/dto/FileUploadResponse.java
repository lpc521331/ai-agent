package com.liupc.aiagent.entity.dto;

import lombok.Data;

// 文件上传响应DTO
@Data
public class FileUploadResponse {
    private boolean success;
    private String message;
    private int segmentCount; // 分割后的片段数

    // 构造器、getter、setter
    public FileUploadResponse(boolean success, String message, int segmentCount) {
        this.success = success;
        this.message = message;
        this.segmentCount = segmentCount;
    }
}