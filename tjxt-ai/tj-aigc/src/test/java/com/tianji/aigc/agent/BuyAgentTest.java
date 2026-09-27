package com.tianji.aigc.agent;

import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
public class BuyAgentTest {
    @Resource
    private BuyAgent buyAgent;

    @Test
    public void testBuyAgent() throws InterruptedException {
        String question = "下单购买，课程id为：1880533253575225346";
        String sessionId = "123";
        UserContext.setUser(123L);
        Flux<ChatEventVO> flux = buyAgent.processStream(question, sessionId);
        flux.subscribe(System.out::println);
        Thread.sleep(10000);
    }
}
