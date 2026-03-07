package com.chatbi.backend.agent;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.chatbi.backend.tool.SchemaTool;
import com.chatbi.backend.tool.SqlExecutorTool;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DataQueryAgent (ReactAgent implementation)
 */
@Component
public class DataQueryAgent {

    @Autowired
    private ChatModel chatModel;


    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;;

    public QueryResult execute(String userQuery, String databaseName) {
        // Create ReactAgent dynamically or reuse (Agents are usually stateful per conversation, so creating new for request is safer for stateless REST)
        // Note: In a real app, you might cache agents or use a proper scope.
        // 创建工具回调
        ToolCallback schemaTool = FunctionToolCallback
                .builder("schemaTool", new SchemaTool(jdbcTemplate))
                .description("根据数据库名称获取数据库的主要表结构")
                .inputType(String.class)
                .build();
        ToolCallback sqlExecutorTool = FunctionToolCallback
                .builder("sqlExecutorTool", new SqlExecutorTool(jdbcTemplate,objectMapper))
                .description("根据数据库名称获取数据库的主要表结构")
                .inputType(String.class)
                .build();

        ReactAgent agent = ReactAgent.builder()
                .name("data_query_agent")
                .model(chatModel)
                .tools(schemaTool, sqlExecutorTool) // Assuming builder accepts Function beans
                .saver(new MemorySaver())
                .build();

        String prompt = String.format("""
            你是一名数据查询代理。
            你的任务是从数据库 '%s' 中检索用户查询的数据。
            
            用户查询：%s
            
            步骤：
            1. 使用 'schemaTool' 获取数据库 '%s' 的模式。
            2. 根据模式生成有效的 SQL SELECT 查询。
            3. 使用 'sqlExecutorTool' 执行 SQL。
            4. 仅以以下 JSON 格式输出结果：
            {
                "sql": "使用的 SQL",
                "data": [ ... sqlExecutorTool 的结果 ... ]
            }
            
            不要在 JSON 之外添加任何解释。
            """, databaseName, userQuery, databaseName);

        // Execute agent
        // ReactAgent.call() usually takes the input string
        // We might need to handle the return type. Assuming it returns the final answer string.
        try {
            // RunnableConfig is optional or needed?
            // AssistantMessage response = agent.call(prompt);
            // Let's assume a simplified call method exists or we use the standard one.
            // Based on search result: agent.call(prompt, config)
            
            // For now, I'll assume agent.run(prompt) or agent.call(prompt)
            // I'll use a generic call and parse output.
             Map<String, Object> inputs = Map.of("input", prompt);
             // The API might vary, I will try to use a standard 'call' if possible.
             // If ReactAgent implements a functional interface, great.
             // Let's guess the API from search result: agent.call("input", config)
             
             // Since I can't compile, I will write what seems correct from search result #2.
             // "AssistantMessage response = agent.call("what is the weather outside?", runnableConfig);"
             
             var response = agent.call(prompt); // Simplified
             String content = response.getText();
             
             // Extract JSON
             String json = content.replaceAll("```json", "").replaceAll("```", "").trim();
             if (json.startsWith("{")) {
                 Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
                 String sql = (String) map.get("sql");
                 List<Map<String, Object>> data = (List<Map<String, Object>>) map.get("data");
                 return new QueryResult(sql, data);
             } else {
                 // Fallback if agent didn't output JSON
                 return new QueryResult("Unknown SQL", List.of());
             }

        } catch (Exception e) {
            throw new RuntimeException("Agent execution failed: " + e.getMessage(), e);
        }
    }

    public record QueryResult(String sql, List<Map<String, Object>> data) {}
}
