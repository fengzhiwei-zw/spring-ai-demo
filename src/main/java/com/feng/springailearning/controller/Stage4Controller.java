package com.feng.springailearning.controller;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v4")
public class Stage4Controller {

    private final ChatModel chatModel;

    private final ChatMemory chatMemory;

    public Stage4Controller(ChatModel chatModel, ChatMemory chatMemory) {
        this.chatModel = chatModel;
        this.chatMemory = chatMemory;
    }

    public record UserProfile(
            String name,
            Integer age,
            String city,
            String job,
            List<String> skills
    ) {
    }

    @GetMapping("/model")
    public String model(
            @RequestParam String msg,
            @RequestParam(defaultValue = "default-session") String sessionId) {
        SystemMessage systemMessage = new SystemMessage.Builder()
                .text("你是一名Java专家，回答问题要专业、简洁。")
                .build();
        UserMessage userMessage = new UserMessage.Builder()
                .text(msg)
                .build();
        chatMemory.add(sessionId, userMessage);
        Prompt prompt = new Prompt.Builder()
                .messages(chatMemory.get(sessionId))
                .build();
        prompt.getInstructions().add(systemMessage);
        ChatResponse chatResponse = chatModel.call(prompt);

        String response = Optional.of(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AbstractMessage::getText)
                .orElse("");

        if (chatResponse.getResult() != null) {
            chatMemory.add(sessionId, chatResponse.getResult().getOutput());
        }
        return chatResponse.toString();
    }

    @GetMapping("/format")
    public UserProfile format(
            @RequestParam String msg,
            @RequestParam(defaultValue = "default-session") String sessionId) {
        SystemMessage systemMessage = new SystemMessage.Builder()
                .text("你是一名Java专家，回答问题要专业、简洁。")
                .build();

        BeanOutputConverter<UserProfile> beanOutputConverter = new BeanOutputConverter<>(UserProfile.class);
        String format = beanOutputConverter.getFormat();
        String us = """
                请分析下面的信息：
                
                %s
                
                %s
                """.formatted(msg, format);

        UserMessage userMessage = new UserMessage.Builder()
                .text(us)
                .build();
        chatMemory.add(sessionId, userMessage);
        Prompt prompt = new Prompt.Builder()
                .messages(chatMemory.get(sessionId))
                .build();
        prompt.getInstructions().add(systemMessage);

        ChatResponse chatResponse = chatModel.call(prompt);

        String response = Optional.of(chatResponse)
                .map(ChatResponse::getResult)
                .map(Generation::getOutput)
                .map(AbstractMessage::getText)
                .orElse("");

        if (chatResponse.getResult() != null) {
            chatMemory.add(sessionId, chatResponse.getResult().getOutput());
        }
        return beanOutputConverter.convert(response);
    }
}
