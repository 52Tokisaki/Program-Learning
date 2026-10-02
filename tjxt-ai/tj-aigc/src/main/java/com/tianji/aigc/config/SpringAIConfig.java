package com.tianji.aigc.config;

import com.tianji.aigc.advisor.RecordOptimizationAdvisor;
import com.tianji.aigc.memory.MongoDBChatMemoryRepository;
import com.tianji.aigc.memory.MyChatMemoryRepository;
import com.tianji.aigc.memory.MysqlChatMemoryRepository;
import com.tianji.aigc.memory.RedisChatMemoryRepository;
import com.tianji.aigc.tools.CourseTools;
import com.tianji.aigc.tools.OrderTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAIConfig {

    @Value("${tj.ai.memory.max:100}")
    private Integer maxMessages;

    /**
     * 配置 ChatClient
     */
    @Bean
    public ChatClient chatClient(@Qualifier("dashscopeChatModel") ChatModel dashScopeChatModel,
                                 Advisor loggerAdvisor,  // 日志记录器
                                 Advisor messageChatMemoryAdvisor, // 会话存储
                                 Advisor recordOptimizationAdvisor, // 会话优化
                                 CourseTools courseTools, // 课程工具
                                 OrderTools orderTools // 订单工具
    ) {
        return ChatClient.builder(dashScopeChatModel)
                .defaultAdvisors(loggerAdvisor, messageChatMemoryAdvisor, recordOptimizationAdvisor) //添加 Advisor 功能增强
//                .defaultAdvisors(loggerAdvisor) //添加 Advisor 功能增强，一定要注册会话记忆，否则无法记录会话
//                .defaultTools(courseTools, orderTools) // 添加课程工具
                .build();
    }

    @Bean
    public ChatClient openAiChatClient(@Qualifier("openAiChatModel") ChatModel openAiChatModel,
                                       Advisor loggerAdvisor  // 日志记录器
    ) {
        return ChatClient.builder(openAiChatModel)
                .defaultAdvisors(loggerAdvisor)
                .build();
    }

    /**
     * 日志记录器
     */
    @Bean
    public Advisor loggerAdvisor() {
        return new SimpleLoggerAdvisor();
    }

    /**
     * 配置 ChatMemoryRepository
     */
    @Bean
    @ConditionalOnProperty(prefix = "tj.ai.memory", value = "type", havingValue = "Redis", matchIfMissing = true)
    public RedisChatMemoryRepository redisChatMemoryRepository() {
        return new RedisChatMemoryRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "tj.ai.memory", value = "type", havingValue = "MYSQL")
    public MysqlChatMemoryRepository mysqlChatMemoryRepository() {
        return new MysqlChatMemoryRepository();
    }

    @Bean
    @ConditionalOnProperty(prefix = "tj.ai.memory", value = "type", havingValue = "MongoDB")
    public MongoDBChatMemoryRepository mongoDBChatMemoryRepository() {
        return new MongoDBChatMemoryRepository();
    }

    @Bean
    public ChatMemory chatMemory(ChatMemoryRepository chatMemoryRepository) {
        // 基于ChatMemory的会话窗口内存
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

    /**
     * 配置消息聊天内存顾问
     */
    @Bean
    public Advisor messageChatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    @Bean
    public Advisor recordOptimizationAdvisor(MyChatMemoryRepository myChatMemoryRepository) {
        return new RecordOptimizationAdvisor(myChatMemoryRepository);
    }

}
