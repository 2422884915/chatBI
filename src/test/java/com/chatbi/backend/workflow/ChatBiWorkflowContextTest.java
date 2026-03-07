package com.chatbi.backend.workflow;

import com.chatbi.backend.agent.ChatBiAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:chatbi_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.ai.dashscope.api-key=dummy"
})
class ChatBiWorkflowContextTest {

    @Autowired
    private ChatBiAgent chatBiAgent;

    @Test
    void contextLoads() {
        assertThat(chatBiAgent).isNotNull();
    }
}

