package com.chatbi.backend.tool;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component("schemaTool")
public class SchemaTool implements BiFunction<String, ToolContext, String> {

    private final  JdbcTemplate jdbcTemplate;

    public SchemaTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

    }

    @Override
    public String apply(
            @ToolParam (description = "数据库名称")
            String databasName, ToolContext toolContext) {
        return getSchemaInfo(databasName);
    }

    public String getSchemaInfo(String databaseName) {
        String sql = """
            SELECT 
                TABLE_NAME, 
                COLUMN_NAME, 
                DATA_TYPE, 
                COLUMN_COMMENT 
            FROM 
                information_schema.COLUMNS 
            WHERE 
                TABLE_SCHEMA = ? 
            ORDER BY 
                TABLE_NAME, ORDINAL_POSITION
        """;

        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql, databaseName);

        if (result.isEmpty()) {
            return "No tables found in database: " + databaseName;
        }

        Map<String, List<Map<String, Object>>> tables = result.stream()
                .collect(Collectors.groupingBy(row -> (String) row.get("TABLE_NAME")));

        StringBuilder schemaBuilder = new StringBuilder();
        schemaBuilder.append("Database Schema for ").append(databaseName).append(":\n");

        for (Map.Entry<String, List<Map<String, Object>>> entry : tables.entrySet()) {
            String tableName = entry.getKey();
            schemaBuilder.append("Table: ").append(tableName).append("\n");
            schemaBuilder.append("Columns:\n");

            for (Map<String, Object> column : entry.getValue()) {
                String columnName = (String) column.get("COLUMN_NAME");
                String dataType = (String) column.get("DATA_TYPE");
                String comment = (String) column.get("COLUMN_COMMENT");

                schemaBuilder.append(String.format(" - %s (%s): %s\n", columnName, dataType, comment));
            }
            schemaBuilder.append("\n");
        }

        return schemaBuilder.toString();
    }
}
