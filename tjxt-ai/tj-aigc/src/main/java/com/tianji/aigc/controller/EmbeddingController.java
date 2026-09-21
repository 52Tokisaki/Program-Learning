package com.tianji.aigc.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/embedding")
@RequiredArgsConstructor
@Slf4j
public class EmbeddingController {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    @PostMapping
    public void saveVectorStore(@RequestParam("messages")List<String> messages) {
        log.info("保存到向量数据库中，消息数据：{}", messages);
        List<Document> documentList = messages.stream().map(message -> Document.builder().text(message).build()).toList();
        vectorStore.add(documentList);
        log.info("保存到向量数据库成功, 数量：{}", messages.size());
    }

    @DeleteMapping
    public void deleteVectorStore(@RequestParam("ids")List<String> ids) {
        log.info("删除向量数据库, 消息数据：{}", ids);
        vectorStore.delete(ids);
    }

    @GetMapping
    public EmbeddingResponse embed(@RequestParam("message")String message) {
        log.info("文本转向量，消息数据：{}", message);
        return embeddingModel.embedForResponse(List.of(message));
    }

    @GetMapping("/search")
    public List<Document> search(@RequestParam("message")String message) {
        log.info("向量搜索，消息数据：{}", message);
        return vectorStore.similaritySearch(SearchRequest.builder().query(message).topK(5).build());
    }

    @GetMapping("/search/all")
    public List<Document> searchAll() {
        return vectorStore.similaritySearch(SearchRequest.builder().query("").topK(999).build());
    }
}
