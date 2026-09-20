package com.tianji.aigc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.SystemPromptConfig;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.dto.ChatDTO;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
            .eventType(ChatEventTypeEnum.STOP.getValue())
            .build();
    private final ChatClient chatClient;
    private final SystemPromptConfig systemPromptConfig;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;

    // 使用一个容器， 来存储会话和是否停止的映射关系
    // 1. 使用ConcurrentHashMap来存储会话和是否停止的映射关系，  2. 对于分布式环境， 可以使用Redis来存储会话和是否停止的映射关系
//    private final ConcurrentHashMap<String, Boolean> GENERATE_STATUS = new ConcurrentHashMap<>();

    private static final String GENERATE_STATUS_KEY = "GENERATE_STATUS_KEY";

    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public Flux<ChatEventVO> chat(ChatDTO chatDTO) {
        StringBuilder outputBuilder = new StringBuilder();
        String conversationId = ChatService.getConversationId(chatDTO.getSessionId());
        String requestId = IdUtil.simpleUUID(); // 生成请求ID
        Long userId = UserContext.getUser();
        // 创建RAG增强
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.6d).topK(6).build()).build();
        return chatClient
                .prompt()
                .system(promptSystem ->
                        promptSystem
                                .text(systemPromptConfig.getChatSystemMessage().get())
                                .params(Map.of("now", DateUtil.now())))
                .advisors(advisor -> advisor
                        .advisors(questionAnswerAdvisor) // 设置RAG增强
                        .param(ChatMemory.CONVERSATION_ID, conversationId)) // 添加会话ID作为顾问参数
                .toolContext(Map.of(Constant.REQUEST_ID, requestId, Constant.USER_ID, userId)) // 将请求ID作为工具上下文传递
                .user(chatDTO.getQuestion())
                .stream()
                .chatResponse()
                .doFirst(() -> stringRedisTemplate.opsForHash().put(GENERATE_STATUS_KEY, chatDTO.getSessionId(), "true"))
                .doOnCancel(() -> saveStopHistoryRecord(conversationId, outputBuilder.toString())) // 停止输出时，将当前输出保存到会话中
                .doOnError(throwable -> stringRedisTemplate.opsForHash().put(GENERATE_STATUS_KEY, chatDTO.getSessionId(), "false"))
                .doOnComplete(() -> stringRedisTemplate.opsForHash().put(GENERATE_STATUS_KEY, chatDTO.getSessionId(), "false"))
                .takeWhile(response -> "true".equals(stringRedisTemplate.opsForHash().get(GENERATE_STATUS_KEY, chatDTO.getSessionId())))
//                .doFirst(() -> GENERATE_STATUS.put(chatDTO.getSessionId(), true))
//                .doOnError(throwable -> GENERATE_STATUS.remove(chatDTO.getSessionId()))
//                .doOnComplete(() -> GENERATE_STATUS.remove(chatDTO.getSessionId()))
//                .takeWhile(response -> GENERATE_STATUS.getOrDefault(chatDTO.getSessionId(), false)) // 用于控制流的结束
                .map(chatResponse -> {
                    // 对于响应结果进行处理，如果是最后一条数据，就把此次消息id放到内存中
                    // 主要用于存储消息数据到 redis中，可以根据消息di获取的请求id，再通过请求id就可以获取到参数列表了
                    // 从而解决，在历史聊天记录中没有外参数的问题
                    String finishReason = chatResponse.getResult().getMetadata().getFinishReason(); // 获取结束原因
                    if (StrUtil.equals(Constant.STOP, finishReason)) {
                        String messageId = chatResponse.getMetadata().getId();
                        ToolResultHolder.put(messageId, Constant.REQUEST_ID, requestId); // 将消息ID和请求ID放入工具结果持有者中
                    }
                    String result = chatResponse.getResult().getOutput().getText();
                    outputBuilder.append(result); // 将结果追加到输出构建器中
                    return ChatEventVO.builder()
                            .eventData(result)
                            .eventType(ChatEventTypeEnum.DATA.getValue())
                            .build();
                })
                .concatWith(Flux.defer(() -> {
                    log.info("concatWith at {}, map={}", System.currentTimeMillis(), ToolResultHolder.get(requestId));
                    Map<String, Object> map = ToolResultHolder.get(requestId);
                    log.info("ChatServiceImpl: map identity={}, classLoader={}, keys={}",
                            ToolResultHolder.mapIdentity(),
                            ToolResultHolder.holderClassLoader(),
                            ToolResultHolder.keys());
                    if (CollUtil.isNotEmpty(map)) {
                        ToolResultHolder.remove(requestId); // 清除参数列表
                        return Flux.just(ChatEventVO.builder()
                                        .eventType(ChatEventTypeEnum.PARAM.getValue())
                                        .eventData(map)
                                        .build(), STOP_EVENT);
                    }
                    return Flux.just(STOP_EVENT);
                }));
    }

    @Override
    public void stop(String sessionId) {
//        GENERATE_STATUS.put(sessionId, false); // 设置会话为停止状态
        stringRedisTemplate.opsForHash().put(GENERATE_STATUS_KEY, sessionId, "false"); // 设置会话为停止状态
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
