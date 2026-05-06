package com.example.Sneakers.ai.services;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@Slf4j
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class ChromaStoreProvider {

    @Value("${chroma.collection.name:sneakers-collection}")
    private String collectionName;

    @Value("${chroma.base.url:http://localhost:8000}")
    private String chromaBaseUrl;

    private final AtomicReference<ChromaEmbeddingStore> storeRef = new AtomicReference<>();

    public ChromaEmbeddingStore get() {
        ChromaEmbeddingStore current = storeRef.get();
        if (current != null) {
            return current;
        }
        ChromaEmbeddingStore created = createStore();
        storeRef.compareAndSet(null, created);
        return storeRef.get();
    }

    public void reset() {
        storeRef.set(createStore());
    }

    public void add(Embedding embedding, TextSegment segment) {
        try {
            get().add(embedding, segment);
        } catch (RuntimeException e) {
            if (isInvalidCollection(e)) {
                log.warn("Chroma collection missing. Recreating store and retrying add...");
                reset();
                get().add(embedding, segment);
                return;
            }
            throw e;
        }
    }

    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        try {
            return get().search(request);
        } catch (RuntimeException e) {
            if (isInvalidCollection(e)) {
                log.warn("Chroma collection missing. Recreating store and retrying search...");
                reset();
                return get().search(request);
            }
            throw e;
        }
    }

    private ChromaEmbeddingStore createStore() {
        log.info("Initializing Chroma embedding store with collection: {} @ {}", collectionName, chromaBaseUrl);
        return ChromaEmbeddingStore.builder()
                .baseUrl(chromaBaseUrl)
                .collectionName(collectionName)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    private static boolean isInvalidCollection(Throwable e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        return message.contains("InvalidCollection") || message.contains("does not exist");
    }
}
