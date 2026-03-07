# ChatBI Backend (Multi-Agent Architecture)

这是一个基于 Spring Boot 3 和 Spring AI Alibaba 的多智能体 ChatBI 系统后端。

## 系统架构 (5大智能体)

系统由以下5个核心智能体协同工作：

1.  **ChatBI Agent (主控智能体)** (`ChatBiAgent.java`)
    -   **职责**：作为系统的大脑，接收用户指令，进行意图识别（查询 vs 闲聊），并协调其他智能体的工作流程。
    -   **逻辑**：如果识别为查询意图，依次调用数据查询 -> (可选)数据预测 -> 图表生成 -> 报告撰写。

2.  **Data Query Agent (数据查询智能体)** (`DataQueryAgent.java`)
    -   **职责**：数据大脑。
    -   **能力**：
        -   获取数据库 Schema (`SchemaTool`)。
        -   将自然语言转化为 SQL (NL2SQL)。
        -   执行 SQL 并获取数据 (`SqlExecutorTool`)。

3.  **Data Prediction Agent (数据预测智能体)** (`DataPredictionAgent.java`)
    -   **职责**：数据分析专家。
    -   **能力**：基于历史数据，分析趋势并预测下一年的数据。仅在用户意图涉及未来预测时触发。

4.  **Chart Agent (图表生成智能体)** (`ChartAgent.java`)
    -   **职责**：视觉专家。
    -   **能力**：根据数据特征和预测结果，自动选择最合适的图表类型（折线图、柱状图等），并生成 ECharts JSON 配置。

5.  **Report Agent (报告撰写智能体)** (`ReportAgent.java`)
    -   **职责**：文档专家。
    -   **能力**：整合用户查询、执行的SQL、查询结果、预测分析，生成带有洞察结论的 Markdown 格式分析报告。

## 技术栈

-   **Java 17+**
-   **Spring Boot 3.2.4**
-   **Spring AI Alibaba (1.0.0-M6.1)**
-   **Spring JDBC**
-   **MySQL**

## 快速开始

1.  **配置数据库**:
    修改 `src/main/resources/application.yml` 中的数据库连接信息。

2.  **配置 AI Key**:
    设置环境变量 `ALIBABA_AI_API_KEY`。

3.  **运行**:
    ```bash
    mvn spring-boot:run
    ```

4.  **接口调用**:
    `POST /api/chatbi/query`
    ```json
    {
        "query": "查询上个月的销售额，并预测明年的趋势",
        "databaseName": "sales_db"
    }
    ```
