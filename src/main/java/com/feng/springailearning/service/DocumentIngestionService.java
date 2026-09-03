package com.feng.springailearning.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DocumentIngestionService {

    private final VectorStore knowledgeVectorStore;  // 你的知识库 VectorStore

    public DocumentIngestionService(VectorStore knowledgeVectorStore) {
        this.knowledgeVectorStore = knowledgeVectorStore;
    }

    /**
     * 从本地文件/classpath 加载并入库
     */
    public void ingestFile(Resource resource) {
        // 1. 读取文档（支持 PDF、Word、TXT、MD 等多种格式）
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();

        // 2. 分块（非常重要！）
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)          // 每块目标 token 数
                .withMinChunkSizeChars(350)
                .withMinChunkLengthToEmbed(5)
                .withMaxNumChunks(10000)
                .withKeepSeparator(true)
                .build();

        List<Document> chunks = splitter.apply(documents);

        // 3. 可选：添加元数据
        chunks.forEach(doc -> {
            doc.getMetadata().put("filename", resource.getFilename() != null ? resource.getFilename() : "");
            doc.getMetadata().put("ingest_time", LocalDateTime.now().toString());
        });

        // 4. 写入向量库（自动调用 EmbeddingModel）
        knowledgeVectorStore.add(chunks);

        System.out.println("成功入库 " + chunks.size() + " 个文档块");
    }

    /**
     * 从 classpath 加载示例
     */
    public void ingestFromClasspath(String classpathLocation) {
        Resource resource = new ClassPathResource(classpathLocation);
        ingestFile(resource);
    }

    /**
     * 从本地文件系统加载
     */
    public void ingestFromFileSystem(String filePath) {
        Resource resource = new FileSystemResource(filePath);
        ingestFile(resource);
    }
}