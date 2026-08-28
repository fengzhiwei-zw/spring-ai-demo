package com.feng.springaidemo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem("你是一位资深Java架构师，回答要专业，有重点，能指出难点和易错点")
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
