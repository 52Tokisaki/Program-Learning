package com.tianji.aigc.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.entity.ChatSession;
import com.tianji.aigc.memory.MyAssistantMessage;
import com.tianji.aigc.service.ChatService;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.MessageVO;
import com.tianji.aigc.vo.SessionVO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    private final SessionProperties sessionProperties;

    private final ChatMemory chatMemory;

    @Override
    public SessionVO createSession(Integer num) {
        SessionVO sessionVO = BeanUtils.toBean(sessionProperties, SessionVO.class);
        // 设置示例列表，随机获取num个
        sessionVO.setExamples(RandomUtil.randomEleList(sessionVO.getExamples(), num));
        // 设置会话ID
        sessionVO.setSessionId(IdUtil.fastSimpleUUID());

        // 保存会话信息
        ChatSession chatSession = ChatSession.builder()
                .sessionId(sessionVO.getSessionId())
                .userId(UserContext.getUser())
                .build();
        super.save(chatSession);
        return sessionVO;
    }

    @Override
    public List<SessionVO.Example> getHotSession(Integer n) {
        return RandomUtil.randomEleList(sessionProperties.getExamples(), n);
    }

    @Override
    public List<MessageVO> queryBySessionId(String sessionId) {
        String conversationId = ChatService.getConversationId(sessionId);
        List<Message> messageList = chatMemory.get(conversationId);
        return messageList.stream().filter(
                message -> message.getMessageType().equals(MessageType.ASSISTANT) || message.getMessageType().equals(MessageType.USER)
        ).map(message -> {
            if (message instanceof MyAssistantMessage) {
                return MessageVO.builder()
                        .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                        .content(message.getText())
                        .params(((MyAssistantMessage) message).getParams())
                        .build();
            }
            return MessageVO.builder()
                    .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                    .content(message.getText())
                    .build();
        }).toList();
    }

    @Async // 异步更新会话信息
    @Override
    public void update(String sessionId, String title, Long userId) {
        List<ChatSession> chatSessionList = super.lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .list();
        // 如果会话不存在，则返回
        if (CollUtil.isEmpty(chatSessionList)) {
            return;
        }
        // 取第一个会话
        ChatSession chatSession = chatSessionList.get(0);
        // 如果会话标题不为空且传入的标题为空，则更新会话标题
        if (StrUtil.isEmpty(chatSession.getTitle()) && !StrUtil.isEmpty(title)) {
            chatSession.setTitle(StrUtil.sub(title, 0, 100));
        }
        chatSession.setUpdateTime(LocalDateTime.now());
        super.updateById(chatSession);
    }
}
