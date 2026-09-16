package com.tianji.aigc.memory;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.entity.ChatRecord;
import com.tianji.aigc.service.ChatRecordService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public class MysqlChatMemoryRepository implements ChatMemoryRepository {

    @Resource
    private ChatRecordService chatRecordService;
    /**
     * 查询所有会话ID
     */
    @Override
    public List<String> findConversationIds() {
        List<ChatRecord> chatRecordList = chatRecordService.lambdaQuery()
                .select(ChatRecord::getConversationId)
                .list();
        return chatRecordList.stream().map(ChatRecord::getConversationId).distinct().toList();
    }

    /**
     * 根据会话ID查询消息
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        List<ChatRecord> chatRecordList = chatRecordService.lambdaQuery()
                .eq(ChatRecord::getConversationId, conversationId)
                .orderByAsc(ChatRecord::getCreateTime)
                .list();
        return chatRecordList.stream().map(chatRecord -> MessageUtil.toMessage(chatRecord.getData())).toList();
    }

    /**
     * 保存消息（全量覆盖：存在则更新，不存在则插入）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(String conversationId, List<Message> messages) {
        deleteByConversationId(conversationId); // 删除会话ID对应的消息
        Long userId = Convert.toLong(StrUtil.subBefore(conversationId, "_", false));
        List<ChatRecord> chatRecordList = messages.stream().map(message ->
                ChatRecord.builder()
                        .creater(userId)
                        .updater(userId)
                        .createTime(LocalDateTime.now())
                        .updateTime(LocalDateTime.now())
                        .data(MessageUtil.toJson(message))
                        .conversationId(conversationId).build()).toList();
        chatRecordService.saveBatch(chatRecordList);
    }

    /**
     * 删除会话ID对应的消息
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        chatRecordService.lambdaUpdate()
                .eq(ChatRecord::getConversationId, conversationId)
                .remove();
    }
}