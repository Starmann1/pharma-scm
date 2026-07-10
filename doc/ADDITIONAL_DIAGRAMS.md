# Additional System Diagrams & Logic Flows

This document contains high-quality Mermaid diagrams detailing the dialect persistence router, tool-calling pipelines, RAG ingestion flows, and scheduling guards.

---

## 1. Dialect-Aware Persistence Flow

Illustrates how the JDBC Repository layer queries configuration profiles and generates database-compatible SQL dialects on the fly:

```mermaid
flowchart TD
    subgraph Repository ["JDBC Repository Layer"]
        Repo[PurchaseOrderJdbcRepository]
        DialectCheck{JdbcSqlDialect.getProfile}
    end

    subgraph Dialect ["Dialect Profile Router"]
        MySQLProfile["MySQL Profile"]
        PostgresProfile["PostgreSQL Profile"]
    end

    subgraph Queries ["SQL Generation (Dialect-Specific)"]
        MySQLQuery["LIMIT / OFFSET / CONCAT<br/>Backtick Column Escapes `code`"]
        PostgresQuery["LIMIT / OFFSET / ||<br/>Double Quote Escapes 'code'"]
    end

    subgraph Persistence ["Hikari Connection Pool"]
        Hikari[HikariDataSource]
    end

    subgraph Engine ["Target Database Engine"]
        MySQL[(MySQL Server 8+)]
        Postgres[(PostgreSQL Server 15+)]
    end

    Repo --> DialectCheck
    DialectCheck -->|MySQL| MySQLProfile
    DialectCheck -->|PostgreSQL| PostgresProfile
    MySQLProfile --> MySQLQuery
    PostgresProfile --> PostgresQuery
    MySQLQuery --> Hikari
    PostgresQuery --> Hikari
    Hikari -->|Dialect SQL| MySQL
    Hikari -->|Dialect SQL| Postgres
```

---

## 2. AI Tool-Calling (LangChain4j) Dataflow

Outlines the transaction flow when `AIReasoningAgent` leverages LangChain4j tool-calling wrappers to invoke deterministic Java queries:

```mermaid
sequenceDiagram
    autonumber
    participant Agent as AIReasoningAgent (JADE)
    participant Gemini as Google Gemini AI (LLM)
    participant JavaTools as LangChain4j Tool Wrapper
    participant Services as Deterministic Services (Java)
    participant Database as PostgreSQL / MySQL

    Agent->>Gemini: Prompt: "Rank suppliers for MAT-001 reorder"
    Note over Gemini: Analyzes prompt;<br/>Identifies requirement for supplier database check
    Gemini-->>Agent: Tool Request: call "SupplierLlmTools.rankSuppliers(MAT-001)"
    Agent->>JavaTools: Route tool call payload
    JavaTools->>Services: Invoke rankSuppliers("MAT-001")
    Services->>Database: SQL SELECT supplier performance ratings
    Database-->>Services: Return scores & capacity
    Services-->>JavaTools: Return list of ranked suppliers
    JavaTools-->>Agent: Format results as tool execution output
    Agent->>Gemini: Send tool results: "[Supplier A: Score 92%, Supplier B: Score 75%]"
    Note over Gemini: Evaluates tool outcomes & generates summary
    Gemini-->>Agent: Final Response: "Supplier A is recommended (92% rating)..."
```

---

## 3. RAG Document Ingestion & Search Pipeline

Details the startup document ingestion pipeline and similarity vector search queries run by the `KnowledgeAgent`:

```mermaid
flowchart TD
    subgraph Ingestion ["1. Document Ingestion (Startup)"]
        SOPs["./sop_documents/ (*.pdf, *.txt)"]
        Parser["DocumentParser / Text Segmenter"]
        Chunks["Text Chunks (1000 chars, 200 overlap)"]
    end

    subgraph Embedding ["2. Embedding & Vector Storage"]
        Model["GoogleAiEmbeddingModel (text-embedding-004)"]
        Vectors["Vector Embeddings (768-dim)"]
        VectorDB[("InMemoryEmbeddingStore (Vector Store)")]
    end

    subgraph Querying ["3. Semantic Query Execution"]
        UserQuery["User Query: 'What is quarantine sampling SOP?'"]
        QueryEmbedding["Convert query to 768-dim vector"]
        SimilarityCheck{"Cosine Similarity Match"}
        Context["Top 3 Matching SOP Context segments"]
        GeminiCognitive["Gemini LLM (Synthesis)"]
        Answer["Grounded Response with citations"]
    end

    SOPs --> Parser
    Parser --> Chunks
    Chunks --> Model
    Model --> Vectors
    Vectors --> VectorDB

    UserQuery --> QueryEmbedding
    QueryEmbedding --> SimilarityCheck
    VectorDB -->|Search Corpus| SimilarityCheck
    SimilarityCheck --> Context
    Context --> GeminiCognitive
    UserQuery --> GeminiCognitive
    GeminiCognitive --> Answer
```

---

## 4. Production Feasibility Sweep & Guard Logic

Illustrates the step-by-step validation logic run before scheduling production order runs:

```mermaid
flowchart TD
    Start["Schedule Production Order"]
    GetBOM["Load BOM Ingredients Formulations"]
    LoopStart["For Each Ingredient in Recipe"]
    GetStock["Query Stock Table: availableQuantity"]
    Calculate["Calculate Required Qty = plannedQty * ingredientQty"]
    Compare{"Is Available Qty >= Required Qty?"}
    Deficit["Calculate Deficit = Required - Available"]
    AddList["Add Deficit details to Shortfalls List"]
    LoopEnd{"Last Ingredient?"}
    CheckShortfalls{"Are there any Shortfalls?"}
    ShowModal["Popup Override Alert:<br/>List shortfalls and demand validation"]
    UserConfirm{"User Confirms Override?"}
    CreateOrder["Create Production Order (status: IN_PRODUCTION)"]
    Cancel["Cancel Scheduling Run"]

    Start --> GetBOM
    GetBOM --> LoopStart
    LoopStart --> GetStock
    GetStock --> Calculate
    Calculate --> Compare
    Compare -->|No| Deficit
    Deficit --> AddList
    Compare -->|Yes| LoopEnd
    AddList --> LoopEnd
    LoopEnd -->|No| LoopStart
    LoopEnd -->|Yes| CheckShortfalls
    CheckShortfalls -->|No| CreateOrder
    CheckShortfalls -->|Yes| ShowModal
    ShowModal --> UserConfirm
    UserConfirm -->|Yes| CreateOrder
    UserConfirm -->|No| Cancel
```
