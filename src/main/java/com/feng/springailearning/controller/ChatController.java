package com.feng.springailearning.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient builder) {
        this.chatClient = builder;
    }

    /**
     * 1. 基础同步调用
     */
    @GetMapping("")
    public String chat(@RequestParam String msg, @RequestParam(defaultValue = "default-session") String sessionId) {
        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(msg)
                .call()
                .content();
    }

    /**
     * 2. 同步调用 - 返回详细信息
     */
    @GetMapping("/detail")
    public Map<String, Object> chatDetail(@RequestParam String msg, @RequestParam(defaultValue = "default-session") String sessionId) {
        ChatResponse response = chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(msg)
                .call()
                .chatResponse();

        // 提取各种元信息
        assert response != null;
        Generation generation = response.getResult();
        assert generation != null;
        String text = generation.getOutput().getText();

        ChatResponseMetadata metadata = response.getMetadata();
        Usage usage = metadata.getUsage();

        assert text != null;
        return Map.of(
                "content", text,
                "model", metadata.getModel(),
                "promptTokens", usage.getPromptTokens(),
                "completionTokens", usage.getCompletionTokens(),
                "totalTokens", usage.getTotalTokens()
        );
    }

    /**
     * 3. 流式调用 - SSE 打字机效果
     * curl -N --get \
     *      --data-urlencode "msg=用一句话介绍你自己" \
     *      <a href="http://localhost:8080/chat/stream">...</a>
     */
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public Flux<String> chatStream(@RequestParam String msg, @RequestParam(defaultValue = "default-session") String sessionId) {
        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(msg)
                .stream()         // 触发流式调用
                .content();       // 返回 Flux<String>，每个元素是一小段文本
    }

    /**
     * 4. 带角色设定的调用
     */
    @GetMapping("/role")
    public String chatWithRole(
            @RequestParam String msg,
            @RequestParam(defaultValue = "资深Java架构师") String role, @RequestParam(defaultValue = "default-session") String sessionId) {
        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .system(sys -> {
                    sys.text("你是一位{role}，回答要专业且简洁");
                    sys.param("role", role);
                })
                .user(msg)
                .call()
                .content();
    }

    /**
     * 5. 带参数调优的调用
     */
    @GetMapping("/chat/creative")
    public String chatCreative(@RequestParam String msg, @RequestParam(defaultValue = "default-session") String sessionId) {
        return chatClient.prompt()
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(msg)
                .options(OpenAiChatOptions.builder()
                        .temperature(0.9)
                        .maxTokens(800))
                .call()
                .content();
    }
}