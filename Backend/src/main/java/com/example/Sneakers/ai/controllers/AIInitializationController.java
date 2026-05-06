package com.example.Sneakers.ai.controllers;

import com.example.Sneakers.ai.services.VectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController
@RequestMapping("${api.prefix}/ai/initialize")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "ai.enabled", havingValue = "true")
public class AIInitializationController {

    private final VectorSearchService vectorSearchService;

    @PostMapping("/index-all")
    public ResponseEntity<Map<String, Object>> indexAllData() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = auth != null ? auth.getName() : "Anonymous";
            
            if (vectorSearchService.isIndexing()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Indexing is already in progress.");
                return ResponseEntity.badRequest().body(response);
            }

            log.info("User '{}' triggered async indexing.", currentUsername);
            
            // Start Async Process
            vectorSearchService.indexAllDataAsync();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Async indexing started.");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to start indexing", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getIndexStatus() {
        try {
            long documentCount = vectorSearchService.getDocumentCount();
            boolean isIndexing = vectorSearchService.isIndexing();
            String currentStatus = vectorSearchService.getIndexingStatus();
            int progress = vectorSearchService.getIndexingProgress();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentCount", documentCount);
            response.put("status", isIndexing ? "indexing" : "idle"); // simple status for frontend badges
            response.put("isIndexing", isIndexing);
            response.put("currentAction", currentStatus); // Detailed status message
            response.put("progress", progress); // 0-100
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get index status", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    @DeleteMapping("/clear-index")
    public ResponseEntity<Map<String, Object>> clearIndex() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = auth != null ? auth.getName() : "Anonymous";
            log.warn("User '{}' requested to CLEAR all indexed documents.", currentUsername);

            vectorSearchService.clearAllDocuments();
            
            log.info("User '{}' successfully cleared all indexed documents.", currentUsername);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Successfully cleared all indexed documents");
            response.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to clear index", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
