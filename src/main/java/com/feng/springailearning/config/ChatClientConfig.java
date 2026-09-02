package com.feng.springailearning.config;

import com.feng.springailearning.advisor.LoggingAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.redis.RedisChatMemoryRepository;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import redis.clients.jedis.RedisClient;

@Configuration
public class ChatClientConfig {

    @Bean
    public EmbeddingModel embeddingModel() {
        OpenAiEmbeddingOptions embeddingOptions = OpenAiEmbeddingOptions.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .model("text-embedding-v4")
                .dimensions(1024)
                .build();
        return OpenAiEmbeddingModel.builder()
                .options(embeddingOptions)
                .build();
    }

    @Bean
    public ChromaApi chromaApi() {
        return ChromaApi.builder()
                .baseUrl("http://localhost:8000")
                // .restClientBuilder()
                // .jsonMapper()
                .build();
    }

    /**
     * 知识库向量存储（RAG 专用）
     */
    @Bean
    public VectorStore knowledgeVectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName("knowledge-base")
                .initializeSchema(true)
                // .databaseName()
                // .tenantName()
                // .initializeImmediately()
                // .filterExpressionConverter()
                .build();
    }

    /**
     * 对话记忆专用向量存储（与知识库分开，避免污染）
     */
    @Bean
    public VectorStore memoryVectorStore(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName("chat-memory")
                .initializeSchema(true)
                // .databaseName()
                // .tenantName()
                // .initializeImmediately()
                // .filterExpressionConverter()
                .build();
    }

    /**
     * 生产推荐：使用 RetrievalAugmentationAdvisor（模块化 RAG）
     */
    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(
            @Qualifier("knowledgeVectorStore") VectorStore knowledgeVectorStore,
            ChatClient.Builder chatClientBuilder) {
        // 可选：查询改写（强烈推荐，提升召回质量）
        var rewriteTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder.build().mutate()) // 改写时温度要低
                .build();

        var documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(knowledgeVectorStore)
                .similarityThreshold(0.55)
                .topK(5)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .queryTransformers(rewriteTransformer)          // 查询改写
                .documentRetriever(documentRetriever)
                // .documentPostProcessors(...)               // 可加重排、去重等
                .order(Ordered.HIGHEST_PRECEDENCE + 200)        // 优先执行 RAG
                .build();
    }

    /**
     * 基于向量的长期对话记忆
     */
    @Bean
    public VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor(
            @Qualifier("memoryVectorStore") VectorStore memoryVectorStore) {
        return VectorStoreChatMemoryAdvisor.builder(memoryVectorStore)
                .defaultTopK(8)                                 // 每次检索相关历史条数
                .order(Ordered.HIGHEST_PRECEDENCE + 300)        // 记忆通常放在 RAG 之后
                .build();
    }

    @Bean
    public RedisChatMemoryRepository redisChatMemoryRepository(RedisClient jedisClient) {
        return RedisChatMemoryRepository.builder()
                .jedisClient(jedisClient)
                .build();
    }

    @Bean
    public ChatMemory chatMemory(RedisChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(10)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory,
                                 RetrievalAugmentationAdvisor retrievalAugmentationAdvisor,
                                 VectorStoreChatMemoryAdvisor vectorStoreChatMemoryAdvisor) {
        return builder
                .defaultSystem("你是一位资深Java架构师，回答要专业，有重点，能指出难点和易错点")
                .defaultAdvisors(
                        retrievalAugmentationAdvisor,
                        new LoggingAdvisor(),
                        MessageChatMemoryAdvisor.builder(chatMemory).order(Ordered.HIGHEST_PRECEDENCE + 100).build(),
                        vectorStoreChatMemoryAdvisor
                )
                .build();
    }
}
