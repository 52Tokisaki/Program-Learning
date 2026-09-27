package com.tianji.aigc.agent;

import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
public class KnowledgeAgentTest {
    @Resource
    private KnowledgeAgent knowledgeAgent;

    @Test
    public void testKnowledgeAgent() throws InterruptedException {
        String question = "请简单介绍一下Kafka";
        String sessionId = "123";
        UserContext.setUser(123L);
        Flux<ChatEventVO> flux = knowledgeAgent.processStream(question, sessionId);
        flux.subscribe(System.out::println);

        Thread.sleep(100000);
    }
}
