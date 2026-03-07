package com.chatbi.backend.workflow;

import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.KeyStrategyFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.chatbi.backend.agent.ChartAgent;
import com.chatbi.backend.agent.DataPredictionAgent;
import com.chatbi.backend.agent.DataQueryAgent;
import com.chatbi.backend.agent.ReportAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

@Configuration
public class ChatBiGraphConfiguration {

    @Bean("chatBiGraph")
    public StateGraph chatBiGraph(
            ChatModel chatModel,
            DataQueryAgent dataQueryAgent,
            DataPredictionAgent dataPredictionAgent,
            ChartAgent chartAgent,
            ReportAgent reportAgent
    ) throws GraphStateException {

        KeyStrategyFactory keyStrategyFactory = () -> {
            HashMap<String, KeyStrategy> strategies = new HashMap<>();
            strategies.put("query", new ReplaceStrategy());
            strategies.put("dbName", new ReplaceStrategy());
            strategies.put("intent", new ReplaceStrategy());
            strategies.put("generatedSql", new ReplaceStrategy());
            strategies.put("queryResult", new ReplaceStrategy());
            strategies.put("needPrediction", new ReplaceStrategy());
            strategies.put("predictionAnalysis", new ReplaceStrategy());
            strategies.put("chartConfig", new ReplaceStrategy());
            strategies.put("analysisReport", new ReplaceStrategy());
            strategies.put("weeklyReportTemplate", new ReplaceStrategy());
            strategies.put("error", new ReplaceStrategy());
            return strategies;
        };

        return new StateGraph(keyStrategyFactory)
                .addNode("intent", node_async(state -> {
                    String query = state.value("query", "");
                    ReactAgent agent = ReactAgent.builder()
                            .name("intent_agent")
                            .model(chatModel)
                            .saver(new MemorySaver())
                            .build();
                    String prompt = String.format("""
                            Analyze the following user query and determine if it requires a database query to answer.

                            Query: %s

                            Return 'QUERY' if it requires fetching data from a database.
                            Return 'CHAT' if it is a general conversational greeting or question that doesn't need specific data.

                            Only return the keyword 'QUERY' or 'CHAT'.
                            """, query);
                    String intent = agent.call(prompt).getText().trim();
                    String normalized = intent.toUpperCase().contains("QUERY") ? "QUERY" : "CHAT";
                    return Map.of("intent", normalized);
                }))
                .addNode("chat", node_async(state -> {
                    String query = state.value("query", "");
                    ReactAgent agent = ReactAgent.builder()
                            .name("chat_agent")
                            .model(chatModel)
                            .saver(new MemorySaver())
                            .build();
                    String text = agent.call(query).getText();
                    return Map.of("analysisReport", text);
                }))
                .addNode("data_query", node_async(state -> {
                    String query = state.value("query", "");
                    String dbName = state.value("dbName", "beijing_trip");
                    DataQueryAgent.QueryResult result = dataQueryAgent.execute(query, dbName);
                    Map<String, Object> out = new HashMap<>();
                    out.put("generatedSql", result.sql());
                    out.put("queryResult", result.data());
                    return out;
                }))
                .addNode("predict_decision", node_async(state -> {
                    String query = state.value("query", "");
                    ReactAgent agent = ReactAgent.builder()
                            .name("decision_agent")
                            .model(chatModel)
                            .saver(new MemorySaver())
                            .build();
                    String prompt = String.format("""
                            Analyze if the user query asks for future prediction (e.g., 'predict', 'next year', 'future trend').

                            Query: %s

                            Return 'YES' or 'NO'.
                            """, query);
                    boolean need = agent.call(prompt).getText().toUpperCase().contains("YES");
                    return Map.of("needPrediction", need);
                }))
                .addNode("prediction", node_async(state -> {
                    List<Map<String, Object>> data = state.value("queryResult", List.of());
                    if (data.isEmpty()) {
                        return Map.of("predictionAnalysis", null);
                    }
                    DataPredictionAgent.PredictionResult result = dataPredictionAgent.predict(data);
                    return Map.of("predictionAnalysis", result.analysis());
                }))
                .addNode("chart", node_async(state -> {
                    List<Map<String, Object>> data = state.value("queryResult", List.of());
                    String predictionAnalysis = state.value("predictionAnalysis")
                            .map(obj -> obj instanceof String ? (String) obj : null)
                            .orElse(null);
                    if (data.isEmpty()) {
                        return Map.of("chartConfig", "{}");
                    }
                    String chartConfig = chartAgent.generateChartConfig(data, predictionAnalysis);
                    return Map.of("chartConfig", chartConfig);
                }))
                .addNode("report", node_async(state -> {
                    String query = state.value("query", "");
                    String sql = state.value("generatedSql", "");
                    List<Map<String, Object>> data = state.value("queryResult", List.of());
                    String predictionAnalysis = state.value("predictionAnalysis")
                            .map(obj -> obj instanceof String ? (String) obj : null)
                            .orElse(null);
                    String weeklyReportTemplate = state.value("weeklyReportTemplate")
                            .map(obj -> obj instanceof String ? (String) obj : null)
                            .orElse(null);

                    String report = reportAgent.generateReport(query, sql, data, predictionAnalysis, weeklyReportTemplate);
                    return Map.of("analysisReport", report);
                }))
                .addEdge(StateGraph.START, "intent")
                .addConditionalEdges("intent", edge_async(state -> {
                    String intent = state.value("intent", "CHAT");
                    return "CHAT".equalsIgnoreCase(intent) ? "chat" : "data_query";
                }), Map.of(
                        "chat", "chat",
                        "data_query", "data_query"
                ))
                .addEdge("chat", StateGraph.END)
                .addEdge("data_query", "predict_decision")
                .addConditionalEdges("predict_decision", edge_async(state -> {
                    boolean needPrediction = state.value("needPrediction", false);
                    return needPrediction ? "prediction" : "chart";
                }), Map.of(
                        "prediction", "prediction",
                        "chart", "chart"
                ))
                .addEdge("prediction", "chart")
                .addEdge("chart", "report")
                .addEdge("report", StateGraph.END);
    }
}
