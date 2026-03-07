package com.chatbi.backend.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatResponse {
    private String intent;
    private String generatedSql;
    private List<Map<String, Object>> queryResult;
    private String chartConfig; // JSON configuration for frontend charting library (e.g., ECharts)
    private String analysisReport;
    private String error;
}
