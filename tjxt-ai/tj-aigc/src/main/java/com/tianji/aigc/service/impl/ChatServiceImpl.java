package com.tianji.aigc.service.impl;

import cn.hutool.core.date.DateUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    private final ChatMemory chatMemory;

    // 使用一个容器， 来存储会话和是否停止的映射关系
    // 1. 使用ConcurrentHashMap来存储会话和是否停止的映射关系，  2. 对于分布式环境， 可以使用Redis来存储会话和是否停止的映射关系
//    private final ConcurrentHashMap<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Flux<ChatEventVO> chat(ChatDTO chatDTO) {
        StringBuilder outputBuilder = new StringBuilder();
        String conversationId = ChatService.getConversationId(chatDTO.getSessionId());
        return chatClient
                .prompt()
                .system(promptSystem ->
                        promptSystem
                                .text(systemPromptConfig.getChatSystemMessage().get())
                                .params(Map.of("now", DateUtil.now())))
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId)) // 添加会话ID作为顾问参数
                .user(chatDTO.getQuestion())
                .stream()
                .chatResponse()
                .doFirst(() -> stringRedisTemplate.opsForValue().set(chatDTO.getSessionId(), "true"))
                .doOnCancel(() -> saveStopHistoryRecord(conversationId, outputBuilder.toString())) // 停止输出时，将当前输出保存到会话中
                .doOnError(throwable -> stringRedisTemplate.opsForValue().set(chatDTO.getSessionId(), "false"))
                .doOnComplete(() -> stringRedisTemplate.opsForValue().set(chatDTO.getSessionId(), "false"))
                .takeWhile(response -> "true".equals(stringRedisTemplate.opsForValue().get(chatDTO.getSessionId())))
//                .doFirst(() -> GENERATE_STATUS.put(chatDTO.getSessionId(), true))
//                .doOnError(throwable -> GENERATE_STATUS.remove(chatDTO.getSessionId()))
//                .doOnComplete(() -> GENERATE_STATUS.remove(chatDTO.getSessionId()))
//                .takeWhile(response -> GENERATE_STATUS.getOrDefault(chatDTO.getSessionId(), false)) // 用于控制流的结束
                .map(chatResponse -> {
                    String result = chatResponse.getResult().getOutput().getText();
                    outputBuilder.append(result); // 将结果追加到输出构建器中
                    return ChatEventVO.builder()
                            .eventData(result)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.just(ChatEventVO.builder()
                        .eventType(ChatEventTypeEnum.STOP.getValue())
                        .build()));
    }

    @Override
    public void stop(String sessionId) {
//        GENERATE_STATUS.put(sessionId, false); // 设置会话为停止状态
        stringRedisTemplate.opsForValue().set(sessionId, "false"); // 设置会话为停止状态
    }

    /**
     * 保存停止输出的记录
     *
     * @param conversationId 会话id
     * @param content        大模型输出的内容
     */
    private void saveStopHistoryRecord(String conversationId, String content) {
        chatMemory.add(conversationId, new AssistantMessage(content));
    }
}
