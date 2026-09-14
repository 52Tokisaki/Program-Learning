package com.tianji.aigc.memory;

import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

public class RedisChatMemoryRepository implements ChatMemoryRepository {
    private static final String DEFAULT_PREFIX = "CHAT:";

    private final String prefix;


    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public RedisChatMemoryRepository() {
        this.prefix = DEFAULT_PREFIX;
    }

    public RedisChatMemoryRepository(String prefix) {
        this.prefix = prefix;
    }

    // 查询所有会话ID
    @Override
    public List<String> findConversationIds() {
        Set<String> keys = stringRedisTemplate.keys(prefix + "*");
        if (null == keys) return List.of();
        return keys.stream().map(key -> key.replace(prefix, "")).toList();
    }

    // 根据会话ID查询消息
    @Override
    public List<Message> findByConversationId(String conversationId) {
        return List.of();
    }

    // 保存消息
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String redisKey = getKey(conversationId);
        BoundListOperations<String, String> listOps = stringRedisTemplate.boundListOps(redisKey); // 根据会话ID获取绑定的列表操作对象

        deleteByConversationId(conversationId); // 由于message时全量的，所以先删除再保存

        messages.forEach(message -> listOps.rightPush(JSONUtil.toJsonStr(message)));
    }

    // 删除会话ID对应的消息
    @Override
    public void deleteByConversationId(String conversationId) {
        String redisKey = getKey(conversationId);
        stringRedisTemplate.delete(redisKey);
    }

    private String getKey(String conversationId) {
        return prefix + conversationId;
    }
}
