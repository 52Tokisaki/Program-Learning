package com.tianji.aigc.memory.mongodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_record")
public class ChatRecord {
    @Id
    private ObjectId id;
    private String conversationId;
    private List<String> messages; // 存 MessageUtil.toJson 后的字符串
}