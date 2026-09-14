package com.tianji.aigc.service;

import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.vo.ChatEventVO;
import reactor.core.publisher.Flux;

public interface ChatService {
    Flux<ChatEventVO> chat(ChatDTO chatDTO);

    void stop(String sessionId);
}
