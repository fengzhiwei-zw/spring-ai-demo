package com.feng.springailearning.controller;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v3")
public class Stage3Controller {

    private final ChatClient chatClient;

    public Stage3Controller(ChatClient builder) {
        this.chatClient = builder;
    }

    /**
     * 1. 外部模板文件调用
     */
    @GetMapping("/review")
    public String reviewCode(@RequestParam String code, @RequestParam(defaultValue = "default-session") String sessionId) {
        // 1. 加载模板文件
        PromptTemplate template = new PromptTemplate(new ClassPathResource("prompts/code-review.st"));
        // 2. 注入变量生成 Prompt 对象
        Prompt prompt = template.create(Map.of(
                "language", "java",
                "code", code)
        );
        // 3. 调用模型
        return chatClient.prompt(prompt)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call().content();
    }

    // 2. 结构化输出 - 单对象
    public record Book(String title, String author, int year, String reason) {
    }

    @GetMapping("/book")
    public Book getBook(@RequestParam String topic, @RequestParam(defaultValue = "default-session") String sessionId) {
        return chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec.text("推荐一本关于{topic}的书")
                        .param("topic", topic))
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .entity(Book.class);
    }

    // 3. 结构化输出 - 列表泛型
    @GetMapping("/books")
    public List<Book> getBooks(@RequestParam String topic, @RequestParam int count,
                               @RequestParam(defaultValue = "default-session") String sessionId) {
        return chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec.text("推荐{count}本关于{topic}的书")
                        .params(Map.of("topic", topic, "count", count)))
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .entity(new ParameterizedTypeReference<>() {
                });
    }

    // 4. 结构化输出 + 自动纠错校验
    public record StockInfo(
            @NotBlank String ticker,
            @Min(0) double price
    ) {
    }

    @GetMapping("/stock")
    public StockInfo getStock(@RequestParam String text, @RequestParam(defaultValue = "default-session") String sessionId) {
        return chatClient.prompt()
                .user(promptUserSpec -> promptUserSpec.text("提取文本中的股票代码和价格：{text}")
                        .param("text", text))
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .entity(StockInfo.class);
    }

    // 5. 多模态：图片理解
    @GetMapping("/image")
    public String analyzeImage(@RequestParam(defaultValue = "default-session") String sessionId) {
        return chatClient.prompt()
                .user(u -> u.text("描述这张图片")
                        .media(MimeTypeUtils.IMAGE_PNG, new ClassPathResource("static/demo.png")))
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
    }

    // 6. 添加 redis-memory
    @GetMapping("/chat")
    public String chat(
            @RequestParam String msg,
            @RequestParam(defaultValue = "default-session") String sessionId) {
        return chatClient.prompt()
                .user(msg)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
    }
}
