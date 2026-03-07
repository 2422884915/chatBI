package com.chatbi.backend.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * ChartAgent (ReactAgent implementation)
 */
@Component
public class ChartAgent {

    @Autowired
    private ChatModel chatModel;

    public String generateChartConfig(List<Map<String, Object>> data, String predictionAnalysis) {
        if (data == null || data.isEmpty()) {
            return "{}";
        }

        ReactAgent agent = ReactAgent.builder()
                .name("chart_agent")
                .model(chatModel)
                .saver(new MemorySaver())
                .build();

        String dataSummary = formatDataForPrompt(data);

        String chartPrompt = String.format("""
            你是一名数据可视化专家（图表代理）。
            
            数据摘要：
            %s
            
            额外预测分析：
            %s
            
            任务：
            建议一个合适的ECharts配置（JSON格式）来可视化数据。
            如果数据是时间序列，优先使用折线图或柱状图。
            
            输出要求：
            - 只返回JSON配置字符串。
            - 不要包含markdown格式如 ```json。
            """, dataSummary, predictionAnalysis != null ? predictionAnalysis : "无");

        try {
            var response = agent.call(chartPrompt);
            String content = response.getText();
            
            // Clean up markdown
            return content.replaceAll("```json", "").replaceAll("```", "").trim();

        } catch (Exception e) {
            return "{}"; // Fallback
        }
    }

    private String formatDataForPrompt(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "No data";
        }
        int limit = 20; 
        StringBuilder sb = new StringBuilder();
        sb.append("Total rows: ").append(data.size()).append("\n");
        sb.append("First ").append(Math.min(data.size(), limit)).append(" rows:\n");
        
        for (int i = 0; i < Math.min(data.size(), limit); i++) {
            sb.append(data.get(i).toString()).append("\n");
        }
        return sb.toString();
    }
}
