package com.tianji.aigc.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.enums.ChatEventTypeEnum;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Flux;

import java.util.Map;

public abstract class AbstractAgent implements Agent{

    @Resource
    private ChatClient chatClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ChatMemory chatMemory;

    @Resource
    private ChatSessionService chatSessionService;

    private static final String GENERATE_STATUS_KEY = "GENERATE_STATUS_KEY";
    public static final ChatEventVO STOP_EVENT = ChatEventVO.builder()
            .eventType(ChatEventTypeEnum.STOP.getValue())
            .build();

    @Override
    public Flux<ChatEventVO> processStream(String question, String sessionId) {
        String requestId = generateRequestId();
        String conversationId = ChatService.getConversationId(sessionId);
        StringBuilder outputBuilder = new StringBuilder();
        Long userId = UserContext.getUser();
        chatSessionService.update(sessionId, question, userId);
        return getChatClientRequest(question, sessionId, requestId)
                .stream()
                .chatResponse()
                .doFirst(() -> stringRedisTemplate.opsForHash().put(GENERATE_STATUS_KEY, sessionId, "true"))
                .doOnCancel(() -> saveStopHistoryRecord(conversationId, outputBuilder.toString())) // 停止输出时，将当前输出保存到会话中
                .doOnError(throwable -> stringRedisTemplate.opsForHash().put(GENERATE_STATUS_KEY, sessionId, "false"))
                .doOnComplete(() -> stringRedisTemplate.opsForHash().put(GENERATE_STATUS_KEY, sessionId, "false"))
                .takeWhile(response -> "true".equals(stringRedisTemplate.opsForHash().get(GENERATE_STATUS_KEY, sessionId)))
                .map(chatResponse -> {
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
                    Map<String, Object> map = ToolResultHolder.get(requestId);
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

    private String generateRequestId() {
        return IdUtil.fastSimpleUUID();
    }

    private void saveStopHistoryRecord(String conversationId, String content) {
        chatMemory.add(conversationId, new AssistantMessage(content));
    }

    @Override
    public String process(String question, String sessionId) {
        String requestId = generateRequestId();
        Long userId = UserContext.getUser();
        chatSessionService.update(sessionId, question, userId);
        return getChatClientRequest(question, sessionId, requestId)
                .call()
                .content();
    }

    @NotNull
    private ChatClient.ChatClientRequestSpec getChatClientRequest(String question, String sessionId, String requestId) {
        return chatClient.prompt()
                .system(promptSystemSpec -> promptSystemSpec.text(this.systemMessage()).params(this.systemMessageParams()))
                .advisors(advisorSpec -> advisorSpec.advisors(this.advisors()).params(this.advisorParams(sessionId, requestId)))
                .tools(this.tools())
                .toolContext(this.toolContext(sessionId, requestId))
                .user(question);
    }

    @Override
    public void stop(String sessionId) {
        stringRedisTemplate.opsForHash().put(GENERATE_STATUS_KEY, sessionId, "false"); // 设置会话为停止状态
    }

    @Override
    public Map<String, Object> advisorParams(String sessionId, String requestId) {
        String conversationId = ChatService.getConversationId(sessionId);
        return Map.of(ChatMemory.CONVERSATION_ID, conversationId);
    }
}
