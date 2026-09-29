package com.tianji.aigc.advisor;

import com.tianji.aigc.enums.AgentTypeEnum;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;

public class RecordOptimizationAdvisor implements BaseAdvisor {
    private final MyChatMemoryRepository myChatMemoryRepository;

    public RecordOptimizationAdvisor(MyChatMemoryRepository myChatMemoryRepository) {
        this.myChatMemoryRepository = myChatMemoryRepository;
    }

    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        return chatClientRequest;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        // 获取智能体响应结果
        String result = chatClientResponse.chatResponse().getResult().getOutput().getText();
        // 将结果转换为 AgentTypeEnum
        AgentTypeEnum agentTypeEnum = AgentTypeEnum.agentNameOf(result);
        if (null != agentTypeEnum) {
            // 若可以转换，则进行优化
            String conversationId = chatClientResponse.context().get(ChatMemory.CONVERSATION_ID).toString();
            myChatMemoryRepository.optimization(conversationId);
        }
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return BaseAdvisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER - 100;
    }
}
