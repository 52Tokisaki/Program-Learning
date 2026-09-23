package com.tianji.aigc.agent;

import com.tianji.aigc.vo.ChatEventVO;
import reactor.core.publisher.Flux;

import java.util.Map;

public abstract class AbstractAgent implements Agent{
    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        return null;
    }

    @Override
    public String process(String question, String sessionId) {
        return "";
    }

    @Override
    public void stop(String sessionId) {

    }

    @Override
    public Map<String, Object> advisorParams(String sessionId, String requestId) {
        return Agent.super.advisorParams(sessionId, requestId);
    }
}
