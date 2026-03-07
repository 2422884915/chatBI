package com.chatbi.backend.agent;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.chatbi.backend.model.ChatResponse;
import com.chatbi.backend.service.WeeklyReportTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatBiAgent {

    private final CompiledGraph compiledGraph;
    private final WeeklyReportTemplateService weeklyReportTemplateService;

    @Autowired
    public ChatBiAgent(
            @Qualifier("chatBiGraph") StateGraph stateGraph,
            WeeklyReportTemplateService weeklyReportTemplateService
    ) throws GraphStateException {
        this.compiledGraph = stateGraph.compile();
        this.weeklyReportTemplateService = weeklyReportTemplateService;
    }

    public ChatResponse process(String userQuery) {
        ChatResponse response = new ChatResponse();
        String dbName = "beijing_trip";

        try {
            RunnableConfig runnableConfig = RunnableConfig.builder()
                    .threadId(UUID.randomUUID().toString())
                    .build();
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("query", userQuery);
            inputs.put("dbName", dbName);
            weeklyReportTemplateService.getTemplate().ifPresent(template -> inputs.put("weeklyReportTemplate", template));

            Optional<OverAllState> invoke = this.compiledGraph.invoke(inputs, runnableConfig);
            Map<String, Object> data = invoke.map(OverAllState::data).orElseGet(Map::of);

            response.setIntent((String) data.get("intent"));
            response.setGeneratedSql((String) data.get("generatedSql"));
            response.setChartConfig((String) data.get("chartConfig"));
            response.setAnalysisReport((String) data.get("analysisReport"));

            Object queryResult = data.get("queryResult");
            if (queryResult instanceof List<?> list) {
                response.setQueryResult((List<Map<String, Object>>) list);
            }
            response.setError((String) data.get("error"));
        } catch (Exception e) {
            response.setError(e.getMessage());
        }

        return response;
    }
}
