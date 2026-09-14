package com.tianji.aigc.service.impl;

import cn.hutool.core.date.DateUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
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

    // 使用一个容器， 来存储会话和是否停止的映射关系
    // 1. 使用ConcurrentHashMap来存储会话和是否停止的映射关系，  2. 对于分布式环境， 可以使用Redis来存储会话和是否停止的映射关系
//    private final ConcurrentHashMap<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Flux<ChatEventVO> chat(ChatDTO chatDTO) {
        return chatClient
                .prompt()
                .system(promptSystem ->
                        promptSystem
                                .text(systemPromptConfig.getChatSystemMessage().get())
                                .params(Map.of("now", DateUtil.now())))
                .user(chatDTO.getQuestion())
                .stream()
                .chatResponse()
                .doFirst(() -> stringRedisTemplate.opsForValue().set(chatDTO.getSessionId(), "true"))
                .doOnError(throwable -> stringRedisTemplate.opsForValue().set(chatDTO.getSessionId(), "false"))
                .doOnComplete(() -> stringRedisTemplate.opsForValue().set(chatDTO.getSessionId(), "false"))
                .takeWhile(response -> "true".equals(stringRedisTemplate.opsForValue().get(chatDTO.getSessionId())))
//                .doFirst(() -> GENERATE_STATUS.put(chatDTO.getSessionId(), true))
//                .doOnError(throwable -> GENERATE_STATUS.remove(chatDTO.getSessionId()))
//                .doOnComplete(() -> GENERATE_STATUS.remove(chatDTO.getSessionId()))
//                .takeWhile(response -> GENERATE_STATUS.getOrDefault(chatDTO.getSessionId(), false)) // 用于控制流的结束
                .map(chatResponse -> {
                    String result = chatResponse.getResult().getOutput().getText();
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
}
