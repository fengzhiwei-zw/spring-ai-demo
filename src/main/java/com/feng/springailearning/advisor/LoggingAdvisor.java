package com.feng.springailearning.advisor;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;

public class LoggingAdvisor implements CallAdvisor {
    @Override
    public @NonNull ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        System.out.println("AI Request:");
        System.out.println(request.prompt());

        ChatClientResponse response = chain.nextCall(request);

        System.out.println("AI Response:");
        System.out.println(response.chatResponse());
        return response;
    }

    @Override
    public @NonNull String getName() {
        return "LoggingAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
