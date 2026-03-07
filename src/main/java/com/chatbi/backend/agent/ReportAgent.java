package com.chatbi.backend.agent;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class ReportAgent {

    @Autowired
    private ChatModel chatModel;

    public String generateReport(
            String userQuery,
            String sql,
            List<Map<String, Object>> data,
            String predictionAnalysis,
            String weeklyTemplate
    ) {
        ReactAgent agent = ReactAgent.builder()
                .name("report_agent")
                .model(chatModel)
                .saver(new MemorySaver())
                .build();

        String dataSummary = formatDataForPrompt(data);
        String predictionPart = predictionAnalysis != null ? predictionAnalysis : "No prediction analysis.";

        String reportPrompt;
        if (StringUtils.hasText(weeklyTemplate)) {
            reportPrompt = String.format("""
                    你是一名智能客服数据分析专家，请生成本周周报。

                    用户需求：%s
                    执行SQL：%s
                    数据摘要：
                    %s

                    趋势预测与分析：
                    %s

                    周报模板（必须严格遵循模板结构、章节层级和格式要求）：
                    %s

                    输出要求：
                    1. 只输出周报正文，不要附加解释。
                    2. 若模板存在占位项，请结合本次数据进行填充。
                    3. 输出语言使用中文。
                    """, userQuery, sql, dataSummary, predictionPart, weeklyTemplate);
        } else {
            reportPrompt = String.format("""
                    你是一名商业智能分析师。

                    用户查询：%s
                    执行SQL：%s
                    数据摘要：
                    %s

                    趋势预测与分析：
                    %s

                    任务：
                    以 Markdown 输出一份结构化分析报告，包含标题、关键发现、数据详情和后续建议。
                    """, userQuery, sql, dataSummary, predictionPart);
        }

        try {
            Flux<NodeOutput> stream = agent.stream(reportPrompt);
            List<NodeOutput> outputs = stream.collectList().block();
            if (outputs != null && !outputs.isEmpty()) {
                NodeOutput lastOutput = outputs.get(outputs.size() - 1);
                Optional<Object> messages = lastOutput.state().value("messages");
                if (messages.isPresent()) {
                    List<Message> messageList = (List<Message>) messages.get();
                    if (!messageList.isEmpty()) {
                        Message lastMessage = messageList.get(messageList.size() - 1);
                        if (lastMessage instanceof AssistantMessage assistantMsg) {
                            return assistantMsg.getText();
                        }
                    }
                }
            }
            return "";
        } catch (Exception e) {
            return "Error generating report: " + e.getMessage();
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
            sb.append(data.get(i)).append("\n");
        }
        return sb.toString();
    }
}
