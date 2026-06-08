# Agentic Pharmaceutical Supply Chain Management System (Agentic Pharma SCM)

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/)
[![Build Tool](https://img.shields.io/badge/Build-Maven-orange.svg)](https://maven.apache.org/)
[![Database](https://img.shields.io/badge/Database-MySQL%208%2B-blue.svg)](https://www.mysql.com/)
[![Architecture](https://img.shields.io/badge/Architecture-Clean--Layers-green.svg)](#architecture-rules)

An enterprise-grade, manufacturer-centric desktop application transitioning from a traditional monolithic Java Swing + MySQL architecture to an advanced **Multi-Agent Orchestrated System** powered by a hybrid **JADE** (Java Agent Development Framework) and **Google ADK** design.

---

## 👁️ System Overview & Paradigm Shift

The system focuses on upstream pharmaceutical manufacturing operations, enforcing strict regulatory compliance, complete batch lineage, and automated supply chain routing. 

```
                                  +-----------------------+
                                  |     Java Swing UI     |
                                  +-----------+-----------+
                                              | (DTOs)
                                              v
                                  +-----------+-----------+
                                  |   JADE / Google ADK   |
                                  |      Agent Layer      |
                                  +-----------+-----------+
                                              | (Service Calls)
                                              v
                                  +-----------+-----------+
                                  |     Service Layer     |
                                  +-----------+-----------+
                                              |
                                              v
                                  +-----------+-----------+
                                  |   Repository Layer    |
                                  +-----------+-----------+
                                              | (JDBC)
                                              v
                                  +-----------+-----------+
                                  |     MySQL Database    |
                                  +-----------------------+
```

Traditionally, the application executed raw database queries directly from the user interface. We are undergoing a phased modernization:
1. **Decoupled Monolith (Completed Phase 0-5)**: Restructuring the database access, service validations, data transfer objects (DTOs), SLF4J audit logging, and scafolding JADE bootstrap.
2. **Deterministic Agent Integration (Active Phase 6-8)**: Introducing JADE agents to handle asynchronous coordination, supplier negotiation (FIPA Contract Net), and automated replenishment rules.
3. **AI & RAG Orchestration (Phase 9-12)**: Enhancing capabilities with Google ADK + LangChain4j + Gemini API for processing unstructured supplier audits, RAG-grounded SOP compliance checks, and risk analysis.
4. **Pure Agentic System (Phase 13)**: Migrating the platform entirely to Google ADK, utilizing LLM tool calls and deterministic Java services.

---

## 📂 Documentation Hub

All detailed technical documentation has been organized under the [doc/](file:///d:/My%20Projects/Agentic%20Pharma%20SCM/pharma-ims/doc) directory. Please consult these files for comprehensive insights:

*   **[ARCHITECTURE.md](file:///d:/My%20Projects/Agentic%20Pharma%20SCM/pharma-ims/doc/ARCHITECTURE.md)**: Deep dive into the architectural layers, package separation, transactional boundaries, and data flow.
*   **[AGENT_ARCHITECTURE.md](file:///d:/My%20Projects/Agentic%20Pharma%20SCM/pharma-ims/doc/AGENT_ARCHITECTURE.md)**: Specifications of all cooperative agents (`CoordinatorAgent`, `InventoryAgent`, etc.), communication schemas, and the JADE-to-ADK migration plan.
*   **[DATABASE_SCHEMA.md](file:///d:/My%20Projects/Agentic%20Pharma%20SCM/pharma-ims/doc/DATABASE_SCHEMA.md)**: Complete database relational schema description, index definitions, and audit trail tables.
*   **[WORKFLOWS.md](file:///d:/My%20Projects/Agentic%20Pharma%20SCM/pharma-ims/doc/WORKFLOWS.md)**: Step-by-step descriptions and Mermaid sequence diagrams for key processes such as Procurement, Production planning, and Quality Gates.
*   **[CODING_STANDARDS.md](file:///d:/My%20Projects/Agentic%20Pharma%20SCM/pharma-ims/doc/CODING_STANDARDS.md)**: Development guidelines, naming conventions, transactional safety patterns, and repository-service rules.
*   **[ROADMAP.md](file:///d:/My%20Projects/Agentic%20Pharma%20SCM/pharma-ims/doc/ROADMAP.md)**: The 14-phase transition plan mapping out the legacy-to-agentic transition.
*   **[AI_CONTEXT.md](file:///d:/My%20Projects/Agentic%20Pharma%20SCM/pharma-ims/doc/AI_CONTEXT.md)**: Context, directives, and system prompt constraints for AI coding agents.
*   **[PHASES_1_TO_5_REVIEW.md](file:///d:/My%20Projects/Agentic%20Pharma%20SCM/pharma-ims/doc/PHASES_1_TO_5_REVIEW.md)**: Structural assessment of the foundation modules implemented so far.

---

## 🛠️ Technology Stack

*   **Runtime Environment**: Java Development Kit (JDK) 21
*   **GUI Library**: Java Swing / AWT (Standard Desktop)
*   **Database**: MySQL 8.0+
*   **Connection Pool & Access**: HikariCP & Native JDBC
*   **Build & Dependency Management**: Apache Maven 3.9+
*   **Logging System**: SLF4J API with Logback implementation
*   **Agentic Frameworks**: JADE 4.6.0 (Legacy Agent Lifecycle) and Google ADK (AI Tool Calling & Routing)
*   **LLM & RAG Frameworks**: Google Gemini API via LangChain4j integration

---

## 🏗️ Architecture Rules

To guarantee system stability, compliance, and transition safety, all code changes **must** respect the following boundary guidelines:

> [!IMPORTANT]
> **1. No JDBC in UI:** Swing ActionListeners must never contain raw SQL, connections, or JDBC statements.
> **2. No JDBC in Agents:** Agents are orchestration entities and must never contact the database directly.
> **3. No Business Logic in Agents:** Agents must delegate to deterministic `pharma.service` classes for validation and data mutations.
> **4. Repositories as Single Source of Database Interaction:** Only class implementations inside `pharma.repository.jdbc` are allowed to write or execute SQL.
> **5. DTO Communication:** Communication between layers (UI, Agents, Services) must occur via Data Transfer Objects (DTOs), never raw JDBC ResultSets or system entities.

---

## 🌿 Git Branching & Lifecycle Strategy

The project utilizes a GitFlow-inspired branching strategy aligned to the [ROADMAP.md](file:///d:/My%20Projects/Agentic%20Pharma%20SCM/pharma-ims/doc/ROADMAP.md):

*   **`main`**: Production-ready, stable, traditional monolith base and fully vetted releases.
*   **`develop`**: Integration branch for the active modernization effort.
*   **`feature/<phase-name>`**: Target feature branches for implementing specific roadmap milestones.
    *   *Current Active Branch*: `feature/operational-agents` (Implementing JADE-based `CoordinatorAgent` and operational companions for Phase 6).

---

## ⚙️ Setup and Installation

### 1. Prerequisites
Ensure you have the following installed and configured on your system:
- **JDK 21** (Ensure `JAVA_HOME` is set up correctly in system environment variables)
- **Maven** (Available on your CLI PATH)
- **MySQL Server 8+**

### 2. Database Initialization
1. Start your local MySQL database service.
2. Log into your command line or database client (e.g., MySQL Workbench).
3. Create the database and seed it by importing the `database.sql` script:
   ```bash
   mysql -u <username> -p < database.sql
   ```
4. If modifying quarantine workflows or importing seed scripts, run:
   ```bash
   mysql -u <username> -p pharma_ims < db_fix.sql
   mysql -u <username> -p pharma_ims < supplier_approval_workflow.sql
   mysql -u <username> -p pharma_ims < supplier_license_seed.sql
   ```

### 3. Environment Variables
Create a file named `.env` in the root folder of the project (`pharma-ims/`) and customize the credentials to match your MySQL server configuration:
```ini
DB_URL=jdbc:mysql://localhost:3306/pharma_ims
DB_USER=your_mysql_username
DB_PASS=your_mysql_password
```

### 4. Build and Compilation
Clean, resolve dependencies, and compile the application using Maven:
```cmd
mvn clean compile
```

### 5. Running Tests
Verify the integrity of the repository and service layers by running unit tests:
```cmd
mvn test
```

### 6. Executing the Application
Start the desktop application using the Maven Execution plugin:
```cmd
mvn exec:java
```

---

## 🤖 Current Phase: Phase 6 - Core Operational Agents
The system is currently prepped for **Phase 6: Core Operational Agents**. 
The foundation layers (Repositories, Services, DTOs, Logs, and JADE Bootstrap Container) are integrated and tested. The next step focuses on implementing:
- `CoordinatorAgent`: Routing Swing UI requests to specific agent handlers.
- `InventoryAgent`: Querying stock limits and validating quarantines.
- `SupplierAgent`: Evaluating vendor capabilities.
- `ProductionAgent` and `QAAgent`: Assisting manufacturing runs and release validations.
