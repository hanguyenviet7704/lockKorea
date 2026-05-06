package com.example.Sneakers.ai.listeners;

import com.example.Sneakers.ai.services.VectorSearchService;
import com.example.Sneakers.models.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@Slf4j
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class ProductEventListener {

    private VectorSearchService vectorSearchService;

    @Autowired
    public void setVectorSearchService(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductSave(ProductSaveEvent event) {
        // Run indexing asynchronously to not block the response
        try {
            Product product = event.getProduct();
            Long productId = product.getId();
            log.info("Scheduling async indexing for product after save: {} (ID: {})", product.getName(), productId);
            // Pass productId instead of product entity to avoid detached entity issues
            vectorSearchService.updateProductIndexAsync(productId);
        } catch (Exception e) {
            log.error("Failed to schedule indexing for product: {}", event.getProduct().getName(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleProductDelete(ProductDeleteEvent event) {
        try {
            log.info("Removing product from index: {}", event.getProductId());
            vectorSearchService.deleteProductFromIndex(event.getProductId());
        } catch (Exception e) {
            log.error("Failed to remove product from index: {}", event.getProductId(), e);
        }
    }

    // Event classes
    public static class ProductSaveEvent {
        private final Product product;

        public ProductSaveEvent(Product product) {
            this.product = product;
        }

        public Product getProduct() {
            return product;
        }
    }

    public static class ProductDeleteEvent {
        private final Long productId;

        public ProductDeleteEvent(Long productId) {
            this.productId = productId;
        }

        public Long getProductId() {
            return productId;
        }
    }
}