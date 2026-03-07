package com.chatbi.backend.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class SqlExecutorTool implements BiFunction<String, ToolContext, String> {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SqlExecutorTool(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }


    @Override
    public String  apply(

            @ToolParam(description = "待执行的sql") String sql, ToolContext toolContext) {
        if (sql == null || !sql.trim().toUpperCase().startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed.");
        }

        try {
            List<Map<String, Object>> result = executeQuery(sql);
           return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new RuntimeException("Error executing SQL: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a SQL query and returns the result as a list of maps.
     * @param sql The SQL query to execute.
     * @return List of rows, where each row is a map of column name to value.
     */
    public List<Map<String, Object>> executeQuery(String sql) {
        // Basic safety check: ensure it's a SELECT statement
        if (sql == null || !sql.trim().toUpperCase().startsWith("SELECT")) {
            throw new IllegalArgumentException("Only SELECT queries are allowed.");
        }

        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            throw new RuntimeException("Error executing SQL: " + e.getMessage(), e);
        }
    }



}
