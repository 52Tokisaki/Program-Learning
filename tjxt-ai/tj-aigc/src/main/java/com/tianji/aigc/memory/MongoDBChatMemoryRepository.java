package com.tianji.aigc.memory;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;

import com.tianji.aigc.memory.mongodb.ChatRecord;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class MongoDBChatMemoryRepository implements ChatMemoryRepository {

    @Resource
    private MongoTemplate mongoTemplate;

    /**
     * 查询所有会话ID
     */
    @Override
    public List<String> findConversationIds() {

        List<ChatRecord> chatRecordList = mongoTemplate.findAll(ChatRecord.class);
        return chatRecordList.stream().map(ChatRecord::getConversationId).distinct().toList();
    }

    /**
     * 根据会话ID查询消息
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        Query query = Query.query(Criteria.where("conversationId").is(conversationId));
        ChatRecord chatRecord = mongoTemplate.findOne(query, ChatRecord.class);
        if (chatRecord == null) {
            return List.of();
        }
        return chatRecord.getMessages().stream().map(message -> MessageUtil.toMessage(message)).collect(Collectors.toList());
    }

    /**
     * 保存消息（全量覆盖：存在则更新，不存在则插入）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(String conversationId, List<Message> messages) {
        deleteByConversationId(conversationId); // 删除会话ID对应的消息
        ChatRecord chatRecord = ChatRecord
                .builder()
                .conversationId(conversationId)
                .messages(messages.stream().map(message -> MessageUtil.toJson(message)).collect(Collectors.toList()))
                .build();
        mongoTemplate.save(chatRecord);
    }

    /**
     * 删除会话ID对应的消息
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        mongoTemplate.remove(Query.query(Criteria.where("conversationId").is(conversationId)), ChatRecord.class);
    }
}