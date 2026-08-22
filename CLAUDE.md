# Pharma IMS — Claude Code Instructions

## Common Commands

- **Build**: `./mvnw.cmd clean compile`
- **Run Application**: `./mvnw.cmd exec:java`
- **Run All Tests**: `./mvnw.cmd test`
- **Run Single Test Class**: `./mvnw.cmd test -Dtest=ClassName`
- **Run Single Test Method**: `./mvnw.cmd test -Dtest=ClassName#methodName`

## 1. Project Identity


This repository is the **Agentic Pharmaceutical Supply Chain Management System (Pharma IMS), v1.1**.

It is a Java 24 + Maven enterprise application for upstream pharmaceutical manufacturing and supply-chain operations, with a web console, deterministic business services, JDBC persistence, JADE-based agents, and LangChain4j/Gemini AI capabilities.

The architecture is intentionally designed so that the operational core can survive a future migration from JADE to Google ADK without rewriting business services, repositories, DTOs, logging, or persistence.

### Core stack

- Java 24
- Maven 3.9+
- Javalin 6.x / web gateway
- MySQL 8+ and PostgreSQL 15+ through dialect-aware persistence
- JDBC + HikariCP
- SLF4J + Logback
- Jackson
- JADE 4.6.0
- LangChain4j 1.16.1
- Google Gemini
- JUnit 5 + Mockito
- dotenv-java

Before assuming a version or API, inspect `pom.xml` and the existing source code. Do not upgrade dependencies unless explicitly requested or genuinely required.

---

## 2. Golden Engineering Rule

**Understand first. Plan second. Change third. Verify fourth.**

Do not optimize for implementation speed at the expense of correctness, architecture, security, testability, or maintainability.

Before changing non-trivial code:

1. Inspect the relevant files completely.
2. Search for callers and dependants.
3. Inspect interfaces, DTOs, services, repositories, agents, and configuration involved in the flow.
4. Read relevant documentation under `doc/`.
5. Identify the actual source of truth.
6. State the intended change and its blast radius.
7. Make the smallest coherent change.
8. Compile and test.
9. Review the final diff.

Do not rewrite a working subsystem merely because a different design appears cleaner.

---

## 3. Non-Negotiable Architecture

The authoritative application flow is:

```text
UI / API
   ↓
PharmaGateway / application facade
   ↓
Agent orchestration (when applicable)
   ↓
Service layer
   ↓
Repository interfaces
   ↓
JDBC repository implementations
   ↓
MySQL / PostgreSQL
```

For AI-assisted workflows:

```text
User request
   ↓
Agent / AI reasoning
   ↓
Approved @Tool
   ↓
Deterministic service
   ↓
Repository
   ↓
Database
```

### Architectural invariants

- UI must not contain SQL or JDBC code.
- Agents must not contain SQL or open JDBC connections.
- AI components must not access the database directly.
- Business rules belong in `pharma.service`.
- SQL belongs in `pharma.repository.jdbc`.
- Repository contracts must remain independent of JDBC implementation details.
- AI output is advisory until deterministic service-layer validation authorizes a state change.
- JADE-specific concerns must remain isolated from reusable business services.
- Google ADK migration must be possible without rewriting the domain/service/persistence core.

If a requested change violates one of these invariants, explain the conflict before implementing it.

---

## 4. Package Responsibilities

Respect existing package ownership.

| Package | Responsibility |
|---|---|
| `pharma.gui` | UI only: panels, dialogs, forms, dashboards |
| `pharma.config` | Environment, dependency wiring, application configuration |
| `pharma.service` | Business rules, validation, use cases, workflows, authorization |
| `pharma.repository` | Persistence contracts |
| `pharma.repository.jdbc` | JDBC/SQL implementations |
| `pharma.model` | Domain entities and persisted business objects |
| `pharma.dto` | Cross-layer and agent/AI contracts |
| `pharma.events` | Domain events and event contracts |
| `pharma.agent.core` | Shared JADE lifecycle infrastructure |
| `pharma.agent.platform` | Agent platform, gateway, bootstrap, correlation |
| `pharma.agent.ontology` | Agent actions, names, statuses, protocol constants |
| `pharma.ai` | AI provider/orchestration adapters |
| `pharma.rag` | Document ingestion, chunking, retrieval, grounding |

Do not move responsibilities between layers without a clear architectural reason.

---

## 5. Service Layer Is the Business Source of Truth

Services own deterministic business behavior.

They are responsible for:

- Validation
- Business invariants
- Authorization-sensitive decisions
- Transaction boundaries
- Workflow coordination
- Inventory state changes
- Procurement rules
- Production rules
- QA/QC state transitions
- Compliance rules
- Risk calculations where deterministic logic exists

Agents, prompts, and LLMs must not duplicate or override these rules.

If a business rule can be expressed deterministically, implement it deterministically in the service/domain layer rather than asking an LLM to calculate or decide it.

---

## 6. Agent Architecture

The v1.1 agent ecosystem includes operational and cognitive agents such as:

- `CoordinatorAgent`
- `ProcurementWorkflowAgent`
- `SupplierAgent`
- `InventoryAgent`
- `ProductionAgent`
- `QAAgent`
- `ComplianceAgent`
- `RiskAnalysisAgent`
- `AIReasoningAgent`
- `KnowledgeAgent`

### Agent responsibilities

Agents may:

- Orchestrate workflows.
- Route requests.
- Select appropriate services/tools.
- Coordinate other agents.
- Assemble context for AI reasoning.
- Return structured request/response envelopes.

Agents must not:

- Execute SQL.
- Open database connections.
- Reimplement service business rules.
- Directly mutate database state.
- Bypass authorization-sensitive services.

Use existing agent contracts such as transaction IDs, request/response envelopes, agent names, actions, and statuses instead of inventing parallel protocols.

### JADE → Google ADK migration rule

Keep JADE behaviours, ACL messages, and JADE lifecycle details inside the agent layer. Do not introduce JADE dependencies into services, repositories, or domain models.

When adding future Google ADK support, create an adapter/orchestration boundary rather than coupling the core application to Google ADK.

---

## 7. LLM Tools

LLM tool classes under `pharma.llm.tools` are controlled interfaces between AI reasoning and deterministic application capabilities.

Examples include:

- `InventoryLlmTools`
- `SupplierLlmTools`
- `ProductionLlmTools`
- `ComplianceLlmTools`
- `RiskLlmTools`
- `QALlmTools`
- `LimToolRegistry`

A tool should generally follow:

```text
LLM tool
  → validate/normalize input
  → deterministic service
  → structured DTO/result
```

Tool classes should remain thin.

Do not put SQL, transaction orchestration, or large business-rule implementations into `@Tool` methods when an appropriate service already exists.

Tool names and descriptions must accurately describe actual behavior so an LLM can select tools reliably.

Prefer strongly typed DTOs over `Map<String,Object>` or free-form strings when the contract is known.

---

## 8. AI Safety and Hallucination Control

The LLM is **not the source of truth** for operational data.

The following must come from deterministic application data/calculation:

- Stock quantities
- Available/reserved quantities
- Supplier records
- Prices and capacities
- Production records
- Batch identifiers
- QA/QC statuses
- Compliance records
- Risk scores when deterministic rules exist
- Inventory mutations

Never allow an LLM to invent missing values and write them to the database.

If required information is unavailable, return an explicit unavailable/insufficient-data result.

Distinguish between:

1. Observed database facts
2. Deterministically calculated values
3. Retrieved knowledge/evidence
4. AI-generated interpretation or recommendation

### Reasoning traces

Do not expose or persist private chain-of-thought.

For explainability, use concise structured records such as:

- Inputs/evidence used
- Tools invoked
- Decisions/actions taken
- Validation results
- Confidence or uncertainty when supported
- Source documents/citations
- Final recommendation

A trace is an audit/explainability record, not a dump of hidden model reasoning.

---

## 9. RAG / Knowledge Layer

RAG is a knowledge-grounding mechanism, not an operational database mutation mechanism.

The RAG layer may:

- Ingest SOPs and approved documents.
- Chunk documents.
- Generate embeddings.
- Retrieve relevant evidence.
- Assemble grounded context.
- Return source references.

RAG must not directly mutate inventory, procurement, production, QA, or compliance records.

AI answers grounded in documents should retain enough source metadata to make the evidence traceable.

Do not silently treat retrieved text as an authoritative business rule when the application service defines a different enforceable rule.

---

## 10. Database and JDBC Rules

The repository layer is the only place where business SQL should live.

### Required

- Use parameterized SQL / prepared statements.
- Use existing repository abstractions.
- Use HikariCP through the established database infrastructure.
- Respect `JdbcSqlDialect` and the project's MySQL/PostgreSQL compatibility design.
- Keep database-specific syntax isolated to the dialect/repository boundary.
- Preserve transaction semantics.
- Preserve foreign-key and referential-integrity assumptions.

### Forbidden

- Raw SQL in UI classes.
- Raw SQL in agents.
- Raw SQL in AI tools.
- String-concatenated user input in SQL.
- Hardcoded database credentials.
- Destructive schema changes without explicit approval.
- Silent data deletion or migration.

Before changing persistence behavior, inspect the repository interface, JDBC implementation, service caller, schema, and relevant tests.

---

## 11. Pharmaceutical Domain Rules

Treat this as a business-critical pharmaceutical system.

Do not invent domain rules.

Existing states, transitions, approval rules, QC/QA rules, supplier qualification rules, batch lineage, inventory availability, quarantine/rejection/release behavior, and compliance constraints must be preserved unless the task explicitly changes them.

Important operational concepts include:

- Material and batch lineage
- Inventory by material/batch/location
- Available vs reserved stock
- QC/QA status
- Quarantine/rejection/release
- Supplier approval
- Purchase orders and GRNs
- Production orders and consumption
- Batch genealogy
- Auditability
- Compliance records

When requirements conflict with existing business behavior, surface the conflict instead of silently choosing a new rule.

---

## 12. Security

Never commit or print secrets.

Protect:

- API keys
- Database passwords
- Authentication tokens
- Private credentials
- Session secrets
- Sensitive connection strings

Use environment/configuration mechanisms already established by the project.

Do not commit `.env` files containing real secrets.

Never weaken authorization or RBAC simply to make a feature easier to implement.

All external input and LLM output must be treated as untrusted until validated.

---

## 13. Error Handling

Do not use broad exception handling as the default.

Avoid:

```java
catch (Exception e) {
    // ignore
}
```

Prefer specific exceptions and preserve the root cause.

Errors should contain enough context to diagnose the failure without leaking secrets or sensitive data.

Do not silently swallow database, agent, or LLM failures.

For AI workflows, distinguish at least conceptually between:

- Validation failure
- Tool/service failure
- Database failure
- LLM/provider failure
- Timeout
- Unsupported request
- Missing evidence/data

Use the project's existing exception/error model where one exists.

---

## 14. Logging

Use SLF4J/Logback consistently.

Good logs should answer:

- What operation occurred?
- Which business object was involved?
- Which transaction/request/agent correlation ID applies?
- Did it succeed or fail?
- What was the failure cause?

Example:

```java
log.info(
    "[InventoryLlmTools] checkStock materialCode={} requiredQty={}",
    materialCode,
    requiredQty
);
```

Never log passwords, API keys, tokens, or other secrets.

Avoid dumping complete prompts, raw sensitive records, or massive LLM responses into production logs.

---

## 15. Configuration and Dependency Injection

Prefer the repository's existing application wiring and configuration patterns.

Use constructor injection for dependencies where practical:

```java
private final InventoryService inventoryService;

public InventoryLlmTools(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
}
```

Do not introduce a new dependency-injection framework merely for convenience.

Do not create global mutable service state.

Keep environment-specific configuration outside source code.

---

## 16. Code Quality

Write production-quality Java 24 code while following the existing project style.

Prefer:

- Small cohesive methods
- Clear names
- Strong typing
- Immutable DTOs/records where appropriate
- Explicit interfaces
- Constructor injection
- Focused classes
- Explicit contracts
- Minimal duplication

Avoid:

- God classes
- Giant methods
- Deeply nested conditionals
- Magic strings/numbers
- Unnecessary abstractions
- Static global state
- Duplicate business logic
- Premature framework adoption

Do not refactor unrelated code during a feature/fix unless the unrelated code directly blocks correctness.

---

## 17. Testing

Never claim a test or build passed unless it was actually executed.

Before completion, use the repository's supported Maven commands. On Windows PowerShell:

```powershell
./mvnw.cmd clean compile
./mvnw.cmd test
./mvnw.cmd test -Dtest=ClassName
./mvnw.cmd test -Dtest=ClassName#methodName
```

If the Maven wrapper is unavailable, inspect the repository and use the configured Maven installation instead.

### Test priorities

For business logic test:

- Happy path
- Invalid input
- Boundary values
- Missing records
- Failure paths
- Important state transitions
- Authorization-sensitive behavior

For AI tools test:

- Valid parameters
- Invalid parameters
- Service delegation
- Structured result
- Failure handling

For agents test:

- Request routing
- Intent/action selection
- Structured envelopes
- Correlation IDs
- Unsupported requests
- Failure paths

For repositories test persistence behavior against representative schema/fixtures where available.

Prefer behavior/contract tests over tests that merely lock in implementation details.

---

## 18. Change Workflow

### Small change

1. Inspect the target file.
2. Search references.
3. Make the minimal change.
4. Compile/test the affected area.
5. Review the diff.

### Medium/large change

1. Inspect relevant architecture documentation.
2. Trace the complete request/data flow.
3. Identify affected layers.
4. Produce a concise implementation plan.
5. Identify risks and compatibility concerns.
6. Implement incrementally.
7. Compile and test.
8. Review the final diff.

For architectural changes, do not start rewriting files before understanding the current implementation.

---

## 19. Git Safety

Before modifying a repository with existing work:

```powershell
git status
git diff
```

Protect unrelated user changes.

Never perform these without explicit instruction:

- `git reset --hard`
- Force push
- History rewriting
- Branch deletion
- Destructive mass revert
- Destructive database operations

Do not commit automatically unless asked.

Before proposing/creating a commit:

1. Review the diff.
2. Check for unrelated changes.
3. Check for secrets.
4. Run relevant validation.
5. Use a focused commit message.

---

## 20. Dependency Policy

Before adding a dependency:

1. Check `pom.xml`.
2. Check whether an existing dependency already solves the problem.
3. Confirm that the dependency is necessary.
4. Consider security, maintenance, and compatibility.
5. Explain the trade-off for significant additions.

Do not upgrade LangChain4j, JADE, Gemini integrations, Javalin, database drivers, or Java solely because a newer version exists.

---

## 21. Documentation Rules

Repository documentation under `doc/` is architectural guidance and should be kept consistent with implementation.

Important documents include:

- `doc/ARCHITECTURE.md`
- `doc/AGENT_ARCHITECTURE.md`
- `doc/DATABASE_SCHEMA.md`
- `doc/WORKFLOWS.md`
- `doc/CODING_STANDARDS.md`

When a substantial architectural change is intentionally made, determine whether the relevant documentation also needs updating.

Do not update documentation merely to hide an implementation mismatch. Fix the implementation or explicitly document the deviation.

---

## 22. Working With Existing Uncommitted Changes

The working tree may contain user changes unrelated to the current task.

Never overwrite, revert, or reformat unrelated modifications.

Before editing a heavily modified file:

- Inspect the current diff.
- Understand which changes are user-owned.
- Modify only the relevant sections.

If the requested change conflicts with existing uncommitted work, stop and explain the conflict.

---

## 23. Ambiguous Requirements

Use this priority order:

1. Explicit user requirement
2. Existing architecture
3. Existing business rules
4. Existing tests
5. Existing configuration
6. Existing documentation
7. Minimal reasonable assumption

Ask before implementing when ambiguity could cause:

- Data loss
- Schema changes
- Security weakening
- Breaking public APIs
- Business-rule changes
- Architectural boundary violations

For low-risk ambiguity, make the smallest reasonable assumption and state it.

---

## 24. Do Not Over-Engineer

Do not introduce new infrastructure such as:

- Microservices
- Kafka
- Redis
- Kubernetes
- A new database
- A second agent framework
- A new UI framework
- A new ORM

unless explicitly requested or clearly justified by the architecture.

Prefer extending the existing system over replacing it.

---

## 25. Performance

Do not optimize without a reason.

When performance matters:

1. Identify or measure the bottleneck.
2. Determine whether it is CPU, database, network, LLM, or concurrency related.
3. Make the smallest effective change.
4. Re-test behavior.

Pay special attention to:

- N+1 queries
- Unnecessary LLM calls
- Repeated database connections
- Unbounded queries
- Large prompt/context sizes
- Blocking agent operations
- Excessive logging
- Inefficient RAG retrieval

Use HikariCP correctly and preserve transaction semantics.

---

## 26. AI Tool/Prompt Design Rules

Prompts and tool descriptions are part of the AI interface, but they are not the business-rule source of truth.

Prompts should be:

- Explicit
- Domain-aware
- Versionable where practical
- Testable
- Structured around known inputs and outputs

When structured extraction is used:

```text
Raw input
  ↓
LLM extraction
  ↓
Schema validation
  ↓
Domain/DTO object
  ↓
Deterministic service validation
  ↓
Business operation
```

Never trust an LLM-generated identifier, quantity, status, or command without validation.

---

## 27. Response Contract For Claude

When completing an implementation task, report:

1. **Changed** — what was modified.
2. **Reason** — why it was necessary.
3. **Files** — the important files affected.
4. **Validation** — exact commands/tests actually run and their results.
5. **Risks** — remaining limitations or follow-up work.

Do not say "looks good" as a substitute for verification.

Do not claim "production-ready" without evidence.

---

## 28. Final Quality Gate

Before declaring a task complete:

- [ ] Existing architecture preserved where appropriate
- [ ] Correct layer owns the change
- [ ] No SQL leaked outside repository implementations
- [ ] No JDBC leaked into agents/UI/AI tools
- [ ] Business rules remain deterministic and centralized
- [ ] AI output is validated
- [ ] No hallucinated operational data is persisted
- [ ] No secrets introduced
- [ ] No unauthorized schema change
- [ ] Error handling is explicit
- [ ] Logging is safe
- [ ] Relevant tests executed
- [ ] Compilation succeeded where applicable
- [ ] `git diff` reviewed
- [ ] No unrelated files changed
- [ ] Documentation updated if architecture materially changed
- [ ] Remaining limitations disclosed

---

## 29. Absolute Rule

**Do not make the codebase simpler by making the architecture weaker.**

Preserve the separation:

```text
Presentation
    ↓
Gateway / Agent Orchestration
    ↓
Deterministic Services
    ↓
Repository Contracts
    ↓
Dialect-Aware JDBC
    ↓
Database
```

And for AI:

```text
LLM
 ↓
Approved Tool
 ↓
Deterministic Service
 ↓
Validated Result
```

The AI may reason, recommend, summarize, and orchestrate.

**The application remains the authority.**
