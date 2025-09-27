package com.liupc.aiagent.controller;

import com.liupc.aiagent.entity.dto.FileUploadResponse;
import com.liupc.aiagent.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * 上传文件到向量库
     */
    @PostMapping("/upload")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // 临时保存文件到本地（方便解析）
            File tempFile = File.createTempFile("rag-", file.getOriginalFilename());
            file.transferTo(tempFile);

            // 调用服务层处理文件
            int segmentCount = fileService.processAndStoreFile(tempFile.getAbsolutePath());

            // 删除临时文件
            tempFile.delete();

            return ResponseEntity.ok(new FileUploadResponse(true, "文件上传成功", segmentCount));
        }  catch (Exception e) {
            return ResponseEntity.badRequest().body(new FileUploadResponse(false, "文件上传失败：" + e.getMessage(), 0));

        }
    }
}