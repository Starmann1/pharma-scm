# Coding Standards

## 1. Java Standards
- **Version**: Java 21 features (Records, Switch Expressions, Pattern Matching, Virtual Threads) should be utilized where appropriate.
- **Formatting**: Use standard Google Java Style formatting (4 spaces for indentation).
- **Immutability**: Prefer immutable objects. Use Java `record` types for DTOs when possible.
- **Optionals**: Return `Optional<T>` from repository and service methods instead of `null`. Do not use `Optional` as method arguments.

## 2. Maven Standards
- Group ID: `com.pharma.ims`
- Enforce dependency convergence using the `maven-enforcer-plugin`.
- Separate unit tests (`*Test.java`) from integration tests (`*IT.java`). `maven-surefire-plugin` for unit tests, `maven-failsafe-plugin` for integration.

## 3. Architecture & SOLID Principles
- **Clean Architecture**: Strict separation of concerns. UI cannot access Database directly. Agents cannot execute SQL.
- **Single Responsibility Principle (SRP)**: Each class should have one reason to change. Separate business validation from persistence.
- **Dependency Inversion Principle (DIP)**: High-level modules (Services) should not depend on low-level modules (JDBC). Both should depend on abstractions (Repository Interfaces).

## 4. Package Naming
- Packages must be lowercase.
- `pharma.gui`: UI Components.
- `pharma.service`: Business Logic.
- `pharma.repository`: Persistence Interfaces.
- `pharma.repository.jdbc`: SQL Implementations.
- `pharma.agent.core`: Agent Framework Base Classes.
- `pharma.dto`: Data Transfer Objects.
- `pharma.model`: Database Entities.

## 5. DTOs (Data Transfer Objects)
- Must be pure data containers with no business logic.
- Must implement `Serializable` if passing across network boundaries (or via JADE).
- Suffix with `DTO`, `Request`, or `Response` (e.g., `MaterialDTO`, `BatchReleaseRequest`).

## 6. Services
- Suffix with `Service` (e.g., `InventoryService`).
- Must handle all transaction boundaries (begin, commit, rollback).
- Must enforce business validation before calling a repository.
- Should throw custom domain exceptions (e.g., `ValidationException`, `InsufficientStockException`).

## 7. Repositories & MySQL
- Suffix interface with `Repository` (e.g., `SupplierRepository`).
- Suffix implementation with `JdbcRepository` (e.g., `SupplierJdbcRepository`).
- **NO SQL IN SERVICES OR UI**. All `SELECT`, `INSERT`, `UPDATE`, `DELETE` statements belong in the JDBC repository implementations.
- Always use `PreparedStatement` to prevent SQL Injection.
- Close `ResultSet`, `PreparedStatement`, and `Connection` resources safely (use `try-with-resources`).

## 8. Agent Development (JADE / ADK)
- **JADE**: Agent classes suffix with `Agent` (e.g., `QAAgent`).
- **Behaviours**: Keep JADE `Behaviour` classes short. They should parse messages and immediately delegate to a Service.
- **No Business Logic**: Do not duplicate validation rules inside Agent Behaviours.
- **ADK Preparedness**: Write agent logic sequentially so it can be easily ported to Google ADK Tools later.

## 9. Logging (SLF4J / Logback)
- Do NOT use `System.out.println()` or `e.printStackTrace()`.
- Use parameterized logging: `logger.info("Processed batch {}", batchNumber);`
- **Levels**:
  - `ERROR`: System failures requiring immediate attention (e.g., DB down).
  - `WARN`: Recoverable errors, suspicious activity, unauthorized access attempts.
  - `INFO`: Significant lifecycle events, transaction completion, agent startup.
  - `DEBUG`: Detailed control flow, useful for troubleshooting.

## 10. Exception Handling
- Do not swallow exceptions (`catch (Exception e) {}`). Always log them or rethrow.
- Catch specific exceptions rather than general `Exception` or `Throwable`.
- Translate technical exceptions (e.g., `SQLException`) into domain exceptions (e.g., `DataAccessException`) at the repository boundary.

## 11. Transaction Management
- Handled at the Service layer. 
- If a method spans multiple repository updates, ensure `Connection.setAutoCommit(false)` is used, and a `rollback()` occurs in the `catch` block.

## 12. LangChain4j & AI
- Do not expose raw internal system details to the LLM context unnecessarily.
- Always validate the output of an LLM before using it to mutate system state. AI output is advisory until validated by a deterministic Service.

## 13. Git & Branching
- Branch naming: `feature/ticket-123-description`, `bugfix/ticket-456-description`.
- Commits must have meaningful messages. `Fix bug` is bad. `Fix NPE in MaterialService during batch approval` is good.

## 14. Anti-Patterns to Avoid
- **God Classes**: E.g., a 3000-line `App.java` that does UI, DB, and Logic.
- **Smart UI**: Putting database queries inside a Swing `ActionListener`.
- **N+1 Queries**: Executing a query inside a loop instead of a `JOIN` or `IN` clause.
- **Hardcoding**: Hardcoding credentials, file paths, or magic numbers. Use `dotenv-java` and constants.
