package com.liupc.aiagent.entity.dto;

import lombok.Data;

@Data
public class QueryRequest {

    // 用户提出的问题内容
    private String question;
}