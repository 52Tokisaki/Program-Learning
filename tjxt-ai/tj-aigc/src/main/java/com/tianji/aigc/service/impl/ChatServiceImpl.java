package com.tianji.aigc.service.impl;

import cn.hutool.core.date.DateUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
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
}
