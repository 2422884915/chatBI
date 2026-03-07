package com.chatbi.backend.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DataPredictionAgent (ReactAgent implementation)
 */
@Component
public class DataPredictionAgent {

    @Autowired
    private ChatModel chatModel;

    public PredictionResult predict(List<Map<String, Object>> historicalData) {
        if (historicalData == null || historicalData.isEmpty()) {
            return new PredictionResult("无足够历史数据进行预测。", List.of());
        }

        ReactAgent agent = ReactAgent.builder()
                .name("data_prediction_agent")
                .model(chatModel)
                // No external tools needed for pure analysis, but using ReactAgent as requested
                .saver(new MemorySaver())
                .build();

        String dataSummary = formatDataForPrompt(historicalData);

        String predictionPrompt = String.format("""
            你是一名数据预测代理。
            你的任务是分析历史数据并预测未来趋势。
            
            历史数据摘要：
            %s
            
            任务：
            1. 分析趋势。
            2. 预测下一年的趋势。
            3. 以JSON格式输出结果：
            {
                "analysis": "你的分析文本...",
                "predictedData": [ ... 可选的预测数据点 ... ]
            }
            """, dataSummary);

        try {
            var response = agent.call(predictionPrompt);
            String content = response.getText();
            
            // Simplified return, assuming the content is the analysis or JSON
            // For now, just return the content as analysis
            return new PredictionResult(content, List.of());

        } catch (Exception e) {
            return new PredictionResult("Prediction failed: " + e.getMessage(), List.of());
        }
    }

    private String formatDataForPrompt(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "No data";
        }
        int limit = 50; 
        StringBuilder sb = new StringBuilder();
        sb.append("Total rows: ").append(data.size()).append("\n");
        sb.append("First ").append(Math.min(data.size(), limit)).append(" rows:\n");
        
        for (int i = 0; i < Math.min(data.size(), limit); i++) {
            sb.append(data.get(i).toString()).append("\n");
        }
        return sb.toString();
    }

    public record PredictionResult(String analysis, List<Map<String, Object>> predictedData) {}
}
