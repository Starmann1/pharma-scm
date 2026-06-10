# Agentic Pharmaceutical Supply Chain Management System (Agentic Pharma SCM)

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/)
[![Build Tool](https://img.shields.io/badge/Build-Maven-orange.svg)](https://maven.apache.org/)
[![Database](https://img.shields.io/badge/Database-MySQL%208%2B-blue.svg)](https://www.mysql.com/)
[![Architecture](https://img.shields.io/badge/Architecture-Clean--Layers-green.svg)](#architecture-rules)

An enterprise-grade, manufacturer-centric desktop application transitioning from a traditional monolithic Java Swing + MySQL architecture into a state-of-the-art **Multi-Agent Orchestrated System**. 

The platform is designed to run intelligent agents that autonomously negotiate raw material procurement, evaluate supplier risk, enforce Quality Assurance (QA) compliance, and coordinate production schedules.

---

## 👁️ System Overview & Paradigm Shift

The system focuses on upstream pharmaceutical manufacturing operations, enforcing strict regulatory compliance, complete batch lineage, and automated supply chain routing. 

```text
                                  +-----------------------+
                                  |     Java Swing UI     |
                                  +-----------+-----------+
                                              | (DTOs)
                                              v
                                  +-----------+-----------+
                                  |     PharmaGateway     |  <-- Abstraction Layer
                                  +-----------+-----------+
                                              | 
         +------------------------------------+------------------------------------+
         |                                    |                                    |
         v                                    v                                    v
+------------------+                 +------------------+                 +------------------+
|   Version 1      |                 |   Version 2      |                 |   Version 3      |
|  JADE + LC4j     |                 | Pure LangChain4j |                 | Google ADK (Java)|
+------------------+                 +------------------+                 +------------------+
         |                                    |                                    |
         +------------------------------------+------------------------------------+
                                              | (Deterministic Service Calls)
                                              v
                                  +-----------+-----------+
                                  |     Service Layer     |  <-- Shared Core Engine
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

### The Three Agentic Paradigms
We have successfully decoupled the database and UI (Phases 1-5) and implemented the core foundation and operational agent framework (Phase 6). 

The project is now splitting into three parallel, Long-Lived Variant Branches to explore and benchmark different AI orchestration frameworks:

1. **Version 1 (`v1/jade-langchain4j`)**: A hybrid approach using traditional JADE (Java Agent Development Framework) for deterministic FIPA-contract-net negotiation, combined with LangChain4j for LLM-powered cognitive tasks (Risk Analysis, Audit processing).
2. **Version 2 (`v2/pure-langchain4j`)**: A complete migration away from JADE, using LangChain4j's built-in memory, tool calling, and AI Services to run the entire supply chain autonomously.
3. **Version 3 (`v3/pure-google-adk`)**: Utilizing the cutting-edge Google ADK for Java to build highly concurrent, schema-driven, and purely native LLM agents.

---

## 📂 Documentation Hub

All detailed technical documentation has been organized under the `doc/` directory:

*   **[ARCHITECTURE.md](doc/ARCHITECTURE.md)**: Deep dive into the architectural layers, package separation, transactional boundaries, and data flow.
*   **[AGENT_ARCHITECTURE.md](doc/AGENT_ARCHITECTURE.md)**: Specifications of all cooperative agents and communication schemas.
*   **[DATABASE_SCHEMA.md](doc/DATABASE_SCHEMA.md)**: Complete database relational schema description, index definitions, and audit trail tables.
*   **[WORKFLOWS.md](doc/WORKFLOWS.md)**: Step-by-step descriptions and Mermaid sequence diagrams for key processes.
*   **[CODING_STANDARDS.md](doc/CODING_STANDARDS.md)**: Development guidelines, naming conventions, and repository-service rules.
*   **[ROADMAP.md](doc/ROADMAP.md)**: The phased transition plan mapping out the legacy-to-agentic transition.

---

## 🛠️ Technology Stack

*   **Runtime Environment**: Java Development Kit (JDK) 21
*   **GUI Library**: Java Swing / AWT
*   **Database**: MySQL 8.0+
*   **Connection Pool & Access**: HikariCP & Native JDBC
*   **Build & Dependency Management**: Apache Maven 3.9+
*   **Logging System**: SLF4J API with Logback implementation
*   **Agentic Frameworks**: JADE 4.6.0, LangChain4j (v0.31.0), Google ADK (v0.1.0-alpha)

---

## 🏗️ Architecture Rules

To guarantee system stability, compliance, and transition safety across all 3 versions, all code changes **must** respect the following boundary guidelines:

> [!IMPORTANT]
> **1. The Gateway Rule:** The UI must ONLY interact with the agents via the `PharmaGateway` interface.
> **2. No JDBC in UI:** Swing ActionListeners must never contain raw SQL, connections, or JDBC statements.
> **3. No JDBC in Agents:** Agents are orchestration entities and must never contact the database directly.
> **4. No Business Logic in Agents:** Agents must delegate to deterministic `pharma.service` classes for validation and data mutations.
> **5. Repositories as Single Source of Database Interaction:** Only class implementations inside `pharma.repository.jdbc` are allowed to write or execute SQL.

---

## 🌿 Git Branching & Lifecycle Strategy

The project utilizes a Long-Lived Variant Branching strategy:

*   **`master`**: Production-ready, stable releases.
*   **`develop`**: The "Core Engine" integration branch. Any updates to shared GUI panels, JDBC repositories, database schemas, or Services go here.
*   **`v1/jade-langchain4j`**: Dedicated branch for the Hybrid JADE + LC4j architecture. Upgraded by merging `develop` into it.
*   **`v2/pure-langchain4j`**: Dedicated branch for the Pure LC4j architecture.
*   **`v3/pure-google-adk`**: Dedicated branch for the Pure Google ADK architecture.

---

## ⚙️ Setup and Installation

### 1. Prerequisites
- **JDK 21** (Ensure `JAVA_HOME` is set up correctly)
- **Maven**
- **MySQL Server 8+**

### 2. Database Initialization
1. Start your local MySQL database service.
2. Create the database and seed it by importing the `database.sql` script:
   ```bash
   mysql -u <username> -p < database.sql
   ```

### 3. Environment Variables
Create a file named `.env` in the root folder of the project (`pharma-ims/`):
```ini
DB_URL=jdbc:mysql://localhost:3306/pharma_ims
DB_USER=root
DB_PASS=yourpassword
```

### 4. Build and Compilation
Clean, resolve dependencies, and compile the application using Maven:
```cmd
mvn clean compile
```

### 5. Running Tests
Verify the integrity of the repository and service layers:
```cmd
mvn test
```

### 6. Executing the Application
Start the desktop application:
```cmd
mvn exec:java
```

---

## 🤖 Current Status: Common Foundation Complete
The system has completed **Step 1: The Common Layer**.
`develop` now contains the fully integrated database schemas, the `PharmaGateway` abstraction, the deterministic AI services, and the core Swing GUI. We are now branching out into `v1/jade-langchain4j` to implement the first agentic architecture!
