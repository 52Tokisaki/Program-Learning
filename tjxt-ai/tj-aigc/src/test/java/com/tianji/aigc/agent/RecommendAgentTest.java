package com.tianji.aigc.agent;


import com.tianji.aigc.vo.ChatEventVO;
import com.tianji.common.utils.UserContext;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;

@SpringBootTest
public class RecommendAgentTest {
    @Resource
    private RecommendAgent recommendAgent;

    @Test
    public void testRecommend() throws InterruptedException {
        String question = "推荐课程，20岁，本科，对Java感兴趣，零基础";
        String sessionId = "1234567890";
        UserContext.setUser(123L);
        Flux<ChatEventVO> flux = recommendAgent.processStream(question, sessionId);
        flux.subscribe(System.out::println);
        // 阻塞主线程，防止主线程结束，子线程终止
        Thread.sleep(100000);
    }
}
