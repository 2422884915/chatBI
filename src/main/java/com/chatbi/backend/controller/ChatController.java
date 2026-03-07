package com.chatbi.backend.controller;

import com.chatbi.backend.agent.ChatBiAgent;
import com.chatbi.backend.model.ChatRequest;
import com.chatbi.backend.model.ChatResponse;
import com.chatbi.backend.service.WeeklyReportTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbi")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private ChatBiAgent chatBiAgent;

    @Autowired
    private WeeklyReportTemplateService weeklyReportTemplateService;

    @GetMapping("/query")
    public ResponseEntity<ChatResponse> handleQuery(@RequestParam("query") String query) {
        return doQuery(query);
    }

    @PostMapping("/query")
    public ResponseEntity<ChatResponse> handleQuery(@RequestBody ChatRequest request) {
        String query = request != null ? request.getQuery() : null;
        return doQuery(query);
    }

    @PostMapping(value = "/weekly-template/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadWeeklyTemplate(@RequestParam("file") MultipartFile file) {
        String saved = weeklyReportTemplateService.saveTemplate(file);
        return ResponseEntity.ok(buildTemplateResponse(saved));
    }

    @PostMapping(value = "/weekly-template", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> saveWeeklyTemplate(@RequestBody Map<String, String> payload) {
        String template = payload != null ? payload.get("template") : null;
        String saved = weeklyReportTemplateService.saveTemplate(template);
        return ResponseEntity.ok(buildTemplateResponse(saved));
    }

    @GetMapping("/weekly-template")
    public ResponseEntity<Map<String, Object>> getWeeklyTemplate() {
        Map<String, Object> result = new HashMap<>();
        String template = weeklyReportTemplateService.getTemplate().orElse("");
        result.put("hasTemplate", StringUtils.hasText(template));
        result.put("template", template);
        return ResponseEntity.ok(result);
    }

    private ResponseEntity<ChatResponse> doQuery(String query) {
        if (!StringUtils.hasText(query)) {
            ChatResponse badRequest = new ChatResponse();
            badRequest.setError("Query cannot be empty.");
            return ResponseEntity.badRequest().body(badRequest);
        }

        try {
            ChatResponse response = chatBiAgent.process(query);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            ChatResponse errorResponse = new ChatResponse();
            errorResponse.setError("Internal service error: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    private Map<String, Object> buildTemplateResponse(String template) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Weekly report template saved.");
        result.put("hasTemplate", true);
        result.put("templateLength", template.length());
        result.put("templatePreview", template.substring(0, Math.min(160, template.length())));
        return result;
    }
}
