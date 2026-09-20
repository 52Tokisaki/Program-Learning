package com.tianji.aigc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/embedding")
@RequiredArgsConstructor
@Slf4j
public class EmbeddingController {

    private final VectorStore vectorStore;

    @PostMapping
    public void saveVectorStore(@RequestParam("messages")List<String> messages) {
        log.info("保存到向量数据库中，消息数据：{}", messages);
        List<Document> documentList = messages.stream().map(message -> Document.builder().text(message).build()).toList();
        vectorStore.add(documentList);
        log.info("保存到向量数据库成功, 数量：{}", messages.size());
    }
}
