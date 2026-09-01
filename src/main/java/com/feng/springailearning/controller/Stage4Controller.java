package com.feng.springailearning.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v4")
public class Stage4Controller {

    private final ChatClient chatClient;

    public Stage4Controller(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/chat")
    public String chat(
            @RequestParam String msg,
            @RequestParam(defaultValue = "default-session") String sessionId) {
        return chatClient.prompt()
                .user(msg)
                .advisors(advisorSpec -> {
                    advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId);
                })
                .call()
                .content();
    }
}
