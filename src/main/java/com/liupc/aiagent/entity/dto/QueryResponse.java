package com.liupc.aiagent.entity.dto;

import lombok.Data;

import java.util.List;

@Data
public class QueryResponse {
    private String answer;
    private List<String> sources; // 参考的文档片段
    private Boolean success;

    // 构造器、getter、setter
    public QueryResponse(String answer, List<String> sources) {
        this.answer = answer;
        this.sources = sources;
    }

}