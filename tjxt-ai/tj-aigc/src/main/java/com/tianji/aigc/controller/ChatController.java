package com.tianji.aigc.controller;

import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public Flux<ChatEventVO> chat(@RequestBody ChatDTO chatDTO) {
        return chatService.chat(chatDTO);
    }

    @PostMapping("/stop")
    public void stop(@RequestParam("sessionId") String sessionId) {
        chatService.stop(sessionId);
    }
}
