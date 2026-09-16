package com.tianji.aigc.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.mapper.ChatSessionMapper;
import com.tianji.aigc.enums.MessageTypeEnum;
import com.tianji.aigc.entity.ChatSession;
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
import org.springframework.stereotype.Service;

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
        ).map(message -> MessageVO.builder()
                .type(MessageTypeEnum.valueOf(message.getMessageType().name()))
                .content(message.getText())
                .build()).collect(Collectors.toList());
    }
}
