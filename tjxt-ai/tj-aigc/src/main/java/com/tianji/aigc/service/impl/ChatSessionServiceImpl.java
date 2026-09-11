package com.tianji.aigc.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.aigc.config.SessionProperties;
import com.tianji.aigc.entity.ChatSessionMapper;
import com.tianji.aigc.mapper.ChatSession;
import com.tianji.aigc.service.ChatSessionService;
import com.tianji.aigc.vo.SessionVO;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession> implements ChatSessionService {

    private final SessionProperties sessionProperties;

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
}
