package com.tianji.aigc.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.tianji.aigc.agent.Agent;
import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
@Slf4j
@ConditionalOnProperty(prefix = "tj.ai", name = "chat-type", havingValue = "ROUTE")
public class AgentServiceImpl implements ChatService {
    @Override
    public Flux<ChatEventVO> chat(ChatDTO chatDTO) {
        String question = chatDTO.getQuestion();
        String sessionId = chatDTO.getSessionId();

        Agent routeAgent = findAgentByType(AgentTypeEnum.ROUTE); // 获取路由智能体
        AgentTypeEnum routeResult = AgentTypeEnum.agentNameOf(routeAgent.process(question, sessionId));
        Agent agent = findAgentByType(routeResult); // 获取对应类型的智能体
        if (null == agent) {
            return Flux.just(
                    ChatEventVO.builder()
                            .eventData(routeResult)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build()
            );
        }
        return agent.processStream(question, sessionId);
    }

    /**
     * 根据代理类型查找对应的Agent实例
     *
     * @param agentType 要查找的代理类型
     * @return 与给定类型匹配的Agent实例，如果未找到或类型为null则返回null
     */
    private Agent findAgentByType(AgentTypeEnum agentType) {
        if (null == agentType) {
            return null;
        }
        Map<String, Agent> agentMap = SpringUtil.getBeansOfType(Agent.class); // 找到所有的Agent实例

        for (Agent agent : agentMap.values()) {
            if (agentType == agent.getAgentType()) {
                return agent;
            }
        }
        return null;
    }

    @Override
    public void stop(String sessionId) {
        Agent routeAgent = findAgentByType(AgentTypeEnum.ROUTE); // 获取路由智能体
        routeAgent.stop(sessionId);
    }
}
