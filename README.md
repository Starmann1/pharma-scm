# Manufacturer-centric Pharmaceutical Supply Chain Management System

The **Manufacturer-centric Pharmaceutical Supply Chain Management System** is a comprehensive Java-based desktop application designed for managing operations in the pharmaceutical supply chain. It features a robust **Role-Based Access Control (RBAC)** architecture and a desktop graphical user interface built using **Java Swing**, backed by a **MySQL** database.

## 🚀 Features

### Core Modules
- **Master Data Management**: Manage details of Drugs/Materials, Suppliers, and Warehouse Locations.
- **Purchase Order Module**: Create, edit, and track Purchase Orders (POs) securely.
- **Goods Received Note (GRN)**: Manage incoming stock and map directly to corresponding POs. 
- **Inventory Management**: Track and adjust stock levels across multiple locations, including batch-level monitoring and transfer tracking.
- **Production Management**: Plan and execute production batches using Bill of Materials (BOM).
- **Quality Control (QC) Dashboard**: Comprehensive batch control including batch release, quarantine, and rejection. It also includes comprehensive **Batch Genealogy** tracking.
- **Reporting & Analytics**: Analytical dashboard for viewing and exporting essential business reports.

### Security & System Administration
- **Role-Based Access Control (RBAC)**: Fine-grained permissions and custom profiles for Admins, Managers (Production, Quality, Warehouse, Procurement), and Viewers.
- **System Audit Trail**: Complete logging of all critical actions and CRUD operations, guaranteeing accountability and traceability across modules.

---

## 🛠️ Technology Stack
- **Frontend/UI**: Java (Swing/AWT)
- **Backend**: Java
- **Database**: MySQL 8+
- **Database Driver**: MySQL Connector/J (JDBC)
- **Multi-Agent Framework**: JADE (Java Agent Development Framework) - *Future Implementation*

---

## 📦 Project Structure

```
pharma-ims/
│
├── src/pharma/                # Application source code
│   ├── gui/                   # Swing UI components (Panels, Dialogs)
│   ├── model/                 # Data classes and logical entities
│   ├── service/               # Database operations and business logic
│   └── App.java               # Main application entry point
│
├── bin/                       # Compiled Java .class files (auto-generated)
├── lib/                       # External libraries (e.g., mysql-connector-java.jar)
│
├── compile.bat                # Windows batch script for compiling Java code
├── run.bat                    # Windows batch script for launching the application
│
├── database.sql               # Automated database schema & seeded data script
```

---

## 🤖 Future Implementation: Multi-Agent System (JADE)
As the project evolves towards a fully agentic architecture, it will integrate the **JADE (Java Agent Development Framework)** to power a decentralized multi-agent system. This new architecture will enable various supply chain entities (such as manufacturing units, raw material suppliers, and quality assurance nodes) to act as autonomous intelligent agents. These agents will communicate, negotiate dynamically, and make real-time decisions, shifting the paradigm to a modern, self-optimizing, manufacturer-centric supply chain network.

---

## ⚙️ Setup and Installation

### 1. Database Setup
1. Ensure you have **MySQL Server** installed and running on your local machine.
2. Log in to your MySQL terminal or use a client like *MySQL Workbench* or *phpMyAdmin*.
3. Execute the standard setup script to initialize the tables, permissions, and demo data:
   ```bash
   mysql -u root -p < database.sql
   ```
   *(This script will create the `pharma_ims` database, establish all tables, and insert sample data).*

### 2. Compilation
Ensure you are on a **Windows** environment and have the **Java Development Kit (JDK 8 or higher)** in your system `PATH`.
Double-click `compile.bat` or run it from the command line:
```cmd
compile.bat
```
This script cleanly compiles all source files found in the `src` folder and outputs `.class` files to the `bin/` directory, while appending JARs from `lib/`.

### 3. Running the Application
To start the application, execute the `run.bat` script:
```cmd
run.bat
```
*(This triggers `pharma.App` via the terminal and launches the Swing GUI interface).*


---

